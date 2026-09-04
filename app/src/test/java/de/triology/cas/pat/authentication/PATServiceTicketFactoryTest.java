package de.triology.cas.pat.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.triology.cas.pat.service.PATService;
import org.apereo.cas.authentication.Authentication;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.ticket.ExpirationPolicy;
import org.apereo.cas.ticket.ExpirationPolicyBuilder;
import org.apereo.cas.ticket.ServiceTicket;
import org.apereo.cas.ticket.TicketGrantingTicket;
import org.apereo.cas.ticket.UniqueTicketIdGenerator;
import org.apereo.cas.ticket.tracking.TicketTrackingPolicy;
import org.apereo.cas.util.crypto.CipherExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.security.auth.login.FailedLoginException;

@ExtendWith(MockitoExtension.class)
class PATServiceTicketFactoryTest {
    private static final String SCOPE = "/usermgt";

    @Mock
    private ExpirationPolicyBuilder<ServiceTicket> expirationPolicyBuilder;
    @Mock
    private TicketTrackingPolicy ticketTrackingPolicy;
    @Mock
    private CipherExecutor<String, String> cipherExecutor;
    @Mock
    private ServicesManager servicesManager;
    @Mock
    private PATService patService;
    @Mock
    private TicketGrantingTicket ticketGrantingTicket;
    @Mock
    private Service service;

    private PATServiceTicketFactory factory;

    @BeforeEach
    void setUp() {
        factory = new PATServiceTicketFactory(
                expirationPolicyBuilder, Map.<String, UniqueTicketIdGenerator>of(),
                ticketTrackingPolicy, cipherExecutor, servicesManager, patService);
    }

    @Test
    void rejectsPatAuthenticationWithoutAService() {
        authenticationWithScope(SCOPE);

        FailedLoginException exception = assertThrows(FailedLoginException.class,
                () -> factory.create(ticketGrantingTicket, null, true, ServiceTicket.class));

        assertEquals("Service is required for PAT authorization", exception.getMessage());
    }

    @Test
    void rejectsMalformedServiceUrl() {
        authenticationWithScope(SCOPE);
        when(service.getId()).thenReturn("https://[");

        FailedLoginException exception = assertThrows(FailedLoginException.class,
                () -> factory.create(ticketGrantingTicket, service, true, ServiceTicket.class));

        assertEquals("Invalid service URL", exception.getMessage());
    }

    @Test
    void rejectsServiceOutsidePatScope() {
        authenticationWithScope(SCOPE);
        when(service.getId()).thenReturn("https://example.test/redmine/api");
        when(patService.isScopeAllowed(SCOPE, "/redmine/api")).thenReturn(false);

        FailedLoginException exception = assertThrows(FailedLoginException.class,
                () -> factory.create(ticketGrantingTicket, service, true, ServiceTicket.class));

        assertEquals("PAT is not authorized for service https://example.test/redmine/api",
                exception.getMessage());
        verify(patService).isScopeAllowed(SCOPE, "/redmine/api");
    }

    @Test
    void delegatesAllowedServiceTicketCreationToCas() throws Throwable {
        authenticationWithScope(SCOPE);
        when(service.getId()).thenReturn("https://example.test/usermgt/api/users");
        when(patService.isScopeAllowed(SCOPE, "/usermgt/api/users")).thenReturn(true);
        when(cipherExecutor.isEnabled()).thenReturn(false);
        ExpirationPolicy expirationPolicy = mock(ExpirationPolicy.class);
        when(expirationPolicyBuilder.buildTicketExpirationPolicy()).thenReturn(expirationPolicy);
        ServiceTicket serviceTicket = mock(ServiceTicket.class);
        when(ticketGrantingTicket.grantServiceTicket(
                anyString(), eq(service), eq(expirationPolicy), eq(true), eq(ticketTrackingPolicy)))
                .thenReturn(serviceTicket);

        ServiceTicket result = factory.create(ticketGrantingTicket, service, true, ServiceTicket.class);

        assertSame(serviceTicket, result);
        verify(patService).isScopeAllowed(SCOPE, "/usermgt/api/users");
    }

    private void authenticationWithScope(String scope) {
        Authentication authentication = mock(Authentication.class);
        Principal principal = mock(Principal.class);
        Map<String, List<Object>> attributes = new HashMap<>();
        attributes.put(PATService.PAT_SCOPE_ATTRIBUTE, List.of(scope));
        when(ticketGrantingTicket.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(principal.getAttributes()).thenReturn(attributes);
    }
}
