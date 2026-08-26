package de.triology.cas.pat.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Type-safe configuration for the optional PAT API and its database.
 */
@ConfigurationProperties(prefix = "custom-token-service")
@Validated
public class PATServiceProperties {
    private boolean enabled;

    @NotBlank(message = "custom-token-service.database-url must be configured")
    private String databaseUrl = "jdbc:sqlite:/var/ces/config/pats.db";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDatabaseUrl() {
        return databaseUrl;
    }

    public void setDatabaseUrl(String databaseUrl) {
        this.databaseUrl = databaseUrl;
    }
}
