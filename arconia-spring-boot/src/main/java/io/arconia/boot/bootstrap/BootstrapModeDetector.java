package io.arconia.boot.bootstrap;

import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplicationAotProcessor;
import org.springframework.core.NativeDetector;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Detects the application bootstrap mode based on various heuristics.
 */
final class BootstrapModeDetector {

    private static final Logger logger = LoggerFactory.getLogger(BootstrapModeDetector.class);

    @Nullable
    private static volatile BootstrapMode cachedMode;
    private static final Object LOCK = new Object();

    static BootstrapMode detect(StackTraceElement @Nullable ... stackTraceElements) {
        if (cachedMode == null) {
            synchronized (LOCK) {
                if (cachedMode == null) {
                    cachedMode = doDetect(stackTraceElements);
                }
            }
        }
        return cachedMode;
    }

    static void clearCache() {
        cachedMode = null;
    }

    private static BootstrapMode doDetect(StackTraceElement @Nullable ... stackTraceElements) {
        // 1. Check for JVM system property set by the Arconia CLI.
        String modeProperty = System.getProperty(BootstrapMode.PROPERTY_KEY);
        if (StringUtils.hasText(modeProperty)) {
            if (BootstrapMode.isValid(modeProperty.toUpperCase())) {
                return BootstrapMode.valueOf(modeProperty.toUpperCase());
            }
            logger.warn("Invalid {} property value: '{}'. Defaulting to PROD mode.", BootstrapMode.PROPERTY_KEY, modeProperty);
            return BootstrapMode.PROD;
        }

        // 2. Check the stack trace for known class prefixes that indicate a certain mode.
        long startTime = System.nanoTime();
        StackTraceElement[] stackTrace = Objects.isNull(stackTraceElements) ? Thread.currentThread().getStackTrace() : stackTraceElements;
        Set<String> testClassPrefixes = Set.of(
            "org.junit.runners.",
            "org.junit.platform.",
            "org.springframework.boot.test.",
            "cucumber.runtime.");
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            if (className.startsWith(SpringApplicationAotProcessor.class.getName())) {
                logger.debug("Prod bootstrap mode detection from stack trace took {} ns", System.nanoTime() - startTime);
                return BootstrapMode.PROD;
            }
            for (String prefix : testClassPrefixes) {
                if (className.startsWith(prefix)) {
                    logger.debug("Test bootstrap mode detection from stack trace took {} ns", System.nanoTime() - startTime);
                    return BootstrapMode.TEST;
                }
            }
        }
        logger.debug("Bootstrap mode detection from stack trace took {} ns", System.nanoTime() - startTime);

        // 3. Check if running in native image context.
        if (isNativeContext()) {
            return BootstrapMode.PROD;
        }

        // 4. Check if running in development context.
        if (isDevelopmentContext()) {
            return BootstrapMode.DEV;
        }

        return BootstrapMode.PROD;
    }

    static boolean isNativeContext() {
        return NativeDetector.inNativeImage();
    }

    static boolean isDevelopmentContext() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        // 1. Check if Spring Boot DevTools is present in the class loader.
        if (ClassUtils.isPresent("org.springframework.boot.devtools.RemoteSpringApplication", classLoader)) {
            return true;
        }
        // 2. Check if the class loader is the one used by Java at development time
        if (classLoader != null && classLoader.getClass().getName().contains("AppClassLoader")) {
            return true;
        }
        return false;
    }

}
