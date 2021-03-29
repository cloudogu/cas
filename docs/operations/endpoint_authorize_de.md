# Authorize

Dieser Endpunkt dient als initialer Start der OAuth-Authorisation.
Der Authorisation-Endpunkt wird benutzt um ein kurzlebiges Token vom CAS anzufordern.

**URL** : `<fqdn>/api/authorize`

**Method** : `GET`

**Bedingung der Daten**

```
?response_type = code
?client_id     = Valide ClientID von dem Dogu
?state         = Irgendeine Zeichenkette
?redirect_url  = <URL zu die der Kurzzeittoken erfolgreicher Authentifizierung weitergeleitet wird>
```

**Daten-Beispiel**

```
?response_type = code
?client_id     = portainer
?state         = b8c57125-9281-4b67-b857-1559cdfcdf31
?redirect_url  = http://local.cloudogu.com/portainer/
```

**Aufruf Beispiel**

```
https://local.cloudogu.com/cas/oauth2.0/authorize?client_id=portainer&redirect_uri=http%3A%2F%2Flocal.cloudogu.com%2Fportainer%2F&response_type=code&state=b8c57125-9281-4b67-b857-1559cdfcdf31
```

## Erfolgreiche Antwort

Leitet einen automatisch zur CAS-Login Maske. 
Nach erfolgreichem Login wird die `redirect_url` mit einem `code` als GET-Parameter übergeben.

Beispiel für `code`: `ST-1-wzG237MUOvfjfZrvRH5s-cas.ces.local`