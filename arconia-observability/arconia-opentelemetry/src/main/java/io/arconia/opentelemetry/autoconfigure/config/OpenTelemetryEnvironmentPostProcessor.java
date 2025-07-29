package io.arconia.opentelemetry.autoconfigure.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.util.Assert;

/**
 * Converts OpenTelemetry Environment Variable Specification properties to Arconia properties
 */
class OpenTelemetryEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "opentelemetry-environment-variable-specification";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Assert.notNull(environment, "environment cannot be null");

        Boolean enabled = environment.getProperty("arconia.otel.compatibility.environment-variable-specification", Boolean.class, true);
        if (!enabled) {
            return;
        }

        Map<String,Object> arconiaProperties = new HashMap<>();
        arconiaProperties.putAll(OpenTelemetryEnvironmentPropertyAdapters.general(environment).getArconiaProperties());
        arconiaProperties.putAll(OpenTelemetryEnvironmentPropertyAdapters.batchSpanProcessor(environment).getArconiaProperties());
        arconiaProperties.putAll(OpenTelemetryEnvironmentPropertyAdapters.logRecordProcessor(environment).getArconiaProperties());
        arconiaProperties.putAll(OpenTelemetryEnvironmentPropertyAdapters.attributeLimits(environment).getArconiaProperties());
        arconiaProperties.putAll(OpenTelemetryEnvironmentPropertyAdapters.spanLimits(environment).getArconiaProperties());
        arconiaProperties.putAll(OpenTelemetryEnvironmentPropertyAdapters.logRecordLimits(environment).getArconiaProperties());
        arconiaProperties.putAll(OpenTelemetryEnvironmentPropertyAdapters.exporterSelection(environment).getArconiaProperties());
        arconiaProperties.putAll(OpenTelemetryEnvironmentPropertyAdapters.metrics(environment).getArconiaProperties());
        arconiaProperties.putAll(OpenTelemetryEnvironmentPropertyAdapters.otlpExporter(environment).getArconiaProperties());

        MapPropertySource propertySource = new MapPropertySource(PROPERTY_SOURCE_NAME, arconiaProperties);
        MutablePropertySources propertySources = environment.getPropertySources();
        propertySources.addFirst(propertySource);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

}
