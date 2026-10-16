package de.triology.cas.sync;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Set;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentLinkedQueue;
import com.sun.net.httpserver.HttpServer;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
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

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"members\":null}", "{\"members\":[]}", "{\"members\":[],\"totalResults\":0}"})
    void acceptsEmptyGroups(String response) {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(containsString("/Groups/group-a?")))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Basic Y2xpZW50OnNlY3JldA=="))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        var client = new ScimClient(builder.build(), RestClient.builder().build(), scimProperties(100), "display");

        assertEquals(Set.of(), client.getGroupMembers("group-a", deadline()));
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "null", "[]", "{\"members\":{}}",
            "{\"members\":[{\"value\":\"1\"}]}",
            "{\"members\":[{\"value\":\"1\",\"display\":null}]}",
            "{\"members\":[{\"value\":\"1\",\"display\":\" \"}]}",
            "{\"members\":[{\"display\":\"alice\"}]}",
            "{\"members\":[{\"value\":null,\"display\":\"alice\"}]}",
            "{\"members\":[{\"value\":\" \",\"display\":\"alice\"}]}",
            "{\"members\":[],\"totalResults\":-1}",
            "{\"members\":[],\"totalResults\":1}",
            "{\"members\":[{\"value\":\"1\",\"display\":\"alice\"},{\"value\":\"2\",\"display\":\"alice\"}]}"
    })
    void rejectsIncompleteOrInconsistentGroups(String response) {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(containsString("/Groups/group-a?")))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        var client = new ScimClient(builder.build(), RestClient.builder().build(), scimProperties(100), "display");

        assertThrows(IllegalStateException.class, () -> client.getGroupMembers("group-a", deadline()));
        server.verify();
    }

    @Test
    void rejectsDuplicateIdentitiesAcrossPages() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(containsString("startIndex=1"))).andRespond(withSuccess(
                "{\"members\":[{\"value\":\"1\",\"display\":\"alice\"}],\"totalResults\":2}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("startIndex=2"))).andRespond(withSuccess(
                "{\"members\":[{\"value\":\"2\",\"display\":\"alice\"}],\"totalResults\":2}", MediaType.APPLICATION_JSON));
        var client = new ScimClient(builder.build(), RestClient.builder().build(), scimProperties(1), "display");

        var failure = assertThrows(IllegalStateException.class, () -> client.getGroupMembers("group-a", deadline()));
        assertTrue(failure.getMessage().contains("duplicate member identity"));
        server.verify();
    }

    @Test
    void acceptsMembersWithoutPaginationMetadata() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(containsString("/Groups/group-a?"))).andRespond(withSuccess(
                "{\"members\":[{\"value\":\"1\",\"display\":\"alice\"}]}", MediaType.APPLICATION_JSON));
        var client = new ScimClient(builder.build(), RestClient.builder().build(), scimProperties(100), "display");

        assertEquals(Set.of(new ScimClient.GroupMember("1", "alice")), client.getGroupMembers("group-a", deadline()));
        server.verify();
    }

    @Test
    void stopsAfterThreeServerErrorsAndIncludesResponseBody() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(org.springframework.test.web.client.ExpectedCount.times(3), requestTo(containsString("/Groups/group-a?")))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE).header("Retry-After", "0")
                        .body("SCIM unavailable"));
        var client = new ScimClient(builder.build(), RestClient.builder().build(), scimProperties(100), "display");

        var failure = assertThrows(IllegalStateException.class, () -> client.getGroupMembers("group-a", deadline()));
        assertTrue(failure.getMessage().contains("SCIM unavailable"));
        server.verify();
    }

    @Test
    void boundsRetriesForTransportFailures() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(org.springframework.test.web.client.ExpectedCount.times(3), requestTo(containsString("/Groups/group-a?")))
                .andRespond(withException(new IOException("connection reset")));
        var client = new ScimClient(builder.build(), RestClient.builder().build(), scimProperties(100), "display");

        var failure = assertThrows(IllegalStateException.class, () -> client.getGroupMembers("group-a", deadline()));
        assertEquals("SCIM request failed for group group-a", failure.getMessage());
        assertNotNull(failure.getCause());
        server.verify();
    }

    @Test
    void rejectsExpiredDeadlineBeforeMakingRequests() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var client = new ScimClient(builder.build(), RestClient.builder().build(), scimProperties(100), "display");

        var failure = assertThrows(IllegalStateException.class,
                () -> client.getGroupMembers("group-a", System.nanoTime() - 1));
        assertEquals("SCIM synchronization timeout exceeded", failure.getMessage());
        server.verify();
    }

    @Test
    void preservesInterruptAndStopsRetrying() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(containsString("/Groups/group-a?")))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        var client = new ScimClient(builder.build(), RestClient.builder().build(), scimProperties(100), "display");

        try {
            Thread.currentThread().interrupt();
            var failure = assertThrows(IllegalStateException.class, () -> client.getGroupMembers("group-a", deadline()));
            assertEquals("SCIM request interrupted", failure.getMessage());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
        server.verify();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "digest"})
    void rejectsUnsupportedAuthentication(String method) {
        var properties = scimProperties(100);
        properties.setAuthenticationMethod(method);
        assertThrows(IllegalStateException.class,
                () -> new ScimClient(RestClient.builder().build(), RestClient.builder().build(), properties, "display"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void requiresTokenUriForBearerAuthentication(String tokenUri) {
        var properties = scimProperties(100);
        properties.setAuthenticationMethod("bearer");
        properties.setTokenUri(tokenUri);
        assertThrows(IllegalStateException.class,
                () -> new ScimClient(RestClient.builder().build(), RestClient.builder().build(), properties, "display"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "invalid"})
    void ignoresMissingOrInvalidRetryAfter(String value) {
        assertNull(ScimClient.parseRetryAfter(value));
    }

    @Test
    void clampsNegativeRetryAfterAndPastDatesToZero() {
        assertEquals(Duration.ZERO, ScimClient.parseRetryAfter(" -5 "));
        assertEquals(Duration.ZERO, ScimClient.parseRetryAfter("Wed, 21 Oct 2015 07:28:00 GMT"));
    }

    @Test
    void cachesBearerTokenAndSendsClientCredentialsForm() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var tokenBuilder = RestClient.builder();
        var tokenServer = MockRestServiceServer.bindTo(tokenBuilder).build();
        var properties = bearerProperties();
        tokenServer.expect(requestTo(properties.getTokenUri()))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Basic Y2xpZW50OnNlY3JldA=="))
                .andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string("grant_type=client_credentials"))
                .andRespond(withSuccess("{\"access_token\":\"cached-token\"}", MediaType.APPLICATION_JSON));
        server.expect(org.springframework.test.web.client.ExpectedCount.times(2), requestTo(containsString("/Groups/group-a?")))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer cached-token"))
                .andRespond(withSuccess("{\"members\":[]}", MediaType.APPLICATION_JSON));
        var client = new ScimClient(builder.build(), tokenBuilder.build(), properties, "display");

        assertEquals(Set.of(), client.getGroupMembers("group-a", deadline()));
        assertEquals(Set.of(), client.getGroupMembers("group-a", deadline()));
        tokenServer.verify();
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "{}", "{\"access_token\":\"\"}"})
    void refusesInvalidTokenResponses(String response) {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var tokenBuilder = RestClient.builder();
        var tokenServer = MockRestServiceServer.bindTo(tokenBuilder).build();
        var properties = bearerProperties();
        tokenServer.expect(org.springframework.test.web.client.ExpectedCount.times(3), requestTo(properties.getTokenUri()))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        var client = new ScimClient(builder.build(), tokenBuilder.build(), properties, "display");

        var failure = assertThrows(IllegalStateException.class, () -> client.getGroupMembers("group-a", deadline()));
        assertTrue(failure.getCause().getMessage().contains("access_token"));
        tokenServer.verify();
        server.verify();
    }

    @Test
    void retriesTokenEndpointErrorsWithoutCallingScim() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var tokenBuilder = RestClient.builder();
        var tokenServer = MockRestServiceServer.bindTo(tokenBuilder).build();
        var properties = bearerProperties();
        tokenServer.expect(org.springframework.test.web.client.ExpectedCount.times(3), requestTo(properties.getTokenUri()))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE).header("Retry-After", "0").body("token service unavailable"));
        var client = new ScimClient(builder.build(), tokenBuilder.build(), properties, "display");

        var failure = assertThrows(IllegalStateException.class, () -> client.getGroupMembers("group-a", deadline()));
        assertTrue(failure.getMessage().contains("token service unavailable"));
        tokenServer.verify();
        server.verify();
    }

    @Test
    void sharesOneTokenAcrossConcurrentRequests() throws Exception {
        var tokensRequested = new AtomicInteger();
        var authorizations = new ConcurrentLinkedQueue<String>();
        var tokenRequested = new CountDownLatch(1);
        var releaseToken = new CountDownLatch(1);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var serverExecutor = Executors.newCachedThreadPool();
        server.setExecutor(serverExecutor);
        server.createContext("/token", exchange -> {
            int number = tokensRequested.incrementAndGet();
            tokenRequested.countDown();
            try {
                if (!releaseToken.await(5, TimeUnit.SECONDS)) {
                    throw new IOException("token response was not released");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException(e);
            }
            byte[] body = ("{\"access_token\":\"token-" + number + "\",\"expires_in\":3600}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.createContext("/Groups", exchange -> {
            authorizations.add(exchange.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION));
            byte[] body = "{\"members\":[]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/scim+json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
        try (var workers = Executors.newFixedThreadPool(8)) {
            var properties = bearerProperties();
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
            properties.setBaseUrl(baseUrl);
            properties.setTokenUri(baseUrl + "/token");
            var client = new ScimClient(RestClient.builder(), properties, "display");
            var start = new CountDownLatch(1);
            var ready = new CountDownLatch(8);
            var results = new ArrayList<java.util.concurrent.Future<Set<ScimClient.GroupMember>>>();
            for (int i = 0; i < 8; i++) {
                results.add(workers.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("concurrent requests did not start");
                    }
                    return client.getGroupMembers("group-a", deadline());
                }));
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            assertTrue(tokenRequested.await(5, TimeUnit.SECONDS));
            releaseToken.countDown();
            for (var result : results) {
                assertEquals(Set.of(), result.get(5, TimeUnit.SECONDS));
            }
            assertEquals(1, tokensRequested.get());
            assertEquals(8, authorizations.size());
            assertEquals(Set.of("Bearer token-1"), Set.copyOf(authorizations));

        } finally {
            releaseToken.countDown();
            server.stop(0);
            serverExecutor.shutdownNow();
        }
    }

    @Test
    void refreshesTokenAfterItsExpiration() throws InterruptedException {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var tokenBuilder = RestClient.builder();
        var tokenServer = MockRestServiceServer.bindTo(tokenBuilder).build();
        var properties = bearerProperties();
        tokenServer.expect(requestTo(properties.getTokenUri())).andRespond(withSuccess(
                "{\"access_token\":\"first\",\"expires_in\":0}", MediaType.APPLICATION_JSON));
        tokenServer.expect(requestTo(properties.getTokenUri())).andRespond(withSuccess(
                "{\"access_token\":\"second\",\"expires_in\":3600}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("/Groups/group-a?")))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer first"))
                .andRespond(withSuccess("{\"members\":[]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("/Groups/group-a?")))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer second"))
                .andRespond(withSuccess("{\"members\":[]}", MediaType.APPLICATION_JSON));
        var client = new ScimClient(builder.build(), tokenBuilder.build(), properties, "display");

        assertEquals(Set.of(), client.getGroupMembers("group-a", deadline()));
        Thread.sleep(1100);
        assertEquals(Set.of(), client.getGroupMembers("group-a", deadline()));

        tokenServer.verify();
        server.verify();
    }

    private static OidcLdapSyncProperties.Scim bearerProperties() {
        var properties = scimProperties(100);
        properties.setAuthenticationMethod("bearer");
        properties.setTokenUri("https://scim.example.test/token");
        return properties;
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
