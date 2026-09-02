package de.triology.cas.pat.authentication;

import java.net.URI;

import de.triology.cas.pat.service.PATService;
import org.apereo.cas.authentication.Authentication;
import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.ticket.ExpirationPolicyBuilder;
import org.apereo.cas.ticket.ServiceTicket;
import org.apereo.cas.ticket.Ticket;
import org.apereo.cas.ticket.TicketGrantingTicket;
import org.apereo.cas.ticket.UniqueTicketIdGenerator;
import org.apereo.cas.ticket.factory.DefaultServiceTicketFactory;
import org.apereo.cas.ticket.tracking.TicketTrackingPolicy;
import org.apereo.cas.util.crypto.CipherExecutor;

import javax.security.auth.login.FailedLoginException;
import java.util.Map;

/** Service-ticket factory that enforces scopes carried by PAT-authenticated TGTs. */
public class PATServiceTicketFactory extends DefaultServiceTicketFactory {
    private final PATService patService;

    public PATServiceTicketFactory(
            ExpirationPolicyBuilder<ServiceTicket> expirationPolicyBuilder,
            Map<String, UniqueTicketIdGenerator> uniqueTicketIdGeneratorsForService,
            TicketTrackingPolicy serviceTicketSessionTrackingPolicy,
            CipherExecutor<String, String> cipherExecutor,
            ServicesManager servicesManager,
            PATService patService) {
        super(expirationPolicyBuilder, uniqueTicketIdGeneratorsForService,
                serviceTicketSessionTrackingPolicy, cipherExecutor, servicesManager);
        this.patService = patService;
    }

    @Override
    public <T extends Ticket> T create(
            TicketGrantingTicket ticketGrantingTicket,
            Service service,
            boolean credentialProvided,
            Class<T> clazz) throws Throwable {
        String scope = ticketGrantingTicket.getAuthentication()
                .getPrincipal().getAttributes()
                .get(PATService.PAT_SCOPE_ATTRIBUTE) instanceof java.util.List<?> values
                && !values.isEmpty()
                && values.getFirst() instanceof String value
                ? value
                : null;

        if (scope != null) {
            if (service == null || service.getId() == null) {
                throw new FailedLoginException("Service is required for PAT authorization");
            }
            String path;
            try {
                path = URI.create(service.getId()).getPath();
            } catch (IllegalArgumentException e) {
                throw new FailedLoginException("Invalid service URL");
            }
            if (!patService.isScopeAllowed(scope, path)) {
                throw new FailedLoginException("PAT is not authorized for service " + service.getId());
            }
        }

        return super.create(ticketGrantingTicket, service, credentialProvided, clazz);
    }
}
