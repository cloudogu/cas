package de.triology.cas.services;

import java.net.URI;

public class LogoutUriEnabledRegexRegisteredService extends org.jasig.cas.services.RegexRegisteredService {

    private URI logoutUri;

    public void setLogoutUri(URI logoutUri) {
        this.logoutUri = logoutUri;
    }

    public URI getLogoutUri() {
        return logoutUri;
    }

}
