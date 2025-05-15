package io.arconia.boot.autoconfigure.env;

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

import io.arconia.boot.mode.ApplicationMode;

/**
 * Configures profiles based on the application mode.
 */
class ProfilesEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(ProfilesEnvironmentPostProcessor.class);

    @Override
    @SuppressWarnings("unchecked")
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Assert.notNull(environment, "environment cannot be null");
        Assert.notNull(application, "application cannot be null");

        Boolean profilesEnabled = environment.getProperty(ProfilesProperties.CONFIG_PREFIX + ".enabled", Boolean.class, true);
        if (!profilesEnabled) {
            return;
        }

        List<String> currentProfiles = environment.getProperty(StandardEnvironment.ACTIVE_PROFILES_PROPERTY_NAME, List.class, List.of());
        List<String> additionalProfiles = new ArrayList<>();

        ApplicationMode mode = ApplicationMode.of(application.getClassLoader());

        switch (mode) {
            case DEVELOPMENT -> {
                List<String> developmentProfiles = environment.getProperty(ProfilesProperties.CONFIG_PREFIX + ".development", List.class, List.of("dev"));
                if (!developmentProfiles.isEmpty()) {
                    for (String profile : developmentProfiles) {
                        if (StringUtils.hasText(profile) && !currentProfiles.contains(profile)) {
                            logger.debug("Adding active profile '{}' for development mode", profile);
                            additionalProfiles.add(profile);
                        }
                    }
                }
            }
            case TEST -> {
                List<String> testProfiles = environment.getProperty(ProfilesProperties.CONFIG_PREFIX + ".test", List.class, List.of("test"));
                if (!testProfiles.isEmpty()) {
                    for (String profile : testProfiles) {
                        if (StringUtils.hasText(profile) && !currentProfiles.contains(profile)) {
                            logger.debug("Adding active profile '{}' for test mode", profile);
                            additionalProfiles.add(profile);
                        }
                    }
                }
            }
            case PRODUCTION -> {
                List<String> productionProfiles = environment.getProperty(ProfilesProperties.CONFIG_PREFIX + ".production", List.class, List.of("prod"));
                if (!productionProfiles.isEmpty()) {
                    for (String profile : productionProfiles) {
                        if (StringUtils.hasText(profile) && !currentProfiles.contains(profile)) {
                            logger.debug("Adding active profile '{}' for production mode", profile);
                            additionalProfiles.add(profile);
                        }
                    }
                }
            }
        }

        ConfigDataEnvironmentPostProcessor.applyTo(environment, application.getResourceLoader(), null, additionalProfiles);
    }

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER + 5;
    }

}
