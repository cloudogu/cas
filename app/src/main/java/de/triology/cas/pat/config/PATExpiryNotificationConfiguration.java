package de.triology.cas.pat.config;

import de.triology.cas.ldap.CesGroupAwareLdapAuthenticationHandler;
import de.triology.cas.pat.notification.PATExpiryMailSender;
import de.triology.cas.pat.notification.ScheduledPATExpiryNotifier;
import de.triology.cas.pat.repository.PATExpiryNotificationRepository;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/** Independent scheduling and mail infrastructure for PAT expiration reminders. */
@AutoConfiguration(after = PATServiceConfiguration.class)
@EnableScheduling
@ConditionalOnProperty(prefix = "personal-acces-token-service", name = "enabled", havingValue = "true")
@ConditionalOnProperty(prefix = "personal-acces-token-service.expiry-notification", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class PATExpiryNotificationConfiguration {
    @Bean(name = "patExpiryTaskScheduler", defaultCandidate = false)
    public ThreadPoolTaskScheduler patExpiryTaskScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("pat-expiry-");
        return scheduler;
    }

    @Bean
    public PATExpiryNotificationRepository patExpiryNotificationRepository(
            @Qualifier("patJdbcTemplate") JdbcTemplate jdbcTemplate) {
        return new PATExpiryNotificationRepository(jdbcTemplate);
    }

    @Bean
    public ScheduledPATExpiryNotifier scheduledPATExpiryNotifier(
            PATExpiryNotificationRepository repository,
            @Qualifier("cesGroupAwareLdapAuthenticationHandler") CesGroupAwareLdapAuthenticationHandler ldapHandler,
            JavaMailSender mailSender, @Qualifier("patClock") Clock clock,
            @Value("${personal-acces-token-service.expiry-notification.from:${cas.authn.pm.reset.mail.from:cas.dogu@cloudogu.com}}")
            String from) {
        return new ScheduledPATExpiryNotifier(repository, new PATExpiryMailSender(ldapHandler, mailSender, from),
                clock);
    }
}
