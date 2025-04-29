package de.triology.cas.logging;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RequestResponseBodyMethodProcessorRewritePolicy}.
 */
class RequestResponseBodyMethodProcessorRewritePolicyTests {

    private RequestResponseBodyMethodProcessorRewritePolicy policy;

    @BeforeEach
    void setUp() {
        policy = RequestResponseBodyMethodProcessorRewritePolicy.createPolicy();
    }

    @Test
    void createPolicy_ShouldReturnInstance() {
        // when
        RequestResponseBodyMethodProcessorRewritePolicy instance = RequestResponseBodyMethodProcessorRewritePolicy.createPolicy();

        // then
        assertNotNull(instance, "Plugin factory should return a non-null instance");
    }

    @Test
    void rewrite_ShouldMaskPassword_WhenPasswordPresent() {
        // given
        String message = "Processing request with body: password=mySecretPassword]}]";
        LogEvent event = Log4jLogEvent.newBuilder()
                .setMessage(new SimpleMessage(message))
                .build();

        // when
        LogEvent rewritten = policy.rewrite(event);

        // then
        assertNotNull(rewritten, "Rewritten event should not be null");
        String modifiedMessage = rewritten.getMessage().getFormattedMessage();

        assertTrue(modifiedMessage.contains("password=******"), "Password should be masked");
        assertFalse(modifiedMessage.contains("mySecretPassword"), "Original password should not appear");
    }

    @Test
    void rewrite_ShouldReturnOriginal_WhenNoPasswordFlagPresent() {
        // given
        String message = "Processing request with no sensitive data";
        LogEvent event = Log4jLogEvent.newBuilder()
                .setMessage(new SimpleMessage(message))
                .build();

        // when
        LogEvent rewritten = policy.rewrite(event);

        // then
        assertSame(event, rewritten, "Event should be returned unchanged when no password flag is present");
    }

    @Test
    void getPasswordFlag_ShouldReturnExpectedFlag() {
        // when
        String flag = policy.getPasswordFlag();

        // then
        assertEquals("password=", flag, "Password flag should match expected text");
    }

    @Test
    void replacePasswordValue_ShouldHandleNullOriginMessage() {
        // when
        String result = policy.replacePasswordValue(null);

        // then
        assertNull(result, "Result should be null when origin message is null");
    }
}
