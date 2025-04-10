package de.triology.cas.services;

import org.apereo.cas.authentication.principal.Service;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CesServiceMatchingStrategyTest {

    @Test
    public void matchesOnePort() throws UnsupportedEncodingException{
        // given
        String serviceUrl = URLEncoder.encode("https://192.168.56.2/scm/api/v2/cas/auth/", StandardCharsets.UTF_8.name());
        Service service = new TestService(serviceUrl);
        String serviceToMatchUrl = URLEncoder.encode("https://192.168.56.2:443/scm/api/v2/cas/auth/", StandardCharsets.UTF_8.name());
        Service serviceToMatch = new TestService(serviceToMatchUrl);
        CesServiceMatchingStrategy strategy = new CesServiceMatchingStrategy();

        // when
        boolean result = strategy.matches(service, serviceToMatch);

        // then
        assertTrue(result);
    }

    @Test
    public void matchesBothPorts() throws UnsupportedEncodingException{
        // given
        String serviceUrl = URLEncoder.encode("https://192.168.56.1:80/scm/auth", StandardCharsets.UTF_8.name());
        Service service = new TestService(serviceUrl);
        String serviceToMatchUrl = URLEncoder.encode("https://192.168.56.1:443/scm/auth", StandardCharsets.UTF_8.name());
        Service serviceToMatch = new TestService(serviceToMatchUrl);
        CesServiceMatchingStrategy strategy = new CesServiceMatchingStrategy();

        // when
        boolean result = strategy.matches(service, serviceToMatch);

        // then
        assertTrue(result);
    }

    @Test
    public void matchesNoPorts() throws UnsupportedEncodingException{
        // given
        String serviceUrl = URLEncoder.encode("https://192.168.56.1/scm/auth", StandardCharsets.UTF_8.name());
        Service service = new TestService(serviceUrl);
        String serviceToMatchUrl = URLEncoder.encode("https://192.168.56.1/scm/auth", StandardCharsets.UTF_8.name());
        Service serviceToMatch = new TestService(serviceToMatchUrl);
        CesServiceMatchingStrategy strategy = new CesServiceMatchingStrategy();

        // when
        boolean result = strategy.matches(service, serviceToMatch);

        // then
        assertTrue(result);
    }
}

class TestService implements Service {

    private final String id;
    private String tenant;

    public TestService(String id) {
        this.id = id;
    }

    @Override
    public void setAttributes(Map<String,Object> attributes) {

    }

    @Override
    public String getOriginalUrl() {
        return "";
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    @Override
    public String getTenant() {
        return this.tenant;
    }
}