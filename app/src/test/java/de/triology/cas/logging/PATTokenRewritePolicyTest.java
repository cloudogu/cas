package de.triology.cas.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent.Builder;
import org.apache.logging.log4j.message.SimpleMessageFactory;
import org.junit.jupiter.api.Test;

class PATTokenRewritePolicyTest {

    @Test
    void masksCleartextPatsAndTokenFieldsCaseInsensitively() {
        LogEvent source = event("created pat_Abc-123 token=secret TOKEN_FINGERPRINT:deadbeef token\":\"json-secret");

        LogEvent rewritten = PATTokenRewritePolicy.createPolicy().rewrite(source);

        assertEquals("created ****** token=****** TOKEN_FINGERPRINT:****** token\":\"******", rewritten.getMessage().getFormattedMessage());
    }

    @Test
    void preservesEventWhenThereIsNothingToMask() {
        LogEvent source = event("ordinary message");
        assertSame(source, PATTokenRewritePolicy.createPolicy().rewrite(source));
    }

    @Test
    void preservesEventWithNullMessage() {
        LogEvent source = new Builder().setMessage(null).build();
        assertSame(source, PATTokenRewritePolicy.createPolicy().rewrite(source));
    }

    private static LogEvent event(String message) {
        return new Builder().setMessage(SimpleMessageFactory.INSTANCE.newMessage(message)).build();
    }
}
