# Use of values from the etcd in standalone app

The legal URLs for the masthead, terms of use, and privacy policy, as well as the text that will be
displayed when the password-forget button is clicked are defined in the `etcd` of the CES. When starting the
Dogus, the values are read from the etcd and set accordingly.

If the CAS is built and started as a standalone app outside the CES, the URLs and the hint text for a
forgotten password can be specified in a property file. For this purpose, the directory
app/src/main/resources with the name `custom_messages.properties`. In this
file the following values can be defined:

- `legal.url.terms.of.service` - Defines the URL for the terms of service.
- `legal.url.imprint` - Defines the URL for the imprint
- `legal.url.privacy.policy` - Defines the URL for the privacy policy
- `forgot.password.text` - Defines the text that will be displayed when you click the password forget button

A sample `custom_messages.properties` looks like this:
```
legal.url.terms.of.service=https://www.itzbund.de/
legal.url.imprint=https://cloudogu.com/
legal.url.privacy.policy=https://www.triology.de/
forgot.password.text=Contact your admin
```

## Troubleshooting

### Java compiler level does not match the version of the installed Java project
CAS is a Gradle Project. Gradle does NOT use the currently configured SDK and needs to be configured separately. 
If IntelliJ reports an errror like *Current Java version 15 does not match required Java version 11* go to
"Settings" > "Build, Exceution, Deployment" > "Build Tools" > Gradle and select the correct SDK (currently Java 11).
![configure sdk for gradle](figures/gradle_java_sdk.png)
