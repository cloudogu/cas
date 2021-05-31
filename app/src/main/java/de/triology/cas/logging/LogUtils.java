package de.triology.cas.logging;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.message.Message;

/**
 * Utility class for logging
 */
public class LogUtils {

    private LogUtils() {
        //
    }

    /**
     * @return the formatted message of the given log event
     */
    public static String getFormattedMessage(LogEvent logEvent) {
        Message message = logEvent != null ? logEvent.getMessage() : null;

        return message != null ? message.getFormattedMessage() : null;
    }
}
