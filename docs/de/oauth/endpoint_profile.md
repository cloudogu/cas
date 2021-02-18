# Profil

Dieser Endpunkt dient zur Abfrage des Userprofil vom eingeloggten User mithilfe eines Langzeittoken (`access_token`).

**URL** : `<fqdn>/api/profile`

**Method** : `GET`

**Request-Header**

```
authorization = Valider Access Token als Bearer
```

**Request-Header Beispiel**

```
authorization: Bearer TGT-1-m2gUNJwEqXyV7aAEXekihcVnFc5iI4mpfdZGOTSiiHzEbwr1cr-cas.ces.local
```

## Erfolgreiche Antwort

**Status:** 200 OK

**Beispiel-Antwort:**

``` json
{
  "id": "cesadmin",
  "attributes": {
    "username": "cesadmin",
    "cn": "admin",
    "mail": "cesadmin@localhost.de",
    "givenName": "admin",
    "surname": "admin",
    "displayName": "admin",
    "groups": [
      "cesManager",
      "cesadmin"
    ]
  }
}
``` 

## Nicht-Erfolgreiche Antwort

**Fehler:** Der Langzeittoken ist invalid oder schon abgelaufen.

**Status:** 500 OK

**Beispiel-Antwort:**

``` json
{
    "message": "expired_accessToken"
}
```