**Note: This file is automatically translated!**

# Developments on the CAS
The CAS (Central Authentification Service) is a Java application that plays the role of an SSO in the Cloudogu ecosystem.
The CAS implements a client for the various CAS libraries of jasig.
The currently used Java JDK is 1.8 (see [pom.xml](https://github.com/cloudogu/cas/blob/develop/app/pom.xml)).

The source code of the CAS is located in the `app/src/main/java` directory.

## Tests
The tests of the CAS are located in the directory `app/src/test/java`. Tests are done with [JUnit4](https://junit.org/junit5/docs/current/user-guide/#writing-tests) in connection with [Mockito](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html). The directory structure is that of the source code.

## Logging
The loglevel of the CAS is set by the parameter `"logging/root"`dogu.json.

## Spring

With the CAS War Overlay upgrade to version 6.5.2, the `@Autowire` feature of Spring Boot can no longer be used as a
Field Injection. "Constructor Injection" should be used in all cases during development.
You could take a good look at the existing code in CAS. This was completely adapted for "Constructor Injection".

## Gradle

Gradle uses its own JDK VM. This may need to be configured appropriately in the IDE (currently Java 21) in order to avoid error messages like this one:

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