# Keycloak zum Testen einrichten
Im Folgenden wird beschrieben, wie ein Keycloak als oidc-provider an das cas angebunden werden kann.
Der keycloak in dem Beispiel enthält einen User, mit dem sich angemeldet werden kann: `ecosystem:ecosystem`.
Nach Abschluss der folgenden Schritte ist ein Login mit diesem User möglich.


## Keycloak starten
* In den Ordner `_keycloakdata` begeben (liegt im cas-repository)
  * Dieser Ordner enthält die Daten eines vorkonfigurierten Keycloaks
* Befehl ausführen: 
  ```sh
     docker run --name keycloak -v $PWD:/opt/keycloak/data -d -p 8080:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak:21.1.1 start-dev
  ```

## Cas konfigurieren

Folgende Befehle im Ecosystem ausführen:
```sh
etcdctl set /config/cas/oidc/display_name keycloak
etcdctl set /config/cas/oidc/attribute_mapping mail:mail,surname:surname,givenName:givenName,username:username,displayName:displayName
etcdctl set /config/cas/oidc/enabled true
etcdctl set /config/cas/oidc/discovery_uri http://192.168.56.1:8080/realms/master/.well-known/openid-configuration
etcdctl set /config/cas/oidc/client_id ecosystem
etcdctl set /config/cas/oidc/optional true
etcdctl set /config/cas/oidc/principal_attribute username
etcdctl set /config/cas/oidc/scopes openid mail surname givenName username displayName
cesapp edit-config cas oidc/client_secret 9AcJiF7Xpvv9Jfg4c3QtrwPukKuLFjwO
docker restart cas
```