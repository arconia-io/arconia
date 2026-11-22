package io.arconia.dev.ui.server.web;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.devtools.restart.Restarter;
import org.springframework.util.ClassUtils;

/**
 * Restarts the application with Spring Boot DevTools, the way DevTools itself does after a
 * change in the code.
 * <p>
 * That takes DevTools on the class path, and its restart support in use: DevTools leaves it
 * out when the application turns it off, runs from a jar, or is started by a test.
 */
final class DevToolsRestart {

    private static final Logger logger = LoggerFactory.getLogger(DevToolsRestart.class);

    private static final String RESTARTER_CLASS = "org.springframework.boot.devtools.restart.Restarter";

    /**
     * How long to wait, in milliseconds, before restarting, so that the request asking for
     * the restart is answered first. DevTools waits for the requests in progress when it stops
     * the application.
     */
    private static final long DELAY = 250;

    private DevToolsRestart() {}

    /**
     * Whether the application uses Spring Boot DevTools.
     */
    static boolean isPresent(@Nullable ClassLoader classLoader) {
        return ClassUtils.isPresent(RESTARTER_CLASS, classLoader);
    }

    /**
     * Whether the application can be restarted.
     */
    static boolean isAvailable(@Nullable ClassLoader classLoader) {
        return isPresent(classLoader) && Restart.isInUse();
    }

    /**
     * Restarts the application, a moment from now and without waiting for it.
     */
    static void restart() {
        Thread.ofPlatform().name("arconia-dev-ui-restart").daemon().start(() -> {
            try {
                Thread.sleep(DELAY);
                Restart.now();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (RuntimeException ex) {
                logger.warn("The application could not be restarted from the Dev UI", ex);
            }
        });
    }

    /**
     * Only loaded when DevTools is on the class path.
     */
    private static final class Restart {

        static boolean isInUse() {
            try {
                return Restarter.getInstance().getInitialUrls() != null;
            } catch (IllegalStateException ex) {
                // DevTools has not set up its restart support.
                return false;
            }
        }

        static void now() {
            Restarter.getInstance().restart();
        }

    }

}
