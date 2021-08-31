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
            String modifiedMessage = truncateLogMessage(originMessage);

            var builder = new Builder(source);
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
         String truncatedMessage = null;

         if (originMessage != null) {
             // There is no unique pattern for recognising the password. Therefore, the log message is truncated here
             // after the 1st line break. The password is only in one of the following lines.
             int firstLineBreak = originMessage.indexOf('\n');
             truncatedMessage = originMessage.substring(0, firstLineBreak);
         }

         return truncatedMessage;
     }
}