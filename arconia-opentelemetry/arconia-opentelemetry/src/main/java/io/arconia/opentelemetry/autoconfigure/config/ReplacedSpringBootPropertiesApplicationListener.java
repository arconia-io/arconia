package io.arconia.opentelemetry.autoconfigure.config;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.context.properties.source.IterableConfigurationPropertySource;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

import io.arconia.opentelemetry.autoconfigure.OpenTelemetryProperties;

/**
 * Warns about the Spring Boot OpenTelemetry and OTLP properties found in the environment.
 * Arconia replaces that part of the Spring Boot configuration with its own, so without
 * a warning such values would be silently ignored.
 * <p>
 * The properties are recognized by their namespace rather than one by one, so there is no
 * mapping to the Arconia properties to maintain as the Spring Boot ones change.
 * <p>
 * The check runs once the application context is prepared, so that the environment includes
 * what the environment post-processors contribute, such as the outcome of {@code OTEL_SDK_DISABLED}.
 */
class ReplacedSpringBootPropertiesApplicationListener implements ApplicationListener<ApplicationPreparedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ReplacedSpringBootPropertiesApplicationListener.class);

    private static final List<ConfigurationPropertyName> REPLACED_NAMESPACES = List.of(
            ConfigurationPropertyName.of("management.opentelemetry"),
            ConfigurationPropertyName.of("management.otlp"));

    @Override
    public void onApplicationEvent(ApplicationPreparedEvent event) {
        Environment environment = event.getApplicationContext().getEnvironment();

        boolean openTelemetryEnabled = environment.getProperty(OpenTelemetryProperties.CONFIG_PREFIX + ".enabled", boolean.class, true);
        if (!openTelemetryEnabled) {
            return;
        }

        Set<String> replacedProperties = findReplacedProperties(environment);
        if (!replacedProperties.isEmpty()) {
            logger.warn("The following Spring Boot properties have no effect, because Arconia OpenTelemetry replaces "
                    + "the Spring Boot configuration for OpenTelemetry and OTLP: {}. Use the '{}.*' properties instead.",
                    replacedProperties, OpenTelemetryProperties.CONFIG_PREFIX);
        }
    }

    /**
     * The names are read through the Spring Boot configuration property sources, so that
     * the properties defined as environment variables are found too.
     */
    private static Set<String> findReplacedProperties(Environment environment) {
        Set<String> replacedProperties = new TreeSet<>();
        for (ConfigurationPropertySource propertySource : ConfigurationPropertySources.get(environment)) {
            if (propertySource instanceof IterableConfigurationPropertySource iterablePropertySource) {
                iterablePropertySource.stream()
                    .filter(ReplacedSpringBootPropertiesApplicationListener::isReplaced)
                    .map(ConfigurationPropertyName::toString)
                    .forEach(replacedProperties::add);
            }
        }
        return replacedProperties;
    }

    private static boolean isReplaced(ConfigurationPropertyName propertyName) {
        return REPLACED_NAMESPACES.stream().anyMatch(namespace -> namespace.isAncestorOf(propertyName));
    }

}
