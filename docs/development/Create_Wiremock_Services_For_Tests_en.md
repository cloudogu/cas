# Create wiremock for etcd registry used by JUnit Tests

The EtcdRegistry implementation is tested with a mocked registry created by wiremock.

Wiremock acts as a station between the client and RestAPI and can be used to record requests to RestAPI and simulate them later.
This works as follows:

1. client sends request to Wiremock
2. wiremock records the request and now forwards it to the configured RestAPI
3. the RestAPI sends the answer back to Wiremock
4. wiremock stores the response and then forwards it back to the client

This results in a set of data that can be used by Wiremock to simulate our configured RestAPI.
We use this data in our unit tests. Therefore, it is important to define the scope of the requests so that our UnitTests will pass.

This guide explains how to update the tests for the mocked etcd.

## 1. Setup vagrant

It is important to install a certain set of dogus that are required for the tests to work.
Make sure the following dogus are installed:

```
- official/cas
- official/cockpit
- official/ldap
- official/nexus
- official/postfix
- official/registrator
- official/scm
- official/usermgt
- premium/portainer
- testing/cas-oidc-client
```

## 2. Record the requests for the tests via wiremock

Just execute the bottom block of command. They do the following:

1. Install Java
1. Make folder for data
1. Download wiremock via CLI
1. Start wiremock in recording mode to request our requests against the etcd

```bash
sudo apt update
sudo apt-get install openjdk-17-jre -y
mkdir data
wget https://repo1.maven.org/maven2/com/github/tomakehurst/wiremock-jre8-standalone/2.31.0/wiremock-jre8-standalone-2.31.0.jar
java -jar wiremock-jre8-standalone-2.31.0.jar \
    --root-dir $(pwd)/data \
    --port 9999 \
    --record-mappings --verbose \
    --preserve-host-header \
    --proxy-all="http://localhost:4001"
```

Now you can perform requests against the wiremock with `curl "http://localhost:9999/<api_call>"`. They will be automatically recorded.

## 3. Record history

The following commands should be recorded to ensure that all available unit tests work as expected:

```bash
echo '''
#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail

curl "http://localhost:9999/v2/keys/config/_global/fqdn"
curl "http://localhost:9999/v2/keys/dogu?dir=true&recursive=true"
curl "http://localhost:9999/v2/keys/dogu?dir=true"

INSTALLED_DOGU_LIST=$(etcdctl ls --sort /dogu | sed "s/\/dogu\///g" | tr "\n" " ")
echo "Installed dogus: ${INSTALLED_DOGU_LIST}"
declare -a dogus=( $INSTALLED_DOGU_LIST )

for val in "${dogus[@]}"; do
  DOGU_VERSION="$(etcdctl get /dogu/"${val}"/current || true)" 
  echo "${val} - ${DOGU_VERSION}"
  curl "http://localhost:9999/v2/keys/dogu/${val}/?dir=true"
  curl "http://localhost:9999/v2/keys/dogu/${val}/current"
  curl "http://localhost:9999/v2/keys/dogu/${val}/${DOGU_VERSION}"
done 

curl "http://localhost:9999/v2/keys/config/cas/service_accounts?dir=true"
# set the secrets to a special value for both dogus to ensure that the new unit pass
etcdctl set /config/cas/service_accounts/portainer "cdf022a1583367cf3fd6795be0eef0c8ce6f764143fcd9d851934750b0f4f39f"
etcdctl set /config/cas/service_accounts/cas-oidc-client "834251c84c1b88ce39351d888ee04df91e89785a28dbd86244e0e22c9d27b41f"
curl "http://localhost:9999/v2/keys/config/cas/service_accounts/cas-oidc-client"
curl "http://localhost:9999/v2/keys/config/cas/service_accounts/portainer"
''' | bash
```

## 4. Copy

The newly recorded mock registry is stored in the `data` folder. The new data should replace the current one of the CAS.

To do so perform the following actions:

1. delete the old folders (`__files`, `mappings`) in the path `<cas_path>/app/src/test/resources`.
1. copy the folder (`__files`, `mappings`) of the newly created mock registry in the path `<cas_path>/app/src/test/resources`.
1. Ensure that everything is correct by executing the tests of the `RegsitryEtcdTest` file.

## 5. Cleanup

Terminate the wiremock process (`STRG+C`) and delete the data folder (`rm -rf data`).