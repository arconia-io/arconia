package io.arconia.boot.autoconfigure.bootstrap;

import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import io.arconia.boot.bootstrap.BootstrapConfigurationFile;
import io.arconia.boot.bootstrap.BootstrapMode;

/**
 * Warns about bootstrap properties defined in the application configuration, where they are
 * not supported because the bootstrap mode and profiles are resolved before the application
 * configuration is loaded. Without a warning, such values would be silently ignored.
 * <p>
 * Similar to how Spring Boot reports {@code spring.profiles.active} used in profile-specific
 * documents, but as a warning rather than a failure.
 *
 * @see BootstrapEnvironmentPostProcessor
 */
class MisplacedBootstrapPropertiesEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(MisplacedBootstrapPropertiesEnvironmentPostProcessor.class);

    /**
     * How Spring Boot names the property sources loaded by the configuration data import.
     * See org.springframework.boot.context.config.StandardConfigDataLoader.
     */
    private static final String CONFIG_RESOURCE_PROPERTY_SOURCE_NAME_PREFIX = "Config resource '";

    private static final List<String> BOOTSTRAP_PROPERTY_KEYS = List.of(
            BootstrapConfigurationFile.PROFILES_ENABLED_PROPERTY,
            BootstrapConfigurationFile.DEV_PROFILES_PROPERTY,
            BootstrapConfigurationFile.TEST_PROFILES_PROPERTY);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Assert.notNull(environment, "environment cannot be null");
        Assert.notNull(application, "application cannot be null");

        warnIfBootstrapModeMisplaced(environment);

        for (PropertySource<?> propertySource : environment.getPropertySources()) {
            if (!propertySource.getName().startsWith(CONFIG_RESOURCE_PROPERTY_SOURCE_NAME_PREFIX)) {
                continue;
            }
            for (String propertyKey : BOOTSTRAP_PROPERTY_KEYS) {
                // The indexed variant covers list values defined in YAML sequence form.
                if (propertySource.containsProperty(propertyKey) || propertySource.containsProperty(propertyKey + "[0]")) {
                    logger.warn("The '{}' property was found in {}, where it is not supported because the bootstrap "
                            + "profiles are activated before the application configuration is loaded. Supported locations: "
                            + "the {} file, JVM system properties, environment variables, and command line arguments.",
                            propertyKey, propertySource.getName(), BootstrapConfigurationFile.LOCATION);
                }
            }
        }
    }

    private static void warnIfBootstrapModeMisplaced(ConfigurableEnvironment environment) {
        String environmentVariableName = BootstrapMode.PROPERTY_KEY.toUpperCase(Locale.ROOT).replace(".", "_");
        String modeProperty;
        try {
            modeProperty = environment.getProperty(BootstrapMode.PROPERTY_KEY);
        } catch (IllegalArgumentException ex) {
            // Unresolvable placeholder in the property value: it is present (and misplaced) either way.
            modeProperty = ex.getMessage();
        }
        if (StringUtils.hasText(modeProperty)
                && !StringUtils.hasText(System.getProperty(BootstrapMode.PROPERTY_KEY))
                && !StringUtils.hasText(System.getenv(environmentVariableName))) {
            logger.warn("The '{}' property was found in an unsupported location, such as the application "
                    + "configuration or the {} file, where it has no effect because the bootstrap mode describes "
                    + "the launch context and is determined before the application configuration is loaded. "
                    + "Set it as a JVM system property (-D{}) or as an environment variable ({}) instead.",
                    BootstrapMode.PROPERTY_KEY, BootstrapConfigurationFile.LOCATION, BootstrapMode.PROPERTY_KEY, environmentVariableName);
        }
    }

    /**
     * The check needs to run after the Spring Boot configuration data import, plus a gap for
     * other post-processors contributing configuration.
     */
    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER + 10;
    }

}
