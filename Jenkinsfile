#!groovy
@Library([
  'pipe-build-lib',
  'ces-build-lib',
  'dogu-build-lib'
]) _

String clientSecret = ''
def pipe = new com.cloudogu.sos.pipebuildlib.DoguPipe(this, [
    doguName           : 'cas',
    shellScripts       : ['''
                          resources/startup.sh
                          resources/logging.sh
                          resources/create-sa.sh
                          resources/remove-sa.sh
                          resources/test-password-logging.sh
                          resources/pre-upgrade.sh
                          resources/post-upgrade.sh
                          resources/upgrade-notification.sh
                          resources/util.sh
                          '''
    ],
    doBatsTests        : true,
    runIntegrationTests: true,
    doSonarTests       : true,
    dependencies       : ['nginx', 'cas'],
    additionalDogus    : ['official/postgresql', 'official/usermgt', 'official/ldap'],
    defaultBranch      : "master"
])

pipe.setBuildProperties()
pipe.addDefaultStages()
com.cloudogu.ces.dogubuildlib.EcoSystem ecoSystem = pipe.ecoSystem

def casConfigOverride = { String externalIp ->
    return """
{
  "forgot_password_text": "Contact your admin",
  "legal_urls": {
    "privacy_policy": "https://www.triology.de/",
    "terms_of_service": "https://docs.cloudogu.com/",
    "imprint": "https://cloudogu.com/"
  },
  "oidc": {
    "enabled": "true",
    "discovery_uri": "http://${externalIp}:9000/auth/realms/Test/.well-known/openid-configuration",
    "client_id": "cas",
    "display_name": "cas",
    "optional": "true",
    "scopes": "openid email profile groups",
    "allowed_groups": "testers",
    "attribute_mapping": "email:mail,family_name:surname,given_name:givenName,preferred_username:username,name:displayName,groups:externalGroups"
  }
}
"""
}

String globalConfigOverride = """
{
  "password-policy": {
    "must_contain_capital_letter": "true",
    "must_contain_lower_case_letter": "true",
    "must_contain_digit": "true",
    "must_contain_special_character": "true",
    "min_length": "14"
  }
}
"""

def mergeConfigMapYaml = { String configMapName, String overrideConfig ->
    sh """
       kubectl get configmap ${configMapName} -n ecosystem -o yaml | .bin/yq '
         .data."config.yaml" |= (
           (from_yaml) * ${overrideConfig}
           | to_yaml
         )
       ' | tee ${configMapName}-output.yml | kubectl apply -f -
       """
}

pipe.insertStageAfter('Bats Tests', 'Gradle Build & Test') {
    String gradleDockerImage = 'eclipse-temurin:21-jdk-alpine'
    com.cloudogu.ces.cesbuildlib.Gradle gradlew = new com.cloudogu.ces.cesbuildlib.GradleWrapperInDocker(this, gradleDockerImage)
    dir('app') {
        gradlew "clean build"
        gradlew 'test'
        junit allowEmptyResults: true, testResults: '**/build/test-results/test/TEST-*.xml'
    }
}

pipe.insertStageBefore('Setup', 'Start OIDC-Provider') {
    // launching and setting up keycloak, adding test user, group, scope mapping etc
    ecoSystem.vagrant.sshOut """
                                cd /dogu/integrationTests/keycloak/ && \
                                ./kc-down.sh && \
                                ./kc-up.sh -H ${ecoSystem.externalIP} && \
                                ./kc-setup.sh -H ${ecoSystem.externalIP} && \
                                ./kc-add-user.sh && \
                                ./kc-group.sh
                                """
    // retrieve secret from setup
    clientSecret = ecoSystem.vagrant.sshOut """
                    cd /dogu/integrationTests/keycloak/
                    cat kc_out.env | \
                    grep CLIENT_SECRET= kc_out.env | cut -d'=' -f2-
                    """

    echo "clientSecret length: ${clientSecret.size()}"
}

pipe.overrideStage('Setup') {
    ecoSystem.loginBackend('cesmarvin-setup')
    String casConfig = casConfigOverride(ecoSystem.externalIP)
    ecoSystem.setup([registryConfig: """
        "cas": ${casConfig},
        "_global": ${globalConfigOverride}
    """, registryConfigEncrypted: """
        "cas": {
            "oidc": {
                "client_secret": "${clientSecret}"
            }
        }
    """])
}

pipe.insertStageBefore('MN-Run Integration Tests', 'Setup Configs') {
     echo "Create custom dogu to access OAuth endpoints for the integration tests"
     def podname = sh(returnStdout: true, script: """kubectl get pod -l dogu.name=cas --namespace=ecosystem -o jsonpath='{.items[0].metadata.name}'""")
     String casConfig = casConfigOverride(pipe.multiNodeEcoSystem.externalIP)

     sh "kubectl --namespace=ecosystem cp ./integrationTests/services/ $podname:/etc/cas/services/production/ "

     pipe.multiNodeEcoSystem.waitForDogu("cas")

     sh "make install-yq"
     mergeConfigMapYaml('cas-config', casConfig)
     sh """kubectl patch blueprint blueprint-ces-module -n ecosystem --type merge -p '{"spec":{"stopped":true}}'"""

     pipe.multiNodeEcoSystem.waitForDogu("cas")

     mergeConfigMapYaml('global-config', globalConfigOverride)

     // This may be extracted to a dogu build lib function
     def globalConfigLastUpdateTime = sh(returnStdout: true, script: """kubectl get configmap -n ecosystem --show-managed-fields global-config -o json | jq -r '.metadata.managedFields[].time' | sort | tail -1""").trim()
     def casDoguStartedAt = sh(returnStdout: true, script: """kubectl get dogu -n ecosystem cas -o json | jq -r '.status.startedAt'""").trim()

     while (casDoguStartedAt < globalConfigLastUpdateTime) {
         echo "Waiting for CAS to restart and pick up the new global config..."
         sleep time: 10, unit: 'SECONDS'
         casDoguStartedAt = sh(returnStdout: true, script: """kubectl get dogu -n ecosystem cas -o json | jq -r '.status.startedAt'""").trim()
         echo "CAS started at: ${casDoguStartedAt}"
     }


     pipe.multiNodeEcoSystem.waitForDogu("cas")
}

pipe.overrideStage('Integration Tests') {
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

pipe.run()
