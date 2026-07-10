package io.arconia.boot.bootstrap;

import java.io.File;
import java.util.Locale;
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

    private static final Set<String> TEST_CLASS_PREFIXES = Set.of(
        "org.junit.runners.",
        "org.junit.platform.",
        "org.springframework.boot.test.",
        "org.testng.",
        "io.cucumber.",
        "cucumber.runtime.");

    @Nullable
    private static volatile BootstrapMode cachedMode;
    private static final Object LOCK = new Object();

    static BootstrapMode detect() {
        return detect(null);
    }

    static BootstrapMode detect(StackTraceElement @Nullable [] stackTraceElements) {
        if (cachedMode == null) {
            synchronized (LOCK) {
                if (cachedMode == null) {
                    cachedMode = doDetect(stackTraceElements);
                    logger.debug("Detected bootstrap mode: {}", cachedMode);
                }
            }
        }
        return cachedMode;
    }

    static void clearCache() {
        cachedMode = null;
    }

    private static BootstrapMode doDetect(StackTraceElement @Nullable [] stackTraceElements) {
        StackTraceElement[] stackTrace = (stackTraceElements == null || stackTraceElements.length == 0)
                ? Thread.currentThread().getStackTrace() : stackTraceElements;

        // 1. Check for an explicit mode: environment variable or JVM system property.
        String modeProperty = System.getenv(BootstrapMode.PROPERTY_KEY.toUpperCase(Locale.ROOT).replace(".", "_"));
        if (!StringUtils.hasText(modeProperty)) {
            modeProperty = System.getProperty(BootstrapMode.PROPERTY_KEY);
        }
        if (StringUtils.hasText(modeProperty)) {
            String normalizedMode = modeProperty.strip().toUpperCase(Locale.ROOT);
            if (BootstrapMode.isValid(normalizedMode)) {
                BootstrapMode mode = BootstrapMode.valueOf(normalizedMode);
                if (mode != BootstrapMode.PROD && containsAotProcessor(stackTrace)) {
                    logger.warn("The bootstrap mode is explicitly set to {} while Spring AOT processing is running. "
                            + "The {} mode behavior will be baked into the AOT artifacts.", mode, mode);
                }
                return mode;
            }
            logger.warn("Invalid {} property value: '{}'. Defaulting to PROD mode.", BootstrapMode.PROPERTY_KEY, modeProperty);
            return BootstrapMode.PROD;
        }

        // 2. Check the stack trace for known class prefixes that indicate a certain mode.
        if (containsAotProcessor(stackTrace)) {
            return BootstrapMode.PROD;
        }
        for (StackTraceElement element : stackTrace) {
            for (String prefix : TEST_CLASS_PREFIXES) {
                if (element.getClassName().startsWith(prefix)) {
                    return BootstrapMode.TEST;
                }
            }
        }

        // 3. Check if running in a native image context.
        if (isNativeContext()) {
            return BootstrapMode.PROD;
        }

        // 4. Check if running in a development context.
        if (isDevelopmentContext()) {
            return BootstrapMode.DEV;
        }

        return BootstrapMode.PROD;
    }

    private static boolean containsAotProcessor(StackTraceElement[] stackTrace) {
        for (StackTraceElement element : stackTrace) {
            if (element.getClassName().startsWith(SpringApplicationAotProcessor.class.getName())) {
                return true;
            }
        }
        return false;
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
        // 2. Check if the class loader is the one used by Java at development time.
        // Packaged Spring Boot applications run under LaunchedClassLoader or a container class loader, never AppClassLoader.
        if (classLoader == null || !classLoader.getClass().getName().contains("AppClassLoader")) {
            return false;
        }
        // 3. Check if the application is running from the output directories of a build tool or IDE,
        // which only happens at development time. Packaged applications (jar, exploded jar,
        // container image) run from different locations, so they are not affected.
        return isDevelopmentClassPath(System.getProperty("java.class.path"));
    }

    static boolean isDevelopmentClassPath(@Nullable String classPath) {
        if (!StringUtils.hasText(classPath)) {
            return false;
        }
        for (String entry : classPath.split(File.pathSeparator)) {
            String path = entry.replace('\\', '/');
            if (path.contains("/build/classes/")            // Gradle
                    || path.endsWith("/target/classes")     // Maven
                    || path.contains("/out/production/")    // IntelliJ IDEA
                    || path.endsWith("/bin/main")) {        // Eclipse (Buildship)
                return true;
            }
        }
        return false;
    }

}
