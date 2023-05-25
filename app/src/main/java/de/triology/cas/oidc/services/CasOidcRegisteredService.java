package de.triology.cas.oidc.services;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ObjectUtils;
import org.apereo.cas.services.*;
import org.apereo.cas.support.oauth.services.OAuthRegisteredService;

import java.util.LinkedHashSet;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@Getter
@Setter
public class CasOidcRegisteredService extends OidcRegisteredService implements CasModelRegisteredService {
    private RegisteredServiceProxyPolicy proxyPolicy = new RefuseRegisteredServiceProxyPolicy();
    private RegisteredServiceProxyTicketExpirationPolicy proxyTicketExpirationPolicy;
    private RegisteredServiceProxyGrantingTicketExpirationPolicy proxyGrantingTicketExpirationPolicy;
    private RegisteredServiceServiceTicketExpirationPolicy serviceTicketExpirationPolicy;
    private String redirectUrl;
    private Set<String> supportedProtocols = new LinkedHashSet<>(0);

    @Override
    public void initialize() {
        super.initialize();
        this.proxyPolicy = ObjectUtils.defaultIfNull(this.proxyPolicy, new RefuseRegisteredServiceProxyPolicy());
    }
}
