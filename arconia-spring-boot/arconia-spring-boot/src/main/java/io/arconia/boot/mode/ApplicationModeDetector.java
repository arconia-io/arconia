package io.arconia.boot.mode;

import org.springframework.core.NativeDetector;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * Heuristics to help determine the application mode.
 */
final class ApplicationModeDetector {

    /**
     * Heuristically determines if the application could be running in development mode.
     */
    static boolean isDevelopmentModeDetected(@Nullable ClassLoader classLoader) {
        if (classLoader == null) {
            return false;
        }

        // 1. Check if the application is running in a native image
        if (NativeDetector.inNativeImage()) {
            return false;
        }

        // 2. Check if Spring Boot DevTools is present
        if (ClassUtils.isPresent("org.springframework.boot.devtools.RemoteSpringApplication", classLoader)) {
            return true;
        }

        // 3. Check if the class loader is the one used by Java at development time
        if (classLoader.getClass().getName().contains("AppClassLoader")) {
            return true;
        }

        return false;
    }

    /**
     * Heuristically determines if the application could be running in test mode.
     */
    static boolean isTestModeDetected(@Nullable ClassLoader classLoader) {
        if (classLoader == null) {
            return false;
        }

        // 1. Check if Spring Boot Test is present
        if (ClassUtils.isPresent("org.springframework.boot.test.context.SpringBootTest", classLoader)) {
            return true;
        }

        // 2. Check if Spring Test is present
        if (ClassUtils.isPresent("org.springframework.test.context.TestContext", classLoader)) {
            return true;
        }

        return false;
    }

}
