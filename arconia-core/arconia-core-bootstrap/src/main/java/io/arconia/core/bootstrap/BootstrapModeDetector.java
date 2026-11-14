package io.arconia.core.bootstrap;

import java.io.File;
import java.util.Locale;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.SpringApplicationAotProcessor;
import org.springframework.core.NativeDetector;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import io.arconia.core.annotations.Internal;

/**
 * Detects the application bootstrap mode based on various heuristics.
 * <p>
 * The mode detected by the heuristics is cached for the lifetime of the JVM, so that it stays
 * the same across Spring Boot DevTools restarts. A mode set explicitly always takes precedence.
 * <p>
 * The detection doesn't log, since it typically runs before the logging system is
 * initialized. The detected mode and any problem with an explicit one are reported
 * by the environment post-processor activating the bootstrap profiles.
 */
@Internal
public final class BootstrapModeDetector {

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
        // An explicit mode is read on each call rather than cached: it's cheap to resolve, and it
        // takes effect whenever it's set, even after the mode has already been detected.
        String explicitMode = getExplicitValue();
        if (explicitMode != null) {
            // An invalid value is reported by the environment post-processor activating the bootstrap profiles.
            BootstrapMode mode = parse(explicitMode);
            return mode != null ? mode : BootstrapMode.PROD;
        }

        // Read the volatile field once, so that a concurrent clearCache() cannot make this method return null.
        BootstrapMode mode = cachedMode;
        if (mode == null) {
            synchronized (LOCK) {
                mode = cachedMode;
                if (mode == null) {
                    mode = doDetect(stackTraceElements);
                    cachedMode = mode;
                }
            }
        }
        return mode;
    }

    static void clearCache() {
        cachedMode = null;
    }

    /**
     * Get the bootstrap mode set explicitly, as it was set, or {@code null} when it's not set.
     */
    public static @Nullable String getExplicitValue() {
        return getExplicitValue(System.getProperty(BootstrapMode.SYSTEM_PROPERTY), System.getenv(BootstrapMode.ENVIRONMENT_VARIABLE));
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

    private static BootstrapMode doDetect(StackTraceElement @Nullable [] stackTraceElements) {
        StackTraceElement[] stackTrace = (stackTraceElements == null || stackTraceElements.length == 0)
                ? Thread.currentThread().getStackTrace() : stackTraceElements;

        // 1. Check the stack trace for known class prefixes that indicate a certain mode.
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

        // 2. Check if running in a native image context.
        if (isNativeContext()) {
            return BootstrapMode.PROD;
        }

        // 3. Check if running in a development context.
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
