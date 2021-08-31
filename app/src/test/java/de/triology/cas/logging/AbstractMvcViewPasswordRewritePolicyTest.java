package de.triology.cas.logging;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent.Builder;
import org.apache.logging.log4j.message.SimpleMessageFactory;
import org.junit.Assert;
import org.junit.Test;

public class AbstractMvcViewPasswordRewritePolicyTest {

    @Test
    public void rewriteMessage() {
        String message = "flowExecutionUrl=/cas/login?service=https%3A%2F%2F192.168.56.2%2Fcockpit%2F&username=admin&password=adminpww&execution";

        Builder builder = new Builder();
        builder.setMessage(SimpleMessageFactory.INSTANCE.newMessage(message));

        LogEvent rewrittenLogEvent = AbstractMvcViewPasswordRewritePolicy.createPolicy().rewrite(builder.build());

        Assert.assertEquals("flowExecutionUrl=/cas/login?service=https%3A%2F%2F192.168.56.2%2Fcockpit%2F&username=admin&password=****&execution",
                rewrittenLogEvent.getMessage().getFormattedMessage());
    }

    @Test
    public void rewriteMessageWhenMessageIsNull() {
        Builder builder = new Builder();
        builder.setMessage(null);

        LogEvent logEvent = builder.build();
        LogEvent rewrittenLogEvent = AbstractMvcViewPasswordRewritePolicy.createPolicy().rewrite(logEvent);

        Assert.assertSame(logEvent, rewrittenLogEvent);
    }
}