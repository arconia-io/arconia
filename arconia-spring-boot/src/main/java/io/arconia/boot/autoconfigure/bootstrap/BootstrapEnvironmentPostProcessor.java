package io.arconia.boot.autoconfigure.bootstrap;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import io.arconia.boot.bootstrap.BootstrapConfigurationFile;
import io.arconia.boot.bootstrap.BootstrapMode;

/**
 * Activates the Spring profiles for the current bootstrap mode before the Spring Boot
 * configuration data import runs, so that profile-specific configuration files are loaded
 * by the regular, single import pass.
 * <p>
 * Since this post-processor runs before the application configuration is loaded, the bootstrap
 * settings can only come from pre-configuration sources: JVM system properties, environment
 * variables, command line arguments, or the {@value BootstrapConfigurationFile#LOCATION} file.
 * {@link MisplacedBootstrapPropertiesEnvironmentPostProcessor} warns about values defined
 * in the application configuration, where they are not supported.
 */
class BootstrapEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(BootstrapEnvironmentPostProcessor.class);

    /**
     * Name of the property source holding the content of the bootstrap configuration file.
     */
    static final String PROPERTY_SOURCE_NAME = "arconiaBootstrap";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Assert.notNull(environment, "environment cannot be null");
        Assert.notNull(application, "application cannot be null");

        addBootstrapConfigurationFile(environment, application);

        Binder binder = Binder.get(environment);

        Boolean profilesEnabled = binder.bind(BootstrapConfigurationFile.PROFILES_ENABLED_PROPERTY, Boolean.class).orElse(true);
        List<String> devProfiles = binder.bind(BootstrapConfigurationFile.DEV_PROFILES_PROPERTY, Bindable.listOf(String.class)).orElse(null);
        List<String> testProfiles = binder.bind(BootstrapConfigurationFile.TEST_PROFILES_PROPERTY, Bindable.listOf(String.class)).orElse(null);

        if (!profilesEnabled) {
            return;
        }

        List<String> modeProfiles = switch (BootstrapMode.detect()) {
            case DEV -> {
                logger.info("The application is running in dev mode");
                yield devProfiles != null ? devProfiles : List.of("dev");
            }
            case TEST -> {
                logger.info("The application is running in test mode");
                yield testProfiles != null ? testProfiles : List.of("test");
            }
            case PROD -> {
                // No additional profiles for prod mode
                logger.debug("The application is running in prod mode");
                yield List.of();
            }
        };

        // Consider both the property-declared and the programmatically-set active profiles,
        // so already-active profiles are not activated twice.
        List<String> activeProfiles = List.of(environment.getActiveProfiles());
        List<String> currentProfiles = new ArrayList<>(binder.bind(StandardEnvironment.ACTIVE_PROFILES_PROPERTY_NAME, Bindable.listOf(String.class)).orElse(List.of()));
        for (String profile : activeProfiles) {
            if (!currentProfiles.contains(profile)) {
                currentProfiles.add(profile);
            }
        }

        List<String> additionalProfiles = new ArrayList<>();
        for (String profile : modeProfiles) {
            if (StringUtils.hasText(profile) && !currentProfiles.contains(profile)) {
                logger.debug("Adding active profile '{}' for the current bootstrap mode", profile);
                additionalProfiles.add(profile);
            }
        }

        if (additionalProfiles.isEmpty()) {
            return;
        }

        // The mode profiles come first so that their profile-specific configuration files have
        // lower precedence than the ones for the profiles activated by the user.
        List<String> mergedProfiles = new ArrayList<>(additionalProfiles);
        for (String profile : activeProfiles) {
            if (!mergedProfiles.contains(profile)) {
                mergedProfiles.add(profile);
            }
        }
        environment.setActiveProfiles(mergedProfiles.toArray(String[]::new));
    }

    /**
     * Exposes the content of the bootstrap configuration file as a property source, below the
     * system and command line ones, so it participates in the binding of the bootstrap settings
     * and remains inspectable at runtime.
     */
    private static void addBootstrapConfigurationFile(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.getPropertySources().contains(PROPERTY_SOURCE_NAME)) {
            return;
        }
        Properties properties = BootstrapConfigurationFile.load(application.getClassLoader());
        if (!properties.isEmpty()) {
            environment.getPropertySources().addLast(new PropertiesPropertySource(PROPERTY_SOURCE_NAME, properties));
        }
    }

    @Override
    public int getOrder() {
        // Right before the Spring Boot configuration data import.
        return ConfigDataEnvironmentPostProcessor.ORDER - 1;
    }

}
