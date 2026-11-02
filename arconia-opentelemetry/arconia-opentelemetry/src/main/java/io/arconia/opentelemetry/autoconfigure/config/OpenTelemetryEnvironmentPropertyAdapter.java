package io.arconia.opentelemetry.autoconfigure.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * Adapts properties defined by the OpenTelemetry Environment Variable Specification
 * to Arconia properties, parsing the values according to the types defined by the specification.
 * <p>
 * The properties are read from the Spring {@link ConfigurableEnvironment} using their relaxed names
 * (e.g. {@code otel.exporter.otlp.endpoint}), so they can be set as environment variables
 * (e.g. {@code OTEL_EXPORTER_OTLP_ENDPOINT}), JVM system properties, command line arguments,
 * or application properties. When the same Arconia property is mapped more than once,
 * the last mapping with a value wins.
 *
 * @see <a href="https://opentelemetry.io/docs/specs/otel/configuration/common/">OpenTelemetry Configuration Types</a>
 */
final class OpenTelemetryEnvironmentPropertyAdapter {

    private static final Pattern DURATION_PATTERN = Pattern.compile("^(\\d+)(ms|s|m|h)?$");

    /**
     * The specification allows to represent "no limit" as a very large duration,
     * suggesting the maximum number of milliseconds representable by a 32-bit integer.
     */
    private static final Duration NO_TIMEOUT = Duration.ofMillis(Integer.MAX_VALUE);

    private final ConfigurableEnvironment environment;

    private final Log logger;

    private final Map<String, OriginTrackedValue> arconiaProperties = new LinkedHashMap<>();

    OpenTelemetryEnvironmentPropertyAdapter(ConfigurableEnvironment environment, Log logger) {
        Assert.notNull(environment, "environment cannot be null");
        Assert.notNull(logger, "logger cannot be null");
        this.environment = environment;
        this.logger = logger;
    }

    /**
     * Maps the property with a custom converter. When the converter returns {@code null},
     * the property is skipped, so the converter is responsible for reporting invalid values.
     */
    <T> OpenTelemetryEnvironmentPropertyAdapter mapProperty(String externalKey, String arconiaKey, Function<String, @Nullable T> converter) {
        Assert.hasText(externalKey, "externalKey cannot be null or empty");
        Assert.hasText(arconiaKey, "arconiaKey cannot be null or empty");
        Assert.notNull(converter, "converter cannot be null");

        String value = environment.getProperty(externalKey);
        if (StringUtils.hasText(value)) {
            T convertedValue = converter.apply(value.strip());
            if (convertedValue != null) {
                Origin origin = new OpenTelemetryPropertyOrigin(externalKey, findOrigin(externalKey));
                arconiaProperties.put(arconiaKey, OriginTrackedValue.of(convertedValue, origin));
            }
        }
        return this;
    }

    /**
     * Whether the property is set.
     */
    boolean hasProperty(String externalKey) {
        Assert.hasText(externalKey, "externalKey cannot be null or empty");
        return StringUtils.hasText(environment.getProperty(externalKey));
    }

    /**
     * Reports that the property is ignored, if it's set.
     */
    OpenTelemetryEnvironmentPropertyAdapter ignoreProperty(String externalKey, String reason) {
        Assert.hasText(externalKey, "externalKey cannot be null or empty");
        Assert.hasText(reason, "reason cannot be null or empty");
        if (StringUtils.hasText(environment.getProperty(externalKey))) {
            logger.warn(LogMessage.format("Ignoring %s because %s", externalKey, reason));
        }
        return this;
    }

    OpenTelemetryEnvironmentPropertyAdapter mapString(String externalKey, String arconiaKey) {
        return mapProperty(externalKey, arconiaKey, value -> value);
    }

    /**
     * Maps a boolean with the opposite meaning (e.g. a "disabled" flag to an "enabled" property).
     * As defined by the specification, only {@code true} and {@code false} are valid, case-insensitively.
     */
    OpenTelemetryEnvironmentPropertyAdapter mapInverseBoolean(String externalKey, String arconiaKey) {
        return mapProperty(externalKey, arconiaKey, value -> {
            if ("true".equalsIgnoreCase(value)) {
                return false;
            }
            if ("false".equalsIgnoreCase(value)) {
                return true;
            }
            logUnsupportedValue(externalKey, value);
            return null;
        });
    }

