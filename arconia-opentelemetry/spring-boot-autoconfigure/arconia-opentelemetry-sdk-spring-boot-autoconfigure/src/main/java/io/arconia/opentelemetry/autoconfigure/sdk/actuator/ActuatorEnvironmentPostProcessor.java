package io.arconia.opentelemetry.autoconfigure.sdk.actuator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.util.StringUtils;

import io.arconia.opentelemetry.autoconfigure.sdk.tracing.OpenTelemetryTracingProperties;

/**
 * Adapts properties between Actuator and Arconia, and disables replaced auto-configurations.
 */
public class ActuatorEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String SPRING_AUTOCONFIGURE_EXCLUDE = "spring.autoconfigure.exclude";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> arconiaProperties = new HashMap<>();

        setExcludedAutoConfigurations(List.of(
                    // SDK Configuration
                    "org.springframework.boot.actuate.autoconfigure.opentelemetry.OpenTelemetryAutoConfiguration",
                    "org.springframework.boot.actuate.autoconfigure.logging.OpenTelemetryLoggingAutoConfiguration",

                    // OTLP Exporters
                    "org.springframework.boot.actuate.autoconfigure.logging.otlp.OtlpLoggingAutoConfiguration",
                    "org.springframework.boot.actuate.autoconfigure.metrics.export.otlp.OtlpMetricsExportAutoConfiguration",
                    "org.springframework.boot.actuate.autoconfigure.tracing.otlp.OtlpTracingAutoConfiguration"
                ), environment, arconiaProperties);

        arconiaProperties.put("management.tracing.enabled", environment.getProperty(OpenTelemetryTracingProperties.CONFIG_PREFIX + ".enabled", Boolean.class, true));

        MapPropertySource mapPropertySource = new MapPropertySource("Arconia Observability", arconiaProperties);
        MutablePropertySources mutablePropertySources = environment.getPropertySources();
        mutablePropertySources.addFirst(mapPropertySource);
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
