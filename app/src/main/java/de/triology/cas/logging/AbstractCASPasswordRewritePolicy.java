package de.triology.cas.logging;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.rewrite.RewritePolicy;
import org.apache.logging.log4j.core.impl.Log4jLogEvent.Builder;
import org.apache.logging.log4j.message.SimpleMessageFactory;

/*
 * Abstract class for rewriting password outputs.
 * <p>
 * In order to ensure data protection, the passwords of CAS must not be logged in plain text.
 * </p>
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
     * Checks whether the passed log event contains the password.
     *
     * @param source the log event to be checked
     * @return true if the message of the given log content contains information about the password
     */
    protected boolean containsPassword(LogEvent source) {
        String formattedMessage = LogUtils.getFormattedMessage(source);

        return formattedMessage != null && formattedMessage.contains(getPasswordFlag());
    }


    /**
     * Returns the flag to identify the password part of the log message.
     *
     * @return the flag to identify the password part of the log message.
     */
    protected abstract String getPasswordFlag();

    /**
     * Replaces the value of a password.
     *
     * @param originMessage the origin log message
     * @return the modified log message with replaced password value.
     */
    protected abstract String replacePasswordValue(String originMessage);
}