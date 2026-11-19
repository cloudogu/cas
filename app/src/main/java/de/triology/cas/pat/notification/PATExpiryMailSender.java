package de.triology.cas.pat.notification;

import de.triology.cas.ldap.CesGroupAwareLdapAuthenticationHandler;
import de.triology.cas.pat.model.PATMetadata;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.authentication.principal.Principal;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/** Resolves the PAT owner through LDAP and sends a plaintext expiration reminder. */
@Slf4j
public class PATExpiryMailSender {
    private final CesGroupAwareLdapAuthenticationHandler ldapHandler;
    private final JavaMailSender mailSender;
    private final String from;

    public PATExpiryMailSender(CesGroupAwareLdapAuthenticationHandler ldapHandler,
                              JavaMailSender mailSender, String from) {
        this.ldapHandler = ldapHandler;
        this.mailSender = mailSender;
        this.from = from;
    }

    public void send(PATMetadata pat, int daysBefore) {
        Principal principal;
        try {
            principal = ldapHandler.resolvePrincipal(pat.owner());
        } catch (Error e) {
            throw e;
        } catch (Throwable e) {
            LOGGER.warn("LDAP lookup failed for PAT expiry reminder (patId={})", pat.id());
            throw new IllegalStateException("Could not resolve PAT owner in LDAP", e);
        }
        String[] recipients = principal.getAttributes().getOrDefault("mail", List.of()).stream()
                .filter(String.class::isInstance).map(String.class::cast)
                .filter(value -> !value.isBlank()).distinct().toArray(String[]::new);
        if (recipients.length == 0) {
            LOGGER.warn("PAT owner has no LDAP mail address for expiry reminder (patId={})", pat.id());
            throw new IllegalStateException("PAT owner has no LDAP mail address");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipients);
        String timeUntilExpiry = daysBefore + (daysBefore == 1 ? " day" : " days");
        message.setSubject("CAS: Personal access token expires in " + timeUntilExpiry);
        message.setText("Your personal access token \"" + pat.displayName() + "\" (ID: " + pat.id()
                + ") expires in " + timeUntilExpiry + ". The expiration time is " + pat.expiresAt() + " (UTC).\n\n"
                + "Please replace the token before it expires and update any applications that use it.\n");
        LOGGER.info("Sending PAT expiry reminder (patId={}, daysBefore={}, recipientCount={})",
                pat.id(), daysBefore, recipients.length);
        try {
            mailSender.send(message);
        } catch (MailException e) {
            LOGGER.warn("PAT expiry reminder send failed (patId={}, failure={})",
                    pat.id(), e.getClass().getSimpleName());
            throw e;
        }
        LOGGER.info("Mail sender accepted PAT expiry reminder (patId={}, daysBefore={})", pat.id(), daysBefore);
    }
}
