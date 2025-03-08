package io.arconia.opentelemetry.autoconfigure.sdk.config;

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
 * Converts OpenTelemetry SDK properties to Arconia properties.
 */
class OpenTelemetrySdkEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "arconia-opentelemetry-sdk";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Assert.notNull(environment, "environment cannot be null");

        Boolean enabled = environment.getProperty("arconia.otel.compatibility.opentelemetry", Boolean.class, true);
        if (!enabled) {
            return;
        }

        Map<String,Object> arconiaProperties = new HashMap<>();
        arconiaProperties.putAll(OpenTelemetrySdkPropertyAdapters.general(environment).getArconiaProperties());
        arconiaProperties.putAll(OpenTelemetrySdkPropertyAdapters.resource(environment).getArconiaProperties());
        arconiaProperties.putAll(OpenTelemetrySdkPropertyAdapters.logs(environment).getArconiaProperties());
        arconiaProperties.putAll(OpenTelemetrySdkPropertyAdapters.metrics(environment).getArconiaProperties());
        arconiaProperties.putAll(OpenTelemetrySdkPropertyAdapters.traces(environment).getArconiaProperties());
        arconiaProperties.putAll(OpenTelemetrySdkPropertyAdapters.exporters(environment).getArconiaProperties());

        MapPropertySource propertySource = new MapPropertySource(PROPERTY_SOURCE_NAME, arconiaProperties);
        MutablePropertySources propertySources = environment.getPropertySources();
        propertySources.addFirst(propertySource);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

}
