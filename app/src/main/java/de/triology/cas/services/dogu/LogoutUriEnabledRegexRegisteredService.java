package de.triology.cas.services.dogu;

import org.apache.commons.lang.builder.ToStringBuilder;

import java.net.URI;

public class LogoutUriEnabledRegexRegisteredService extends org.jasig.cas.services.RegexRegisteredService {

    private URI logoutUri;

    public void setLogoutUri(URI logoutUri) {
        this.logoutUri = logoutUri;
    }

    public URI getLogoutUri() {
        return logoutUri;
    }

    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.appendSuper(super.toString());
        builder.append("logoutURI", getLogoutUri());
        return builder.toString();
    }

}
