package io.arconia.opentelemetry.autoconfigure.config;

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

import io.arconia.opentelemetry.autoconfigure.OpenTelemetryProperties;
import io.arconia.opentelemetry.autoconfigure.exporter.ExporterType;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.OpenTelemetryLoggingExporterProperties;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.OpenTelemetryMetricsExporterProperties;
import io.arconia.opentelemetry.autoconfigure.resource.OpenTelemetryResourceProperties;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.OpenTelemetryTracingExporterProperties;

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
        System.clearProperty("arconia.otel.compatibility.opentelemetry");
        System.clearProperty("otel.sdk.disabled");
        System.clearProperty("otel.service.name");
        System.clearProperty("otel.logs.exporter");
        System.clearProperty("otel.metrics.exporter");
        System.clearProperty("otel.traces.exporter");
    }

    @Test
    void runWhenCompatibilityIsDisabledInSystemPropertiesShouldNotConvertProperties() {
        System.setProperty("arconia.otel.compatibility.opentelemetry", "false");
        System.setProperty("otel.sdk.disabled", "true");
        System.setProperty("otel.service.name", "test-service");
        System.setProperty("otel.logs.exporter", "otlp");
        System.setProperty("otel.metrics.exporter", "otlp");
        System.setProperty("otel.traces.exporter", "otlp");

        ConfigurableApplicationContext context = this.application.run();

        assertThat(context.getEnvironment().getProperty(OpenTelemetryProperties.CONFIG_PREFIX + ".enabled"))
            .isNull();
        assertThat(context.getEnvironment().getProperty(OpenTelemetryResourceProperties.CONFIG_PREFIX + ".service-name"))
            .isNull();
        assertThat(context.getEnvironment().getProperty(OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".type"))
            .isNull();
        assertThat(context.getEnvironment().getProperty(OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".type"))
            .isNull();
        assertThat(context.getEnvironment().getProperty(OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".type"))
            .isNull();
    }

    @Test
    void runWhenCompatibilityIsDisabledInEnvironmentVariablesShouldNotConvertProperties() {
        ConfigurableEnvironment environment = new StandardEnvironment();
        Map<String, Object> envVars = new HashMap<>();
        envVars.put("ARCONIA_OTEL_COMPATIBILITY_OPENTELEMETRY", "false");
        envVars.put("OTEL_SDK_DISABLED", "true");
        envVars.put("OTEL_SERVICE_NAME", "test-service");
        envVars.put("OTEL_LOGS_EXPORTER", "otlp");
        envVars.put("OTEL_METRICS_EXPORTER", "otlp");
        envVars.put("OTEL_TRACES_EXPORTER", "otlp");
        environment.getPropertySources().addFirst(new MapPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, envVars));
        this.application.setEnvironment(environment);

        ConfigurableApplicationContext context = this.application.run();

        assertThat(context.getEnvironment().getProperty(OpenTelemetryProperties.CONFIG_PREFIX + ".enabled"))
            .isNull();
        assertThat(context.getEnvironment().getProperty(OpenTelemetryResourceProperties.CONFIG_PREFIX + ".service-name"))
            .isNull();
        assertThat(context.getEnvironment().getProperty(OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".type"))
            .isNull();
        assertThat(context.getEnvironment().getProperty(OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".type"))
            .isNull();
        assertThat(context.getEnvironment().getProperty(OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".type"))
            .isNull();
    }

    @Test
    void runWhenCompatibilityIsDisabledInCommandLineArgumentsShouldNotConvertProperties() {
        ConfigurableApplicationContext context = this.application.run(
            "--arconia.otel.compatibility.opentelemetry=false",
            "--otel.sdk.disabled=true",
            "--otel.service.name=test-service",
            "--otel.logs.exporter=otlp",
            "--otel.metrics.exporter=otlp",
            "--otel.traces.exporter=otlp"
        );

        assertThat(context.getEnvironment().getProperty(OpenTelemetryProperties.CONFIG_PREFIX + ".enabled"))
            .isNull();
        assertThat(context.getEnvironment().getProperty(OpenTelemetryResourceProperties.CONFIG_PREFIX + ".service-name"))
            .isNull();
        assertThat(context.getEnvironment().getProperty(OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".type"))
            .isNull();
        assertThat(context.getEnvironment().getProperty(OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".type"))
            .isNull();
        assertThat(context.getEnvironment().getProperty(OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".type"))
            .isNull();
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
        assertThat(context.getEnvironment().getProperty(OpenTelemetryResourceProperties.CONFIG_PREFIX + ".service-name"))
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
        assertThat(context.getEnvironment().getProperty(OpenTelemetryResourceProperties.CONFIG_PREFIX + ".service-name"))
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
        assertThat(context.getEnvironment().getProperty(OpenTelemetryResourceProperties.CONFIG_PREFIX + ".service-name"))
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
        assertThat(context.getEnvironment().getProperty(OpenTelemetryResourceProperties.CONFIG_PREFIX + ".service-name"))
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
        assertThat(context.getEnvironment().getProperty(OpenTelemetryResourceProperties.CONFIG_PREFIX + ".service-name"))
            .isEqualTo("test-service");
        assertThat(context.getEnvironment().getProperty(OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".type"))
            .isEqualTo(ExporterType.OTLP.name());
        assertThat(context.getEnvironment().getProperty(OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".type"))
            .isEqualTo(ExporterType.OTLP.name());
        assertThat(context.getEnvironment().getProperty(OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".type"))
            .isEqualTo(ExporterType.OTLP.name());
    }

    @Test
    void runWhenCompatibilityIsDisabledInApplicationPropertiesShouldNotConvertProperties() {
        ConfigurableApplicationContext context = this.application.run(
            "--spring.config.location=classpath:application-otel-disabled.properties"
        );

        assertThat(context.getEnvironment().getProperty(OpenTelemetryProperties.CONFIG_PREFIX + ".enabled"))
            .isNull();
        assertThat(context.getEnvironment().getProperty(OpenTelemetryResourceProperties.CONFIG_PREFIX + ".service-name"))
            .isNull();
        assertThat(context.getEnvironment().getProperty(OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".type"))
            .isNull();
        assertThat(context.getEnvironment().getProperty(OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".type"))
            .isNull();
        assertThat(context.getEnvironment().getProperty(OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".type"))
            .isNull();
    }

    @Test
    void runWhenCompatibilityIsDisabledInApplicationYamlShouldNotConvertProperties() {
        ConfigurableApplicationContext context = this.application.run(
            "--spring.config.location=classpath:application-otel-disabled.yml"
        );

        assertThat(context.getEnvironment().getProperty(OpenTelemetryProperties.CONFIG_PREFIX + ".enabled"))
            .isNull();
        assertThat(context.getEnvironment().getProperty(OpenTelemetryResourceProperties.CONFIG_PREFIX + ".service-name"))
            .isNull();
        assertThat(context.getEnvironment().getProperty(OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".type"))
            .isNull();
        assertThat(context.getEnvironment().getProperty(OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".type"))
            .isNull();
        assertThat(context.getEnvironment().getProperty(OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".type"))
            .isNull();
    }

    static class Config {

    }

}
