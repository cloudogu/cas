# Entwicklungen die den CAS Benutzen

TODO Generelle Benutzung des CAS bei der Entwicklung von anderen Dogus.

## OAuth


Das ein Service Account angelegt werden soll kann in der `dogu.json` des betreffenden Dogus hinterlegt werden.
``` json
"ServiceAccounts": [
    {
        "Type": "cas"
    }
]
```

Die Credentials der Service Accounts werden (siehe [create-sa.sh](../resources/create-sa.sh)) gehasht und im etcd hinterlegt.
Die credentials setzten sich aus der `CLIENT_ID` und dem `CLIENT_SECRET` zusammen. Die `CLIENT_ID`ist der Name des aktuellen Services. Möchten wir also in unserer CES Instanz für den Portainer Dogu OAuth aktivieren, so lautet die ClientID schlicht "portainer".

Im __etcd__ werden `CLIENT_ID` und dem `CLIENT_SECRET` im Pfad `/config/cas/service_accounts/<CLIENT_ID>` abgelegt.

### OAuth Ablauf

1. Anfordern eines kurzzeit Tokens mit Hilfe eines validen Logins beim CAS.
2. Kurzzeittoken gegen ein Langzeittoken tauschen.
   ```
   grant_type: athorization_code
   code: <code aus url-parameter>
   client_id: <client id. Bspw. "portainer">
   client_secret: <client secret>
   redirect_uri: <URL auf die nach erfolgreicher Authentifizierung zugegriffen werden soll>
   ```
    Bei erfolgreichem Tokentausch leifert CAS einen Response im JSON Format zurück der wie folgt aufgebaut ist:
   ``` json
   {
        "access_token": "TGT-1-mlso2-...",
        "expires": "7196",
        "token_type": "Bearer"
   }
   ``` 
    
3. Langzeittoken kann nun zu Authentifizierung gegen Ressourcen benutzen werden. Bspw. um auf das Profil zuzugreifen (https://local.cloudogu.com/cas/oauth2.0/profile).
   Dazu muss nur der Token aus dem vorherigen Schritt im Request mit übergeben werden.
``` json
   {
        "access_token": "TGT-1-mlso2-..."
   }
   ``` 
![Ablauf OAuth](http://www.plantuml.com/plantuml/png/bP31JYCn38RlUGfBUsvtLQcxIoMWGqjFbK225qWJJnin4mVRQH5FJsPADQqu84wExF___SLpKSkQsyQaKeCBoKQ5HHCmwfAs6NxzNTnFLzFBSVLK-fEh_wlAUAjsOmM1KIYpmswogkx-6NEMChfv6mJcz2hjyrMp8l61qIaemKELTGThsePucTIlxxIl6QMNOlI9GbGWMpoJFx-xGXpUqHJjboqsoW4P3g7atphoU3qUZo5PjYqgSfdxWIHp0oQI8ZHOwmnqXz1v80ZiRHCyrCGm1K57qSlFKPr34QKNZ3tHCRncQY4XxmDbEVc__SmnwxdBkkGORZ_x50sTtgcznpYRD524zR9wXDQcGb248wQiAB09qPzO17Hd5ImZI48Nwk10gRzERvR2Zcpc67rkcmy0)