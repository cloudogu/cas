# Release Notes

Im Folgenden finden Sie die Release Notes für das CAS-Dogu. 

Technische Details zu einem Release finden Sie im zugehörigen [Changelog](https://docs.cloudogu.com/de/docs/dogus/cas/CHANGELOG/).

## Release 7.0.5.1-1

- Das Dogu bietet nun die CAS-Version 7.0.5.1 an. Die Release Notes von CAS finden Sie [in den CAS-Github-Releases](https://github.com/apereo/cas/releases/tag/v7.0.5.1).
- Das Dogu bietet eine Funktionalität zur Blockierung wiederholt falscher Anmeldungen über einen definierten Zeitraum (throttling). 
- In der Vergangenheit insbes. mit der Einführung von CAS 7.x führte hier eine fehlerhafte Standardkonfiguration im Zusammenhang mit Dogu-internen Nutzern zu Sperrungen des gesamten Cloudogu EcoSystems. Dieser Fehler ist in dieser Version behoben worden. Hierbei wurden einige Dogu-Konfigurationsschlüssel zugunsten neuer Konfigurationsschlüssel eingeführt:
  - nicht mehr unterstützt: `limit/max_numbers`
  - `limit/failure_threshold` konfiguriert nun die maximale Anzahl fehlerhafter Anmeldeversuche in einem Zeitraum, das `limit/range_seconds` in Sekunden konfiguriert
  - `limit/stale_removal_interval` konfiguriert den Zeitraum, den ein Aufräumjob im Hintergrund abwartet, bis er erneut startet, um nach veralteten Throttling-Einträgen zu suchen

## Release 7.0.4.1-2

- Das Dogu loggt nun im Debug-Loglevel keine Passwörter 
- Das Dogu bietet ein Security-Fix gegenüber der CAS-Dogu-Version 7.0.4.1-1 an. Die Release Notes von Redmine finden Sie [in den CAS-Github-Releases](https://github.com/apereo/cas/releases/tag/v7.0.4.1).
- In der Vergangenheit kam es vor, dass nach einer Umstellung auf das Debug-Loglevel Passwörter in einzelne Zeichen geteilt in den CAS-Logs auftauchten. Der Fehler ist in dieser Version behoben worden.