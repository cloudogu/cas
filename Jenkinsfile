#!groovy
@Library('github.com/cloudogu/ces-build-lib@a5799c2')
import com.cloudogu.ces.cesbuildlib.*

node() { // No specific label

    properties([
            // Keep only the last 10 build to preserve space
            buildDiscarder(logRotator(numToKeepStr: '10')),
            // Don't run concurrent builds for a branch, because they use the same workspace directory
            disableConcurrentBuilds()
    ])

    String defaultEmailRecipients = env.EMAIL_RECIPIENTS

    catchError {

        def mvnHome = tool 'M3'
        def javaHome = tool 'JDK8'

        Maven mvn = new MavenLocal(this, mvnHome, javaHome)
        Git git = new Git(this)

        stage('Checkout') {
            checkout scm
            git.clean("")
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
                def sonarQube = new SonarQube(this, 'ces-sonar')
                sonarQube.updateAnalysisResultOfPullRequestsToGitHub('sonarqube-gh-token')

                sonarQube.analyzeWith(mvn)
                if (!sonarQube.waitForQualityGateWebhookToBeCalled()) {
                    currentBuild.result ='UNSTABLE'
                }
            }
        }
    }

    // Archive Unit and integration test results, if any
    junit allowEmptyResults: true, testResults: '**/target/failsafe-reports/TEST-*.xml,**/target/surefire-reports/TEST-*.xml'

    mailIfStatusChanged(findEmailRecipients(defaultEmailRecipients))
}

def setupMaven(mvn) {
    if ("master".equals(env.BRANCH_NAME)) {
        mvn.additionalArgs = "-DperformRelease"
        currentBuild.description = mvn.getVersion()
    }
}
