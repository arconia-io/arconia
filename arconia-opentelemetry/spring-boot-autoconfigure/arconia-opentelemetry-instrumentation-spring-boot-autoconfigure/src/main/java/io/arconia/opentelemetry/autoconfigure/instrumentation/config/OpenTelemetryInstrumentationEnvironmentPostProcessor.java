package io.arconia.opentelemetry.autoconfigure.instrumentation.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

/**
 * Converts OpenTelemetry Instrumentation properties to Arconia properties.
 */
public class OpenTelemetryInstrumentationEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "arconia-opentelemetry-instrumentation";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String,Object> arconiaProperties = new HashMap<>();
        arconiaProperties.putAll(OpenTelemetryInstrumentationPropertyAdapters.logbackAppender(environment).getArconiaProperties());
        arconiaProperties.putAll(OpenTelemetryInstrumentationPropertyAdapters.micrometer(environment).getArconiaProperties());

        MapPropertySource propertySource = new MapPropertySource(PROPERTY_SOURCE_NAME, arconiaProperties);
        MutablePropertySources propertySources = environment.getPropertySources();
        propertySources.addFirst(propertySource);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

}
