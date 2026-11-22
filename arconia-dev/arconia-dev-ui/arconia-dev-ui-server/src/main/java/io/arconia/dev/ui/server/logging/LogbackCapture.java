package io.arconia.dev.ui.server.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

/**
 * Captures the logs of an application that logs with Logback, with an appender on the root
 * logger that passes every line to the logs kept for the Dev UI. The appender comes on top of
 * the ones of the application and changes nothing about them.
 * <p>
 * The class is only loaded when Logback is on the class path.
 */
final class LogbackCapture {

    static final String APPENDER_NAME = "ARCONIA_DEV_UI";

    /**
     * The longest message or stack trace kept in full, in characters.
     */
    static final int MAX_LENGTH = 16_000;

    private LogbackCapture() {}

    /**
     * Starts capturing, unless Logback is not the logging system in use or the capture is
     * already in place. Returns whether it started.
     */
    static boolean start(ApplicationLogs logs) {
        ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
        if (!(loggerFactory instanceof LoggerContext loggerContext)) {
            return false;
        }
        Logger rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        if (rootLogger.getAppender(APPENDER_NAME) != null) {
            return false;
        }
        CapturingAppender appender = new CapturingAppender(logs);
        appender.setName(APPENDER_NAME);
        appender.setContext(loggerContext);
        appender.start();
        rootLogger.addAppender(appender);
        return true;
    }

    /**
     * Cuts a text that is too long to keep. The lines are kept in memory, and an application
     * can log whole documents.
     */
    static String shortened(String text) {
        if (text.length() <= MAX_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_LENGTH) + "... (" + (text.length() - MAX_LENGTH) + " more characters)";
    }

    /**
     * The appender doesn't make the threads of the application wait for each other: the logs
     * it passes the lines to take care of that for the short time they need.
     */
    private static final class CapturingAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

        private final ApplicationLogs logs;

        private CapturingAppender(ApplicationLogs logs) {
            this.logs = logs;
        }

        @Override
        protected void append(ILoggingEvent event) {
            IThrowableProxy throwable = event.getThrowableProxy();
            this.logs.add(new LogLine(event.getTimeStamp(), event.getLevel().toString(), event.getLoggerName(),
                    shortened(event.getFormattedMessage()),
                    (throwable != null) ? shortened(ThrowableProxyUtil.asString(throwable).strip()) : null, false));
        }

    }

}
