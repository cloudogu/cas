package de.triology.cas.pat.notification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import de.triology.cas.ldap.CesGroupAwareLdapAuthenticationHandler;
import de.triology.cas.pat.model.PATMetadata;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apereo.cas.authentication.principal.Principal;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class PATExpiryMailSenderTest {
    private PATMetadata pat() {
        return new PATMetadata(UUID.randomUUID(), "different-user", "Deployment", Instant.EPOCH,
                Instant.parse("2026-12-01T12:00:00Z"), "/*", "description", "ldap-owner", true);
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
        assertTrue(captor.getValue().getText().contains("Deployment"));
        assertTrue(captor.getValue().getText().contains(pat.expiresAt().toString()));
        verify(ldap).resolvePrincipal("ldap-owner");
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
