**Note: This file is automatically translated!**

# Profile

This endpoint is used to retrieve the user profile from the logged in user using a long term token (`access_token`).

**URL** : `<fqdn>/api/profile`

**Method** : GET

**Request header**

```
authorization = Valid Access Token as Bearer
```

**Request header example**

```
authorization: Bearer TGT-1-m2gUNJwEqXyV7aAEXekihcVnFc5iI4mpfdZGOTSiiHzEbwr1cr-cas.ces.local
```

## Successful response

**Status:** 200 OK

**Example response:**

``` json
{
  "id": "cesadmin",
  "attributes": [
    {
      "username": "cesadmin"
    },
    {
      "cn": "admin"
    },
    {
      "mail": "cesadmin@localhost.de"
    },
    {
      "givenName": "admin"
    },
    {
      "surname": "admin"
    },
    {
      "displayName": "admin"
    },
    {
      "groups": [
        "cesManager",
        "cesadmin"
      ]
    }
  ]
}
``` 

## Unsuccessful response

**Error:** The long-term token is invalid or has already expired.

**Status:** 500 OK

**Example response:**

``` json
{
    "message": "expired_accessToken"
}
```