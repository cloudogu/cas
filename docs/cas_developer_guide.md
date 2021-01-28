# Entwicklungen am CAS
Der CAS (Central Authentification Service) ist eine Java Applikation die im Cloudogu Ecosystem die Rolle einenes SSO einnimmt.
Der CAS implementiert einen Client für die verschiedenen CAS Bibliotheken von jasig . Die derzeit benutzte Java JDK die ist 1.8 verwendet (Nachzulesen in [pom.xml](../app/pom.xml)).

Der Sourcecode des CAS liegt im Verzeichnis `app/src/main/java`.

## Tests
Die Tests des CAS liegt im Verzeichnis `app/src/test/java`. Getestet wird mit [JUnit4](https://junit.org/junit5/docs/current/user-guide/#writing-tests) in verbindung mit [Mockito](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html). Die Verzeichnisstruktur bildet die des Sourcecodes an.

## Logging
Das Loglevel des CAS wird über den Parameter `"logging/root"`dogu.json eingestellt. Seit Issue [#64](https://github.com/cloudogu/cas/pull/66) wird außerdem das Log-Level für Übersetzungsstrings durch `"logging/translation_messages"` gesteuert. `"logging/translation_messages"` ist per default auf `Error` eingestellt.

## OAuth
Für die implementierung von OAuth musste der Prozess wie Services erstellt werden umgestellt werden. Eintrittspunkt der Entwicklung ist die `CesServicesManagerStage.addNewService` Methode. Diese nimmt nicht mehr einen name, serviceId und logoutUri entgegen, sondern direkt einen Service. Ausgehend davon werden in den beiden Stages `CesServicesManagerStageDevelopment` und `CesServicesManagerStageProductive` die neuen Factories die das Interface `ICESServiceFactory` implementiert genutzt um Services für OAuth, PersistantServices und DoguServices zu erstellen.
Neu ist ebenfalls das Package [de.triology.cas.services.oauth](../app/src/main/java/de/triology/cas/services/oauth). Die enthaltenden Controller verwalten die Requests an die Endpunkte für OAuth-Requests (`oauth20/authorize`, `oauth20/accesstoken` usw.). Um alle nötigen Informationen abzuziehen wurde außerdem die Klasse `RegestryEtcd` dahingehend erwetert, dass die Informationen die für OAuth Services benötigt werden (ClientId und Secret-Hash) aus dem etcd abgezogen werden können.

<!---
https://stackoverflow.com/questions/32203610/how-to-integrate-uml-diagrams-into-gitlab-or-github

![alternative text](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.https://github.com/cloudogu/cas/master/docs/diagramms/classdiagramm_cesservicefactory.txt)
-->
![CesServiceFactory](http://www.plantuml.com/plantuml/png/ZPDlRnCn3CPVaq_Xbq2vVG0qeKqhI9DODr6-W9VpNIEbIHKxPQFik-DmAMjtXTkzIFFdzzZMO_U21PFajJSk2AKEBS7r5V6IqjPP-e9MOdg4dG6OVQEU7dHnh2Ir5G6R28KMzADUl7rNu1qBYhDFq9FCgSFinJmPOpqoWoQOnpwFaXmHjvTPLYFaJJFa-_DwvHq5gSoHkDxDKR28Paj9BlIbc0MkoO1-1tVNBGLndQCql8mTA5JT3iWDRiw701zW-FrKc4lH_NqMubFby0q6x2ajHTUTcM7RFlkcWl_oTocssOhR7YcBi9pkAB5ZmMSx6duCTfLYS3zf7yyqYTPn-_VyVOMoY5zZq2IV7rG_LSISjkJZPar1j0eNN4atHDOYzFMW6UPWfePlxAivOVNvhwOP3y0aNYTgWcwQzo_duoDGiBZXx5680Rs7GDOIe_Aj4iRymusQUwfVa_89fyra1ZXhchDJzhvEENOaCp5qrJE9zKyENBjhGjIJfquWTY_KPXLzou2Gg4iKB1Kzmhu165gm641Mf41CPEwpywqIDGOWAmItgDs7cCe-KPyzfZrXI6EK2io5xlBQ_mO0)