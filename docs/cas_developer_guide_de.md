# Entwicklungen am CAS
Der CAS (Central Authentification Service) ist eine Java Applikation die im Cloudogu Ecosystem die Rolle eines SSO einnimmt.
Der CAS implementiert einen Client für die verschiedenen CAS Bibliotheken von jasig.
Die derzeit benutzte Java JDK die ist 1.8 verwendet (Nachzulesen in [pom.xml](https://github.com/cloudogu/cas/blob/develop/app/pom.xml)).

Der Sourcecode des CAS liegt im Verzeichnis `app/src/main/java`.

## Tests
Die Tests des CAS liegen im Verzeichnis `app/src/test/java`. Getestet wird mit [JUnit4](https://junit.org/junit5/docs/current/user-guide/#writing-tests) in verbindung mit [Mockito](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html). Die Verzeichnisstruktur bildet die des Sourcecodes ab.

## Logging
Das Loglevel des CAS wird über den Parameter `"logging/root"`dogu.json eingestellt. 
Seit Issue [#64](https://github.com/cloudogu/cas/pull/66) wird außerdem das Log-Level für Übersetzungsstrings durch `"logging/translation_messages"` gesteuert.
`"logging/translation_messages"` ist per default auf `Error` eingestellt.
