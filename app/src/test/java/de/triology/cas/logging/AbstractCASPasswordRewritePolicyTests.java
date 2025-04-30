package de.triology.cas.logging;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AbstractCASPasswordRewritePolicy}.
 */
class AbstractCASPasswordRewritePolicyTests {

    private AbstractCASPasswordRewritePolicy policy;

    @BeforeEach
    void setUp() {
        // Define a simple test subclass
        policy = new AbstractCASPasswordRewritePolicy() {
            @Override
            protected String getPasswordFlag() {
                return "password=";
            }

            @Override
            protected String replacePasswordValue(String originMessage) {
                return originMessage.replaceAll("password=[^&\\s]+", "password=******");
            }
        };
    }

    @Test
    void rewrite_ShouldModifyMessage_WhenPasswordDetected() {
        // given
        String originalMessage = "Authentication request with username=user and password=secret123";
        LogEvent event = Log4jLogEvent.newBuilder()
                .setMessage(new SimpleMessage(originalMessage))
                .build();

        // when
        LogEvent result = policy.rewrite(event);

        // then
        assertNotNull(result, "Rewritten event should not be null");
        assertTrue(result.getMessage().getFormattedMessage().contains("password=******"),
                "Password should be masked in rewritten message");
    }

    @Test
    void rewrite_ShouldReturnOriginalEvent_WhenNoPasswordDetected() {
        // given
        String originalMessage = "Authentication request without sensitive data";
        LogEvent event = Log4jLogEvent.newBuilder()
                .setMessage(new SimpleMessage(originalMessage))
                .build();

        // when
        LogEvent result = policy.rewrite(event);

        // then
        assertSame(event, result, "Event without password should not be modified");
    }

    @Test
    void containsPassword_ShouldReturnTrue_WhenPasswordPresent() {
        // given
        String message = "Login attempt with password=secret";
        LogEvent event = Log4jLogEvent.newBuilder()
                .setMessage(new SimpleMessage(message))
                .build();

        // when
        boolean contains = policy.containsPassword(event);

        // then
        assertTrue(contains, "Should detect password in message");
    }

    @Test
    void containsPassword_ShouldReturnFalse_WhenNoPasswordPresent() {
        // given
        String message = "Login attempt without sensitive information";
        LogEvent event = Log4jLogEvent.newBuilder()
                .setMessage(new SimpleMessage(message))
                .build();

        // when
        boolean contains = policy.containsPassword(event);

        // then
        assertFalse(contains, "Should not detect password when none present");
    }
}
