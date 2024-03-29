package de.triology.cas.services;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public class CesServiceManagerConfiguration {
    private final String stage;
    private final List<String> allowedAttributes;
    private final Map<String, String> attributesMappingRules;
    private final boolean oidcAuthenticationDelegationEnabled;
    private final String oidcClientDisplayName;
    private final String oidcPrincipalsAttribute;
}