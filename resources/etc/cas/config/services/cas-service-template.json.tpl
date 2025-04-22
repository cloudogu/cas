{
  "@class" : "org.apereo.cas.services.CasRegisteredService",
  "id" : {{SERVICE_ID}},
  "serviceId": "^https://{{FQDN}}/{{SERVICE}}/.*$",
  "name": {{SERVICE}},
  "templateName": "{{TEMPLATES}}",
  "evaluationOrder": 1,
  "proxyPolicy": {
    "@class": "org.apereo.cas.services.RegexMatchingRegisteredServiceProxyPolicy",
    "pattern": "^https://{{FQDN}}/{{SERVICE}}/.*$",
  }
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