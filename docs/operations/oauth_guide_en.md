**Note: This file is automatically translated!**

# Use CAS as OAuth provider

CAS provides OAuth as a protocol for authentication including SSO/SSL.
The following describes the specification of the OAuth protocol in CAS.

## Create OAuth Service Account for a Dogu

In order for a Dogu to use the CAS's OAuth endpoints, it must log in to the CAS as an OAuth client.
To do this, the request for a CAS service account can be stored in the `dogu.json` of the dogu in question.

``` json
"ServiceAccounts": [
   {
      "type": cas
   }
]
```

The credentials of the service account are randomly generated (see [create-sa.sh](../resources/create-sa.sh)) 
and stored encrypted in etcd under the path `/config/<dogu>/sa-cas/oauth_client_id` and `/config/<dogu>/sa-cas/oauth_client_secret`.
The credentials are composed of the `CLIENT_ID` and the `CLIENT_SECRET`. 
For the CAS, the `CLIENT_SECRET` is stored as a hash in the __etcd__ under the path `/config/cas/service_accounts/<CLIENT_ID>`.

### OAuth Endpoints and Flow

The following steps describe a successful OAuth authentication flow. 

1. request a short-term token: [authorize-endpoint](oauth/endpoint_authorize.md)
2. swap short-term token for long-term token: [AccessToken endpoint](oauth/endpoint_accessToken.md).    
3. long term token can now be used to authenticate against resources. 
   Currently CAS only offers user's profile as resource: [profile endpoint](oauth/endpoint_profile.md)

![CesServiceFactory](figures/sequence_diagramm_oauth.png)