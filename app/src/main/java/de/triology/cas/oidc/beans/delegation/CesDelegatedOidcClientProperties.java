package de.triology.cas.oidc.beans.delegation;

import lombok.Data;
import java.io.Serializable;

/**
 * Simple configuration holder for a single OIDC delegated client.
 */
@Data
public class CesDelegatedOidcClientProperties implements Serializable {
    private String discoveryUri;
    private String clientId;
    private String clientSecret;
    private String clientName;
    private String preferredJwsAlgorithm;
    private String clientAuthenticationMethod;
}

