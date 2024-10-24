{
  "@class" : "org.apereo.cas.services.CasRegisteredService",
  "id" : {{SERVICE_ID}},
  "templateName": "{{TEMPLATES}}",
  "properties" : {
    "@class" : "java.util.HashMap",
    "ServiceClass": {
        "@class": "org.apereo.cas.services.DefaultRegisteredServiceProperty",
        "values": [  "java.util.LinkedHashSet", [ "org.apereo.cas.services.CasRegisteredService" ] ]
    },
    "Fqdn": {
        "@class": "org.apereo.cas.services.DefaultRegisteredServiceProperty",
        "values": [  "java.util.LinkedHashSet", [ "{{FQDN}}" ] ]
    },
    "ServiceName": {
        "@class": "org.apereo.cas.services.DefaultRegisteredServiceProperty",
        "values": [  "java.util.LinkedHashSet", [ "{{SERVICE}}" ] ]
    },
    "LogoutUrl": {
        "@class": "org.apereo.cas.services.DefaultRegisteredServiceProperty",
        "values": [  "java.util.LinkedHashSet", [ "{{LOGOUT_URL}}" ] ]
    }
  }
}