package io.arconia.core.bootstrap;

import io.arconia.core.annotations.Incubating;

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
    public static final String SYSTEM_PROPERTY = "arconia.bootstrap.mode";

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

}
