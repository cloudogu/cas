#!groovy
@Library('github.com/cloudogu/ces-build-lib@develop')
import com.cloudogu.ces.cesbuildlib.*

node() { // No specific label

    properties([
            // Keep only the last 10 build to preserve space
            buildDiscarder(logRotator(numToKeepStr: '10')),
            // Don't run concurrent builds for a branch, because they use the same workspace directory
            disableConcurrentBuilds()
    ])

    String emailRecipients = env.EMAIL_RECIPIENTS

    catchError {

        def mvnHome = tool 'M3'
        def javaHome = tool 'JDK8'
        def sonarQube = 'ces-sonar'

        Maven mvn = new Maven(this, mvnHome, javaHome)
        Git git = new Git(this)

        // TODO refactor this in an object-oriented way and move to build-lib
        if ("master".equals(env.BRANCH_NAME)) {
            mvn.additionalArgs = "-DperformRelease"
            currentBuild.description = mvn.getVersion()
        } else if (!"develop".equals(env.BRANCH_NAME)) {
            // run SQ analysis in specific project for feature, hotfix, etc.
            mvn.additionalArgs = "-Dsonar.branch=" + script.env.BRANCH_NAME
        }

        stage('Checkout') {
            checkout scm
            git.clean("")
        }

        stage('Build') {
            mvn 'clean install -DskipTests'
            archive '**/target/*.jar,**/target/*.zip'
        }

        stage('Unit Test') {
            mvn 'test'
        }

        stage('SonarQube') {
            withSonarQubeEnv(sonarQube) {
                mvn "$SONAR_MAVEN_GOAL -Dsonar.host.url=$SONAR_HOST_URL " +
                        //exclude generated code in target folder
                        "-Dsonar.exclusions=target/**"
            }
        }
    }

    // Archive Unit and integration test results, if any
    junit allowEmptyResults: true, testResults: '**/target/failsafe-reports/TEST-*.xml,**/target/surefire-reports/TEST-*.xml'

    // email on fail
    step([$class: 'Mailer', notifyEveryUnstableBuild: true, recipients: emailRecipients, sendToIndividuals: true])
}