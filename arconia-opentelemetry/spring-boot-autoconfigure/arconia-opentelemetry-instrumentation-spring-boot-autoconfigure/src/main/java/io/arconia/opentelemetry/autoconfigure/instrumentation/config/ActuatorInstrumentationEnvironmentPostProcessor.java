package io.arconia.opentelemetry.autoconfigure.instrumentation.config;

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
 * Converts Actuator properties to Arconia properties.
 */
class ActuatorInstrumentationEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "arconia-opentelemetry-instrumentation-actuator";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Assert.notNull(environment, "environment cannot be null");

        Boolean enabled = environment.getProperty("arconia.otel.compatibility.actuator", Boolean.class, false);
        if (!enabled) {
            return;
        }

        Map<String,Object> arconiaProperties = new HashMap<>();
        arconiaProperties.putAll(ActuatorInstrumentationPropertyAdapters.metrics(environment).getArconiaProperties());

        MapPropertySource propertySource = new MapPropertySource(PROPERTY_SOURCE_NAME, arconiaProperties);
        MutablePropertySources propertySources = environment.getPropertySources();
        propertySources.addFirst(propertySource);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

}
