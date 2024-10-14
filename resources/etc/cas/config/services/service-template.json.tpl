{
  "@class" : "${ServiceClass}",
  "serviceId" : "https://((?i){{FQDN}})(:443)?/{{SERVICE}}(/.*)?",
  "name" : "{{SERVICE}}",
  "id" : {{SERVICE_ID}},
  "templateName": "{{TEMPLATES}}",
  "logoutType" : "BACK_CHANNEL",
  "properties" : {
    "@class" : "java.util.HashMap",
    "LogoutUrl": {
        "@class": "org.apereo.cas.services.DefaultRegisteredServiceProperty",
        "values": [  "java.util.LinkedHashSet", [ "{{LOGOUT_URL}}" ] ]
    },
    "ServiceClass": {
        "@class": "org.apereo.cas.services.DefaultRegisteredServiceProperty",
        "values": [  "java.util.LinkedHashSet", [ "{{SERVICE_CLASS}}" ] ]
    }
  }
}