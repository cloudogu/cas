# Remote Debugging in a Local CES

To test CAS behavior against a local CES instance, it is possible to analyze CAS via remote debugging.
The following section describes the steps required to enable remote debugging.

## Changes in the CAS Repository

To start CAS in debugging mode, the following changes must be made in the `resources` directory:
* add the entry `JPDA_OPTS="-agentlib:jdwp=transport=dt_socket,address=*:8000,server=y,suspend=n"` to `resources/opt/apache-tomcat/bin/setenv.sh`.
  This starts a debugging session on port 8000.
* change the entry `${CATALINA_SH} run` to `${CATALINA_SH} jpda run` in `resources/startup.sh`.

## Debugging in CES Multinode

To debug CAS in CES Multinode, all that is needed is to create a port forward for port 8000 to the CAS pod.
The port forward can be created with kubectl or with k9s.

```shell
kubectl port-forward cas-6d7b47cd7b-pqprr 8000:8000
```

The remote debugger can then connect to `localhost:8000`.
