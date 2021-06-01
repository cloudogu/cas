package de.triology.cas.logging;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent.Builder;
import org.apache.logging.log4j.message.SimpleMessageFactory;
import org.junit.Assert;
import org.junit.Test;

public class LoggingHandlerPasswordRewritePolicyTest {

    @Test
    public void rewriteMessage() {
        String firstLineContent = "[id: 0x3aa06c40, L:/172.18.0.6:47912 - R:ldap-mapper/172.18.0.4:3893] WRITE: 93B";

        String message = firstLineContent + "\n";
        message += "          +-------------------------------------------------+\n";
        message += " |00000030| 6f 6d 80 07 61 64 6d 69 6e 70 77 a0 20 30 1e 04 |om..adminpw. 0..|\n";

        Builder builder = new Builder();
        builder.setMessage(SimpleMessageFactory.INSTANCE.newMessage(message));

        LogEvent rewrittenLogEvent = LoggingHandlerPasswordRewritePolicy.createPolicy().rewrite(builder.build());

        Assert.assertEquals(firstLineContent, rewrittenLogEvent.getMessage().getFormattedMessage());
    }

    @Test
    public void rewriteMessageWhenMessageIsNull() {
        Builder builder = new Builder();
        builder.setMessage(null);

        LogEvent logEvent = builder.build();
        LogEvent rewrittenLogEvent = LoggingHandlerPasswordRewritePolicy.createPolicy().rewrite(logEvent);

        Assert.assertSame(logEvent, rewrittenLogEvent);
    }
}