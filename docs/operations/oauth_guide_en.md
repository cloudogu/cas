# Use CAS as OAuth/OIDC Provider

CAS provides OAuth/OIDC as a protocol for authentication along with SSO/SSL.
The following describes the specification of OAuth protocol in CAS.

## Create OAuth/OIDC Service Account for Dogu

In order for a Dogu to use the CAS's OAuth/OIDC endpoints, it must log in to the CAS as a client.
To do this, the request for a CAS service account can be stored in the `dogu.json` of the dogu in question.

**entry for an OAuth client:**
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

**entry for an OIDC client:**
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

The service account credentials are randomly generated (see [create-sa.sh](https://github.com/cloudogu/cas/blob/develop/resources/create-sa.sh))
and stored encrypted in etcd under the path `/config/<dogu>/sa-cas/<oauth|oidc>_client_id` and `/config/<dogu>/sa-cas/<oauth|oidc>_client_secret`.
The credentials are composed of the `CLIENT_ID` and the `CLIENT_SECRET`.
For the CAS, the `CLIENT_SECRET` is stored as a hash in the __etcd__ under the path `/config/cas/service_accounts/<oidc|oauth>/<CLIENT_ID>`.

### OAuth endpoints and flow

The following steps describe a successful OAuth authentication flow.

1. request a short-term token: [authorize-endpoint](endpoint_authorize_en.md)
2. swap short-term token for long-term token: [AccessToken endpoint](endpoint_accessToken_en.md)
3. long term token can now be used to authenticate against resources.
   Currently CAS offers only user's profile as resource: [profile endpoint](endpoint_profile_en.md)

![CesServiceFactory](figures/sequenzediagramm_oauth.png)
