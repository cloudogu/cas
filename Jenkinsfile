#!groovy
@Library([
  'pipe-build-lib',
  'ces-build-lib@feature/164-fix-mavenindocker-registry-url-when-passing-credentialsid',
  'dogu-build-lib'
]) _

import com.cloudogu.ces.cesbuildlib.K3d
import com.cloudogu.ces.cesbuildlib.Makefile
import com.cloudogu.ces.cesbuildlib.Maven
import com.cloudogu.ces.cesbuildlib.MavenInDocker



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

def componentRegistry = "registry.cloudogu.com"
def componentRegistryNamespace = "k8s"
def componentChartTargetDir = "target/k8s/helm"
def componentBuildImageRepository = "registry.cloudogu.com/official/cas"
def componentReleaseName = "cas"
def helmTestReleaseName = "lop-idp-${componentReleaseName}"
def goVersion = "1.26.0"

pipe.setBuildProperties()
pipe.addDefaultStages()
com.cloudogu.ces.dogubuildlib.EcoSystem ecoSystem = pipe.ecoSystem

def runMakeInGoContainer = { target ->
    new com.cloudogu.ces.cesbuildlib.Docker(this)
        .image("golang:${goVersion}")
        .mountJenkinsUser()
        .inside("--volume ${WORKSPACE}:/workdir -w /workdir") {
            sh "make ${target}"
        }
}

String getDoguVersion(boolean withVersionPrefix) {
    def doguJson = this.readJSON file: 'dogu.json'
    String version = doguJson.Version

    if (withVersionPrefix) {
        return "v" + version
    } else {
        return version
    }
}

def componentStages = { group ->
    group.stage('Component Checkout') {
        checkout scm
    }

    group.stage('Component Build') {
        runMakeInGoContainer("install-yq")
        docker.withRegistry('https://registry.cloudogu.com/', 'cesmarvin-setup') {
            sh "make docker-build"
        }
    }

    group.stage('Component Lint') {
        runMakeInGoContainer("helm-lint")
    }

    group.stage('Component Smoke Test (k3d)') {
        K3d k3d = new K3d(this, "${WORKSPACE}", "${WORKSPACE}/k3d", env.PATH)
        String imageTag = getDoguVersion(false)

        try {
            echo "[Component k3d] Start cluster"
            k3d.startK3d()
            k3d.yqEvalYamlFile("k3d_values.yaml", ".defaultConfig.env.enableFqdnApplier = true")
            k3d.setup()

            echo "[Component k3d] Prepare prerequisites"
            k3d.kubectl("delete secret ldap-cas-sa || true")
            // Steal username and password for ldap from cas dogu to use in component.
            // Once we have completely transitioned to the lop-idp component in ecosystem-core,
            // this will come from the ldap component and we don't need it anymore.
            String casSecretRaw = k3d.kubectl("get secret cas-config -o jsonpath='{.data.config\\.yaml}'", true)
            String casSecretYaml = new String(casSecretRaw.decodeBase64())

            def ldapUsername = ""
            def ldapPassword = ""

            k3d.doInYQContainer {
               ldapUsername = sh(
                    script: "echo '${casSecretYaml}' | yq '.sa-ldap.username'",
                    returnStdout: true
               ).trim()
               ldapPassword = sh(
                    script: "echo '${casSecretYaml}' | yq '.sa-ldap.password'",
                    returnStdout: true
               ).trim()

               echo "Read ldap secret from cas config..."
            }
            k3d.kubectl("create secret generic ldap-cas-sa --from-literal=username='${ldapUsername}' --from-literal=password='${ldapPassword}'")

            echo "[Component k3d] Generate helm chart"
            runMakeInGoContainer("helm-generate")

            echo "[Component k3d] Retag image for local smoke test"
            sh "docker tag ${componentBuildImageRepository}:${imageTag} local-smoke/cas:${imageTag}"

            echo "[Component k3d] Import previously built image"
            retry(3) {
                sh "sudo ${WORKSPACE}/k3d/.k3d/bin/k3d image import local-smoke/cas:${imageTag} -c ${k3d.registryName}"
                // check if the image is actually there
                sh "sudo docker exec k3d-${k3d.registryName}-server-0 ctr -n k8s.io images list -q | grep -F 'cas:${imageTag}'"
            }

            echo "[Component k3d] Deploy component via helm"
            k3d.helm("upgrade --install ${helmTestReleaseName} ${componentChartTargetDir}"
            + " --namespace default --set nameOverride=${helmTestReleaseName}"
            + " --set fullnameOverride=${helmTestReleaseName}"
            + " --set containers.cas.image.registry=''"
            + " --set containers.cas.image.repository=local-smoke/cas"
            + " --set containers.cas.image.tag=${imageTag}"
            + " --set containers.cas.imagePullPolicy=Never"
            // use ldap dogu instead of component service
            + " --set configuration.normal.ldap.host=ldap"
            // disable ingress to avoid conflicts with the cas dogu
            + " --set ingress.enabled=false"
            + " --wait --timeout 5m")

            echo "[Component k3d] Verify component startup"
            k3d.kubectl("rollout status deployment/${helmTestReleaseName} --timeout=300s")
            k3d.kubectl("wait --for=condition=ready pod -l app.kubernetes.io/instance=${helmTestReleaseName} --timeout=300s")
        } catch (Exception e) {
            k3d.collectAndArchiveLogs()
            throw e as java.lang.Throwable
        } finally {
            k3d.deleteK3d()
        }
    }

    if (pipe.gitflow.isReleaseBranch()) {
        group.stage('Push Component Chart to Harbor') {
            sh "make helm-package"

            def componentChartFile = sh(returnStdout: true, script: "ls -1t ${componentChartTargetDir}/*.tgz 2>/dev/null | head -n 1").trim()
            if (!componentChartFile) {
                error("No packaged component chart found in ${componentChartTargetDir}")
            }

            withCredentials([usernamePassword(credentialsId: 'harborhelmchartpush', usernameVariable: 'HARBOR_USERNAME', passwordVariable: 'HARBOR_PASSWORD')]) {
                sh ".bin/helm registry login ${componentRegistry} --username '${HARBOR_USERNAME}' --password '${HARBOR_PASSWORD}'"
                sh ".bin/helm push ${componentChartFile} oci://${componentRegistry}/${componentRegistryNamespace}/"
                sh ".bin/helm registry logout ${componentRegistry}"
            }
        }
    }
}

