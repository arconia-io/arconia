package io.arconia.boot.autoconfigure.bootstrap;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import io.arconia.boot.autoconfigure.bootstrap.dev.BootstrapDevProperties;
import io.arconia.boot.autoconfigure.bootstrap.test.BootstrapTestProperties;
import io.arconia.boot.bootstrap.BootstrapMode;

/**
 * Configures the environment for the application based on the bootstrap mode.
 */
class BootstrapEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(BootstrapEnvironmentPostProcessor.class);

    @Override
    @SuppressWarnings("unchecked")
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Assert.notNull(environment, "environment cannot be null");
        Assert.notNull(application, "application cannot be null");

        Boolean profilesEnabled = environment.getProperty(BootstrapProperties.CONFIG_PREFIX + ".profiles.enabled", Boolean.class, true);
        if (!profilesEnabled) {
            return;
        }

        List<String> currentProfiles = environment.getProperty(StandardEnvironment.ACTIVE_PROFILES_PROPERTY_NAME, List.class, List.of());
        List<String> additionalProfiles = new ArrayList<>();

        BootstrapMode mode = BootstrapMode.detect();

        switch (mode) {
            case DEV -> {
                logger.info("The application is running in dev mode");
                List<String> developmentProfiles = environment.getProperty(BootstrapDevProperties.CONFIG_PREFIX + ".profiles", List.class, List.of("dev"));
                if (!developmentProfiles.isEmpty()) {
                    for (String profile : developmentProfiles) {
                        if (StringUtils.hasText(profile) && !currentProfiles.contains(profile)) {
                            logger.debug("Adding active profile '{}' for dev mode", profile);
                            additionalProfiles.add(profile);
                        }
                    }
                }
            }
            case TEST -> {
                logger.info("The application is running in test mode");
                List<String> testProfiles = environment.getProperty(BootstrapTestProperties.CONFIG_PREFIX + ".profiles", List.class, List.of("test"));
                if (!testProfiles.isEmpty()) {
                    for (String profile : testProfiles) {
                        if (StringUtils.hasText(profile) && !currentProfiles.contains(profile)) {
                            logger.debug("Adding active profile '{}' for test mode", profile);
                            additionalProfiles.add(profile);
                        }
                    }
                }
            }
            case PROD -> {
                // No additional profiles for prod mode
                logger.debug("The application is running in prod mode");
            }
        }

        ConfigDataEnvironmentPostProcessor.applyTo(environment, application.getResourceLoader(), null, additionalProfiles);
    }

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER + 5;
    }

}
