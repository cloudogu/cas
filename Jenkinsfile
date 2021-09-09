#!groovy
@Library(['github.com/cloudogu/ces-build-lib@1.47.0', 'github.com/cloudogu/dogu-build-lib@v1.4.0'])
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
String defaultEmailRecipients = env.EMAIL_RECIPIENTS

parallel(
        "source code": {
            node('docker') {
                timestamps {
                    project = "github.com/${repositoryOwner}/${doguName}"
                    String gradleDockerImage = 'openjdk:11.0.10-jdk'
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

                        stage('SonarQube') {
                            withSonarQubeEnv {
                                sh "git config 'remote.origin.fetch' '+refs/heads/*:refs/remotes/origin/*'"
                                gitWithCredentials("fetch --all")
                                String parameters = ' -Dsonar.projectKey=cas'
                                if (branch == productionReleaseBranch) {
                                    echo "This branch has been detected as the " + productionReleaseBranch + " branch."
                                    parameters += " -Dsonar.branch.name=${env.BRANCH_NAME}"
                                } else if (branch == "develop") {
                                    echo "This branch has been detected as the develop branch."
                                    parameters += " -Dsonar.branch.name=${env.BRANCH_NAME} -Dsonar.branch.target=" + productionReleaseBranch
                                } else if (env.CHANGE_TARGET) {
                                    echo "This branch has been detected as a pull request."
                                    parameters += " -Dsonar.branch.name=${env.CHANGE_BRANCH}-PR${env.CHANGE_ID} -Dsonar.branch.target=${env.CHANGE_TARGET}"
                                } else if (branch.startsWith("feature/")) {
                                    echo "This branch has been detected as a feature branch."
                                    parameters += " -Dsonar.branch.name=${env.BRANCH_NAME} -Dsonar.branch.target=develop"
                                } else {
                                    echo "This branch has been detected as a miscellaneous branch."
                                    parameters += " -Dsonar.branch.name=${env.BRANCH_NAME} -Dsonar.branch.target=develop"
                                }
                                gradlew "sonarqube ${parameters}"
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
            }
        },
        "dogu-integration": {
            node('vagrant') {
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
                                    booleanParam(defaultValue: true, description: 'Enables cypress to take screenshots of failing integration tests.', name: 'EnableScreenshotRecording')
                            ])
                    ])

                    stage('Checkout') {
                        checkout scm
                    }

                    stage('Lint') {
                        lintDockerfile()
                    }

                    stage('Shellcheck') {
                        // TODO: Change this to shellCheck("./resources") as soon as https://github.com/cloudogu/dogu-build-lib/issues/8 is solved
                        shellCheck("./resources/startup.sh ./resources/logging.sh ./resources/util.sh ./resources/create-sa.sh ./resources/remove-sa.sh")
                    }

                    try {
                        stage('Provision') {
                            ecoSystem.provision("/dogu")
                        }

                        stage('Start OIDC-Provider') {
                            // template realm file
                            ecoSystem.vagrant.sshOut "sed 's/192.168.56.2/${ecoSystem.externalIP}/g' -i /dogu/integrationTests/keycloak-realm/realm-cloudogu.json"
                            sh "echo \"Starting Keycloak as OIDC provider on ${ecoSystem.externalIP}:9000\""
                            // start keycloak
                            ecoSystem.vagrant.sshOut 'sudo docker run -d --name kc -e KEYCLOAK_USER=admin -e KEYCLOAK_PASSWORD=admin -p 9000:8080 -e KEYCLOAK_IMPORT=\'/realm-cloudogu.json -Dkeycloak.profile.feature.upload_scripts=enabled\' -v  /dogu/integrationTests/keycloak-realm/realm-cloudogu.json:/realm-cloudogu.json jboss/keycloak:15.0.2'
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
                                    "service_accounts": {
                                        "inttest": "fda8e031d07de22bf14e552ab12be4bc70b94a1fb61cb7605833765cb74f2dea"
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
                                }
                            """, registryConfigEncrypted:'''
                                 "cas" : {
                                    "oidc": {
                                        "client_secret": "c21a7690-1ca3-4cf9-bef3-22f37faf5144"
                                    }
                                 }
                            '''])
                        }

                        stage('Build') {
                            ecoSystem.build("/dogu")
                        }

                        stage('Verify') {
                            ecoSystem.verify("/dogu")
                        }

                        stage('Wait for dependencies') {
                            timeout(15) {
                                ecoSystem.waitForDogu("nginx")
                                ecoSystem.waitForDogu("cas")
                            }
                        }

                        stage('Integration Tests') {
                            echo "Create custom dogu to access OAuth endpoints for the integration tests"
                            ecoSystem.vagrant.sshOut "etcdctl mkdir /dogu/inttest"
                            ecoSystem.vagrant.sshOut '''etcdctl set /dogu/inttest/0.0.1 '{\\"Name\\":\\"official/inttest\\",\\"Dependencies\\":[\\"cas\\"]}' '''
                            ecoSystem.vagrant.sshOut "etcdctl set /dogu/inttest/current \"0.0.1\""

                            ecoSystem.runCypressIntegrationTests([
                                    cypressImage     : "cypress/included:7.4.0",
                                    enableVideo      : params.EnableVideoRecording,
                                    enableScreenshots: params.EnableScreenshotRecording])
                        }

                        if (params.TestDoguUpgrade != null && params.TestDoguUpgrade) {
                            stage('Upgrade dogu') {
                                // Remove new dogu that has been built and tested above
                                ecoSystem.purgeDogu(doguName)

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
                                        cypressImage     : "cypress/included:7.4.0",
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