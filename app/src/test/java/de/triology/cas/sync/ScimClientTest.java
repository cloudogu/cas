package de.triology.cas.sync;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.containsString;

class ScimClientTest {
    @Test
    void loadsMembersFromEveryRequestedPageAndKeepsScimId() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(containsString("/Groups/group-a?attributes=members&startIndex=1&count=2")))
                .andRespond(withSuccess("""
                        {"members":[{"value":"scim-1","display":"alice"},{"value":"scim-2","display":"bob"}],"totalResults":3}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("/Groups/group-a?attributes=members&startIndex=3&count=2")))
                .andRespond(withSuccess("""
                        {"members":[{"value":"scim-3","display":"carol"}],"totalResults":3}
                        """, MediaType.APPLICATION_JSON));

        OidcLdapSyncProperties.Scim properties = scimProperties(2);
        ScimClient client = new ScimClient(builder.build(), RestClient.builder().build(), properties, "display");

        Set<ScimClient.GroupMember> members = client.getGroupMembers("group-a", deadline());

        assertEquals(Set.of(
                new ScimClient.GroupMember("scim-1", "alice"),
                new ScimClient.GroupMember("scim-2", "bob"),
                new ScimClient.GroupMember("scim-3", "carol")), members);
        server.verify();
    }

    @Test
    void doesNotRetryBadRequest() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(containsString("/Groups/missing-group?")))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));
        ScimClient client = new ScimClient(builder.build(), RestClient.builder().build(), scimProperties(100), "value");

        assertThrows(IllegalStateException.class, () -> client.getGroupMembers("missing-group", deadline()));

        server.verify();
    }

    @Test
    void retriesRateLimitResponseUsingRetryAfterHeader() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(containsString("/Groups/group-a?")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "0"));
        server.expect(requestTo(containsString("/Groups/group-a?")))
                .andRespond(withSuccess("{\"members\":[]}", MediaType.APPLICATION_JSON));
        ScimClient client = new ScimClient(builder.build(), RestClient.builder().build(), scimProperties(100), "value");

        assertEquals(Set.of(), client.getGroupMembers("group-a", deadline()));

        server.verify();
    }

    @Test
    void parsesRetryAfterAsDeltaSecondsOrHttpDate() {
        assertEquals(Duration.ZERO, ScimClient.parseRetryAfter("0"));
        org.junit.jupiter.api.Assertions.assertNotNull(
                ScimClient.parseRetryAfter("Wed, 21 Oct 2037 07:28:00 GMT"));
    }

    private static OidcLdapSyncProperties.Scim scimProperties(int pageSize) {
        OidcLdapSyncProperties.Scim properties = new OidcLdapSyncProperties.Scim();
        properties.setBaseUrl("https://scim.example.test");
        properties.setUsername("client");
        properties.setPassword("secret");
        properties.setAuthenticationMethod("basic");
        properties.setPageSize(pageSize);
        return properties;
    }

    private static long deadline() {
        return System.nanoTime() + Duration.ofSeconds(10).toNanos();
    }
}
