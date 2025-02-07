package de.triology.cas.poc;

import org.apereo.cas.authentication.Credential;
import org.apereo.cas.authentication.CredentialMetadata;
import org.apereo.cas.authentication.metadata.BasicCredentialMetadata;


// TODO required?
public class PATCredential implements Credential {
    private final PersonalAccessToken personalAccessToken;

    public PATCredential(PersonalAccessToken personalAccessToken) {
        this.personalAccessToken = personalAccessToken;
    }
    public PersonalAccessToken getPersonalAccessToken() {
        return personalAccessToken;
    }

    @Override
    public String getId() {
        return getPersonalAccessToken().getToken();
    }

    @Override
    public CredentialMetadata getCredentialMetadata() {
        return new BasicCredentialMetadata();
    }
}
