package io.arconia.opentelemetry.autoconfigure.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import io.arconia.opentelemetry.autoconfigure.OpenTelemetryProperties;

/**
 * Disable auto-configurations from Spring Boot Actuator partial integration with OpenTelemetry.
 */
class SpringBootEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "arconia-opentelemetry-spring-boot";

    private static final String SPRING_AUTOCONFIGURE_EXCLUDE = "spring.autoconfigure.exclude";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Assert.notNull(environment, "environment cannot be null");

        Map<String, Object> arconiaProperties = new HashMap<>();

        setExcludedAutoConfigurations(getAutoConfigurations(environment), environment, arconiaProperties);

        MapPropertySource mapPropertySource = new MapPropertySource(PROPERTY_SOURCE_NAME, arconiaProperties);
        MutablePropertySources mutablePropertySources = environment.getPropertySources();
        mutablePropertySources.addFirst(mapPropertySource);
    }

    private static List<String> getAutoConfigurations(ConfigurableEnvironment environment) {
        var autoConfigurations = new ArrayList<>(List.of(
                // SDK Configuration
                "org.springframework.boot.actuate.autoconfigure.opentelemetry.OpenTelemetryAutoConfiguration",
                "org.springframework.boot.actuate.autoconfigure.logging.OpenTelemetryLoggingAutoConfiguration",

                // OTLP Exporters
                "org.springframework.boot.actuate.autoconfigure.logging.otlp.OtlpLoggingAutoConfiguration",
                "org.springframework.boot.actuate.autoconfigure.metrics.export.otlp.OtlpMetricsExportAutoConfiguration",
                "org.springframework.boot.actuate.autoconfigure.tracing.otlp.OtlpTracingAutoConfiguration"
        ));

        if (!environment.getProperty(OpenTelemetryProperties.CONFIG_PREFIX + ".enabled", Boolean.class, true)) {
            autoConfigurations.add("org.springframework.boot.actuate.autoconfigure.tracing.OpenTelemetryTracingAutoConfiguration");
        }

        return autoConfigurations;
    }

    static void setExcludedAutoConfigurations(List<String> autoConfigurations, ConfigurableEnvironment environment, Map<String, Object> arconiaProperties) {
        String value = environment.getProperty(SPRING_AUTOCONFIGURE_EXCLUDE);
        String additionalValue = String.join(",", autoConfigurations);

        if (StringUtils.hasText(value)) {
            arconiaProperties.put(SPRING_AUTOCONFIGURE_EXCLUDE, value + "," + additionalValue);
        } else {
            arconiaProperties.put(SPRING_AUTOCONFIGURE_EXCLUDE, additionalValue);
        }
    }

}
