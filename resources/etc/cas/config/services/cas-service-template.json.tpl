{
  "@class" : "org.apereo.cas.services.CasRegisteredService",
  "serviceId" : "https://((?i){{FQDN}})(:443)?/{{SERVICE}}(/.*)?",
  "name" : "{{SERVICE}}",
  "id" : {{SERVICE_ID}},
  "templateName": "{{TEMPLATES}}",
  "attributeReleasePolicy": {
      "@class" : "org.apereo.cas.services.ReturnAllowedAttributeReleasePolicy",
      "allowedAttributes" : [ "java.util.ArrayList", [ "username", "cn", "mail", "givenName", "surname", "displayName", "groups" ] ]
  },
  "logoutType" : "BACK_CHANNEL",
  "properties" : {
    "@class" : "java.util.HashMap",
    "LogoutUrl": {
        "@class": "org.apereo.cas.services.DefaultRegisteredServiceProperty",
        "values": [  "java.util.LinkedHashSet", [ "{{LOGOUT_URL}}" ] ]
    }
  }
}