package de.triology.cas.logging;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.rewrite.RewritePolicy;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent.Builder;
import org.apache.logging.log4j.message.MessageFormatMessage;

@Plugin(
        name = "PasswordRewritePolicy",
        category = "Core",
        elementType = "rewritePolicy",
        printObject = true
)
public final class PasswordRewritePolicy implements RewritePolicy {
    public PasswordRewritePolicy() {
        //
    }

    public LogEvent rewrite(LogEvent event) {
        Builder builder = new Builder(event);
        String originMessage = event.getMessage().getFormattedMessage();
        if (originMessage.contains("parameter:'password'")) {
            originMessage = this.replacePasswordValue(originMessage);
        }

        builder.setMessage(new MessageFormatMessage(originMessage, new Object[0]));
        return builder.build();
    }

    @PluginFactory
    public static PasswordRewritePolicy createPolicy() {
        return new PasswordRewritePolicy();
    }

    /**
     * Replaces the value of a password with '****'.
     * <p>
     * In order to ensure data protection, the passwords of CAS must not be logged in plain text.
     * </p>
     *
     * @param originMessage the origin log message
     * @return the modified log message with replaced password value.
     */
    private String replacePasswordValue(String originMessage) {
        String modifiedMessage = originMessage.replaceAll("mappedValue = '\\S+'", "mappedValue = '****'");
        modifiedMessage = modifiedMessage.replaceAll("originalValue = '\\S+'", "originValue = '****'");

        return modifiedMessage;
    }
}