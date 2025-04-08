package io.arconia.dev.tools.profiles;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Configures development and test profiles.
 */
public class ProfilesEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(ProfilesEnvironmentPostProcessor.class);

    @Override
    @SuppressWarnings("unchecked")
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Assert.notNull(environment, "environment cannot be null");

        Boolean profilesEnabled = environment.getProperty("arconia.dev.profiles.enabled", Boolean.class, true);
        if (!profilesEnabled) {
            return;
        }

        boolean isTestEnvironment = ClassUtils.isPresent("org.junit.jupiter.api.Test", application.getClassLoader());

        List<String> devProfiles = environment.getProperty("arconia.dev.profiles.development", List.class, List.of("dev"));
        if (!isTestEnvironment && !devProfiles.isEmpty()) {
            for (String profile : devProfiles) {
                if (StringUtils.hasText(profile)) {
                    logger.debug("Adding active development profile '{}'", profile);
                    environment.addActiveProfile(profile);
                }
            }
        }

        List<String> testProfiles = environment.getProperty("arconia.dev.profiles.test", List.class, List.of("test"));
        if (isTestEnvironment && !testProfiles.isEmpty()) {
            for (String profile : testProfiles) {
                if (StringUtils.hasText(profile)) {
                    logger.debug("Adding active test profile '{}'", profile);
                    environment.addActiveProfile(profile);
                }
            }
        }
    }

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER - 10;
    }

}
