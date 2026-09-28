package de.triology.cas.pat.authentication;

import java.util.List;

import org.apereo.cas.authentication.Credential;
import org.apereo.cas.authentication.credential.UsernamePasswordCredential;
import org.apereo.cas.rest.factory.RestHttpRequestCredentialFactory;
import org.springframework.http.HttpMethod;
import org.springframework.util.MultiValueMap;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Extracts PAT credentials from the CAS REST TGT endpoint.
 *
 * <p>Normal REST username/password authentication remains unchanged. A
 * password with the PAT prefix is converted to a {@link PATCredential} only
 * for {@code POST /v1/tickets}; it is not allowed to fall through as a normal
 * username/password credential anywhere else in the REST API.</p>
 */
public class PATRestHttpRequestCredentialFactory implements RestHttpRequestCredentialFactory {
    private static final String TGT_ENDPOINT = "/v1/tickets";
    private static final String PAT_PREFIX = "pat_";

    @Override
    public List<Credential> fromRequest(HttpServletRequest request,
                                        MultiValueMap<String, String> requestBody) throws Throwable {
        String username = requestBody == null ? null : requestBody.getFirst(RestHttpRequestCredentialFactory.PARAMETER_USERNAME);
        String password = requestBody == null ? null : requestBody.getFirst(RestHttpRequestCredentialFactory.PARAMETER_PASSWORD);
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return List.of();
        }

        Credential credentialsCredential = prepareCredential(request,
                new UsernamePasswordCredential(username, password));
        List<Credential> credentials = List.of(credentialsCredential);
        if (credentials.isEmpty()) {
            return credentials;
        }

        Credential credential = credentials.getFirst();
        if (!(credential instanceof UsernamePasswordCredential usernamePassword)) {
            return credentials;
        }

        String token = usernamePassword.toPassword();
        if (token == null || !token.startsWith(PAT_PREFIX)) {
            return credentials;
        }

        if (!HttpMethod.POST.matches(request.getMethod()) || !isTicketGrantingTicketEndpoint(request)) {
            return List.of();
        }

        return List.of(prepareCredential(request,
                new PATCredential(usernamePassword.getUsername(), token)));
    }

    private boolean isTicketGrantingTicketEndpoint(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        if (TGT_ENDPOINT.equals(servletPath)) {
            return true;
        }

        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (requestUri == null) {
            return false;
        }
        if (contextPath != null && !contextPath.isEmpty() && requestUri.startsWith(contextPath)) {
            requestUri = requestUri.substring(contextPath.length());
        }
        return TGT_ENDPOINT.equals(requestUri);
    }
}
