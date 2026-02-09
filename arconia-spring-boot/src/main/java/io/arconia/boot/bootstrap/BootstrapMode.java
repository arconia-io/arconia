package io.arconia.boot.bootstrap;

import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

import io.arconia.core.support.Incubating;
import io.arconia.core.support.Internal;

/**
 * The mode used to bootstrap the application.
 */
@Incubating(since = "0.13.0")
public enum BootstrapMode {

    DEV,
    TEST,
    PROD;

    public static final String PROPERTY_KEY = "arconia.bootstrap.mode";

    /**
     * Determines the application bootstrap mode with heuristics.
     */
    public static BootstrapMode detect() {
        return BootstrapModeDetector.detect();
    }

    /**
     * Whether the application is running in dev mode.
     */
    public static boolean isDev() {
        return detect() == DEV;
    }

    /**
     * Whether the application is running in test mode.
     */
    public static boolean isTest() {
        return detect() == TEST;
    }

    /**
     * Clears the detected bootstrap mode cache.
     * Do NOT use this method in production code.
     */
    @Internal
    public static void clear() {
        BootstrapModeDetector.clearCache();
    }

    static boolean isValid(@Nullable String modeProperty) {
        if (!StringUtils.hasText(modeProperty)) {
            return false;
        }

        try {
            BootstrapMode.valueOf(modeProperty.strip().toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

}
