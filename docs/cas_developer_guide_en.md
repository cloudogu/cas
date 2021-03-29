**Note: This file is automatically translated!**

# Developments on the CAS
The CAS (Central Authentification Service) is a Java application that plays the role of an SSO in the Cloudogu ecosystem.
The CAS implements a client for the various CAS libraries of jasig.
The currently used Java JDK is 1.8 (see [pom.xml](../app/pom.xml)).

The source code of the CAS is located in the `app/src/main/java` directory.

## Tests
The tests of the CAS are located in the directory `app/src/test/java`. Tests are done with [JUnit4](https://junit.org/junit5/docs/current/user-guide/#writing-tests) in connection with [Mockito](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html). The directory structure is that of the source code.

## Logging
The loglevel of the CAS is set by the parameter `"logging/root"`dogu.json.
Since Issue [#64](https://github.com/cloudogu/cas/pull/66) the log level for translation strings is also controlled by `"logging/translation_messages"`.
`"logging/translation_messages"` is set to `Error` by default.