#!groovy
@Library(['github.com/cloudogu/ces-build-lib@1.44.2', 'github.com/cloudogu/dogu-build-lib@v1.1.0'])
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
            def mvnDockerName = '3.6-openjdk-8'
            Maven mvn = new MavenInDocker(this, mvnDockerName)

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
                    setupMaven(mvn)
                    mvn 'clean install -DskipTests'
                    archiveArtifacts '**/target/*.jar,**/target/*.zip'
                }

                stage('Unit Test') {
                    mvn 'test'
                }

                stage('SonarQube') {
                    def sonarQube = new SonarQube(this, [sonarQubeEnv: 'ces-sonar'])
                    sonarQube.analyzeWith(mvn)
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

                    stage ('Add Github-Release'){
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

def setupMaven(mvn) {
    if ("master".equals(env.BRANCH_NAME)) {
        mvn.additionalArgs = "-DperformRelease"
        currentBuild.description = mvn.getVersion()
    }
}
