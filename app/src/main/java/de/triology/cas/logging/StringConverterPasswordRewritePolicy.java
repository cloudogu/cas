package de.triology.cas.logging;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.rewrite.RewritePolicy;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent.Builder;
import org.apache.logging.log4j.message.SimpleMessageFactory;
import org.apache.logging.log4j.util.Strings;


@Plugin(
        name = "StringConverterPasswordRewritePolicy",
        category = "Core",
        elementType = "rewritePolicy",
        printObject = true
)
/*
 * Password rewriter for class org.apache.commons.beanutils.converters.StringConverter.
 */
public final class StringConverterPasswordRewritePolicy implements RewritePolicy {
    @PluginFactory
    public static StringConverterPasswordRewritePolicy createPolicy() {
        return new StringConverterPasswordRewritePolicy();
    }

    private StringConverterPasswordRewritePolicy() {
        //
    }

    @Override
    public LogEvent rewrite(LogEvent source) {
        String originMessage = LogUtils.getFormattedMessage(source);

        // Since there is no unique pattern for recognising passwords, the value is removed from all log messages.
        if (originMessage != null) {
            String modifiedMessage = removeValue(originMessage);

            Builder builder = new Builder(source);
            builder.setMessage(SimpleMessageFactory.INSTANCE.newMessage(modifiedMessage));

            return builder.build();
        }

        return source;
    }

    /**
     * Removes the value part in the given log message.
     * <p>
     * In order to ensure data protection, the passwords of CAS must not be logged in plain text.
     * </p>
     *
     * @param originMessage the origin log message
     * @return the modified log message with removed value.
     */
    private String removeValue(String originMessage) {
        return originMessage.replaceAll(" value '\\S+'", Strings.EMPTY);
    }
}