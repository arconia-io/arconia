package io.arconia.boot.env;

import org.springframework.boot.SpringApplication;
import org.springframework.util.ClassUtils;

/**
 * The mode used to run the application.
 */
public enum ApplicationMode {

    DEVELOPMENT,
    TEST,
    PRODUCTION;

    /**
     * Determines the application mode based on the application classpath.
     */
    public static ApplicationMode of(SpringApplication application) {
        if (ClassUtils.isPresent("org.junit.jupiter.api.Test", application.getClassLoader())) {
            return ApplicationMode.TEST;
        } else if (ClassUtils.isPresent("org.springframework.boot.devtools.RemoteSpringApplication", application.getClassLoader())) {
            return ApplicationMode.DEVELOPMENT;
        } else {
            return ApplicationMode.PRODUCTION;
        }
    }

}
