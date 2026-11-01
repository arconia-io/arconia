package io.arconia.observation.openinference.ai.autoconfigure;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.boot.origin.PropertySourceOrigin;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.log.LogMessage;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Adapts the environment variables defined by the OpenInference Configuration Specification
 * to Arconia properties, parsing the values as the OpenInference reference implementation does.
 * Invalid values are reported and ignored.
 *
 * @link <a href="https://arize-ai.github.io/openinference/spec/configuration.html">OpenInference Configuration</a>
 */
final class OpenInferenceEnvironmentPropertyAdapter {

    private final ConfigurableEnvironment environment;

    private final Log logger;

    private final Map<String, OriginTrackedValue> arconiaProperties = new LinkedHashMap<>();

    OpenInferenceEnvironmentPropertyAdapter(ConfigurableEnvironment environment, Log logger) {
        Assert.notNull(environment, "environment cannot be null");
        Assert.notNull(logger, "logger cannot be null");
        this.environment = environment;
        this.logger = logger;
    }

    /**
     * Maps a boolean. Only {@code true} and {@code false} are valid, case-insensitively.
     */
    OpenInferenceEnvironmentPropertyAdapter mapBoolean(String externalKey, String arconiaKey) {
        return mapProperty(externalKey, arconiaKey, value -> {
            if ("true".equalsIgnoreCase(value)) {
                return true;
            }
            if ("false".equalsIgnoreCase(value)) {
                return false;
            }
            logUnsupportedValue(externalKey, value);
            return null;
        });
    }

    OpenInferenceEnvironmentPropertyAdapter mapInteger(String externalKey, String arconiaKey) {
        return mapProperty(externalKey, arconiaKey, value -> {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                logUnsupportedValue(externalKey, value);
                return null;
            }
        });
    }

    /**
     * Get the properties adapted so far, without their origins.
     */
    Map<String, Object> getArconiaProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        arconiaProperties.forEach((key, value) -> properties.put(key, value.getValue()));
        return properties;
    }

    /**
     * Get the properties adapted so far as a property source, where each value records
     * the environment variable it was adapted from.
     */
    PropertySource<?> toPropertySource(String name) {
        Assert.hasText(name, "name cannot be null or empty");
        return new OriginTrackedMapPropertySource(name, Map.copyOf(arconiaProperties));
    }

    private OpenInferenceEnvironmentPropertyAdapter mapProperty(String externalKey, String arconiaKey, Function<String, @Nullable Object> converter) {
        Assert.hasText(externalKey, "externalKey cannot be null or empty");
        Assert.hasText(arconiaKey, "arconiaKey cannot be null or empty");

        String value = environment.getProperty(externalKey);
        if (StringUtils.hasText(value)) {
            Object convertedValue = converter.apply(value.strip());
            if (convertedValue != null) {
                Origin origin = new OpenInferencePropertyOrigin(externalKey, findOrigin(externalKey));
                arconiaProperties.put(arconiaKey, OriginTrackedValue.of(convertedValue, origin));
            }
        }
        return this;
    }

    /**
     * Finds where the property is defined, so that it can be reported when the adapted value cannot be bound.
     */
    private @Nullable Origin findOrigin(String externalKey) {
        for (PropertySource<?> propertySource : environment.getPropertySources()) {
            if (propertySource.containsProperty(externalKey)) {
                return PropertySourceOrigin.get(propertySource, externalKey);
            }
        }
        return null;
    }

    private void logUnsupportedValue(String externalKey, String value) {
        logger.warn(LogMessage.format("Unsupported value for %s: %s", externalKey, value));
    }

    /**
     * Origin of an Arconia property adapted from an OpenInference environment variable.
     */
    private record OpenInferencePropertyOrigin(String externalKey, @Nullable Origin parent) implements Origin {

        @Override
        public @Nullable Origin getParent() {
            return parent;
        }

        @Override
        public String toString() {
            String description = "Adapted from the OpenInference property '%s'".formatted(externalKey);
            return parent != null ? description + " (" + parent + ")" : description;
        }

    }

}
