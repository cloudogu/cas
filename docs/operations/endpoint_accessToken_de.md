# Access Token

Dieser Endpunkt dient zum Austausch eines Kurzzeittokens (`code`) gegen ein Langzeittoken (`access_token`).

**URL** : `<fqdn>/api/accessToken`

**Method** : `GET`

**Data constraints**

```
?grant_type    = athorization_code
?code          = Valider Code vom `authorize` Endpunkt
?client_id     = Valide ClientID von dem Dogu
?client_secret = Valides Secret von dem Dogu
?redirect_url  = <URL zu die der Langzeittoken erfolgreicher Authentifizierung geschickt wird>
```

**Data example**

```
?grant_type    = athorization_code
?code          = ST-1-wzG237MUOvfjfZrvRH5s-cas.ces.local
?client_id     = portainer
?client_secret = sPJtcNrmROZ3sZu3
?redirect_url  = https://local.cloudogu.com/portainer/
```

**Call example**

```
https://local.cloudogu.com/cas/oauth2.0/accessToken?grant_type=authorization_code&code=ST-1-wzG237MUOvfjfZrvRH5s-cas.ces.local&client_id=portainer&client_secret=sPJtcNrmROZ3sZu3&redirect_uri=https%3A%2F%2Flocal.cloudogu.com%2Fportainer%2F
```

## Erfolgreiche Antwort

**Status:** 200 OK

**Beispiel-Antwort:**

``` json
{
    "access_token": "TGT-1-m2gUNJwEqXyV7aAEXekihcVnFc5iI4mpfdZGOTSiiHzEbwr1cr-cas.ces.local",
    "expires_in": "7196",
    "token_type": "Bearer"
}
``` 

## Nicht-Erfolgreiche Antwort

**Fehler:** Der Kurzzeittoken ist invalid oder schon abgelaufen.

**Status:** 500 OK

**Beispiel-Antwort:**

``` json
{
    "message": "invalid_grant"
}
```