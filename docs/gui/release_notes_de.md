# Release Notes

Im Folgenden finden Sie die Release Notes für das CAS-Dogu. 

Technische Details zu einem Release finden Sie im zugehörigen [Changelog](https://docs.cloudogu.com/de/docs/dogus/cas/CHANGELOG/).

## [Unreleased]
- Das Dogu wurde intern auf eine JSON Registry umgestellt, wodurch sich die Logik zum Anlegen und Löschen von Service-Accounts geändert hat.
- Einheitliche Verwendung von Service-Accounts sowohl in einer Multinode- als auch Singlenode-Umgebung.
- Behebung des Bugs, dass ein Upgrade abbricht, wenn keine Serviceaccounts vorhanden sind.

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