package de.triology.cas.pat.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import java.time.Clock;
import de.triology.cas.ldap.CesGroupAwareLdapAuthenticationHandler;
import de.triology.cas.pat.notification.ScheduledPATExpiryNotifier;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;

class PATExpiryNotificationConfigurationTest {
    @Test
    void doesNotRegisterPatWorkerWhenPatServiceIsDisabled() {
        try (var context = new AnnotationConfigApplicationContext(PATExpiryNotificationConfiguration.class)) {
            assertTrue(context.getBeansOfType(ScheduledPATExpiryNotifier.class).isEmpty());
        }
    }

    @Test
    void canDisableNotificationsWithoutMailOrLdapBeans() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", java.util.Map.of(
                    "personal-acces-token-service.enabled", "true",
                    "personal-acces-token-service.expiry-notification.enabled", "false")));
            context.register(PATExpiryNotificationConfiguration.class);
            context.refresh();
            assertTrue(context.getBeansOfType(ScheduledPATExpiryNotifier.class).isEmpty());
            assertFalse(context.containsBean("patExpiryTaskScheduler"));
        }
    }

    @Test
    void startsItsOwnSchedulerWithoutAnyOidcConfiguration() {
        try (var context = new AnnotationConfigApplicationContext()) {
            String cron = "0 15 9 * * *";
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", java.util.Map.of(
                    "personal-acces-token-service.enabled", "true",
                    "personal-acces-token-service.expiry-notification.cron", cron)));
            context.registerBean("patClock", Clock.class, Clock::systemUTC);
            context.registerBean("cesGroupAwareLdapAuthenticationHandler", CesGroupAwareLdapAuthenticationHandler.class,
                    () -> mock(CesGroupAwareLdapAuthenticationHandler.class));
            context.registerBean(JavaMailSender.class, () -> mock(JavaMailSender.class));
            context.register(PATExpiryNotificationConfiguration.class);
            var dataSource = new org.sqlite.SQLiteDataSource();
            dataSource.setUrl("jdbc:sqlite::memory:");
            context.registerBean("patJdbcTemplate", JdbcTemplate.class, () -> new JdbcTemplate(dataSource));
            context.refresh();
            assertNotNull(context.getBean(ScheduledPATExpiryNotifier.class));
            assertNotNull(context.getBean("patExpiryTaskScheduler"));
            assertFalse(context.containsBean("scheduledOidcLdapSynchronizer"));
            var tasks = context.getBean(ScheduledTaskHolder.class).getScheduledTasks();
            assertEquals(1, tasks.size());
            assertEquals(cron, assertInstanceOf(CronTask.class, tasks.iterator().next().getTask()).getExpression());
        }
    }
}
