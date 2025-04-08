package io.arconia.dev.tools.profiles;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.env.EnvironmentPostProcessorApplicationListener;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Configures development and test profiles.
 */
public class ProfilesApplicationListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(ProfilesApplicationListener.class);

    @Override
    @SuppressWarnings("unchecked")
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        Assert.notNull(event, "event cannot be null");

        ConfigurableEnvironment environment = event.getEnvironment();
        SpringApplication application = event.getSpringApplication();

        Boolean profilesEnabled = environment.getProperty("arconia.dev.profiles.enabled", Boolean.class, true);
        if (!profilesEnabled) {
            return;
        }

        List<String> additionalProfiles = new ArrayList<>();
        List<String> currentProfiles = environment.getProperty(StandardEnvironment.ACTIVE_PROFILES_PROPERTY_NAME, List.class, List.of());
        boolean isTestEnvironment = ClassUtils.isPresent("org.junit.jupiter.api.Test", application.getClassLoader());

        List<String> devProfiles = environment.getProperty("arconia.dev.profiles.development", List.class, List.of("dev"));
        if (!isTestEnvironment && !devProfiles.isEmpty()) {
            for (String profile : devProfiles) {
                if (StringUtils.hasText(profile) && !currentProfiles.contains(profile)) {
                    logger.debug("Adding active development profile '{}'", profile);
                    additionalProfiles.add(profile);
                }
            }
        }

        List<String> testProfiles = environment.getProperty("arconia.dev.profiles.test", List.class, List.of("test"));
        if (isTestEnvironment && !testProfiles.isEmpty()) {
            for (String profile : testProfiles) {
                if (StringUtils.hasText(profile) && !currentProfiles.contains(profile)) {
                    logger.debug("Adding active test profile '{}'", profile);
                    additionalProfiles.add(profile);
                }
            }
        }

        ConfigDataEnvironmentPostProcessor.applyTo(environment, application.getResourceLoader(), null, additionalProfiles);
    }

    @Override
    public int getOrder() {
        return EnvironmentPostProcessorApplicationListener.DEFAULT_ORDER + 10;
    }

}
