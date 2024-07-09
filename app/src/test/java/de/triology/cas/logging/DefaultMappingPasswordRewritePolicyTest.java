package de.triology.cas.logging;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent.Builder;
import org.apache.logging.log4j.message.SimpleMessageFactory;
import org.junit.Assert;
import org.junit.Test;

public class DefaultMappingPasswordRewritePolicyTest {

    @Test
    public void rewriteMessage() {
        String message = "Adding mapping result [Success@7636f00b mapping = parameter:'password' -> password, code = 'success', error = false, originalValue = 'adminpw', mappedValue = array<Character>[a, d, m, i ,n, p, w]";

        Builder builder = new Builder();
        builder.setMessage(SimpleMessageFactory.INSTANCE.newMessage(message));

        LogEvent rewrittenLogEvent = DefaultMappingPasswordRewritePolicy.createPolicy().rewrite(builder.build());

        Assert.assertEquals("this log line contained sensitive data and was removed",
                rewrittenLogEvent.getMessage().getFormattedMessage());
    }

    @Test
    public void rewriteMessageWhenMessageIsNull() {
        Builder builder = new Builder();
        builder.setMessage(null);

        LogEvent logEvent = builder.build();
        LogEvent rewrittenLogEvent = DefaultMappingPasswordRewritePolicy.createPolicy().rewrite(logEvent);

        Assert.assertSame(logEvent, rewrittenLogEvent);
    }
}