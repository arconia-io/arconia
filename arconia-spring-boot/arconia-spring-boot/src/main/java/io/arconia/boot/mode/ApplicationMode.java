package io.arconia.boot.mode;

import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;

/**
 * The mode used to run the application.
 */
@Incubating
public enum ApplicationMode {

    DEVELOPMENT,
    TEST,
    PRODUCTION;

    /**
     * Determines the application mode with heuristics.
     */
    public static ApplicationMode of(ApplicationContext applicationContext) {
        Assert.notNull(applicationContext, "applicationContext cannot be null");
        return ApplicationMode.of(getClassLoader(applicationContext));
    }

    /**
     * Determines the application mode with heuristics.
     */
    public static ApplicationMode of(@Nullable ClassLoader classLoader) {
        if (ApplicationModeDetector.isTestModeDetected(classLoader)) {
            return ApplicationMode.TEST;
        } else if (ApplicationModeDetector.isDevelopmentModeDetected(classLoader)) {
            return ApplicationMode.DEVELOPMENT;
        } else {
            return ApplicationMode.PRODUCTION;
        }
    }

    @Nullable
    private static ClassLoader getClassLoader(ApplicationContext applicationContext) {
        ClassLoader classLoader = applicationContext.getClassLoader();
        return classLoader != null ? classLoader : Thread.currentThread().getContextClassLoader();
    }

}
