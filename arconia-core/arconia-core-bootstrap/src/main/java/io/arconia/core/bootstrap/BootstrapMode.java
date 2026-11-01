package io.arconia.core.bootstrap;

import java.util.Locale;

import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

import io.arconia.core.annotations.Incubating;
import io.arconia.core.annotations.Internal;

/**
 * The mode used to bootstrap the application.
 */
@Incubating
public enum BootstrapMode {

    DEV,
    TEST,
    PROD;

    /**
     * JVM system property to set the bootstrap mode explicitly.
     */
    public static final String PROPERTY_KEY = "arconia.bootstrap.mode";

    /**
     * Environment variable to set the bootstrap mode explicitly.
     */
    public static final String ENVIRONMENT_VARIABLE = "ARCONIA_BOOTSTRAP_MODE";

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

    /**
     * Get the bootstrap mode set explicitly, as it was set, or {@code null} when it's not set.
     */
    @Internal
    public static @Nullable String getExplicitValue() {
        return getExplicitValue(System.getProperty(PROPERTY_KEY), System.getenv(ENVIRONMENT_VARIABLE));
    }

    static @Nullable String getExplicitValue(@Nullable String systemProperty, @Nullable String environmentVariable) {
        if (StringUtils.hasText(systemProperty)) {
            return systemProperty;
        }
        return StringUtils.hasText(environmentVariable) ? environmentVariable : null;
    }

    /**
     * Parses a bootstrap mode, case-insensitively and ignoring surrounding whitespace.
     * Returns {@code null} when the value is not a valid bootstrap mode.
     */
    @Internal
    public static @Nullable BootstrapMode parse(@Nullable String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return BootstrapMode.valueOf(value.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

}
