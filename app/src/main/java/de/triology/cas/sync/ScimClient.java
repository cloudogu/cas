package de.triology.cas.sync;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpHeaders;
import tools.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Retrieves identities of members in configured SCIM groups. */
@Slf4j
public class ScimClient {
    private static final int MAX_ATTEMPTS = 3;
    private static final long TOKEN_REFRESH_SKEW_SECONDS = 30;

    private final RestClient restClient;
    private final RestClient tokenClient;
    private final OidcLdapSyncProperties.Scim properties;
    private final String memberIdentityAttribute;
    private volatile AccessToken accessToken;

    /** Builds an SCIM client and validates the configured authentication method. */
    public ScimClient(RestClient.Builder restClientBuilder,
                      OidcLdapSyncProperties.Scim properties,
                      String memberIdentityAttribute) {
        this(createScimClient(restClientBuilder, properties), createTokenClient(), properties,
                memberIdentityAttribute);
    }

    ScimClient(RestClient restClient, RestClient tokenClient,
               OidcLdapSyncProperties.Scim properties, String memberIdentityAttribute) {
        this.properties = properties;
        this.memberIdentityAttribute = memberIdentityAttribute;
        this.restClient = restClient;
        this.tokenClient = tokenClient;
        validateAuthenticationConfiguration();
    }

