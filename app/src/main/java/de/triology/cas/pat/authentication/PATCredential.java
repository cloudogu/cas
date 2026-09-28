package de.triology.cas.pat.authentication;

import org.apereo.cas.authentication.credential.UsernamePasswordCredential;

/**
 * Credential used exclusively for personal access token authentication.
 *
 * <p>The token is kept in the inherited password field so that CAS can handle
 * the credential like its other username/password credentials without making
 * a second copy of the secret.</p>
 */
public class PATCredential extends UsernamePasswordCredential {
    public PATCredential(String username, String token) {
        super(username, token);
    }
}
