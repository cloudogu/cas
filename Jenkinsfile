#!groovy
@Library(['github.com/cloudogu/ces-build-lib@4.2.0', 'github.com/cloudogu/dogu-build-lib@v3.2.0'])
import com.cloudogu.ces.cesbuildlib.*
import com.cloudogu.ces.dogubuildlib.*

String repositoryOwner = 'cloudogu'
String doguName = "cas"
String branch = "${env.BRANCH_NAME}"
String clientSecret=""

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


                    try {
                        stage('Provision') {
                            // change namespace to prerelease_namespace if in develop-branch
                            if (gitflow.isPreReleaseBranch()) {
                                sh "make prerelease_namespace"
                            }
                            ecoSystem.provision("/dogu", "n2-standard-8", 21)
                        }

                        stage('Start OIDC-Provider') {
                            ecoSystem.vagrant.sshOut """
                                                       cd /dogu/integrationTests/keycloak/ && \
                                                       ./kc-down.sh && \
                                                       ./kc-up.sh -H localhost && \
                                                       ./kc-setup.sh -H ${ecoSystem.externalIP} \
                                                       ./kc-add-user.sh && \
                                                       ./kc-group.sh
                                                     """
                            clientSecret = ecoSystem.vagrant.sshOut """
                                            cd /dogu/integrationTests/keycloak/
                                            cat kc_out.env | \
                                            grep CLIENT_SECRET= kc_out.env | cut -d'=' -f2-
                                            """

                            echo "clientSecret length: ${clientSecret.size()}"
                        }

                        stage('Generate encrypted secret') {
                            withCredentials([usernamePassword(credentialsId: 'ces-mirror-harbor-release', usernameVariable: 'EASY_USER', passwordVariable: 'EASY_API_TOKEN')]) {
                                def pw = "Dropdead80!"
                                sh "${pw} | docker login registry.cloudogu.com -u dschwarzer --password-stdin"
                                def img = docker.image('registry.cloudogu.com/official/base:3.22.0-4')
                                img.pull()
                                img.withRun('-v $PWD:/ws -w /ws', 'tail -f /dev/null') {
                                        sh "doguctl config oidc/client_secret ${clientSecret}"
                                        sh 'doguctl config -e oidc/client_secret $(doguctl config oidc/client_secret)'
                                        // return the stdout of this command from the closure
                                        sh(returnStdout: true, script: 'doguctl config oidc/client_secret').trim()
                                    }

                                // use it outside the container
                                clientSecret = outputfromcontainer
                                echo "clientSecret length: ${clientSecret.size()}"
                                echo "clientSecret: ${clientSecret}"
                            }
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
                                        "client_id": "cas",
                                        "display_name": "MyProvider",
                                        "optional": "true",
                                        "scopes": "openid email profile groups",
                                        "allowed_groups": "testers"
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
                            """, registryConfigEncrypted:"""
                                 "cas" : {
                                    "oidc": {
                                        "client_secret": "${clientSecret}"
                                    }
                                 }
                            """])
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
