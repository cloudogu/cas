# CAS als OAuth/OIDC Provider benutzen

CAS bietet OAuth/OIDC als Protokoll zur Authentifizierung samt SSO/SSL an. 
Im Folgenden werden die Spezifikation des OAuth Protokolls in CAS beschrieben.

## OAuth/OIDC Service Account für Dogu erstellen

Damit ein Dogu die OAuth/OIDC-Endpunkte des CAS benutzen kann, muss sich dieser beim CAS als Client anmelden.
Dafür kann die Aufforderung eines CAS-Service Account in der `dogu.json` des betreffenden Dogus hinterlegt werden.

**Eintrag für einen OAuth Client:**
``` json
"ServiceAccounts": [
    {
        "Type": "cas",
        "Params": [
            "oauth"
        ]
    }
]
```

**Eintrag für einen OIDC Client:**
``` json
"ServiceAccounts": [
    {
        "Type": "cas",
        "Params": [
            "oidc"
        ]
    }
]
```

Die Credentials des Service Accounts werden zufällig generiert (siehe [create-sa.sh](https://github.com/cloudogu/cas/blob/develop/resources/create-sa.sh)) 
und verschlüsselt im etcd unter dem Pfad `/config/<dogu>/sa-cas/<oauth|oidc>_client_id` und `/config/<dogu>/sa-cas/<oauth|oidc>_client_secret` hinterlegt.
Die credentials setzten sich aus der `CLIENT_ID` und dem `CLIENT_SECRET` zusammen. 
Für den CAS wird das `CLIENT_SECRET` als Hash im __etcd__ unter dem Pfad `/config/cas/service_accounts/<oidc|oauth>/<CLIENT_ID>` abgelegt.

### OAuth Endpunkte und Ablauf

Die folgenden Schritte beschreiben einen erfolgreichen Ablauf der OAuth-Authentifizierung. 

1. Anfordern eines Kurzzeit-Tokens: [Authorize-Endpunkt](endpoint_authorize_de.md)
2. Kurzzeittoken gegen ein Langzeittoken tauschen: [AccessToken-Endpunkt](endpoint_accessToken_de.md)    
3. Langzeittoken kann nun zu Authentifizierung gegen Ressourcen benutzen werden. 
   Derzeit bietet CAS nur das Profil der User als Resource an: [Profil-Endpunkt](endpoint_profile_de.md)

![CesServiceFactory](figures/sequenzediagramm_oauth.png)
