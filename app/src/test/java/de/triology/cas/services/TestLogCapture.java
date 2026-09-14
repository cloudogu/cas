package de.triology.cas.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Attaches an in-memory appender to the application logging backend for a focused test.
 */
final class TestLogCapture implements AutoCloseable {

    private final Logger logger;
    private final InMemoryAppender appender;
    private final Level previousLevel;

    private TestLogCapture() {
        logger = (Logger) LogManager.getRootLogger();
        previousLevel = logger.getLevel();
        logger.setLevel(Level.ALL);
        appender = new InMemoryAppender();
        appender.start();
        logger.addAppender(appender);
    }

    static TestLogCapture start() {
        return new TestLogCapture();
    }

    List<LogEvent> events() {
        return appender.events;
    }

    @Override
    public void close() {
        logger.removeAppender(appender);
        logger.setLevel(previousLevel);
        appender.stop();
    }

    private static final class InMemoryAppender extends AbstractAppender {

        private final List<LogEvent> events = new CopyOnWriteArrayList<>();

        private InMemoryAppender() {
            super("test-log-capture", null, PatternLayout.createDefaultLayout(), false, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(LogEvent event) {
            events.add(event.toImmutable());
        }
    }
}
