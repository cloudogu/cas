package de.triology.cas.logging;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.rewrite.RewritePolicy;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent.Builder;
import org.apache.logging.log4j.message.SimpleMessageFactory;

@Plugin(
        name = "LoggingHandlerPasswordRewritePolicy",
        category = "Core",
        elementType = "rewritePolicy",
        printObject = true
)
/*
 * Password rewriter for class io.netty.handler.logging.LoggingHandler.
 */
public final class LoggingHandlerPasswordRewritePolicy implements RewritePolicy {
    private static final String PARAMETER_WRITE = "WRITE";

    @PluginFactory
    public static LoggingHandlerPasswordRewritePolicy createPolicy() {
        return new LoggingHandlerPasswordRewritePolicy();
    }

    private LoggingHandlerPasswordRewritePolicy() {
        //
    }

    @Override
    public LogEvent rewrite(LogEvent source) {
        if (canContainPasswordText(source)) {
            String originMessage = LogUtils.getFormattedMessage(source);
            String modifiedMessage = originMessage != null ? truncateLogMessage(originMessage) : originMessage;

            Builder builder = new Builder(source);
            builder.setMessage(SimpleMessageFactory.INSTANCE.newMessage(modifiedMessage));

            return builder.build();
        }

        return source;
    }

    /**
     * @return true if the message of the given log event could potentially contain the password.
     */
    private boolean canContainPasswordText(LogEvent source) {
        String formattedMessage = LogUtils.getFormattedMessage(source);

        return formattedMessage != null && formattedMessage.contains(PARAMETER_WRITE);
    }

    /**
     * @return Return the log message truncated after the 1st line break
     */
     private String truncateLogMessage(String originMessage) {
        int firstLineBreak = originMessage.indexOf("\n");
        return originMessage.substring(0, firstLineBreak);
    }
}