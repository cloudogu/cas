package de.triology.cas.pat.notification;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import de.triology.cas.pat.model.PATMetadata;
import de.triology.cas.pat.repository.PATExpiryNotificationRepository;
import java.time.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ScheduledPATExpiryNotifierTest {
    private static final Instant NOW = LocalDate.of(2026, 11, 17).atTime(8, 0)
            .atZone(ZoneId.systemDefault()).toInstant();

    private PATMetadata pat(Instant expiry) {
        return new PATMetadata(UUID.randomUUID(), "user", "name", NOW.minusSeconds(3600), expiry,
                "/*", "description", "ldap-owner", true);
    }

    @Test
    void sendsOnlyAtTheFourCalendarDayStages() {
        var repository = mock(PATExpiryNotificationRepository.class);
        var mail = mock(PATExpiryMailSender.class);
        var in30 = pat(NOW.plus(Duration.ofDays(30)));
        var in14 = pat(NOW.plus(Duration.ofDays(14)));
        var in7 = pat(NOW.plus(Duration.ofDays(7)));
        var in1 = pat(NOW.plus(Duration.ofDays(1)));
        var in2 = pat(NOW.plus(Duration.ofDays(2)));
        var today = pat(NOW.plusSeconds(3600));
        when(repository.findCandidates(NOW)).thenReturn(List.of(in30, in14, in7, in1, in2, today));
        when(repository.sendOnce(any(), anyInt(), eq(NOW), any())).thenAnswer(call -> {
            call.getArgument(3, Runnable.class).run();
            return true;
        });

        new ScheduledPATExpiryNotifier(repository, mail, Clock.fixed(NOW, ZoneOffset.UTC)).run();

        verify(mail).send(in30, 30);
        verify(mail).send(in14, 14);
        verify(mail).send(in7, 7);
        verify(mail).send(in1, 1);
        verifyNoMoreInteractions(mail);
    }

    @Test
    void usesCalendarDatesRatherThanTruncatedTwentyFourHourPeriods() {
        var repository = mock(PATExpiryNotificationRepository.class);
        var mail = mock(PATExpiryMailSender.class);
        Instant midnight = LocalDate.of(2026, 11, 18).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant now = midnight.minus(Duration.ofMinutes(30));
        var tomorrow = pat(midnight.plus(Duration.ofMinutes(15)));
        when(repository.findCandidates(now)).thenReturn(List.of(tomorrow));

        new ScheduledPATExpiryNotifier(repository, mail, Clock.fixed(now, ZoneOffset.UTC)).run();

        verify(repository).sendOnce(eq(tomorrow), eq(1), eq(now), any());
    }

    @Test
    void continuesAfterOneFailedOwnerAndSurvivesDatabaseFailure() {
        var repository = mock(PATExpiryNotificationRepository.class);
        var mail = mock(PATExpiryMailSender.class);
        var first = pat(NOW.plus(Duration.ofDays(1)));
        var second = pat(NOW.plus(Duration.ofDays(7)));
        when(repository.findCandidates(NOW)).thenReturn(List.of(first, second));
        when(repository.sendOnce(eq(first), anyInt(), any(), any())).thenThrow(new IllegalStateException("offline"));
        var worker = new ScheduledPATExpiryNotifier(repository, mail, Clock.fixed(NOW, ZoneOffset.UTC));

        worker.run();
        verify(repository).sendOnce(eq(second), eq(7), eq(NOW), any());
        when(repository.findCandidates(NOW)).thenThrow(new IllegalStateException("database offline"));
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(worker::run);
    }
}
