# Setup für die Integrationstests

In diesem Abschnitt werden die benötigten Schritte beschrieben um die Integrationstests korrekt ausführen zu können.

## Voraussetzungen

* Es ist notwendig das Program `yarn` zu installieren

## Konfiguration 

Damit alle Integrationstests auch einwandfrei funktionieren, müssen vorher einige Daten konfiguriert werden. 

**integrationTests/cypress.json** [[Link zur Datei](../../integrationTests/cypress.json)]

1) Es muss die base-URL auf das Hostsystem angepasst werden.
   Dafür muss das Feld `baseUrl` auf die Host-FQDN angepasst werden (`https://local.cloudogu.com`)
2) Es müssen noch weitere Aspekte konfiguriert werden. 
   Diese werdeb als Umgebungsvariablen in der `cypress.json` gesetzt:
- `DoguName` - Bestimmt den Namen des jetzigen Dogus und wir beim Routing benutzt.
- `MaxLoginRetries` - Bestimmt die Anzahl der Loginversuche, bevor ein Test fehlschlägt.
- `AdminUsername` - Der Benutzername des CES-Admins.
- `AdminPassword` - Das Passwort des CES-Admins.
- `AdminGroup` - Die Benutzergruppe für CES-Administratoren.
- `ClientID` - Die Client-ID des Integrationstests-Clients (default:`inttest`)
- `ClientSecret` - Das Client-Secret in Klartextform (default:`integrationTestClientSecret`)
  
Eine Beispiel-`cypress.json` sieht folgendermaßen aus:
```json
{
  "baseUrl": "https://192.168.56.2",
  "env": {
    "DoguName": "redmine",
    "MaxLoginRetries": 3,
    "AdminUsername":  "ces-admin",
    "AdminPassword":  "ecosystem2016",
    "AdminGroup":  "CesAdministrators", 
    "ClientID" : "inttest",
    "ClientSecret"  : "integrationTestClientSecret"
  }
}
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
Nun existiert ein "leeres" Dogu, für welches der CAS einen Service registriert. Dieser wird von den Integrationstest benutzt um mit den notwendigen Endpunkten zu kommunizieren. Der Name des leeren Dogus muss mit dem Wert für die `ClientID` aus der `cypress.json` übereinstimmen. 

**Schritt 2:**

Damit unsere OAuth-Tests erfolgreich durchgeführt werden können, müssen wir im CAS einen Service-Account anlegen. Dis können wir ebenfalls simulieren, indem wir einen Service-Account im etcd unter dem CAS Pfad hinterlegen:
```bash
etcdctl set /config/cas/service_accounts/inttest "fda8e031d07de22bf14e552ab12be4bc70b94a1fb61cb7605833765cb74f2dea"
```
Hier muss `inttest` dem Namen des "leeren" Dogus aus dem ersten Schritt entsprechen. Bei dem Wert handelt es sich um das konfigurierte Client-Secret aus der `cypress.json` als SHA-256 Hash.

## Starten der Integrationstests

Die Integrationstests können auf zwei Arten gestartet werden:

1. Mit `yarn cypress run` starten die Tests nur in der Konsole ohne visuelles Feedback.
   Dieser Modus ist hilfreich, wenn die Ausführung im Vordergrund steht.
   Beispielsweise bei einer Jenkins-Pipeline.
   
1. Mit `yarn cypress open` startet ein interaktives Fenster, wo man die Tests ausführen, visuell beobachten und debuggen kann.
   Dieser Modus ist besonders hilfreich bei der Entwicklung neuer Tests und beim Finden von Fehlern.