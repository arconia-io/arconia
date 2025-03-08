package io.arconia.opentelemetry.autoconfigure.instrumentation.config;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.Assert;

import io.arconia.core.config.adapter.PropertyAdapter;
import io.arconia.opentelemetry.autoconfigure.instrumentation.micrometer.MicrometerProperties;

/**
 * Provides adapters from Spring Boot Actuator properties to Arconia properties.
 */
class ActuatorInstrumentationPropertyAdapters {

    /**
     * Properties for configuring the OpenTelemetry Micrometer Bridge.
     * <p>
     * All properties supported, except:
     * <ul>
     *      <li>{@code management.otlp.metrics.export.batch-size}</li>
     *      <li>{@code management.otlp.metrics.export.max-bucket-count}</li>
     *      <li>{@code management.otlp.metrics.export.max-scale}</li>
     * </ul>
     */
    static PropertyAdapter metrics(ConfigurableEnvironment environment) {
        Assert.notNull(environment, "environment cannot be null");
        return PropertyAdapter.builder(environment)
                .mapEnum("management.otlp.metrics.export.base-time-unit", MicrometerProperties.CONFIG_PREFIX + ".base-time-unit", ActuatorInstrumentationPropertyConverters::baseTimeUnit)
                .build();
    }

}