    private static RestClient createScimClient(RestClient.Builder restClientBuilder,
                                                OidcLdapSyncProperties.Scim properties) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(30));
        return restClientBuilder
                .requestFactory(requestFactory)
                .baseUrl(properties.getBaseUrl())
                .defaultHeaders(headers -> headers.setAccept(List.of(MediaType.valueOf("application/scim+json"), MediaType.APPLICATION_JSON)))
                .build();
    }

    private static RestClient createTokenClient() {
        var tokenRequestFactory = new SimpleClientHttpRequestFactory();
        tokenRequestFactory.setConnectTimeout(Duration.ofSeconds(10));
        tokenRequestFactory.setReadTimeout(Duration.ofSeconds(30));
        return RestClient.builder()
                .requestFactory(tokenRequestFactory)
                .build();
    }

    /** Retrieves all member identities for a group, following the SCIM pagination response. */
    public Set<GroupMember> getGroupMembers(String groupId, long deadlineNanos) {
        Set<GroupMember> result = new HashSet<>();
        Set<String> identities = new HashSet<>();
        int startIndex = 1;
        int totalResults;

        do {
            JsonNode group = getPage(groupId, startIndex, deadlineNanos);
            if (group == null || !group.isObject()) {
                throw new IllegalStateException("SCIM group response is incomplete for group " + groupId);
            }

            JsonNode members = group.get("members");
            if (members == null || members.isNull()) {
                LOGGER.debug("SCIM group {} contains no members; treating it as empty", groupId);
                return result;
            }

            if (!group.get("members").isArray()) {
                throw new IllegalStateException("SCIM group response contains invalid members data for group " + groupId);
            }

            for (JsonNode member : group.get("members")) {
                JsonNode identity = member.get(memberIdentityAttribute);
                if (identity == null || identity.isNull() || identity.asString().isBlank()) {
                    throw new IllegalStateException("SCIM group member has no configured identity attribute "
                            + memberIdentityAttribute);
                }
                JsonNode id = member.get("value");
                if (id == null || id.isNull() || id.asString().isBlank()) {
                    throw new IllegalStateException("SCIM group member has no SCIM ID (value)");
                }
                String identityValue = identity.asString();
                if (!identities.add(identityValue)) {
                    throw new IllegalStateException("SCIM pagination returned a duplicate member identity " + identityValue
                            + " for group " + groupId);
                }
                result.add(new GroupMember(id.asString(), identityValue));
            }

            int pageSize = group.get("members").size();
            if (!group.has("totalResults")) {
                break;
            }
            totalResults = group.get("totalResults").asInt(-1);
            if (totalResults < 0) {
                throw new IllegalStateException("SCIM group response contains invalid pagination data for group " + groupId);
            }
            if (pageSize == 0 && startIndex <= totalResults) {
                throw new IllegalStateException("SCIM pagination made no progress for group " + groupId);
            }
            startIndex += pageSize;
        } while (startIndex <= totalResults);

        LOGGER.debug("Loaded {} members from SCIM group {}", result.size(), groupId);
        return result;
    }

    /** Requests a group page with bounded retries. */
    private JsonNode getPage(String groupId, int startIndex, long deadlineNanos) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            ensureWithinDeadline(deadlineNanos);
            try {
                return restClient.get()
                        .uri(uriBuilder -> uriBuilder.path("/Groups/{groupId}")
                                .queryParam("attributes", "members")
                                .queryParam("startIndex", startIndex)
                                .queryParam("count", properties.getPageSize())
                                .build(groupId))
                        .headers(headers -> applyAuthentication(headers, deadlineNanos))
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, (_, errorResponse) -> {
                            String responseBody = new String(errorResponse.getBody().readAllBytes(), StandardCharsets.UTF_8);
                            throw new ScimHttpException(errorResponse.getStatusCode(),
                                    errorResponse.getHeaders().getFirst(HttpHeaders.RETRY_AFTER),
                                    "SCIM request returned HTTP " + errorResponse.getStatusCode()
                                            + " for group " + groupId + (responseBody.isBlank() ? "" : ": " + responseBody));
                        })
                        .body(JsonNode.class);
            } catch (ScimHttpException e) {
                lastFailure = e;
                if (e.statusCode().value() == 400 || attempt == MAX_ATTEMPTS) {
                    throw e;
                }
                sleepBeforeRetry(e.retryAfter(), attempt);
            } catch (RuntimeException e) {
                lastFailure = e;
                if (attempt < MAX_ATTEMPTS) {
                    sleepBeforeRetry(null, attempt);
                }
            }
        }
        throw new IllegalStateException("SCIM request failed for group " + groupId, lastFailure);
    }

    /** Adds the configured authentication credentials to an SCIM request. */
    private void applyAuthentication(org.springframework.http.HttpHeaders headers, long deadlineNanos) {
        if ("bearer".equalsIgnoreCase(properties.getAuthenticationMethod())) {
            headers.setBearerAuth(getAccessToken(deadlineNanos));
        } else {
            headers.setBasicAuth(properties.getUsername(), properties.getPassword());
        }
    }

    /** Fetches and caches an OAuth access token for bearer authentication. */
    private String getAccessToken(long deadlineNanos) {
        AccessToken current = accessToken;
        if (current != null && !current.expired()) {
            return current.value();
        }

        synchronized (this) {
            current = accessToken;
            if (current != null && !current.expired()) {
                return current.value();
            }

            ensureWithinDeadline(deadlineNanos);
            byte[] formBody = "grant_type=client_credentials".getBytes(StandardCharsets.UTF_8);

            JsonNode response = tokenClient.post()
                    .uri(properties.getTokenUri())
                    .headers(headers -> {
                        headers.setBasicAuth(properties.getUsername(), properties.getPassword());
                        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                        headers.setContentLength(formBody.length);
                        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
                    })
                    .body(formBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (_, errorResponse) -> {
                        String responseBody = new String(errorResponse.getBody().readAllBytes());
                        throw new ScimHttpException(errorResponse.getStatusCode(),
                                errorResponse.getHeaders().getFirst(HttpHeaders.RETRY_AFTER),
                                "OAuth token request failed with HTTP "
                                        + errorResponse.getStatusCode() + ": " + responseBody);
                    })
                    .body(JsonNode.class);

            if (response == null || response.path("access_token").asString().isBlank()) {
                throw new IllegalStateException("OAuth token response does not contain an access_token");
            }

            long expiresIn = response.path("expires_in").asLong(300);
            accessToken = new AccessToken(response.path("access_token").asString(),
                    System.currentTimeMillis() + Math.max(1, expiresIn - TOKEN_REFRESH_SKEW_SECONDS) * 1000);
            return accessToken.value();
        }
    }

    /** Rejects unsupported or incomplete authentication settings. */
    private void validateAuthenticationConfiguration() {
        String method = properties.getAuthenticationMethod();
        if (!"basic".equalsIgnoreCase(method) && !"bearer".equalsIgnoreCase(method)) {
            throw new IllegalStateException("Unsupported SCIM authentication method: " + method
                    + " (expected basic or bearer)");
        }
        if ("bearer".equalsIgnoreCase(method)
                && (properties.getTokenUri() == null || properties.getTokenUri().isBlank())) {
            throw new IllegalStateException("SCIM token-uri is required for bearer authentication");
        }
    }

    /** Fails a request when its synchronization deadline has elapsed. */
    private static void ensureWithinDeadline(long deadlineNanos) {
        if (System.nanoTime() >= deadlineNanos) {
            throw new IllegalStateException("SCIM synchronization timeout exceeded");
        }
    }

    private static void sleepBeforeRetry(Duration retryAfter, int attempt) {
        Duration delay = retryAfter == null ? Duration.ofMillis(250L * (1L << (attempt - 1))) : retryAfter;
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("SCIM request interrupted", interruptedException);
        }
    }

    static Duration parseRetryAfter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Duration.ofSeconds(Math.max(0, Long.parseLong(value.trim())));
        } catch (NumberFormatException ignored) {
            try {
                Duration delay = Duration.between(Instant.now(), ZonedDateTime.parse(value,
                        DateTimeFormatter.RFC_1123_DATE_TIME).toInstant());
                return delay.isNegative() ? Duration.ZERO : delay;
            } catch (RuntimeException invalidHttpDate) {
                LOGGER.warn("Ignoring invalid Retry-After header value: {}", value);
                return null;
            }
        }
    }

    /** A SCIM HTTP failure including retry metadata from the response. */
    private static final class ScimHttpException extends IllegalStateException {
        private final HttpStatusCode statusCode;
        private final Duration retryAfter;

        private ScimHttpException(HttpStatusCode statusCode, String retryAfter, String message) {
            super(message);
            this.statusCode = statusCode;
            this.retryAfter = parseRetryAfter(retryAfter);
        }

        private HttpStatusCode statusCode() {
            return statusCode;
        }

        private Duration retryAfter() {
            return retryAfter;
        }
    }

    /** Identifies a SCIM group member for matching and audit logging. */
    public record GroupMember(String id, String identity) {
    }

    /** Stores an access token and the time at which it should be refreshed. */
    private record AccessToken(String value, long expiresAt) {
        /** Returns whether this token has reached its refresh time. */
        private boolean expired() {
            return System.currentTimeMillis() >= expiresAt;
        }
    }
}
