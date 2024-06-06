# Setup für die Integrationstests

In diesem Abschnitt werden die benötigten Schritte beschrieben, um die Integrationstests korrekt ausführen zu können.

## Voraussetzungen

* Es ist notwendig, das Programm `yarn` zu installieren

## Konfiguration 

Damit alle Integrationstests auch einwandfrei funktionieren, müssen vorher einige Daten konfiguriert werden. 

**integrationTests/cypress.config.js** [[Link zur Datei](../../integrationTests/cypress.config.js)]

1) Es muss die base-URL auf das Hostsystem angepasst werden.
   Dafür muss das Feld `baseUrl` auf die Host-FQDN angepasst werden (`https://192.168.56.2`)
2) Es müssen noch weitere Aspekte konfiguriert werden. 
   Diese werden als Umgebungsvariablen in der `cypress.config.js` gesetzt:
- `DoguName` - Bestimmt den Namen des jetzigen Dogus und wir beim Routing benutzt.
- `MaxLoginRetries` - Bestimmt die Anzahl der Loginversuche, bevor ein Test fehlschlägt.
- `AdminUsername` - Der Benutzername des CES-Admins.
- `AdminPassword` - Das Passwort des CES-Admins.
- `AdminGroup` - Die Benutzergruppe für CES-Administratoren.
- `ClientID` - Die Client-ID des Integrationstests-Clients (default:`inttest`)
- `ClientSecret` - Das Client-Secret in Klartextform (default:`integrationTestClientSecret`)
- `PasswordHintText` - Der erwartete, angezeigte Text beim Klicken auf den Passwort-Vergessen-Button
- `PrivacyPolicyURL` - Der erwartete Link für die Datenschutzrichtlinie
- `TermsOfServiceURL` - Der erwartete Link für die Nutzungsbedingungen
- `ImprintURL` - Der erwartete Link für das Impressum
  
Eine Beispiel-`cypress.config.js` sieht folgendermaßen aus:
```javascript
module.exports = defineConfig({
    e2e: {
        "baseUrl": "https://192.168.56.2",
        "env": {
            "DoguName": "cas/login",
            "MaxLoginRetries": 3,
            "AdminUsername": "ces-admin",
            "AdminPassword": "Ecosystem2016!",
            "AdminGroup": "CesAdministrators",
            "ClientID": "inttest",
            "ClientSecret": "integrationTestClientSecret",
            "PasswordHintText": "Contact your admin",
            "PrivacyPolicyURL": "https://www.triology.de/",
            "TermsOfServiceURL": "https://www.itzbund.de/",
            "ImprintURL": "https://cloudogu.com/"
        },

        specPattern: ["cypress/e2e/**/*.feature"],
        videoCompression: false,
        setupNodeEvents,
        nonGlobalStepBaseDir: false,
        chromeWebSecurity: false,
        experimentalRunAllSpecs: true,
    }
})
```

## Vorbereiten der Integrationstests

Damit die Integrationstests für CAS erfolgreich durchlaufen müssen vorher folgende Schritte ausgeführt werden:

**Schritt 1:**

Es muss ein registrierter Service für CAS angelegt werden, damit die Tests mit den Endpunkten von CAS kommunizieren darf. Dies kann einfach simuliert werden, indem wir folgende Keys in den etcd schreiben: 
```bash
etcdctl mkdir /dogu/inttest
etcdctl set /dogu/inttest/0.0.1 '{"Name":"official/inttest","Dependencies":["cas"]}'
etcdctl set /dogu/inttest/current "0.0.1"
```
Nun existiert ein "leeres" Dogu, für welches der CAS einen Service registriert. Dieser wird von den Integrationstests benutzt, um mit den notwendigen Endpunkten zu kommunizieren. Der Name des leeren Dogus muss mit dem Wert für die `ClientID` aus der `cypress.json` übereinstimmen. 

**Schritt 2:**

Damit unsere OAuth-Tests erfolgreich durchgeführt werden können, müssen wir im CAS einen Service-Account anlegen. Dis können wir ebenfalls simulieren, indem wir einen Service-Account im etcd unter dem CAS Pfad hinterlegen:
```bash
etcdctl set /config/cas/service_accounts/oauth/inttest "fda8e031d07de22bf14e552ab12be4bc70b94a1fb61cb7605833765cb74f2dea"
```
Hier muss `inttest` dem Namen des "leeren" Dogus aus dem ersten Schritt entsprechen. Bei dem Wert handelt es sich um das konfigurierte Client-Secret aus der `cypress.json` als SHA-256 Hash.

