package de.triology.cas.logging;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent.Builder;
import org.apache.logging.log4j.message.SimpleMessageFactory;
import org.junit.Assert;
import org.junit.Test;

public class LogUtilsTest {

    @Test
    public void getFormattedMessage() {
        String messageText = "It’s not a bird, it’s not a plane. It must be Dave who’s on the train";
        Builder builder = new Builder();
        builder.setMessage(SimpleMessageFactory.INSTANCE.newMessage(messageText));

        LogEvent logEvent = builder.build();
        String formattedMessage = LogUtils.getFormattedMessage(logEvent);

        Assert.assertEquals(formattedMessage, messageText);
    }

    @Test
    public void getFormattedMessageWhenLogEvenIsNull() {
        Assert.assertNull(LogUtils.getFormattedMessage(null));
    }

    @Test
    public void getFormattedMessageWhenMessageIsNull() {
        Builder builder = new Builder();
        builder.setMessage(null);

        LogEvent logEvent = builder.build();

        Assert.assertNull(LogUtils.getFormattedMessage(logEvent));
    }
}