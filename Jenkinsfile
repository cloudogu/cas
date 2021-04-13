#!groovy
@Library(['github.com/cloudogu/ces-build-lib@bdde6ddb', 'github.com/cloudogu/dogu-build-lib@v1.1.1'])
import com.cloudogu.ces.cesbuildlib.*
import com.cloudogu.ces.dogubuildlib.*

node() { // No specific label
    Git git = new Git(this, "cesmarvin")
    git.committerName = 'cesmarvin'
    git.committerEmail = 'cesmarvin@cloudogu.com'
    GitFlow gitflow = new GitFlow(this, git)
    GitHub github = new GitHub(this, git)
    Changelog changelog = new Changelog(this)
    EcoSystem ecoSystem = new EcoSystem(this, "gcloud-ces-operations-internal-packer", "jenkins-gcloud-ces-operations-internal")

    timestamps {
        properties([
                // Keep only the last 10 build to preserve space
                buildDiscarder(logRotator(numToKeepStr: '10')),
                // Don't run concurrent builds for a branch, because they use the same workspace directory
                disableConcurrentBuilds()
        ])

        String defaultEmailRecipients = env.EMAIL_RECIPIENTS

        catchError {
            def gradleDockerImage = 'openjdk11:alpine-slim'
            Gradle gradlew = new GradleWrapperInDocker(this, gradleDockerImage)

            stage('Checkout') {
                checkout scm
                git.clean("")
            }

            stage('Lint') {
                lintDockerfile()
            }

            stage('Shellcheck') {
                // TODO: Change this to shellCheck("./resources") as soon as https://github.com/cloudogu/dogu-build-lib/issues/8 is solved
                shellCheck("./resources/startup.sh ./resources/logging.sh")
            }

            dir('app') {
                stage('Build') {
                    gradlew "clean build"
                }

                stage('Unit Test') {
                    gradlew 'test'
                }
            }

            stage('SonarQube') {
                def scannerHome = tool name: 'sonar-scanner', type: 'hudson.plugins.sonar.SonarRunnerInstallation'
                withSonarQubeEnv {
                    sh "git config 'remote.origin.fetch' '+refs/heads/*:refs/remotes/origin/*'"
                    gitWithCredentials("fetch --all")

                    if (branch == "master") {
                        echo "This branch has been detected as the master branch."
                        sh "${scannerHome}/bin/sonar-scanner -Dsonar.branch.name=${env.BRANCH_NAME}"
                    } else if (branch == "develop") {
                        echo "This branch has been detected as the develop branch."
                        sh "${scannerHome}/bin/sonar-scanner -Dsonar.branch.name=${env.BRANCH_NAME} -Dsonar.branch.target=master"
                    } else if (env.CHANGE_TARGET) {
                        echo "This branch has been detected as a pull request."
                        sh "${scannerHome}/bin/sonar-scanner -Dsonar.branch.name=${env.CHANGE_BRANCH}-PR${env.CHANGE_ID} -Dsonar.branch.target=${env.CHANGE_TARGET}"
                    } else if (branch.startsWith("feature/")) {
                        echo "This branch has been detected as a feature branch."
                        sh "${scannerHome}/bin/sonar-scanner -Dsonar.branch.name=${env.BRANCH_NAME} -Dsonar.branch.target=develop"
                    } else {
                        echo "This branch has been detected as a miscellaneous branch."
                        sh "${scannerHome}/bin/sonar-scanner -Dsonar.branch.name=${env.BRANCH_NAME} -Dsonar.branch.target=develop"
                    }
                }
                timeout(time: 2, unit: 'MINUTES') { // Needed when there is no webhook for example
                    def qGate = waitForQualityGate()
                    if (qGate.status != 'OK') {
                        unstable("Pipeline unstable due to SonarQube quality gate failure")
                    }
                }
            }

            try {

                stage('Provision') {
                    ecoSystem.provision("/dogu");
                }

                stage('Setup') {
                    ecoSystem.loginBackend('cesmarvin-setup')
                    ecoSystem.setup()
                }

                stage('Wait for dependencies') {
                    timeout(15) {
                        ecoSystem.waitForDogu("nginx")
                    }
                }

                stage('Build') {
                    ecoSystem.build("/dogu")
                }

                stage('Verify') {
                    ecoSystem.verify("/dogu")
                }

                if (gitflow.isReleaseBranch()) {
                    String releaseVersion = git.getSimpleBranchName();

                    stage('Finish Release') {
                        gitflow.finishRelease(releaseVersion)
                    }

                    stage('Push Dogu to registry') {
                        ecoSystem.push("/dogu")
                    }

                    stage('Add Github-Release') {
                        github.createReleaseWithChangelog(releaseVersion, changelog)
                    }
                }
            } finally {
                stage('Clean') {
                    ecoSystem.destroy()
                }
            }


        }

        // Archive Unit and integration test results, if any
        junit allowEmptyResults: true, testResults: '**/target/failsafe-reports/TEST-*.xml,**/target/surefire-reports/TEST-*.xml'

        mailIfStatusChanged(findEmailRecipients(defaultEmailRecipients))
    }
}