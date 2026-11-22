package io.arconia.dev.ui.server.logging;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.logging.LoggingApplicationListener;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.util.ClassUtils;

import io.arconia.core.annotations.Internal;
import io.arconia.core.bootstrap.BootstrapMode;

/**
 * Starts capturing the logs of the application for the console of the Dev UI, as early as the
 * application has set up its logging, so that the console also shows how the application
 * started. It does so for the applications the Dev UI is served in: servlet web applications
 * in dev mode, unless the Dev UI is turned off.
 * <p>
 * Spring Boot sets up logging again each time the application is restarted, which drops the
 * capture, and this listener puts it back.
 */
@Internal
public final class ApplicationLogsListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

    private static final String LOGBACK_CLASS = "ch.qos.logback.classic.LoggerContext";

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        if (!BootstrapMode.isDev() || !event.getEnvironment().getProperty("arconia.dev.ui.enabled", Boolean.class, true)
                || event.getSpringApplication().getWebApplicationType() != WebApplicationType.SERVLET) {
            return;
        }
        if (ClassUtils.isPresent(LOGBACK_CLASS, getClass().getClassLoader()) && LogbackCapture.start(ApplicationLogs.get())) {
            ApplicationLogs.get().startCapturing();
        }
    }

    /**
     * After the listener of Spring Boot that sets up logging.
     */
    @Override
    public int getOrder() {
        return LoggingApplicationListener.DEFAULT_ORDER + 1;
    }

}
