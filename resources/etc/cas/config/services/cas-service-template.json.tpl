{
  "@class" : "org.apereo.cas.services.CasRegisteredService",
  "id" : "{{SERVICE_ID}}",
  "serviceId": "{{PATTERN}}",
  "name": "{{NAME}}",
  "evaluationOrder": 1,
  "proxyPolicy": {
    "@class": "org.apereo.cas.services.RegexMatchingRegisteredServiceProxyPolicy",
    "pattern": "{{PATTERN}}",
  },
  "attributeReleasePolicy": {
    "@class": "org.apereo.cas.services.ReturnAllAttributeReleasePolicy"
  },
  "accessStrategy": {
    "@class": "org.apereo.cas.services.DefaultRegisteredServiceAccessStrategy",
    "enabled": true,
    "ssoEnabled": true
  },
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