**Note: This file is automatically translated!**

# Access token

This endpoint is used to exchange a short term token (`code`) for a long term token (`access_token`).

**URL** : `<fqdn>/api/accessToken`

**Method** : GET

**Data constraints**

```
?grant_type = athorization_code
?code = Valid code from `authorize` endpoint
?client_id = Valid clientID from the dogu
?client_secret = Valid secret from the dogu
?redirect_url = <URL to which the long term token of successful authentication is sent>
```

**Data example**

```
?grant_type = athorization_code
?code = ST-1-wzG237MUOvfjfZrvRH5s-cas.ces.local
?client_id = portainer
?client_secret = sPJtcNrmROZ3sZu3
?redirect_url = https://local.cloudogu.com/portainer/
```

**Call example**

```
https://local.cloudogu.com/cas/oauth2.0/accessToken?grant_type=authorization_code&code=ST-1-wzG237MUOvfjfZrvRH5s-cas.ces.local&client_id=portainer&client_secret=sPJtcNrmROZ3sZu3&redirect_uri=https%3A%2F%2Flocal.cloudogu.com%2Fportainer%2F
```

## Successful response

**Status:** 200 OK

**Example response:**

``` json
{
    "access_token": "TGT-1-m2gUNJwEqXyV7aAEXekihcVnFc5iI4mpfdZGOTSiiHzEbwr1cr-cas.ces.local",
    "expires_in": "7196",
    "token_type": "bearer"
}
``` 

## Unsuccessful response

**Error:** The short-term token is invalid or has already expired.

**Status:** 500 OK

**Example response:**

``` json
{
    "message": "invalid_grant"
}
```