**Schritt 3:**

Damit unsere Tests für die Passwort-Vergessen-Funktion durchgeführt werden können, müssen wir im CAS einen Text definieren, der bei einem Klick auf den Passwort-Vergessen-Button angezeigt werden soll.
Auf folgende Weise kann ein entsprechender Eintrag im etcd konfiguriert werden:

```bash
etcdctl set /config/cas/forgot_password_text 'Contact your admin'
```

Der von den Tests erwartete Wert ist in der `cypress.config.js` unter dem Attribut `PasswordHintText` definiert.

Außerdem müssen die Passwort-Regeln entsprechend gesetzt werden:
```bash
etcdctl set /config/_global/password-policy/must_contain_capital_letter true
etcdctl set /config/_global/password-policy/must_contain_lower_case_letter true
etcdctl set /config/_global/password-policy/must_contain_digit true
etcdctl set /config/_global/password-policy/must_contain_special_character true
etcdctl set /config/_global/password-policy/min_length 14
```

**Schritt 4**

Damit unsere Tests für die rechtlichen URLs wie dem Impressum durchgeführt werden können, müssen entsprechende URLs im CAS definiert werden, damit diese im Footer angezeigt werden.
Auf folgende Weise können entsprechende Einträge im etcd konfiguriert werden:

```bash
etcdctl set /config/cas/legal_urls/imprint 'https://cloudogu.com/'
etcdctl set /config/cas/legal_urls/privacy_policy 'https://www.triology.de/'
etcdctl set /config/cas/legal_urls/terms_of_service 'https://docs.cloudogu.com/'
```

Die von den Tests erwarteten URLs sind in der `cypress.json` unter den Attributen `PrivacyPolicyURL`, `TermsOfServiceURL` und `ImprintURL` definiert.

### Vorbereitung: OIDC Provider

**Schritt 1:** Keycloak auf der Host-Maschine starten und Realm importieren (**Achtung: Der Pfad zur JSON muss angepasst werden!**)

```bash
docker run --name kc -e KEYCLOAK_USER=admin -e KEYCLOAK_PASSWORD=admin -p 9000:8080 -e KEYCLOAK_IMPORT="/realm-cloudogu.json -Dkeycloak.profile.feature.upload_scripts=enabled" -v  /vagrant/containers/cas/integrationTests/keycloak-realm/realm-cloudogu.json:/realm-cloudogu.json quay.io/keycloak/keycloak:15.0.2
```

**Schritt 2:** Konfiguration für den CAS im CES:

```bash
etcdctl set /config/cas/oidc/enabled "true"
etcdctl set /config/cas/oidc/discovery_uri "http://192.168.56.1:9000/auth/realms/Cloudogu/.well-known/openid-configuration"
etcdctl set /config/cas/oidc/client_id "casClient"
etcdctl set /config/cas/oidc/display_name "MyProvider"
etcdctl set /config/cas/oidc/optional "true"
etcdctl set /config/cas/oidc/scopes "openid email profile groups"
etcdctl set /config/cas/oidc/attribute_mapping "email:mail,family_name:surname,given_name:givenName,preferred_username:username,name:displayName"
```

**Schritt 3:** Per `cesapp edit-config cas` das oidc/client_secret auf `c21a7690-1ca3-4cf9-bef3-22f37faf5144` setzen. Dies wird dann korrekt verschlüsselt abgelegt.

## Starten der Integrationstests

Die Integrationstests können auf zwei Arten gestartet werden:

1. Mit `yarn cypress run` starten die Tests nur in der Konsole ohne visuelles Feedback.
   Dieser Modus ist hilfreich, wenn die Ausführung im Vordergrund steht.
   Beispielsweise bei einer Jenkins-Pipeline.
   
1. Mit `yarn cypress open` startet ein interaktives Fenster, wo man die Tests ausführen, visuell beobachten und debuggen kann.
   Dieser Modus ist besonders hilfreich bei der Entwicklung neuer Tests und beim Finden von Fehlern.