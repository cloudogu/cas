{
  "@class" : "org.apereo.cas.support.oauth.services.OAuthRegisteredService",
  "serviceId" : "https://((?i){{FQDN}})(:443)?/{{SERVICE}}(/.*)?",
  "name" : "{{SERVICE}}",
  "id" : {{SERVICE_ID}},
  "templateName": "{{TEMPLATES}}",
  "clientId": "{{SERVICE}}",
  "clientSecret": "{{CLIENT_SECRET_HASH}}",
  "bypassApprovalPrompt": true,
  "supportedResponseTypes": [ "java.util.HashSet", [ "code" ] ],
  "supportedGrantTypes": [ "java.util.HashSet", [ "authorization_code" ] ],
  "logoutType" : "BACK_CHANNEL",
  "properties" : {
    "@class" : "java.util.HashMap",
    "LogoutUrl": {
        "@class": "org.apereo.cas.services.DefaultRegisteredServiceProperty",
        "values": [  "java.util.LinkedHashSet", [ "{{LOGOUT_URL}}" ] ]
    },
    "ServiceClass": {
        "@class": "org.apereo.cas.services.DefaultRegisteredServiceProperty",
        "values": [  "java.util.LinkedHashSet", [ "org.apereo.cas.services.OidcRegisteredService" ] ]
    }
  }
}