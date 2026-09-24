package de.triology.cas.pat.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.apereo.cas.authentication.Credential;
import org.apereo.cas.authentication.credential.UsernamePasswordCredential;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.LinkedMultiValueMap;

class PATRestHttpRequestCredentialFactoryTest {
    private final PATRestHttpRequestCredentialFactory factory = new PATRestHttpRequestCredentialFactory();

    @Test
    void createsPatCredentialOnlyForTgtCreationEndpoint() throws Throwable {
        MockHttpServletRequest request = request("POST", "/cas/v1/tickets");

        List<Credential> credentials = factory.fromRequest(request, body("alice", "pat_secret"));

        PATCredential credential = assertInstanceOf(PATCredential.class, credentials.getFirst());
        assertEquals("alice", credential.getUsername());
        assertEquals("pat_secret", credential.toPassword());
    }

    @Test
    void keepsNormalPasswordsAsUsernamePasswordCredentials() throws Throwable {
        List<Credential> credentials = factory.fromRequest(
                request("POST", "/cas/v1/tickets"), body("alice", "normal-password"));

        assertInstanceOf(UsernamePasswordCredential.class, credentials.getFirst());
        assertTrue(!(credentials.getFirst() instanceof PATCredential));
    }

    @Test
    void doesNotLetPatFallThroughAtOtherRestEndpoints() throws Throwable {
        List<Credential> credentials = factory.fromRequest(
                request("POST", "/cas/v1/tickets/TGT-123"), body("alice", "pat_secret"));

        assertTrue(credentials.isEmpty());
    }

    private static MockHttpServletRequest request(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setContextPath("/cas");
        request.setServletPath(uri.substring("/cas".length()));
        return request;
    }

    private static LinkedMultiValueMap<String, String> body(String username, String password) {
        LinkedMultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("username", username);
        body.add("password", password);
        return body;
    }
}
