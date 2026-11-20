package io.arconia.opentelemetry.autoconfigure.traces.propagation;

import java.util.List;

import io.micrometer.tracing.otel.bridge.BaggageTaggingSpanProcessor;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SdkTracerProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

    @Test
    void baggageTaggingSpanProcessorWhenTagFieldsAreConfigured() {
        contextRunner
            .withPropertyValues("management.tracing.baggage.tag-fields=t1,t2")
            .run(context -> {
                assertThat(context).hasSingleBean(BaggageTaggingSpanProcessor.class);
                assertThat(context).hasBean("otelBaggageTaggingSpanProcessor");
            });
    }

    @Test
    void noBaggageTaggingSpanProcessorWhenTagFieldsAreNotConfigured() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(BaggageTaggingSpanProcessor.class));
    }

    @Test
    void noBaggageTaggingSpanProcessorWhenBaggageDisabled() {
        contextRunner
            .withPropertyValues("management.tracing.baggage.enabled=false", "management.tracing.baggage.tag-fields=t1")
            .run(context -> assertThat(context).doesNotHaveBean(BaggageTaggingSpanProcessor.class));
    }

    @Test
    void customBaggageTaggingSpanProcessorTakesPrecedence() {
        contextRunner
            .withPropertyValues("management.tracing.baggage.tag-fields=t1")
            .withUserConfiguration(CustomBaggageTaggingSpanProcessorConfiguration.class)
            .run(context -> {
                assertThat(context).hasSingleBean(BaggageTaggingSpanProcessor.class);
                assertThat(context).hasBean("customBaggageTaggingSpanProcessor");
            });
    }

    @Test
    void spansTaggedWithBaggageFromParentContext() {
        contextRunner
            .withPropertyValues("management.tracing.baggage.tag-fields=t1")
            .run(context -> {
                var tracer = context.getBean(SdkTracerProvider.class).get("test");
                try (Scope scope = Baggage.builder().put("t1", "v1").put("other", "v2").build().makeCurrent()) {
                    Span span = tracer.spanBuilder("test").startSpan();
                    span.end();
                    assertThat(span).isInstanceOf(ReadableSpan.class);
                    assertThat(((ReadableSpan) span).getAttribute(AttributeKey.stringKey("t1"))).isEqualTo("v1");
                    assertThat(((ReadableSpan) span).getAttribute(AttributeKey.stringKey("other"))).isNull();
                }
            });
    }

    @Test
    void spansNotTaggedWithBaggageWhenTagFieldsAreNotConfigured() {
        contextRunner.run(context -> {
            var tracer = context.getBean(SdkTracerProvider.class).get("test");
            try (Scope scope = Baggage.builder().put("t1", "v1").build().makeCurrent()) {
                Span span = tracer.spanBuilder("test").startSpan();
                span.end();
                assertThat(span).isInstanceOf(ReadableSpan.class);
                assertThat(((ReadableSpan) span).getAttribute(AttributeKey.stringKey("t1"))).isNull();
            }
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomBaggageTaggingSpanProcessorConfiguration {

        @Bean
        BaggageTaggingSpanProcessor customBaggageTaggingSpanProcessor() {
            return new BaggageTaggingSpanProcessor(List.of("custom"));
        }

    }

}
