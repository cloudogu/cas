# Entwicklungen am CAS
Der CAS (Central Authentification Service) ist eine Java Applikation die im Cloudogu Ecosystem die Rolle eines SSO einnimmt.
Der CAS implementiert einen Client für die verschiedenen CAS Bibliotheken von jasig.
Die derzeit benutzte Java JDK die ist 1.8 verwendet (Nachzulesen in [pom.xml](https://github.com/cloudogu/cas/blob/develop/app/pom.xml)).

Der Sourcecode des CAS liegt im Verzeichnis `app/src/main/java`.

## Tests
Die Tests des CAS liegen im Verzeichnis `app/src/test/java`. Getestet wird mit [JUnit4](https://junit.org/junit5/docs/current/user-guide/#writing-tests) in verbindung mit [Mockito](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html). Die Verzeichnisstruktur bildet die des Sourcecodes ab.

## Logging
Das Loglevel des CAS wird über den Parameter `"logging/root"`dogu.json eingestellt.

## Spring

Mit dem CAS War Overlay Upgrade auf Version 6.5.2 lässt sich das `@Autowire`-Feature von Spring Boot nicht mehr als 
Field Injection verwenden. Bei der Entwicklung sollte in allen Fällen die "Constructor Injection" benutzt werden.
Man kann sich gut an den vorhandenen Code im CAS orientieren. Dieser wurde für "Constructor Injection" komplett 
angepasst.