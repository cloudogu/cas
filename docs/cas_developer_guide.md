# Entwicklungen am CAS
Der CAS (Central Authentification Service) ist eine Java Applikation die im Cloudogu Ecosystem die Rolle einenes SSO einnimmt.
Der CAS implementiert einen Client für die verschiedenen CAS Bibliotheken von jasig . Die derzeit benutzte Java JDK die ist 1.8 verwendet (Nachzulesen in [pom.xml](../app/pom.xml)).

Der Sourcecode des CAS liegt im Verzeichnis `app/src/main/java`.

## Tests
Die Tests des CAS liegt im Verzeichnis `app/src/test/java`. Getestet wird mit [JUnit4](https://junit.org/junit5/docs/current/user-guide/#writing-tests) in verbindung mit [Mockito](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html). Die Verzeichnisstruktur bildet die des Sourcecodes an.

## Logging
Das Loglevel des CAS wird über den Parameter `"logging/root"`dogu.json eingestellt. Seit Issue [#64](https://github.com/cloudogu/cas/pull/66) wird außerdem das Log-Level für Übersetzungsstrings durch `"logging/translation_messages"` gesteuert. `"logging/translation_messages"` ist per default auf `Error` eingestellt.

## OAuth
Für die implementierung von OAuth musste der Prozess wie Services erstellt werden umgestellt werden. Eintrittspunkt der Entwicklung ist die `CesServicesManagerStage.addNewService` Methode. Diese nimmt nicht mehr einen name, serviceId und logoutUri entgegen, sondern direkt einen Service. Ausgehend davon werden in den beiden Stages `CesServicesManagerStageDevelopment` und `CesServicesManagerStageProductive` die neuen Factories die das Interface `ICESServiceFactory` implementiert genutzt um Services für OAuth, PersistantServices und DoguServices.
Neu ist das Package [de.triology.cas.services.oauth](../app/src/main/java/de/triology/cas/services/oauth). Die enthaltenden Controller verwalten die Requests an die Endpunkte für OAuth-Requests (`oauth20/authorize`, `oauth20/accesstoken` usw.)


![alternative text](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.https://github.com/cloudogu/cas/master/docs/diagramms/classdiagramm_cesservicefactory.txt)

![CesServiceFactory](http://www.plantuml.com/plantuml/png/XP5FRYen3CRNBwVmybKK3b0X109LOb6GoWbCnZCJAIJ8SVf7bBitQO84p08lPYpxP_lJbtaGP8a7gumJuXOrmNf9OKVyOJIzeXRFtt1I0ADtwkWhVKn893KL0Ss4GXlwh8N_Jna-1K4n-WmToOfQZ5RKMIbjCGH81rQ-YuCZDyHs4QKV8F_6vTvVTsaDEQdCpN2jhL0m3ursmiPrMSm63wGWnLLjtP0JqnhYqXawaAcQ5kGDZzEou1dAVvQPXQHy7uL2bgEkN2qntAo69PZ-ZCS3anxGrR37-8HDryjaTc_J2yH00SJN5uLGca4fvuLIumY-lPjI0BstJG04ffR9wGm2mkKQkWOqEf2U4k2PO1-DRH9Hjm261C4MatZmd7EcBbha2w4exSWIr3nbyM1_0G00)