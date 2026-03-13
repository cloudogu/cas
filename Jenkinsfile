#!groovy
@Library([
  'pipe-build-lib',
  'ces-build-lib',
  'dogu-build-lib'
]) _

import com.cloudogu.ces.cesbuildlib.K3d
import com.cloudogu.ces.cesbuildlib.Makefile

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
    defaultBranch      : "master"
])

def componentRegistry = "registry.cloudogu.com"
def componentRegistryNamespace = "k8s"
def componentChartTargetDir = "target/k8s/helm"
def componentBuildImageRepository = "registry.cloudogu.com/official/cas"
def componentReleaseName = "lop-idp-cas"
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

def yq = { yaml, command ->
    new com.cloudogu.ces.cesbuildlib.Docker(this)
        .image("mikefarah/yq:latest")
        .mountJenkinsUser()
        .inside("--volume ${WORKSPACE}:/workdir -w /workdir") {
            return sh (script: "echo '${yaml}' | yq ${command}", returnStdout: true)
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
            k3d.kubectl("delete secret cas-ldap || true")
            // Steal username and password for ldap from cas dogu to use in component.
            // Once we have completely transitioned to the lop-idp component in ecosystem-core,
            // this will come from the ldap component and we don't need it anymore.
            String originalCasConfigYaml = new String(
                k3d.kubectl("get secret cas-config -o jsonpath='{.data.config\\.yaml}'", true)
                .decodeBase64()
            )
            def ldapUsername = yq(originalCasConfigYaml, ".sa-ldap.username")
            def ldapPassword = yq(originalCasConfigYaml, ".sa-ldap.password")
            k3d.kubectl("create secret generic cas-ldap --from-literal=username='${ldapUsername}' --from-literal=password='${ldapPassword}'")

            echo "[Component k3d] Generate helm chart"
            runMakeInGoContainer("helm-generate")

            echo "[Component k3d] Retag image for local smoke test"
            sh "docker tag ${componentBuildImageRepository}:${imageTag} local-smoke/cas:${imageTag}"

            echo "[Component k3d] Import previously built image"
            sh "sudo ${WORKSPACE}/k3d/.k3d/bin/k3d image import local-smoke/cas:${imageTag} -c ${k3d.registryName}"

            echo "[Component k3d] Deploy component via helm"
            k3d.helm("upgrade --install ${componentReleaseName} ${componentChartTargetDir}"
            + " --namespace default --set containers.cas.image.registry=''"
            + " --set containers.cas.image.repository=local-smoke/cas"
            + " --set containers.cas.image.tag=${imageTag}"
            + " --set containers.cas.imagePullPolicy=Never"
            // disable ingress to avoid conflicts with the cas dogu
            + " --set ingress.enabled=false"
            + " --wait --timeout 5m")

            echo "[Component k3d] Verify component startup"
            k3d.kubectl("rollout status deployment/${componentReleaseName} --timeout=300s")
            k3d.kubectl("wait --for=condition=ready pod -l app.kubernetes.io/instance=${componentReleaseName} --timeout=300s")
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
                "discovery_uri": "http://${ecoSystem.externalIP}:9000/auth/realms/Test/.well-known/openid-configuration",
                "client_id": "cas",
                "display_name": "cas",
                "optional": "true",
                "scopes": "openid email profile groups",
                "allowed_groups": "testers",
                "attribute_mapping": "email:mail,family_name:surname,given_name:givenName,preferred_username:username,name:displayName,groups:externalGroups"
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
