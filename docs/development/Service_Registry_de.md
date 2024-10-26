# Service Registry

CAS erlaubt es Services zu deklarieren, die für eine CAS-Authentifizierung genutzt werden dürfen. Die Service-Registry
speichert die Services zusammen mit Metadaten und steuert somit das Verhalten des CAS.

Für die Service-Registry gibt es [verschiedene Implementierungen](https://apereo.github.io/cas/7.0.x/services/Service-Management.html#storage), die genutzt werden können.
Aktuell wird von uns die [JSON Service-Registry](https://apereo.github.io/cas/7.0.x/services/JSON-Service-Management.html) genutzt, bei der die Services als JSON abgelegt werden und zur Laufzeit in den Speicher geladen werden.

Für die Registry wird ein zentraler Ablageort definiert, der sich jedoch in Abhängigkeit von der aktuell genutzten [Stage](develop_stage_de.md) unterscheiden kann.
Befindet sich der CAS im Produktionsmodus, werden die Services vom Pfad `/etc/cas/services/production` geladen. Dabei werden die Services dynamisch, basierend auf den tatsächlich
installierten Dogus, erstellt. Für den [Entwicklungsmodus](develop_stage_de.md) sind statische Services für die Protokolle CAS, OAUTH und OIDC unter dem Pfad `/etc/cas/services/development`
deklariert, die generisch für die jeweiligen Protokolle gelten. Eine Unterscheidung zwischen einzelnen Applikationen findet nicht statt.

Services werden in der Regel über den Installationsprozess eines Dogus erzeugt. Besitzt ein Dogu eine Abhängigkeit auf den CAS, wird während der Installation das ExposedCommand `service-account-create`
aufgerufen, welches den Service als JSON-Konfiguration in der Service-Registry erstellt. Hierfür werden Konfigurationsvorlagen verwendet, die unter dem Pfad `/etc/cas/config/services` hinterlegt sind. Anhängig von den
Eingabeparametern sowie dem Typ des genutzten Protokolls werden verschiedene [Properties](https://apereo.github.io/cas/7.0.x/services/Configuring-Service-Custom-Properties.html) befüllt, die dann wiederum von [CAS-internen Templates](https://apereo.github.io/cas/7.0.x/services/Configuring-Service-Template-Definitions.html) genutzt werden. Diese Templates werden über ihren Namen
referenziert und dienen als Vorlage für die im Speicher erzeugten Services des CAS. Sie sind unter `/etc/cas/services/templates` zu finden. Die final erzeugten JSON-Dateien in der Service-Registry folgen
dabei stets der Namenskonvention `<Applikation>-<ServiceID>.json`.



