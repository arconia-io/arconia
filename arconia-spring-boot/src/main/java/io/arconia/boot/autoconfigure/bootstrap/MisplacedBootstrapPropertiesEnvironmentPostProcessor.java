package io.arconia.boot.autoconfigure.bootstrap;

import java.util.List;

import org.apache.commons.logging.Log;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.log.LogMessage;
import org.springframework.util.Assert;

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

    /**
     * How Spring Boot names the property sources loaded by the configuration data import.
     * See org.springframework.boot.context.config.StandardConfigDataLoader.
     */
    private static final String CONFIG_RESOURCE_PROPERTY_SOURCE_NAME_PREFIX = "Config resource '";

    private static final List<String> BOOTSTRAP_PROPERTY_KEYS = List.of(
            BootstrapConfigurationFile.PROFILES_ENABLED_PROPERTY,
            BootstrapConfigurationFile.DEV_PROFILES_PROPERTY,
            BootstrapConfigurationFile.TEST_PROFILES_PROPERTY);

    private final Log logger;

    /**
     * The logger is deferred until the logging system is initialized, since any message
     * logged before that would be lost.
     */
    MisplacedBootstrapPropertiesEnvironmentPostProcessor(DeferredLogFactory logFactory) {
        this.logger = logFactory.getLog(MisplacedBootstrapPropertiesEnvironmentPostProcessor.class);
    }

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
                    logger.warn(LogMessage.format("The '%s' property was found in %s, where it is not supported because the bootstrap "
                            + "profiles are activated before the application configuration is loaded. Supported locations: "
                            + "the %s file, JVM system properties, environment variables, and command line arguments.",
                            propertyKey, propertySource.getName(), BootstrapConfigurationFile.LOCATION));
                }
            }
        }
    }

    /**
     * The bootstrap mode is only read from the JVM system properties and the environment variables,
     * so it has no effect in any other property source.
     */
    private void warnIfBootstrapModeMisplaced(ConfigurableEnvironment environment) {
        for (PropertySource<?> propertySource : environment.getPropertySources()) {
            if (isModeSource(propertySource) || ConfigurationPropertySources.isAttachedConfigurationPropertySource(propertySource)) {
                continue;
            }
            if (propertySource.containsProperty(BootstrapMode.PROPERTY_KEY)) {
                logger.warn(LogMessage.format("The '%s' property was found in an unsupported location (%s), where it has no effect "
                        + "because the bootstrap mode describes the launch context and is determined before the application "
                        + "configuration is loaded. Set it as a JVM system property (-D%s) or as an environment variable (%s) instead.",
                        BootstrapMode.PROPERTY_KEY, propertySource.getName(), BootstrapMode.PROPERTY_KEY, BootstrapMode.ENVIRONMENT_VARIABLE));
            }
        }
    }

    private static boolean isModeSource(PropertySource<?> propertySource) {
        return StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME.equals(propertySource.getName())
                || StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME.equals(propertySource.getName());
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
