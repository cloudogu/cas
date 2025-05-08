# Release Notes

Im Folgenden finden Sie die Release Notes für das CAS-Dogu. 

Technische Details zu einem Release finden Sie im zugehörigen [Changelog](https://docs.cloudogu.com/de/docs/dogus/cas/CHANGELOG/).

## [Unreleased]

## [v7.0.10-3] - 2025-05-09
- Beenden der OIDC-Session beim Abmelden
    - Wenn die Session beim Abmelden nicht beendet wurde, konnte das Benutzerprofil in der OIDC-Sitzung nicht aktualisiert werden, da eine "alte" Session mit dem "alten" Profil vorhanden war.
    - Dies führte dazu, dass eventuelle Änderungen des Benutzers (z.B. Benutzername oder Gruppenzuordnungen) nicht aktualisiert wurden.
  
## [v7.0.10-2] - 2025-04-23
### Changed
- Die Verwendung von Speicher und CPU wurden für die Kubernetes-Multinode-Umgebung optimiert. 

## [v7.0.10-1] - 2025-04-17
### Changed
- [#261] Upgrade CAS zu Version 7.0.10.1

## [v7.0.8-14] - 2025-04-16
### Sicherheit
- [Fix CVE-2024-38821](https://nvd.nist.gov/vuln/detail/CVE-2024-38821) – Behebt eine unzureichende Absicherung von statischen Ressourcen bei WebFlux-Anwendungen
- [Fix CVE-2025-24813](https://nvd.nist.gov/vuln/detail/CVE-2025-24813) – Behebt eine Schwachstelle bei gleichzeitigen HTTP/2-Verbindungen
- [Fix OIDC WebAuthn Vulnerability](https://apereo.github.io/2025/04/11/oidc-webauthn-vuln/) – Behebt ein Problem, bei dem Benutzeraktionen im WebAuthn-Kontext nicht korrekt verifiziert wurden

## [v7.0.8-13] - 2025-04-07
- Der CAS-Start ist jetzt robuster bei ungesetzten oder leeren Konfigurationsschlüsseln.

## [v7.0.8-12] - 2025-03-12
- Bereinigung alter Service-Accounts aus der JSON-Registry vor der Erstellung eines neuen Service-Accounts

## Release 7.0.8-11
- Es wurde der CAS-Start robuster bei ungesetzten Konfigurationsschlüssel gestaltet.

## Release 7.0.8-10
- Bei der Anmeldung über eine delegierte Authentifizierung können jetzt `allowed_groups` und `initial_admin_usernames` konfiguriert werden.
  - `allowed_groups`: Gibt eine Liste von OIDC-Gruppen an, die sich mit delegierter Authentifizierung anmelden dürfen. Die Gruppen werden durch Komma getrennt. Eine leere Liste erlaubt den Zugang für alle.
  - `initial_admin_usernames`: Gibt eine Liste von Benutzernamen an, die der CES-Admin-Gruppe bei der ersten Anmeldung zugewiesen werden.

## Release 7.0.8-9
- Es wurde ein Problem behoben, bei dem das Dogu unter hoher Systemlast nicht mehr startet.

## Release 7.0.8-8
- Die Schaltfläche zum Aufdecken des Passworts wurde zur leichteren Bedienung in ein Checkbox umgewandelt.

## Release 7.0.8-7
- Es wurde ein technischer Fehler behoben, bei dem der Upgrade-Prozess unterbrochen wurde
- Es wurde ein technischer Fehler behoben, bei dem das Cockpit-Dopu unter bestimmten Bedinungen nicht augerufen werden konnte

## Release 7.0.8-6
- Invalide Anmeldedaten werden nicht mehr geloggt

## Release 7.0.8-5
- Das Dogu wurde intern auf eine JSON Registry umgestellt, wodurch sich die Logik zum Anlegen und Löschen von Service-Accounts geändert hat.
- Einheitliche Verwendung von Service-Accounts sowohl in einer Multinode- als auch Singlenode-Umgebung.

### Breaking Change
- Neu zu installierende Dogus müssen explizit die Erstellung eines Serviceaccounts im CAS über die dogu.json anfordern. Weitere Informationen hierfür finden Sie in der [Entwicklerdokumentation](https://github.com/cloudogu/dogu-development-docs/blob/main/docs/important/relevant_functionalities_de.md#authentifizierung)

## Release 7.0.8-4
- Bei der Anmeldung über eine delegierte Authentifizierung (durch einen OIDC-Provider) werden die Nutzer in den internen LDAP repliziert
    - Die replizierten Nutzer werden als "extern" gekennzeichnet und können, bis auf die Gruppenzuordnung, nicht editiert werden.

## Release 7.0.8-3
- Es wurde eine Anpassung gemacht, welche die Kompatibilität für Dogus erweitert, welche Open ID Connect nutzen.

## Release 7.0.8-2
- Es wurde ein technischer Fehler behoben der in Multinode-Umgebungen verhindert hat, dass Dogus mit Service-Accounts `cas` erreichbar sind.

## Release 7.0.8-1
Das Dogu bietet nun die CAS-Version 7.0.8 an. Die Release Notes von CAS finden Sie [in den CAS-Github-Releases](https://github.com/apereo/cas/releases/tag/v7.0.8).
- Die CAS-HTML-Seiten enthalten nun ein "lang"-Attribut um die Barrierefreiheit zu erhöhen.

## Release 7.0.5.1-8
- Die Cloudogu-eigenen Quellen werden von der MIT-Lizenz auf die AGPL-3.0-only relizensiert.

## Release 7.0.5.1-7
- Ein Dogu-Upgrade funktioniert nun besser in Cloudogu EcoSystem-Multinode-Instanzen ohne etcd

## Release 7.0.5.1-6
- Dieses Release behebt Fehler während des Dogu-Upgrades, die in den Versionen 7.0.5.1-4 and 7.0.5.1-5 eingeführt wurden

## Release 7.0.5.1-5
- Fehlende Übersetzungen wurden in diesem Release nachgereicht.

## Release 7.0.5.1-4
- In diesem Release ändert sich die Weise, wie andere Dogus Service-Accounts gegenüber dem CAS anlegen um einen reibungsloseren Betrieb in Cloudogu EcoSystem-Multinode-Instanzen zu gewährleisten.
- Dogu-Zustände, die während des Startens benötigt werden, finden aus Sicherheitsgründen nun in Volumes und nicht mehr im etcd statt. 

## Release 7.0.5.1-3
- Behebt Style-Probleme in der Anmeldemaske

## Release 7.0.5.1-2

- Das Design des Dogus wurde umgebaut, sodass es unserem neuen Theme entspricht
  - Dieses neue design ist in Kombination mit dem Whitelabeling-Dogu und der neuen Version vom Nginx (>=v1.26.1-5) komplett whitelabelbar.

## Release 7.0.5.1-1

- Das Dogu bietet nun die CAS-Version 7.0.5.1 an. Die Release Notes von CAS finden Sie [in den CAS-Github-Releases](https://github.com/apereo/cas/releases/tag/v7.0.5.1).
- Das Dogu bietet eine Funktionalität zur Blockierung wiederholt falscher Anmeldungen über einen definierten Zeitraum (throttling). 
- In der Vergangenheit insbes. mit der Einführung von CAS 7.x führte hier eine fehlerhafte Standardkonfiguration im Zusammenhang mit Dogu-internen Nutzern zu Sperrungen des gesamten Cloudogu EcoSystems. Dieser Fehler ist in dieser Version behoben worden. Hierbei wurden einige Dogu-Konfigurationsschlüssel zugunsten neuer Konfigurationsschlüssel eingeführt:
  - nicht mehr unterstützt:
    - `limit/max_numbers`
    - `limit/failure_store_time`
  - `limit/failure_threshold` konfiguriert nun die maximale Anzahl fehlerhafter Anmeldeversuche in einem Zeitraum, das `limit/range_seconds` in Sekunden konfiguriert
  - `limit/stale_removal_interval` konfiguriert den Zeitraum, den ein Aufräumjob im Hintergrund abwartet, bis er erneut startet, um nach veralteten Throttling-Einträgen zu suchen

## Release 7.0.4.1-2

- Das Dogu loggt nun im Debug-Loglevel keine Passwörter 
- Das Dogu bietet ein Security-Fix gegenüber der CAS-Dogu-Version 7.0.4.1-1 an. Die Release Notes von Redmine finden Sie [in den CAS-Github-Releases](https://github.com/apereo/cas/releases/tag/v7.0.4.1).
- In der Vergangenheit kam es vor, dass nach einer Umstellung auf das Debug-Loglevel Passwörter in einzelne Zeichen geteilt in den CAS-Logs auftauchten. Der Fehler ist in dieser Version behoben worden.