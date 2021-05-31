package de.triology.cas.logging;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.rewrite.RewritePolicy;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent.Builder;
import org.apache.logging.log4j.message.SimpleMessageFactory;


@Plugin(
        name = "DefaultMappingPasswordRewritePolicy",
        category = "Core",
        elementType = "rewritePolicy",
        printObject = true
)
/*
  Password rewriter for class org.springframework.binding.mapping.impl.DefaultMapping.
 */
public final class DefaultMappingPasswordRewritePolicy implements RewritePolicy {
    private static final String PARAMETER_PASSWORD_TEXT = "parameter:'password'";

    @PluginFactory
    public static DefaultMappingPasswordRewritePolicy createPolicy() {
        return new DefaultMappingPasswordRewritePolicy();
    }

    private DefaultMappingPasswordRewritePolicy() {
        //
    }

    @Override
    public LogEvent rewrite(LogEvent source) {
        if (containsPassword(source)) {
            String originMessage = LogUtils.getFormattedMessage(source);
            String modifiedMessage = replacePasswordValue(originMessage);

            Builder builder = new Builder(source);
            builder.setMessage(SimpleMessageFactory.INSTANCE.newMessage(modifiedMessage));

            return builder.build();
        }

        return source;
    }


    /**
     * @return true if the message of the given log content contains information about the password
     */
    private boolean containsPassword(LogEvent source) {
        String formattedMessage = LogUtils.getFormattedMessage(source);

        return formattedMessage != null && formattedMessage.contains(PARAMETER_PASSWORD_TEXT);
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
        String modifiedMessage = null;
        if(originMessage != null) {
            modifiedMessage = originMessage.replaceAll("mappedValue = '\\S+'", "mappedValue = '****'");
            modifiedMessage = modifiedMessage.replaceAll("originalValue = '\\S+'", "originValue = '****'");
        }

        return modifiedMessage;
    }
}