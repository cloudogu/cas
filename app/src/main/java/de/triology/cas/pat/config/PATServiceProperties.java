package de.triology.cas.pat.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Type-safe configuration for the optional PAT API and its database.
 */
@ConfigurationProperties(prefix = "personal-acces-token-service")
@Validated
public class PATServiceProperties {
    private boolean enabled;

    @NotBlank(message = "personal-acces-token-service.database-url must be configured")
    private String databaseUrl;

    /**
     * Returns the value provided by isEnabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Updates the value managed by setEnabled.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns the value provided by getDatabaseUrl.
     */
    public String getDatabaseUrl() {
        return databaseUrl;
    }

    /**
     * Updates the value managed by setDatabaseUrl.
     */
    public void setDatabaseUrl(String databaseUrl) {
        this.databaseUrl = databaseUrl;
    }
}
