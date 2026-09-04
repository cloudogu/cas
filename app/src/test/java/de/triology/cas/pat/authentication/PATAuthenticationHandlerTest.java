package de.triology.cas.pat.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import de.triology.cas.ldap.CesGroupAwareLdapAuthenticationHandler;
import de.triology.cas.pat.model.PATMetadata;
import de.triology.cas.pat.service.PATService;
import org.apereo.cas.authentication.Credential;
import org.apereo.cas.authentication.credential.UsernamePasswordCredential;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.authentication.principal.PrincipalFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.security.auth.login.FailedLoginException;

@ExtendWith(MockitoExtension.class)
class PATAuthenticationHandlerTest {
    private static final String TOKEN = "pat_secret";

    @Mock
    private PrincipalFactory principalFactory;
    @Mock
    private PATService patService;
    @Mock
    private CesGroupAwareLdapAuthenticationHandler ldapHandler;

    private PATAuthenticationHandler handler;

    @BeforeEach
    void setUp() {
        handler = new PATAuthenticationHandler(
                "patAuthenticationHandler", principalFactory, 0, patService, ldapHandler);
    }

    @Test
    void supportsOnlyUsernamePasswordCredentialsContainingAPat() {
        assertTrue(handler.supports(new UsernamePasswordCredential("user", TOKEN)));
        assertFalse(handler.supports(new UsernamePasswordCredential("user", "password")));
        assertFalse(handler.supports(new UsernamePasswordCredential("user", (String) null)));
        assertFalse(handler.supports(org.mockito.Mockito.mock(Credential.class)));
        assertFalse(handler.supports((Credential) null));
    }

    @Test
    void resolvesPatOwnerAndAddsPatAttributesToLdapPrincipal() throws Throwable {
        UsernamePasswordCredential credential = new UsernamePasswordCredential("request-user", TOKEN);
        PATMetadata metadata = metadata("owner", "/usermgt");
        Principal ldapPrincipal = org.mockito.Mockito.mock(Principal.class);
        Principal authenticatedPrincipal = org.mockito.Mockito.mock(Principal.class);
        Map<String, List<Object>> ldapAttributes = Map.of("mail", List.of("owner@example.test"));

        when(patService.resolve(TOKEN)).thenReturn(Optional.of(metadata));
        when(ldapHandler.resolvePrincipal("owner")).thenReturn(ldapPrincipal);
        when(ldapPrincipal.getId()).thenReturn("owner");
        when(ldapPrincipal.getAttributes()).thenReturn(ldapAttributes);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, List<Object>>> attributes = ArgumentCaptor.forClass(Map.class);
        when(principalFactory.createPrincipal(org.mockito.ArgumentMatchers.eq("owner"), attributes.capture()))
                .thenReturn(authenticatedPrincipal);

        var result = handler.authenticateUsernamePasswordInternal(credential, TOKEN);

        assertSame(authenticatedPrincipal, result.getPrincipal());
        assertSame(credential, result.getCredential());
        assertEquals(ldapAttributes.get("mail"), attributes.getValue().get("mail"));
        assertEquals(List.of("/usermgt"), attributes.getValue().get(PATService.PAT_SCOPE_ATTRIBUTE));
        assertEquals(List.of("true"), attributes.getValue().get(PATService.PAT_AUTH_ATTRIBUTE));
    }

    @Test
    void rejectsUnknownOrExpiredPatBeforeLdapLookup() throws Throwable {
        UsernamePasswordCredential credential = new UsernamePasswordCredential("user", TOKEN);
        when(patService.resolve(TOKEN)).thenReturn(Optional.empty());

        FailedLoginException exception = assertThrows(FailedLoginException.class,
                () -> handler.authenticateUsernamePasswordInternal(credential, TOKEN));

        assertEquals("Invalid or expired PAT", exception.getMessage());
        verify(ldapHandler, never()).resolvePrincipal(org.mockito.ArgumentMatchers.anyString());
    }

    private static PATMetadata metadata(String userId, String scope) {
        return new PATMetadata(UUID.randomUUID(), userId, "token", Instant.EPOCH, null, scope);
    }
}
