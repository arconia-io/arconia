package io.arconia.core.config.adapter;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.Assert;
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
            Assert.notNull(environment, "environment cannot be null");
            this.environment = environment;
            this.adapter = new PropertyAdapter();
        }

        public <T> Builder mapProperty(String externalKey, String arconiaKey, Function<String, T> converter) {
            Assert.hasText(externalKey, "externalKey cannot be null or empty");
            Assert.hasText(arconiaKey, "arconiaKey cannot be null or empty");
            Assert.notNull(converter, "converter cannot be null");

            String value = environment.getProperty(externalKey);
            if (StringUtils.hasText(value)) {
                var convertedValue = converter.apply(value.strip());
                if (convertedValue != null) {
                    adapter.arconiaProperties.put(arconiaKey, convertedValue);
                }
            }
            return this;
        }

        public <T> Builder mapEnum(String externalKey, String arconiaKey, Function<String, Function<String, T>> converterFactory) {
            Assert.notNull(converterFactory, "converterFactory cannot be null");
            return mapProperty(externalKey, arconiaKey, converterFactory.apply(externalKey));
        }

        public Builder mapBoolean(String externalKey, String arconiaKey) {
            return mapProperty(externalKey, arconiaKey, Boolean::valueOf);
        }

        @Nullable
        public Builder mapInteger(String externalKey, String arconiaKey) {
            return mapProperty(externalKey, arconiaKey, value -> {
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    logUnsupportedValue(externalKey, value);
                    return null;
                }
            });
        }

        @Nullable
        public Builder mapDouble(String externalKey, String arconiaKey) {
            return mapProperty(externalKey, arconiaKey, value -> {
                try {
                    return Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    logUnsupportedValue(externalKey, value);
                    return null;
                }
            });
        }

        @Nullable
        public Builder mapDuration(String externalKey, String arconiaKey) {
            return mapProperty(externalKey, arconiaKey, value -> {
                try {
                    Matcher matcher = DURATION_PATTERN.matcher(value.strip());
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
                    return Duration.ofMillis(Long.parseLong(value.strip()));
                } catch (Exception e) {
                    logUnsupportedValue(externalKey, value);
                    return null;
                }
            });
        }

        @Nullable
        public Builder mapList(String externalKey, String arconiaKey) {
            return mapProperty(externalKey, arconiaKey, value -> {
                List<String> propertyList = List.of(value.split(","));
                return CollectionUtils.isEmpty(propertyList) ? null : propertyList;
            });
        }

        @Nullable
        public Builder mapMap(String externalKey, String arconiaKey) {
            return mapMap(externalKey, arconiaKey, null);
        }

        @Nullable
        public Builder mapMap(String externalKey, String arconiaKey, @Nullable Function<Map<String,String>,Map<String,String>> postProcessor) {
            return mapProperty(externalKey, arconiaKey, value -> {
                Map<String, String> propertyMap = new HashMap<>();
                String[] keyValuePairs = value.split("\\s*,\\s*");
                for (String pair : keyValuePairs) {
                    String[] entry = pair.split("=");
                    if (entry.length == 2 && StringUtils.hasText(entry[0]) && StringUtils.hasText(entry[1])) {
                        propertyMap.put(entry[0].strip(), entry[1].strip());
                    } else {
                        logger.warn("Invalid key-value pair in {}: {}", externalKey, pair);
                    }
                }
                if (postProcessor != null) {
                    propertyMap = postProcessor.apply(propertyMap);
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

    private static void logUnsupportedValue(String externalKey, String value) {
        logger.warn("Unsupported value for {}: {}", externalKey, value);
    }

}
