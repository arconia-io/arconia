package io.arconia.opentelemetry.autoconfigure.traces.propagation;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.resources.Resource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import io.arconia.opentelemetry.autoconfigure.traces.OpenTelemetryTracingAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenTelemetryPropagationConfiguration}.
 */
class OpenTelemetryPropagationConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OpenTelemetryTracingAutoConfiguration.class))
            .withBean(Clock.class, Clock::getDefault)
            .withBean(Resource.class, Resource::empty)
            .withBean(OpenTelemetry.class, OpenTelemetry::noop);

    @ParameterizedTest
    @ValueSource(strings = { "otlp", "console", "none" })
    void tracesAndBaggagePropagatedWithAnyExporterType(String exporterType) {
        contextRunner
            .withPropertyValues("arconia.otel.traces.exporter.type=" + exporterType)
            .run(context -> {
                assertThat(context).hasBean("textMapPropagatorWithBaggage");
                assertThat(context.getBean(ContextPropagators.class).getTextMapPropagator().fields())
                    .contains("traceparent", "baggage");
            });
    }

    @Test
    void tracesPropagatedWithoutBaggageWhenBaggageDisabled() {
        contextRunner
            .withPropertyValues("management.tracing.baggage.enabled=false", "arconia.otel.traces.exporter.type=none")
            .run(context -> {
                assertThat(context).hasBean("textMapPropagator");
                assertThat(context).doesNotHaveBean("textMapPropagatorWithBaggage");
                assertThat(context.getBean(ContextPropagators.class).getTextMapPropagator().fields())
                    .contains("traceparent")
                    .doesNotContain("baggage");
            });
    }

}
