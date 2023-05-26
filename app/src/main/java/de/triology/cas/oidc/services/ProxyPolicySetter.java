package de.triology.cas.oidc.services;

import org.apereo.cas.services.CasModelRegisteredService;
import org.apereo.cas.services.RegisteredServiceProxyPolicy;

public interface ProxyPolicySetter {
    void setProxyPolicy(RegisteredServiceProxyPolicy policy);
}
