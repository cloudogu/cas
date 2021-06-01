package de.triology.cas.logging;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.rewrite.RewritePolicy;
import org.apache.logging.log4j.core.impl.Log4jLogEvent.Builder;
import org.apache.logging.log4j.message.SimpleMessageFactory;

/*
 * Abstract class for rewriting password outputs.
 */
public abstract class AbstractCASPasswordRewritePolicy implements RewritePolicy {

    @Override
    public LogEvent rewrite(LogEvent source) {
        if (containsPassword(source)) {
            String originMessage = LogUtils.getFormattedMessage(source);
            String modifiedMessage = replacePasswordValue(originMessage);

            var builder = new Builder(source);
            builder.setMessage(SimpleMessageFactory.INSTANCE.newMessage(modifiedMessage));

            return builder.build();
        }

        return source;
    }

    /**
     * @return true if the message of the given log content contains information about the password
     */
    protected abstract boolean containsPassword(LogEvent source);

    /**
     * Replaces the value of a password.
     * <p>
     * In order to ensure data protection, the passwords of CAS must not be logged in plain text.
     * </p>
     *
     * @param originMessage the origin log message
     * @return the modified log message with replaced password value.
     */
    protected abstract String replacePasswordValue(String originMessage);
}