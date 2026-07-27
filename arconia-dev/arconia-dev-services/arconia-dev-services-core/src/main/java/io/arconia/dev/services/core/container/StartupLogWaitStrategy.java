package io.arconia.dev.services.core.container;

import java.time.Duration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.logging.LogLevel;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;

import io.arconia.core.support.Incubating;

/**
 * A {@link WaitStrategy} decorator that writes the container's logs to the application log when the
 * wrapped strategy fails (for example, on a startup timeout), so an otherwise opaque startup failure
 * becomes diagnosable, then rethrows the original failure.
 * <p>
 * The logs are fetched on demand from the running-but-unready container ({@code getLogs()}), so no
 * continuous log consumer is needed. The level is configurable and {@code OFF} disables the dump.
 */
@Incubating
public class StartupLogWaitStrategy implements WaitStrategy {

    private static final Log logger = LogFactory.getLog(StartupLogWaitStrategy.class);

    private final WaitStrategy delegate;

    private final String serviceName;

    private final LogLevel logLevel;

    public StartupLogWaitStrategy(WaitStrategy delegate, String serviceName, LogLevel logLevel) {
        this.delegate = delegate;
        this.serviceName = serviceName;
        this.logLevel = logLevel;
    }

    @Override
    public void waitUntilReady(WaitStrategyTarget waitStrategyTarget) {
        try {
            delegate.waitUntilReady(waitStrategyTarget);
        } catch (RuntimeException ex) {
            if (logLevel != LogLevel.OFF) {
                logLevel.log(logger, "The '%s' dev service container failed to start. Container logs:%n%n%s"
                        .formatted(serviceName, retrieveLogs(waitStrategyTarget)));
            }
            throw ex;
        }
    }

    @Override
    public WaitStrategy withStartupTimeout(Duration startupTimeout) {
        delegate.withStartupTimeout(startupTimeout);
        return this;
    }

    /**
     * Fetch the container logs, degrading gracefully so a logging failure never masks the original
     * startup failure (mirrors Spring Boot's Docker Compose {@code retrieveLogsIfPossible}).
     */
    private static String retrieveLogs(WaitStrategyTarget target) {
        try {
            return target.getLogs();
        } catch (Exception ex) {
            return "<unavailable>";
        }
    }

}
