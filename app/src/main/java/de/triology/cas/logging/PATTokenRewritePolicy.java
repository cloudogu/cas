package de.triology.cas.logging;

import java.util.regex.Pattern;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.rewrite.RewritePolicy;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent.Builder;
import org.apache.logging.log4j.message.SimpleMessageFactory;

/**
 * Log4j rewrite policy that masks cleartext PATs and token fields in framework log messages.
 * It acts as a final safeguard in addition to log-safe PAT model representations.
 */
@Plugin(
        name = "PATTokenRewritePolicy",
        category = "Core",
        elementType = "rewritePolicy",
        printObject = true
)
public final class PATTokenRewritePolicy implements RewritePolicy {
    private static final Pattern TOKEN_FIELD_VALUE = Pattern.compile(
            "(?i)((?:token[_-]?fingerprint|token)(?:\\\"?\\s*[:=]\\s*\\\"?))([^\\\",}\\]\\s]+)");
    private static final Pattern CLEAR_TEXT_PAT = Pattern.compile(
            "(?i)(?<![a-z0-9_-])pat_[a-z0-9_-]+");

    /**
     * Prevents direct construction; Log4j creates instances through {@link #createPolicy()}.
     */
    private PATTokenRewritePolicy() {
    }

    /**
     * Creates the rewrite policy for Log4j plugin configuration.
     *
     * @return PAT token rewrite policy
     */
    @PluginFactory
    public static PATTokenRewritePolicy createPolicy() {
        return new PATTokenRewritePolicy();
    }

    /**
     * Rewrites token-like fields while preserving all other log event metadata.
     *
     * @param source original log event
     * @return original event when no token is present, otherwise a sanitized copy
     */
    @Override
    public LogEvent rewrite(LogEvent source) {
        String message = LogUtils.getFormattedMessage(source);
        if (message == null) {
            return source;
        }
        String sanitized = TOKEN_FIELD_VALUE.matcher(message).replaceAll("$1******");
        sanitized = CLEAR_TEXT_PAT.matcher(sanitized).replaceAll("******");
        if (message.equals(sanitized)) {
            return source;
        }
        return new Builder(source)
                .setMessage(SimpleMessageFactory.INSTANCE.newMessage(sanitized))
                .build();
    }
}
