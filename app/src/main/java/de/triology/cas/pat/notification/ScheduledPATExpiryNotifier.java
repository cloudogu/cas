package de.triology.cas.pat.notification;

import de.triology.cas.pat.repository.PATExpiryNotificationRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

/** Daily PAT expiration reminders with their own scheduler, independent of OIDC. */
@Slf4j
public class ScheduledPATExpiryNotifier {
    private static final Set<Long> REMINDER_DAYS = Set.of(30L, 14L, 7L, 1L);
    private final PATExpiryNotificationRepository repository;
    private final PATExpiryMailSender mailSender;
    private final Clock clock;

    public ScheduledPATExpiryNotifier(PATExpiryNotificationRepository repository, PATExpiryMailSender mailSender,
                                     Clock clock) {
        this.repository = repository;
        this.mailSender = mailSender;
        this.clock = clock;
    }

    @Scheduled(cron = "${personal-acces-token-service.expiry-notificationcron:0 0 8 * * *}",
            scheduler = "patExpiryTaskScheduler")
    public void run() {
        try {
            var now = clock.instant();
            var zone = ZoneId.systemDefault();
            var today = LocalDate.ofInstant(now, zone);
            for (var pat : repository.findCandidates(now)) {
                long days = ChronoUnit.DAYS.between(today, LocalDate.ofInstant(pat.expiresAt(), zone));
                if (!REMINDER_DAYS.contains(days)) {
                    continue;
                }
                try {
                    if (repository.sendOnce(pat, (int) days, now, () -> mailSender.send(pat, (int) days))) {
                        LOGGER.info("event=pat_expiry_notification patId={} daysBefore={} result=sent", pat.id(), days);
                    }
                } catch (Exception e) {
                    LOGGER.error("event=pat_expiry_notification patId={} daysBefore={} result=failed", pat.id(), days, e);
                }
            }
        } catch (Exception e) {
            LOGGER.error("PAT expiration reminder job failed; future runs remain scheduled", e);
        }
    }
}
