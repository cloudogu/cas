package de.triology.cas.pat.notification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import de.triology.cas.ldap.CesGroupAwareLdapAuthenticationHandler;
import de.triology.cas.pat.model.PATMetadata;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apereo.cas.authentication.principal.Principal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class PATExpiryMailSenderTest {
    private PATMetadata pat() {
        return pat("ldap-owner");
    }

    private PATMetadata pat(String owner) {
        return new PATMetadata(UUID.randomUUID(), "different-user", "Deployment", Instant.EPOCH,
                Instant.parse("2026-12-01T12:00:00Z"), "/*", "description", owner, true);
    }

    @Test
    void resolvesOwnerAndSendsNameAndExpiryToLdapMailAddresses() throws Throwable {
        var ldap = mock(CesGroupAwareLdapAuthenticationHandler.class);
        var mail = mock(JavaMailSender.class);
        var principal = mock(Principal.class);
        when(ldap.resolvePrincipal("ldap-owner")).thenReturn(principal);
        when(principal.getAttributes()).thenReturn(Map.of("mail", List.of("owner@example.org")));
        var pat = pat();

        new PATExpiryMailSender(ldap, mail, "cas@example.org").send(pat, 14);

        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail).send(captor.capture());
        assertArrayEquals(new String[] {"owner@example.org"}, captor.getValue().getTo());
        assertEquals("cas@example.org", captor.getValue().getFrom());
        assertEquals("CAS: Personal access token expires in 14 days", captor.getValue().getSubject());
        assertTrue(captor.getValue().getText().startsWith(
                "Your personal access token \"Deployment\" expires in 14 days."));
        assertTrue(captor.getValue().getText().contains(pat.expiresAt().toString()));
        verify(ldap).resolvePrincipal("ldap-owner");
    }

    @Test
    void usesTechnicalAccountDisplayNameAndSendsToOwner() throws Throwable {
        var ldap = mock(CesGroupAwareLdapAuthenticationHandler.class);
        var mail = mock(JavaMailSender.class);
        var owner = mock(Principal.class);
        when(ldap.resolvePrincipal("ldap-owner")).thenReturn(owner);
        when(owner.getAttributes()).thenReturn(Map.of("mail", List.of("owner@example.org")));
        when(ldap.findTechnicalAccountDisplayName("different-user")).thenReturn(Optional.of("Build automation"));

        new PATExpiryMailSender(ldap, mail, "cas@example.org").send(pat(), 1);

        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail).send(captor.capture());
        assertArrayEquals(new String[] {"owner@example.org"}, captor.getValue().getTo());
        assertEquals("CAS: Technical account expires in 1 day", captor.getValue().getSubject());
        assertEquals("""
                The access token for the technical account "Build automation" expires in 1 day. The expiration time is 2026-12-01T12:00:00Z (UTC).

                Please replace the token before it expires and update any applications that use it.
                """, captor.getValue().getText());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " \t")
    void sendsToTokenUserWhenOwnerIsMissingOrBlank(String owner) throws Throwable {
        var ldap = mock(CesGroupAwareLdapAuthenticationHandler.class);
        var mail = mock(JavaMailSender.class);
        var principal = mock(Principal.class);
        var pat = pat(owner);
        when(ldap.resolvePrincipal(pat.userId())).thenReturn(principal);
        when(principal.getAttributes()).thenReturn(Map.of("mail", List.of("user@example.org")));

        new PATExpiryMailSender(ldap, mail, "cas@example.org").send(pat, 1);

        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail).send(captor.capture());
        assertArrayEquals(new String[] {"user@example.org"}, captor.getValue().getTo());
    }

    @Test
    void doesNotSendWhenLdapMailIsMissing() throws Throwable {
        var ldap = mock(CesGroupAwareLdapAuthenticationHandler.class);
        var mail = mock(JavaMailSender.class);
        var principal = mock(Principal.class);
        when(ldap.resolvePrincipal("ldap-owner")).thenReturn(principal);
        when(principal.getAttributes()).thenReturn(Map.of());

        assertThrows(IllegalStateException.class,
                () -> new PATExpiryMailSender(ldap, mail, "cas@example.org").send(pat(), 1));
        verifyNoInteractions(mail);
    }
}
