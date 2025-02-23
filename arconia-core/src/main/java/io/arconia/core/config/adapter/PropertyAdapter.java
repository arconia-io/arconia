package io.arconia.core.config.adapter;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import io.arconia.core.support.Internal;

/**
 * Adapter for converting properties from an external configuration source
 * to properties recognized by Arconia.
 * <p>
 * For example, this can be used to adapt properties from the OpenTelemetry
 * environment configuration or from Spring Boot features replaced by more
 * advanced Arconia features.
 */
@Internal
public class PropertyAdapter {

    private static final Logger logger = LoggerFactory.getLogger(PropertyAdapter.class);

    private static final Pattern DURATION_PATTERN = Pattern.compile("^(\\d+)(ms|s|m|h)$");

    private final Map<String, Object> arconiaProperties = new HashMap<>();

    /**
     * Get the properties adapted from an external source.
     */
    public Map<String, Object> getArconiaProperties() {
        return arconiaProperties;
    }

    public static Builder builder(ConfigurableEnvironment environment) {
        return new Builder(environment);
    }

    public static class Builder {
        private final ConfigurableEnvironment environment;
        private final PropertyAdapter adapter;

        private Builder(ConfigurableEnvironment environment) {
            this.environment = environment;
            this.adapter = new PropertyAdapter();
        }

        public <T> Builder mapProperty(String externalKey, String arconiaKey, Function<String, T> converter) {
            String value = environment.getProperty(externalKey);
            if (StringUtils.hasText(value)) {
                var convertedValue = converter.apply(value.trim());
                if (convertedValue != null) {
                    adapter.arconiaProperties.put(arconiaKey, convertedValue);
                }
            }
            return this;
        }

        public <T> Builder mapEnum(String externalKey, String arconiaKey, Function<String, Function<String, T>> converterFactory) {
            return mapProperty(externalKey, arconiaKey, converterFactory.apply(externalKey));
        }

        public Builder mapBoolean(String externalKey, String arconiaKey) {
            return mapProperty(externalKey, arconiaKey, Boolean::valueOf);
        }

        public Builder mapInteger(String externalKey, String arconiaKey) {
            return mapProperty(externalKey, arconiaKey, value -> {
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    logger.warn("Unsupported value for {}: {}", externalKey, value);
                    return null;
                }
            });
        }

        public Builder mapDouble(String externalKey, String arconiaKey) {
            return mapProperty(externalKey, arconiaKey, value -> {
                try {
                    return Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    logger.warn("Unsupported value for {}: {}", externalKey, value);
                    return null;
                }
            });
        }

        public Builder mapDuration(String externalKey, String arconiaKey) {
            return mapProperty(externalKey, arconiaKey, value -> {
                try {
                    Matcher matcher = DURATION_PATTERN.matcher(value.trim());
                    if (matcher.matches()) {
                        long amount = Long.parseLong(matcher.group(1));
                        String unit = matcher.group(2);
                        return switch (unit) {
                            case "ms" -> Duration.ofMillis(amount);
                            case "s" -> Duration.ofSeconds(amount);
                            case "m" -> Duration.ofMinutes(amount);
                            case "h" -> Duration.ofHours(amount);
                            default -> null;
                        };
                    }
                    // Try parsing as milliseconds
                    return Duration.ofMillis(Long.parseLong(value.trim()));
                } catch (Exception e) {
                    logger.warn("Unsupported value for {}: {}", externalKey, value);
                    return null;
                }
            });
        }

        public Builder mapList(String externalKey, String arconiaKey) {
            return mapProperty(externalKey, arconiaKey, value -> {
                List<String> propertyList = List.of(value.split(","));
                return CollectionUtils.isEmpty(propertyList) ? null : propertyList;
            });
        }

        public Builder mapMap(String externalKey, String arconiaKey) {
            return mapProperty(externalKey, arconiaKey, value -> {
                Map<String, String> propertyMap = new HashMap<>();
                String[] keyValuePairs = value.split(",");
                for (String pair : keyValuePairs) {
                    String[] entry = pair.split("=");
                    if (entry.length == 2) {
                        propertyMap.put(entry[0], entry[1]);
                    } else {
                        logger.warn("Invalid key-value pair in {}: {}", externalKey, pair);
                    }
                }
                return CollectionUtils.isEmpty(propertyMap) ? null : propertyMap;
            });
        }

        public Builder mapString(String externalKey, String arconiaKey) {
            return mapProperty(externalKey, arconiaKey, Function.identity());
        }

        public PropertyAdapter build() {
            return adapter;
        }
    }
}
