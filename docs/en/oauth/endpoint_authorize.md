**Note: This file is automatically translated!**

# Authorize

This endpoint serves as the initial start of the OAuth authorization.
The Authorization endpoint is used to request a short-lived token from the CAS.

**URL** : `<fqdn>/api/authorize`

**Method** : `GET`

**Condition of the data**

```
?response_type = code
?client_id = Valid clientID from the dogu
?state = Any string
?redirect_url = <URL to which the short term token of successful authentication will be redirected>.
```

**Data example**

```
?response_type = code
?client_id = portainer
?state = b8c57125-9281-4b67-b857-1559cdfcdf31
?redirect_url = http://local.cloudogu.com/portainer/
```

**call example**

```
https://local.cloudogu.com/cas/oauth2.0/authorize?client_id=portainer&redirect_uri=http%3A%2F%2Flocal.cloudogu.com%2Fportainer%2F&response_type=code&state=b8c57125-9281-4b67-b857-1559cdfcdf31
```

## Successful response

Automatically redirects one to the CAS login screen.
After a successful login the `redirect_url` is passed with a `code` as GET parameter.

Example for `code`: `ST-1-wzG237MUOvfjfZrvRH5s-cas.ces.local`