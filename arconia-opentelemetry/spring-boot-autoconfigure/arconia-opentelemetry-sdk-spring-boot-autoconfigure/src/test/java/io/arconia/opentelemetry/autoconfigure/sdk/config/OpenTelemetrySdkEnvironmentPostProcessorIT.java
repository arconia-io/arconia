package io.arconia.opentelemetry.autoconfigure.sdk.config;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import io.arconia.opentelemetry.autoconfigure.sdk.OpenTelemetryProperties;
import io.arconia.opentelemetry.autoconfigure.sdk.exporter.ExporterType;
import io.arconia.opentelemetry.autoconfigure.sdk.logs.exporter.OpenTelemetryLoggingExporterProperties;
import io.arconia.opentelemetry.autoconfigure.sdk.metrics.exporter.OpenTelemetryMetricsExporterProperties;
import io.arconia.opentelemetry.autoconfigure.sdk.tracing.exporter.OpenTelemetryTracingExporterProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OpenTelemetrySdkEnvironmentPostProcessor}.
 */
class OpenTelemetrySdkEnvironmentPostProcessorIT {

    private SpringApplication application;

    @TempDir
    java.io.File temp;

    @BeforeEach
    void setup() {
        this.application = new SpringApplication(Config.class);
        this.application.setWebApplicationType(WebApplicationType.NONE);
    }

    @AfterEach
    void cleanup() {
        System.clearProperty("otel.sdk.disabled");
        System.clearProperty("otel.service.name");
        System.clearProperty("otel.logs.exporter");
        System.clearProperty("otel.metrics.exporter");
        System.clearProperty("otel.traces.exporter");
    }

    @Test
    void runWhenHasOtelPropertiesInSystemPropertiesShouldConvertToArconiaProperties() {
        System.setProperty("otel.sdk.disabled", "true");
        System.setProperty("otel.service.name", "test-service");
        System.setProperty("otel.logs.exporter", "otlp");
        System.setProperty("otel.metrics.exporter", "otlp");
        System.setProperty("otel.traces.exporter", "otlp");

        ConfigurableApplicationContext context = this.application.run();

        assertThat(context.getEnvironment().getProperty(OpenTelemetryProperties.CONFIG_PREFIX + ".enabled"))
            .isEqualTo("false");
        assertThat(context.getEnvironment().getProperty("spring.application.name"))
            .isEqualTo("test-service");
        assertThat(context.getEnvironment().getProperty(OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".type"))
            .isEqualTo(ExporterType.OTLP.name());
        assertThat(context.getEnvironment().getProperty(OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".type"))
            .isEqualTo(ExporterType.OTLP.name());
        assertThat(context.getEnvironment().getProperty(OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".type"))
            .isEqualTo(ExporterType.OTLP.name());
    }

    @Test
    void runWhenHasOtelPropertiesInEnvironmentVariablesShouldConvertToArconiaProperties() {
        ConfigurableEnvironment environment = new StandardEnvironment();
        Map<String, Object> envVars = new HashMap<>();
        envVars.put("OTEL_SDK_DISABLED", "true");
        envVars.put("OTEL_SERVICE_NAME", "test-service");
        envVars.put("OTEL_LOGS_EXPORTER", "otlp");
        envVars.put("OTEL_METRICS_EXPORTER", "otlp");
        envVars.put("OTEL_TRACES_EXPORTER", "otlp");
        environment.getPropertySources().addFirst(new MapPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, envVars));
        this.application.setEnvironment(environment);

        ConfigurableApplicationContext context = this.application.run();

        assertThat(context.getEnvironment().getProperty(OpenTelemetryProperties.CONFIG_PREFIX + ".enabled"))
            .isEqualTo("false");
        assertThat(context.getEnvironment().getProperty("spring.application.name"))
            .isEqualTo("test-service");
        assertThat(context.getEnvironment().getProperty(OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".type"))
            .isEqualTo(ExporterType.OTLP.name());
        assertThat(context.getEnvironment().getProperty(OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".type"))
            .isEqualTo(ExporterType.OTLP.name());
        assertThat(context.getEnvironment().getProperty(OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".type"))
            .isEqualTo(ExporterType.OTLP.name());
    }

    @Test
    void runWhenHasOtelPropertiesInCommandLineArgumentsShouldConvertToArconiaProperties() {
        ConfigurableApplicationContext context = this.application.run(
            "--otel.sdk.disabled=true",
            "--otel.service.name=test-service",
            "--otel.logs.exporter=otlp",
            "--otel.metrics.exporter=otlp",
            "--otel.traces.exporter=otlp"
        );

        assertThat(context.getEnvironment().getProperty(OpenTelemetryProperties.CONFIG_PREFIX + ".enabled"))
            .isEqualTo("false");
        assertThat(context.getEnvironment().getProperty("spring.application.name"))
            .isEqualTo("test-service");
        assertThat(context.getEnvironment().getProperty(OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".type"))
            .isEqualTo(ExporterType.OTLP.name());
        assertThat(context.getEnvironment().getProperty(OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".type"))
            .isEqualTo(ExporterType.OTLP.name());
        assertThat(context.getEnvironment().getProperty(OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".type"))
            .isEqualTo(ExporterType.OTLP.name());
    }

    @Test
    void runWhenHasOtelPropertiesInApplicationPropertiesShouldConvertToArconiaProperties() {
        ConfigurableApplicationContext context = this.application.run(
            "--spring.config.location=classpath:application-otel.properties"
        );

        assertThat(context.getEnvironment().getProperty(OpenTelemetryProperties.CONFIG_PREFIX + ".enabled"))
            .isEqualTo("false");
        assertThat(context.getEnvironment().getProperty("spring.application.name"))
            .isEqualTo("test-service");
        assertThat(context.getEnvironment().getProperty(OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".type"))
            .isEqualTo(ExporterType.OTLP.name());
        assertThat(context.getEnvironment().getProperty(OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".type"))
            .isEqualTo(ExporterType.OTLP.name());
        assertThat(context.getEnvironment().getProperty(OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".type"))
            .isEqualTo(ExporterType.OTLP.name());
    }

    @Test
    void runWhenHasOtelPropertiesInApplicationYamlShouldConvertToArconiaProperties() {
        ConfigurableApplicationContext context = this.application.run(
            "--spring.config.location=classpath:application-otel.yml"
        );

        assertThat(context.getEnvironment().getProperty(OpenTelemetryProperties.CONFIG_PREFIX + ".enabled"))
            .isEqualTo("false");
        assertThat(context.getEnvironment().getProperty("spring.application.name"))
            .isEqualTo("test-service");
        assertThat(context.getEnvironment().getProperty(OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".type"))
            .isEqualTo(ExporterType.OTLP.name());
        assertThat(context.getEnvironment().getProperty(OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".type"))
            .isEqualTo(ExporterType.OTLP.name());
        assertThat(context.getEnvironment().getProperty(OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".type"))
            .isEqualTo(ExporterType.OTLP.name());
    }

    static class Config {

    }

}
