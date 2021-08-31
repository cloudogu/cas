package de.triology.cas.logging;

import junit.framework.TestCase;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessageFactory;
import org.junit.Assert;
import org.junit.Test;
import org.apache.logging.log4j.core.impl.Log4jLogEvent.Builder;
public class StringConverterPasswordRewritePolicyTest {

    @Test
    public void rewriteMessage() {
        String message = "<Converting 'String' value 'adminpw' to type 'String'>";

        Builder builder = new Builder();
        builder.setMessage(SimpleMessageFactory.INSTANCE.newMessage(message));

        LogEvent rewrittenLogEvent = StringConverterPasswordRewritePolicy.createPolicy().rewrite(builder.build());

        Assert.assertEquals("<Converting 'String' to type 'String'>", rewrittenLogEvent.getMessage().getFormattedMessage());
    }

    @Test
    public void rewriteMessageWhenMessageIsNull() {
        Builder builder = new Builder();
        builder.setMessage(null);

        LogEvent logEvent = builder.build();
        LogEvent rewrittenLogEvent = StringConverterPasswordRewritePolicy.createPolicy().rewrite(logEvent);

        Assert.assertSame(logEvent, rewrittenLogEvent);
    }
}