pipe.addStageGroup('component', pipe.agentMultinode, componentStages)

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


pipe.insertStageBefore('MN-Run Integration Tests', 'Setup Configs and Keycloak') {
    echo "Setup Keycloak as OIDC provider for integration tests"
    def currentContext = sh(returnStdout: true, script: "kubectl config current-context").trim()
    def namespace = "ecosystem"
    def random_suffix = sh(returnStdout: true, script: "head /dev/urandom | tr -dc a-z0-9 | head -c 8").trim()
    def dirName = "keycloak-repo-${random_suffix}"

    withCredentials([usernamePassword(credentialsId: 'SCM-Manager', usernameVariable: 'AUTH_USR', passwordVariable: 'AUTH_PS')]) {
        sh(
                script: "git clone --branch feature/add-namespace-parameter --single-branch --depth 1 https://\"${AUTH_USR}\":\"${AUTH_PS}\"@ecosystem.cloudogu.com/scm/repo/platform/account.cloudogu.com ${dirName}",
                returnStdout: true
        )
    }

    dir(dirName) {

        Maven mvn = new MavenInDocker(this, "3.9.9-eclipse-temurin-17")
        mvn.enableDockerHost = true
        mvn "clean verify -Dmaven.test.skip=true -Ddocker.imageName=account.cloudogu.com/cloudogu-keycloak io.fabric8:docker-maven-plugin:build"
        sh "docker tag account.cloudogu.com/cloudogu-keycloak:latest registry.cloudogu.com/ci/account.cloudogu.com:1.0.0"
        sh "docker push registry.cloudogu.com/ci/account.cloudogu.com:1.0.0"
        sh("""
        sudo apt-get install curl gpg apt-transport-https --yes
        curl -fsSL https://packages.buildkite.com/helm-linux/helm-debian/gpgkey | gpg --dearmor | sudo tee /usr/share/keyrings/helm.gpg > /dev/null
        echo "deb [signed-by=/usr/share/keyrings/helm.gpg] https://packages.buildkite.com/helm-linux/helm-debian/any/ any main" | sudo tee /etc/apt/sources.list.d/helm-stable-debian.list
        sudo apt-get update
        sudo apt-get install helm""")

        sh 'helm repo add bitnami https://charts.bitnami.com/bitnami'
        sh 'helm repo update'

        def HELM_CMD = "install"
        def deploymentExists = sh(returnStatus: true, script: "helm --kube-context=${currentContext} --namespace=${namespace} list -q | grep -q '^keycloak\$'")
        if (deploymentExists == 0) {
            HELM_CMD = "upgrade"
        }

        /**

        externalDatabase:
          host: ""
          port: 5432
          user: bn_keycloak
          database: bitnami_keycloak
          schema: public
          password: ""
          existingSecret: ""
          existingSecretUserKey: ""
          existingSecretPasswordKey: ""
          annotations: {}
          extraParams: ""

        **/

        pipe.multiNodeEcoSystem.waitForDogu("postgresql")

        def postgreSqlPodName = sh(returnStdout: true, script: """kubectl get pod -l dogu.name=postgresql --namespace=ecosystem -o jsonpath='{.items[0].metadata.name}'""")

        def postgresqlCreds = sh(returnStdout: true, script: "kubectl --namespace=ecosystem exec ${postgreSqlPodName} -- /create-sa.sh keycloak")
        /**
        # print details
        echo "database: ${DATABASE}"
        echo "username: ${USER}"
        echo "password: ${PASSWORD}"
        **/

       def lines = postgresqlCreds.trim().split("\n")

       def postgresqlDatabase = lines.find { it.startsWith("database:") }?.split(":", 2)[1]?.trim()
       def postgresqlUsername = lines.find { it.startsWith("username:") }?.split(":", 2)[1]?.trim()
       def postgresqlPassword = lines.find { it.startsWith("password:") }?.split(":", 2)[1]?.trim()

        sh """
            helm --kube-context=${currentContext} --namespace=${namespace} ${HELM_CMD} keycloak --version 24.2.0 bitnami/keycloak \
              -f ./k8s/values-shared.yaml \
              -f ../integrationTests/k8s/keycloak-values.yaml \
              --set "image.pullSecrets={'ces-container-registries'}" \
              --set externalDatabase.user=${postgresqlUsername} \
              --set externalDatabase.database=${postgresqlDatabase} \
              --set externalDatabase.password=${postgresqlPassword} \

        """
    }

    // Retrieve OIDC client secret from the running Keycloak pod.
    echo "Retrieving OIDC client secret from running Keycloak..."

    // Wait for Keycloak pod to be ready
    echo "Waiting for Keycloak pod to be ready..."
    sh "kubectl -n ecosystem wait --for=condition=ready pod -l app.kubernetes.io/name=keycloak --timeout=600s"

    def keycloakPodName = sh(returnStdout: true, script: """kubectl -n ecosystem get pod -l app.kubernetes.io/name=keycloak -o jsonpath='{.items[0].metadata.name}'""").trim()
    def keycloakRealm = 'Cloudogu'

    clientSecret = sh(returnStdout: true, script: '''
kubectl -n ecosystem exec -i ''' + keycloakPodName + ''' -- sh -s <<'EOF'
set -eu
: "${KEYCLOAK_ADMIN:=admin}"
: "${KEYCLOAK_ADMIN_PASSWORD:=admin}"

KCADM=/opt/keycloak/bin/kcadm.sh
"$KCADM" config credentials --server http://localhost:8080 --realm master --user "$KEYCLOAK_ADMIN" --password "$KEYCLOAK_ADMIN_PASSWORD" >/dev/null

client_id="$("$KCADM" get clients -r ''' + keycloakRealm + ''' -q clientId=casClient | python3 -c 'import json,sys; data=json.load(sys.stdin); print(next((c["id"] for c in data if c.get("clientId")=="casClient"), ""))')"
if [ -z "$client_id" ]; then
  echo "Failed to resolve casClient in realm ''' + keycloakRealm + '''" >&2
  exit 1
fi

"$KCADM" get "clients/$client_id/client-secret" -r ''' + keycloakRealm + ''' | python3 -c 'import json,sys; print(json.load(sys.stdin)["value"])'
EOF
''').trim()

    echo "Retrieved client secret: ${clientSecret}"

    def podname = sh(returnStdout: true, script: """kubectl get pod -l dogu.name=cas --namespace=ecosystem -o jsonpath='{.items[0].metadata.name}'""").trim()

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
        echo "${casDoguStartedAt} is not after ${globalConfigLastUpdateTime} yet."
        echo "Waiting for CAS to restart and pick up the new global config..."
        sleep time: 10, unit: 'SECONDS'
        casDoguStartedAt = sh(returnStdout: true, script: """kubectl get dogu -n ecosystem cas -o json | jq -r '.status.startedAt'""").trim()
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
