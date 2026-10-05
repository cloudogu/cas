# Remote Debugging im lokalen CES

Um Verhalten des CAS gegenüber einer lokalen CES Instanz zu testen, ist es möglich, den CAS über eine Remote Debugging
zu analysieren. Im folgenden Abschnitt werden die Schritte beschrieben, um das Remote Debugging zu ermöglichen. 

## Anpassungen im CAS Repository

Um den CAS im Debugging Modus zu starten, müssen folgenden Änderungen in `resources` Verzeichnis vorgenommen werden:
* `resources/opt/apache-tomcat/bin/setenv.sh` um den Eintrag `JPDA_OPTS="-agentlib:jdwp=transport=dt_socket,address=*:8000,server=y,suspend=n"` ergänzen.
  Hiermit wird eine Debugging Session auf Port 8000 gestartet.
* `resources/startup.sh` den Eintrag `${CATALINA_SH} run` zu `${CATALINA_SH} jpda run` ändern.

## Debug in CES-Multinode

Um den CAS in CES-Multinode zu debuggen muss lediglich ein Port-Forward für Port 8000 auf den CAS-Pod erstellt werden.
Der Port-Forward kann mit kubectl oder mit k9s erstellt werden.

```shell
kubectl port-forward cas-6d7b47cd7b-pqprr 8000:8000
```

Anschließend kann sich der Remote-Debugger auf mit `localhost:8000` verbinden.