    OpenTelemetryEnvironmentPropertyAdapter mapInteger(String externalKey, String arconiaKey) {
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
     * Maps a double within the given range, bounds included.
     */
    OpenTelemetryEnvironmentPropertyAdapter mapDouble(String externalKey, String arconiaKey, double min, double max) {
        return mapProperty(externalKey, arconiaKey, value -> {
            try {
                double number = Double.parseDouble(value);
                if (number >= min && number <= max) {
                    return number;
                }
            } catch (NumberFormatException ex) {
                // Reported below.
            }
            logUnsupportedValue(externalKey, value);
            return null;
        });
    }

    /**
     * Maps a duration expressed in milliseconds, as defined by the specification.
     * The {@code ms}, {@code s}, {@code m}, and {@code h} units are also accepted.
     */
    OpenTelemetryEnvironmentPropertyAdapter mapDuration(String externalKey, String arconiaKey) {
        return mapProperty(externalKey, arconiaKey, value -> parseDuration(externalKey, value));
    }

    /**
     * Maps a timeout, which is a duration where zero means no limit, as defined by the specification.
     */
    OpenTelemetryEnvironmentPropertyAdapter mapTimeout(String externalKey, String arconiaKey) {
        return mapProperty(externalKey, arconiaKey, value -> {
            Duration timeout = parseDuration(externalKey, value);
            return (timeout != null && timeout.isZero()) ? NO_TIMEOUT : timeout;
        });
    }

    /**
     * Maps a comma-separated list of percent-encoded {@code key=value} pairs.
     * The values are never logged, since they may hold secrets (e.g. authorization headers).
     */
    OpenTelemetryEnvironmentPropertyAdapter mapMap(String externalKey, String arconiaKey) {
        return mapProperty(externalKey, arconiaKey, value -> {
            Map<String, String> propertyMap = new HashMap<>();
            for (String pair : StringUtils.tokenizeToStringArray(value, ",")) {
                String[] entry = pair.split("=", 2);
                if (entry.length != 2 || !StringUtils.hasText(entry[0]) || !StringUtils.hasText(entry[1])) {
                    logger.warn(LogMessage.format("Ignoring an invalid key-value pair in %s", externalKey));
                    continue;
                }
                String key = entry[0].strip();
                try {
                    propertyMap.put(key, StringUtils.uriDecode(entry[1].strip(), StandardCharsets.UTF_8));
                } catch (IllegalArgumentException ex) {
                    logger.warn(LogMessage.format("Ignoring the '%s' entry in %s because its value is not correctly percent-encoded", key, externalKey));
                }
            }
            return propertyMap.isEmpty() ? null : propertyMap;
        });
    }

    /**
     * Maps one of the values defined by the specification, matched case-insensitively.
     */
    <T> OpenTelemetryEnvironmentPropertyAdapter mapEnum(String externalKey, String arconiaKey, Map<String, T> values) {
        Assert.notNull(values, "values cannot be null");
        return mapProperty(externalKey, arconiaKey, value -> {
            T convertedValue = values.get(value.toLowerCase(Locale.ROOT));
            if (convertedValue == null) {
                logUnsupportedValue(externalKey, value);
            }
            return convertedValue;
        });
    }

    /**
     * Maps a comma-separated list of the values defined by the specification, matched
     * case-insensitively. Unsupported values are skipped and duplicates are removed.
     * As defined by the specification, {@code none} results in an empty list.
     */
    <T> OpenTelemetryEnvironmentPropertyAdapter mapEnumList(String externalKey, String arconiaKey, Map<String, T> values) {
        Assert.notNull(values, "values cannot be null");
        return mapProperty(externalKey, arconiaKey, value -> {
            List<String> items = List.of(StringUtils.tokenizeToStringArray(value.toLowerCase(Locale.ROOT), ","));
            if (items.contains("none")) {
                if (items.size() > 1) {
                    logger.warn(LogMessage.format("Ignoring the other values of %s because it includes 'none'", externalKey));
                }
                return List.of();
            }
            Set<T> convertedValues = new LinkedHashSet<>();
            for (String item : items) {
                T convertedValue = values.get(item);
                if (convertedValue == null) {
                    logUnsupportedValue(externalKey, item);
                } else {
                    convertedValues.add(convertedValue);
                }
            }
            return convertedValues.isEmpty() ? null : List.copyOf(convertedValues);
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
     * the OpenTelemetry property it was adapted from.
     */
    PropertySource<?> toPropertySource(String name) {
        Assert.hasText(name, "name cannot be null or empty");
        return new OriginTrackedMapPropertySource(name, Map.copyOf(arconiaProperties));
    }

    private @Nullable Duration parseDuration(String externalKey, String value) {
        Matcher matcher = DURATION_PATTERN.matcher(value);
        if (!matcher.matches()) {
            logUnsupportedValue(externalKey, value);
            return null;
        }
        try {
            long amount = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);
            // Without a unit, the value is in milliseconds.
            return switch (unit == null ? "ms" : unit) {
                case "s" -> Duration.ofSeconds(amount);
                case "m" -> Duration.ofMinutes(amount);
                case "h" -> Duration.ofHours(amount);
                default -> Duration.ofMillis(amount);
            };
        } catch (NumberFormatException | ArithmeticException ex) {
            // The value overflows a duration.
            logUnsupportedValue(externalKey, value);
            return null;
        }
    }

    /**
     * Finds where the property is defined, e.g. the actual environment variable name,
     * so that it can be reported when the adapted value cannot be bound.
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
     * Origin of an Arconia property adapted from an OpenTelemetry property.
     */
    private record OpenTelemetryPropertyOrigin(String externalKey, @Nullable Origin parent) implements Origin {

        @Override
        public @Nullable Origin getParent() {
            return parent;
        }

        @Override
        public String toString() {
            String description = "Adapted from the OpenTelemetry property '%s'".formatted(externalKey);
            return parent != null ? description + " (" + parent + ")" : description;
        }

    }

}
