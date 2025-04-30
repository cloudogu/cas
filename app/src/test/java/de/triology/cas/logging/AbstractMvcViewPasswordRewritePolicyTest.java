package de.triology.cas.logging;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AbstractMvcViewPasswordRewritePolicyTest {

    @Test
    void rewriteMessage_ShouldMaskPassword() {
        String originalMessage = "flowExecutionUrl=/cas/login?service=https%3A%2F%2F192.168.56.2%2Fcockpit%2F&username=admin&password=adminpww&execution";

        Log4jLogEvent event = Log4jLogEvent.newBuilder()
            .setMessage(new SimpleMessage(originalMessage))
            .build();

        AbstractMvcViewPasswordRewritePolicy policy = AbstractMvcViewPasswordRewritePolicy.createPolicy();
        LogEvent rewrittenEvent = policy.rewrite(event);

        String expectedMessage = "flowExecutionUrl=/cas/login?service=https%3A%2F%2F192.168.56.2%2Fcockpit%2F&username=admin&password=****&execution";
        assertEquals(expectedMessage, rewrittenEvent.getMessage().getFormattedMessage());
    }

    @Test
    void rewriteMessage_ShouldReturnSameEvent_WhenMessageIsNull() {
        Log4jLogEvent event = Log4jLogEvent.newBuilder()
            .setMessage(null)
            .build();

        AbstractMvcViewPasswordRewritePolicy policy = AbstractMvcViewPasswordRewritePolicy.createPolicy();
        LogEvent rewrittenEvent = policy.rewrite(event);

        assertSame(event, rewrittenEvent);
    }

    @Test
    void rewriteMessage_ShouldReturnUnchanged_WhenNoPasswordInMessage() {
        String messageWithoutPassword = "flowExecutionUrl=/cas/login?service=https%3A%2F%2F192.168.56.2%2Fcockpit%2F&username=admin&execution";

        Log4jLogEvent event = Log4jLogEvent.newBuilder()
            .setMessage(new SimpleMessage(messageWithoutPassword))
            .build();

        AbstractMvcViewPasswordRewritePolicy policy = AbstractMvcViewPasswordRewritePolicy.createPolicy();
        LogEvent rewrittenEvent = policy.rewrite(event);

        assertEquals(messageWithoutPassword, rewrittenEvent.getMessage().getFormattedMessage());
    }
}
