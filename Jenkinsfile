#!groovy
@Library(['github.com/cloudogu/ces-build-lib@4.2.0', 'github.com/cloudogu/dogu-build-lib@v3.2.0'])
import com.cloudogu.ces.cesbuildlib.*
import com.cloudogu.ces.dogubuildlib.*

String repositoryOwner = 'cloudogu'
String doguName = "cas"
String branch = "${env.BRANCH_NAME}"

EcoSystem ecoSystem = new EcoSystem(this, "gcloud-ces-operations-internal-packer", "jenkins-gcloud-ces-operations-internal")

Git git = new Git(this, "cesmarvin")
git.committerName = 'cesmarvin'
git.committerEmail = 'cesmarvin@cloudogu.com'
GitFlow gitflow = new GitFlow(this, git)
GitHub github = new GitHub(this, git)
Changelog changelog = new Changelog(this)
String productionReleaseBranch = "master"
String developmentBranch = "develop"
currentBranch = "${env.BRANCH_NAME}"
String defaultEmailRecipients = env.EMAIL_RECIPIENTS

parallel(
        "source code": {
            node('sos-stable') {
                timestamps {
                    project = "github.com/${repositoryOwner}/${doguName}"
                    String gradleDockerImage = 'eclipse-temurin:21-jdk-alpine'
                    Gradle gradlew = new GradleWrapperInDocker(this, gradleDockerImage)

                    stage('Checkout') {
                        checkout scm
                    }

                    dir('app') {
                        stage('Build') {
                            gradlew "clean build"
                        }

                        stage('Unit Test') {
                            gradlew 'test'
                            junit allowEmptyResults: true, testResults: '**/build/test-results/test/TEST-*.xml'
                        }
                    }

                    stage('SonarQube') {
                        def scannerHome = tool name: 'sonar-scanner', type: 'hudson.plugins.sonar.SonarRunnerInstallation'
                        withSonarQubeEnv {
                            sh "git config 'remote.origin.fetch' '+refs/heads/*:refs/remotes/origin/*'"
                            gitWithCredentials("fetch --all")

                            if (currentBranch == productionReleaseBranch) {
                                echo "This branch has been detected as the production branch."
                                sh "${scannerHome}/bin/sonar-scanner -Dsonar.branch.name=${env.BRANCH_NAME}"
                            } else if (currentBranch == developmentBranch) {
                                echo "This branch has been detected as the development branch."
                                sh "${scannerHome}/bin/sonar-scanner -Dsonar.branch.name=${env.BRANCH_NAME}"
                            } else if (env.CHANGE_TARGET) {
                                echo "This branch has been detected as a pull request."
                                sh "${scannerHome}/bin/sonar-scanner -Dsonar.pullrequest.key=${env.CHANGE_ID} -Dsonar.pullrequest.branch=${env.CHANGE_BRANCH} -Dsonar.pullrequest.base=${developmentBranch}"
                            } else if (currentBranch.startsWith("feature/")) {
                                echo "This branch has been detected as a feature branch."
                                sh "${scannerHome}/bin/sonar-scanner -Dsonar.branch.name=${env.BRANCH_NAME}"
                            } else {
                                echo "This branch has been detected as a miscellaneous branch."
                                sh "${scannerHome}/bin/sonar-scanner -Dsonar.branch.name=${env.BRANCH_NAME} "
                            }
                        }
                        timeout(time: 2, unit: 'MINUTES') { // Needed when there is no webhook for example
                            def qGate = waitForQualityGate()
                            if (qGate.status != 'OK') {
                                unstable("Pipeline unstable due to SonarQube quality gate failure")
                            }
                        }
                    }
                }
            }
        },
        "dogu-integration": {
            node('sos-stable') {
                sh 'echo testing dogu integration with ces'
                timestamps {
                    properties([
                            // Keep only the last x builds to preserve space
                            buildDiscarder(logRotator(numToKeepStr: '10')),
                            // Don't run concurrent builds for a branch, because they use the same workspace directory
                            disableConcurrentBuilds(),
                            // Parameter to activate dogu upgrade test on demand
                            parameters([
                                    booleanParam(defaultValue: false, description: 'Test dogu upgrade from latest release or optionally from defined version below', name: 'TestDoguUpgrade'),
                                    string(defaultValue: '', description: 'Old Dogu version for the upgrade test (optional; e.g. 3.23.0-1)', name: 'OldDoguVersionForUpgradeTest'),
                                    booleanParam(defaultValue: true, description: 'Enables cypress to record video of the integration tests.', name: 'EnableVideoRecording'),
                                    booleanParam(defaultValue: true, description: 'Enables cypress to take screenshots of failing integration tests.', name: 'EnableScreenshotRecording'),
                                    choice(name: 'TrivySeverityLevels', choices: [TrivySeverityLevel.CRITICAL, TrivySeverityLevel.HIGH_AND_ABOVE, TrivySeverityLevel.MEDIUM_AND_ABOVE, TrivySeverityLevel.ALL], description: 'The levels to scan with trivy'),
                                    choice(name: 'TrivyStrategy', choices: [TrivyScanStrategy.UNSTABLE, TrivyScanStrategy.FAIL, TrivyScanStrategy.IGNORE], description: 'Define whether the build should be unstable, fail or whether the error should be ignored if any vulnerability was found.'),
                            ])
                    ])

                    stage('Checkout') {
                        checkout scm
                    }

                    stage('Lint') {
                        Dockerfile dockerfile = new Dockerfile(this)
                        dockerfile.lint()
                    }

                    stage('Shellcheck') {
                        // TODO: Change this to shellCheck("./resources") as soon as https://github.com/cloudogu/dogu-build-lib/issues/8 is solved
                        shellCheck("./resources/startup.sh ./resources/logging.sh ./resources/util.sh ./resources/create-sa.sh ./resources/remove-sa.sh")
                    }

                    stage('Bats Tests') {
                        Bats bats = new Bats(this, docker)
                        bats.checkAndExecuteTests()
                    }

                    try {
                        stage('Provision') {
                            // change namespace to prerelease_namespace if in develop-branch
                            if (gitflow.isPreReleaseBranch()) {
                                sh "make prerelease_namespace"
                            }
                            ecoSystem.provision("/dogu", "n1-standard-4", 15)
                        }

                        stage('Start OIDC-Provider') {
                            // template realm file
                            ecoSystem.vagrant.sshOut "sed 's/192.168.56.2/${ecoSystem.externalIP}/g' -i /dogu/integrationTests/keycloak-realm/realm-cloudogu.json"
                            sh "echo \"Starting Keycloak as OIDC provider on ${ecoSystem.externalIP}:9000\""
                            // start keycloak
                            ecoSystem.vagrant.sshOut 'sudo docker run -d --name kc -e KEYCLOAK_USER=admin -e KEYCLOAK_PASSWORD=admin -p 9000:8080 -e KEYCLOAK_IMPORT=\'/realm-cloudogu.json -Dkeycloak.profile.feature.upload_scripts=enabled\' -v  /dogu/integrationTests/keycloak-realm/realm-cloudogu.json:/realm-cloudogu.json quay.io/keycloak/keycloak:15.0.2'
                        }

                        stage('Setup') {
                            ecoSystem.loginBackend('cesmarvin-setup')
                            ecoSystem.setup([registryConfig:"""
                                "cas": {
                                    "forgot_password_text": "Contact your admin",
                                    "legal_urls": {
                                        "privacy_policy": "https://www.triology.de/",
                                        "terms_of_service": "https://docs.cloudogu.com/",
                                        "imprint": "https://cloudogu.com/"
                                    },
                                    "oidc": {
                                        "enabled": "true",
                                        "discovery_uri": "http://${ecoSystem.externalIP}:9000/auth/realms/Cloudogu/.well-known/openid-configuration",
                                        "client_id": "casClient",
                                        "display_name": "MyProvider",
                                        "optional": "true",
                                        "scopes": "openid email profile groups",
                                        "attribute_mapping": "email:mail,family_name:surname,given_name:givenName,preferred_username:username,name:displayName"
                                    }
                                },
                                "_global": {
                                    "password-policy": {
                                        "must_contain_capital_letter": "true",
                                        "must_contain_lower_case_letter": "true",
                                        "must_contain_digit": "true",
                                        "must_contain_special_character": "true",
                                        "min_length": "14"
                                    }
                                }
                            """, registryConfigEncrypted:'''
                                 "cas" : {
                                    "oidc": {
                                        "client_secret": "c21a7690-1ca3-4cf9-bef3-22f37faf5144"
                                    }
                                 }
                            '''])
                        }

                        stage('Build dogu') {
                            // purge cas from official namespace to prevent conflicts while building prerelease_official/cas
                            if (gitflow.isPreReleaseBranch()) {
                                ecoSystem.purgeDogu("cas", "--keep-config --keep-volumes --keep-service-accounts --keep-logs")
                            }
                            // force post-upgrade from cas version 7.0.8-4 to migrate existing services from defaultSetupConfig
                            ecoSystem.vagrant.sshOut "sed 's/7.0.8-4/7.0.8-5/g' -i /dogu/dogu.json"
                            ecoSystem.build("/dogu")
                        }

                        stage('Trivy scan') {
                            ecoSystem.copyDoguImageToJenkinsWorker("/dogu")
                            Trivy trivy = new Trivy(this)
                            trivy.scanDogu(".", params.TrivySeverityLevels, params.TrivyStrategy)
                            trivy.saveFormattedTrivyReport(TrivyScanFormat.TABLE)
                            trivy.saveFormattedTrivyReport(TrivyScanFormat.JSON)
                            trivy.saveFormattedTrivyReport(TrivyScanFormat.HTML)
                        }

                        stage('Verify') {
                            ecoSystem.verify("/dogu")
                        }

                        stage('Wait for dependencies') {
                            timeout(15) {
                                ecoSystem.waitForDogu("nginx")
                                ecoSystem.waitForDogu("cas")

                                // The http health check is not yet implemented, so this is the manual workaround.
                                waitForCondition(20, 10) {
                                    def status = sh(
                                        script: """
                                        wget --spider -S --tries=1 --timeout=10 --no-check-certificate http://${ecoSystem.externalIP}/cas/actuator/health 2>&1 \
                                        | awk '/^  HTTP/{print \$2}' | tail -1
                                        """,
                                        returnStdout: true
                                    ).trim()

                                    echo "HTTP status: ${status}"
                                    return status == "200"
                                }
                            }
                        }

                        stage('Integration Tests') {
                            echo "Create custom dogu to access OAuth endpoints for the integration tests"
                            ecoSystem.vagrant.ssh "sudo docker cp /dogu/integrationTests/services/ cas:/etc/cas/services/production/"
                            ecoSystem.vagrant.sshOut "sudo docker exec cas ls /etc/cas/services/production"
                            // Wait for Service-Watch start delay (see: cas.service-registry.schedule.start-delay)
                            sleep time: 30, unit: 'SECONDS'

                           ecoSystem.runCypressIntegrationTests([
                                    cypressImage     : "cypress/included:13.13.2",
                                    enableVideo      : params.EnableVideoRecording,
                                   enableScreenshots: params.EnableScreenshotRecording])
                           // run special non-encrypted password test
                           echo "Run unencrypted password test script"
                           ecoSystem.vagrant.sshOut 'chmod +x /dogu/resources/test-password-logging.sh'
                           def testreport = ecoSystem.vagrant.sshOut "sudo /dogu/resources/test-password-logging.sh ${ecoSystem.externalIP}"
                           echo "${testreport}"
                        }

                        if (params.TestDoguUpgrade != null && params.TestDoguUpgrade) {
                            stage('Upgrade dogu') {
                                // Remove new dogu that has been built and tested above
                                ecoSystem.purgeDogu("--keep-config --keep-service-accounts --keep-volumes " + doguName)

                                if (params.OldDoguVersionForUpgradeTest != '' && !params.OldDoguVersionForUpgradeTest.contains('v')) {
                                    println "Installing user defined version of dogu: " + params.OldDoguVersionForUpgradeTest
                                    ecoSystem.installDogu("official/" + doguName + " " + params.OldDoguVersionForUpgradeTest)
                                } else {
                                    println "Installing latest released version of dogu..."
                                    ecoSystem.installDogu("official/" + doguName)
                                }
                                ecoSystem.startDogu(doguName)
                                ecoSystem.waitForDogu(doguName)
                                ecoSystem.upgradeDogu(ecoSystem)

                                // Wait for upgraded dogu to get healthy
                                ecoSystem.waitForDogu(doguName)
                            }

                            stage('Integration Tests - After Upgrade') {
                                ecoSystem.runCypressIntegrationTests([
                                        cypressImage     : "cypress/included:13.13.2",
                                        enableVideo      : params.EnableVideoRecording,
                                        enableScreenshots: params.EnableScreenshotRecording])
                            }
                        }

                        if (gitflow.isReleaseBranch()) {
                            String releaseVersion = git.getSimpleBranchName()

                            stage('Finish Release') {
                                gitflow.finishRelease(releaseVersion, productionReleaseBranch)
                            }

                            stage('Push Dogu to registry') {
                                ecoSystem.push("/dogu")
                            }

                            stage('Add Github-Release') {
                                github.createReleaseWithChangelog(releaseVersion, changelog, productionReleaseBranch)
                            }
                        } else if (gitflow.isPreReleaseBranch()) {
                            // push to registry in prerelease_namespace
                            stage('Push Prerelease Dogu to registry') {
                                ecoSystem.pushPreRelease("/dogu")
                            }
                        }
                    } finally {
                        stage('Clean') {
                            ecoSystem.destroy()
                        }
                    }

                    mailIfStatusChanged(findEmailRecipients(defaultEmailRecipients))
                }
            }

        }
)

void gitWithCredentials(String command) {
    withCredentials([usernamePassword(credentialsId: 'cesmarvin', usernameVariable: 'GIT_AUTH_USR', passwordVariable: 'GIT_AUTH_PSW')]) {
        sh(
                script: "git -c credential.helper=\"!f() { echo username='\$GIT_AUTH_USR'; echo password='\$GIT_AUTH_PSW'; }; f\" " + command,
                returnStdout: true
        )
    }
}

def waitForCondition(maxRetries = 10, sleepSeconds = 5, checkClosure) {
    def retries = 0
    while (retries < maxRetries) {
        if (checkClosure()) {
            echo "Condition met after ${retries} attempt(s)"
            return true
        }
        echo "Condition not met, retrying... (${retries + 1}/${maxRetries})"
        sleep time: sleepSeconds, unit: 'SECONDS'
        retries++
    }
    error "Condition not met after ${maxRetries} attempts"
}
