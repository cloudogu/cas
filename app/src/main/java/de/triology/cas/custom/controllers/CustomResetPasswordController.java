package de.triology.cas.custom.controllers;

import lombok.val;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.pm.PasswordManagementService;
import org.apereo.cas.pm.config.PasswordManagementConfiguration;
import org.apereo.cas.ticket.TicketFactory;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/custom-reset-password")
public class CustomResetPasswordController {

    protected PasswordManagementConfiguration.PasswordManagementCoreConfiguration passwordManagementCoreConfiguration;
    protected CasConfigurationProperties casConfigurationProperties;
    protected PasswordManagementService passwordChangeService;
    protected TicketRegistry ticketRegistry;
    protected TicketFactory ticketFactory;

    public CustomResetPasswordController(
            PasswordManagementConfiguration.PasswordManagementCoreConfiguration passwordManagementCoreConfiguration,
            CasConfigurationProperties casConfigurationProperties,
            PasswordManagementService passwordChangeService,
            TicketRegistry ticketRegistry,
            @Qualifier(TicketFactory.BEAN_NAME) TicketFactory ticketFactory
    ) {
        this.passwordManagementCoreConfiguration = passwordManagementCoreConfiguration;
        this.casConfigurationProperties = casConfigurationProperties;
        this.passwordChangeService = passwordChangeService;
        this.ticketRegistry = ticketRegistry;
        this.ticketFactory = ticketFactory;
    }

    @PostMapping("{username}")
    public ResponseEntity<String> resetPassword(@PathVariable String username) throws Exception {
        val builder = this.passwordManagementCoreConfiguration.passwordResetUrlBuilder(
                this.casConfigurationProperties,
                this.passwordChangeService,
                this.ticketRegistry,
                this.ticketFactory);
        return ResponseEntity.status(HttpStatus.OK).body(builder.build(username).toString());
    }
}