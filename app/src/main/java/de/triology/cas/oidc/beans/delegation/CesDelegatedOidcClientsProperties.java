package de.triology.cas.oidc.beans.delegation;

import lombok.Data;
import java.io.Serializable;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple configuration holder for a single OIDC delegated client.
 */
@Data
public class CesDelegatedOidcClientsProperties implements Serializable {
    private List<CesDelegatedOidcClientProperties> clients = new ArrayList<>();
}
