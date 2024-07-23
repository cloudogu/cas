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

## Gradle

Gradle benutzt eine eigene JDK-VM. In der IDE muss diese ggf. angemessen konfiguriert werden (aktuell Java 21), um Fehlermeldungen wie diese zu vermeiden:

```
A problem occurred configuring root project 'cas'.
> Could not resolve all files for configuration ':classpath'.
   > Could not resolve org.springframework.boot:spring-boot-gradle-plugin:3.2.1.
     Required by:
         project :
      > No matching variant of org.springframework.boot:spring-boot-gradle-plugin:3.2.1 was found. The consumer was configured to find a library for use during runtime, compatible with Java 8, packaged as a jar, and its dependencies declared externally, as well as attribute 'org.gradle.plugin.api-version' with value '8.5' but:
          - Variant 'apiElements' capability org.springframework.boot:spring-boot-gradle-plugin:3.2.1 declares a library, packaged as a jar, and its dependencies declared externally:
              - Incompatible because this component declares a component for use during compile-time, compatible with Java 17 and the consumer needed a component for use during runtime, compatible with Java 8
```