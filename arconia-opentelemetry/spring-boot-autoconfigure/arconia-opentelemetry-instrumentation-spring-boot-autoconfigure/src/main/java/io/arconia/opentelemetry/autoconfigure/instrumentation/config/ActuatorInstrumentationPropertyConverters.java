package io.arconia.opentelemetry.autoconfigure.instrumentation.config;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 * Utility class for converting Actuator configuration types to Arconia configuration types.
 */
class ActuatorInstrumentationPropertyConverters {

    private static final Logger logger = LoggerFactory.getLogger(ActuatorInstrumentationPropertyConverters.class);

    static Function<String,@Nullable TimeUnit> baseTimeUnit(String externalKey) {
        Assert.hasText(externalKey, "externalKey cannot be null or empty");
        return value -> {
            TimeUnit baseTimeUnit;
            try {
                baseTimeUnit = TimeUnit.valueOf(value.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                logger.warn("Unsupported value for {}: {}", externalKey, value);
                return null;
            }
            return baseTimeUnit;
        };
    }

}
