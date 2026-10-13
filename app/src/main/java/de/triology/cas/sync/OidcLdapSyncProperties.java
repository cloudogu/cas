package de.triology.cas.sync;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Holds settings for the OIDC to LDAP synchronization. */
@Getter
@Setter
@ConfigurationProperties(prefix = "oidc.ldap-sync")
public class OidcLdapSyncProperties {
    private boolean enabled;
    private Duration interval;
    private Duration timeout;
    private Scim scim = new Scim();
    private Mapping mapping = new Mapping();
    private Ldap ldap = new Ldap();

    /** Holds settings for SCIM access and group lookup. */
    @Getter
    @Setter
    public static class Scim {
        private String baseUrl;
        private List<String> groupIds = new ArrayList<>();
        private String username;
        private String password;
        private String authenticationMethod;
        private String tokenUri;
        private int pageSize;
    }

    /** Maps identity attributes between SCIM and LDAP. */
    @Getter
    @Setter
    public static class Mapping {
        private String scimIdentityAttribute;
        private String ldapIdentityAttribute;
    }

    /** Selects LDAP users managed by the synchronization. */
    @Getter
    @Setter
    public static class Ldap {
        private String externalAttribute;
        private String externalValue;
    }
}
