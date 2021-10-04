# Wiremock für die von JUnit-Tests verwendete etcd-Registry erstellen.

Die EtcdRegistry-Implementierung wird mit einer von wiremock erstellten Mocked-Registry getestet.
In dieser Anleitung wird erklärt, wie man die Tests für den gespiegelten etcd aktualisiert.

### 1. Vagrant einrichten

Es ist wichtig, eine Reihe von Dogus zu installieren, die für das Funktionieren der Tests erforderlich sind.
Stellen Sie sicher, dass die folgenden Dogus installiert sind:

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

### 2. Zeichnen Sie die Anfragen für die Tests über wiremock auf

Führen Sie einfach den unteren Block der Befehle aus. Sie tun das Folgende:

1. Java installieren
1. Ordner für Daten erstellen
1. Wiremock über CLI herunterladen
1. Starten Sie wiremock im Aufzeichnungsmodus, um unsere Anfragen gegen den etcd abzufragen

```bash
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

Jetzt können Sie mit `curl "http://localhost:9999/<api_call>"` Anfragen an das Wiremock stellen. Sie werden automatisch aufgezeichnet.

### 3. Historie aufzeichnen

Die folgenden Befehle sollten aufgezeichnet werden, um sicherzustellen, dass alle verfügbaren Unit-Tests wie erwartet funktionieren:

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

# set the secrets to a special value for both dogus to ensure that the new unit pass
etcdctl set /config/cas/service_accounts/oauth/portainer "cdf022a1583367cf3fd6795be0eef0c8ce6f764143fcd9d851934750b0f4f39f"
etcdctl set /config/cas/service_accounts/oidc/cas-oidc-client "834251c84c1b88ce39351d888ee04df91e89785a28dbd86244e0e22c9d27b41f"
curl "http://localhost:9999/v2/keys/config/cas/service_accounts?dir=true"
curl "http://localhost:9999/v2/keys/config/cas/service_accounts/oidc?dir=true"
curl "http://localhost:9999/v2/keys/config/cas/service_accounts/oidc/cas-oidc-client"
curl "http://localhost:9999/v2/keys/config/cas/service_accounts/oauth?dir=true"
curl "http://localhost:9999/v2/keys/config/cas/service_accounts/oauth/portainer"
''' | bash
```

### 4. Kopieren

Die neu aufgenommene Mock-Registrierung wird im Datenordner gespeichert. Die neuen Daten sollten die aktuellen Daten des cas ersetzen.

Führen Sie dazu die folgenden Aktionen durch:

1. Löschen Sie die alten Ordner (`__files`, `mappings`) im Pfad `<cas_path>/app/src/test/resources`.
1. Kopieren Sie den Ordner (`__files`, `mappings`) der neu erstellten Mock-Registry in den Pfad `<cas_path>/app/src/test/resources`.
1. Stellen Sie sicher, dass alles korrekt ist, indem Sie die Tests der Datei `RegsitryEtcdTest` ausführen.

### 5. Aufräumen

Beenden Sie den wiremock-Prozess (`STRG+C`) und löschen Sie den Datenordner (`rm -rf data`).