package de.triology.cas.pat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Schedule for PAT expiration reminders, independent of OIDC synchronization. */
@ConfigurationProperties(prefix = "pat.expiry-notification")
public class PATExpiryNotificationProperties {
    private String cron = "0 0 8 * * *";

    public String getCron() { return cron; }
    public void setCron(String cron) { this.cron = cron; }
}
