package io.arconia.opentelemetry.autoconfigure.traces;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanLimits;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link OpenTelemetryTracingAutoConfiguration}.
 */
class OpenTelemetryTracingAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OpenTelemetryTracingAutoConfiguration.class))
            .withBean(Clock.class, Clock::getDefault)
            .withBean(Resource.class, Resource::empty)
            .withBean(OpenTelemetry.class, OpenTelemetry::noop);

    @Test
    void autoConfigurationNotActivatedWhenOpenTelemetryDisabled() {
        contextRunner
            .withPropertyValues("arconia.otel.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(SdkTracerProvider.class));
    }

    @Test
    void autoConfigurationNotActivatedWhenArconiaTracingDisabled() {
        contextRunner
            .withPropertyValues("arconia.otel.traces.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(SdkTracerProvider.class));
    }

    @Test
    void tracerProviderAvailableWithDefaultConfiguration() {
        contextRunner
            .run(context -> {
                assertThat(context).hasSingleBean(SdkTracerProvider.class);
                assertThat(context).hasSingleBean(Sampler.class);
                assertThat(context).hasSingleBean(SpanLimits.class);
                assertThat(context).hasSingleBean(BatchSpanProcessor.class);
                assertThat(context).hasSingleBean(ContextPropagators.class);
                assertThat(context).hasSingleBean(Tracer.class);
            });
    }

    @Test
    void samplerConfigurationApplied() {
        contextRunner
            .withPropertyValues("arconia.otel.traces.sampling.strategy=always-on")
            .run(context -> {
                Sampler sampler = context.getBean(Sampler.class);
                assertThat(sampler).isEqualTo(Sampler.alwaysOn());
            });

        contextRunner
            .withPropertyValues("arconia.otel.traces.sampling.strategy=always-off")
            .run(context -> {
                Sampler sampler = context.getBean(Sampler.class);
                assertThat(sampler).isEqualTo(Sampler.alwaysOff());
            });

        contextRunner
            .withPropertyValues(
                "arconia.otel.traces.sampling.strategy=trace-id-ratio",
                "management.tracing.sampling.probability=0.5"
            )
            .run(context -> {
                Sampler sampler = context.getBean(Sampler.class);
                assertThat(sampler).isEqualTo(Sampler.traceIdRatioBased(0.5));
            });

        contextRunner
            .withPropertyValues("arconia.otel.traces.sampling.strategy=parent-based-always-on")
            .run(context -> {
                Sampler sampler = context.getBean(Sampler.class);
                assertThat(sampler).isEqualTo(Sampler.parentBased(Sampler.alwaysOn()));
            });

        contextRunner
            .withPropertyValues("arconia.otel.traces.sampling.strategy=parent-based-always-off")
            .run(context -> {
                Sampler sampler = context.getBean(Sampler.class);
                assertThat(sampler).isEqualTo(Sampler.parentBased(Sampler.alwaysOff()));
            });

        contextRunner
            .withPropertyValues(
                "arconia.otel.traces.sampling.strategy=parent-based-trace-id-ratio",
                "management.tracing.sampling.probability=0.5"
            )
            .run(context -> {
                Sampler sampler = context.getBean(Sampler.class);
                assertThat(sampler).isEqualTo(Sampler.parentBased(Sampler.traceIdRatioBased(0.5)));
            });
    }

    @Test
    void spanLimitsConfigurationApplied() {
        contextRunner
            .withPropertyValues(
                "arconia.otel.traces.limits.max-number-of-attributes=10",
                "arconia.otel.traces.limits.max-number-of-events=20",
                "arconia.otel.traces.limits.max-number-of-links=30",
                "arconia.otel.traces.limits.max-number-of-attributes-per-event=40",
                "arconia.otel.traces.limits.max-number-of-attributes-per-link=50",
                "arconia.otel.traces.limits.max-attribute-value-length=60"
            )
            .run(context -> {
                SpanLimits spanLimits = context.getBean(SpanLimits.class);
                assertThat(spanLimits.getMaxNumberOfAttributes()).isEqualTo(10);
                assertThat(spanLimits.getMaxNumberOfEvents()).isEqualTo(20);
                assertThat(spanLimits.getMaxNumberOfLinks()).isEqualTo(30);
                assertThat(spanLimits.getMaxNumberOfAttributesPerEvent()).isEqualTo(40);
                assertThat(spanLimits.getMaxNumberOfAttributesPerLink()).isEqualTo(50);
                assertThat(spanLimits.getMaxAttributeValueLength()).isEqualTo(60);
            });
    }

    @Test
    void otelSpanProcessorConfigurationApplied() {
        contextRunner
            .withUserConfiguration(CustomSpanExporterConfiguration.class)
            .withPropertyValues(
                "arconia.otel.traces.processor.export-timeout=10s",
                "arconia.otel.traces.processor.schedule-delay=5s",
                "arconia.otel.traces.processor.max-export-batch-size=512",
                "arconia.otel.traces.processor.max-queue-size=2048",
                "arconia.otel.traces.processor.export-unsampled-spans=false",
                "arconia.otel.traces.processor.metrics=true"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(SpanExporter.class);
                assertThat(context).getBeanNames(BatchSpanProcessor.class)
                        .hasSize(1)
                        .containsExactly("micrometerBatchSpanProcessor");
            });
    }

    @Test
    void otelSpanProcessorUsesCompositeExporterWithMultipleExporters() {
        contextRunner
            .withUserConfiguration(MultipleSpanExportersConfiguration.class)
            .run(context -> {
                assertThat(context).hasSingleBean(BatchSpanProcessor.class);
                assertThat(context.getBeansOfType(SpanExporter.class)).hasSize(2);
            });
    }

    @Test
    void customTracerProviderBuilderCustomizerApplied() {
        contextRunner
            .withUserConfiguration(CustomTracerProviderConfiguration.class)
            .run(context -> {
                assertThat(context).hasSingleBean(OpenTelemetryTracerProviderBuilderCustomizer.class);
                assertThat(context).hasSingleBean(OpenTelemetryTracerProviderBuilderCustomizer.class);
            });
    }

    @Test
    void customContextPropagatorsAvailable() {
        contextRunner
            .withUserConfiguration(CustomContextPropagatorsConfiguration.class)
            .run(context -> {
                assertThat(context).hasSingleBean(ContextPropagators.class);
                assertThat(context).getBeanNames(TextMapPropagator.class)
                        .hasSize(2)
                        .containsExactly("customTextMapPropagator", "textMapPropagatorWithBaggage");
            });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomTracerProviderConfiguration {

        private final OpenTelemetryTracerProviderBuilderCustomizer customizer = mock(OpenTelemetryTracerProviderBuilderCustomizer.class);

        @Bean
        OpenTelemetryTracerProviderBuilderCustomizer customTracerProviderBuilderCustomizer() {
            return customizer;
        }

    }

    @Configuration(proxyBeanMethods = false)
    static class CustomSpanExporterConfiguration {

        private final SpanExporter customSpanExporter = mock(SpanExporter.class);

        @Bean
        SpanExporter customSpanExporter() {
            return customSpanExporter;
        }

    }

    @Configuration(proxyBeanMethods = false)
    static class MultipleSpanExportersConfiguration {

        @Bean
        SpanExporter firstSpanExporter() {
            return mock(SpanExporter.class);
        }

        @Bean
        SpanExporter secondSpanExporter() {
            return mock(SpanExporter.class);
        }

    }

    @Configuration(proxyBeanMethods = false)
    static class CustomContextPropagatorsConfiguration {

        private final TextMapPropagator customTextMapPropagator = mock(TextMapPropagator.class);

        @Bean
        TextMapPropagator customTextMapPropagator() {
            return customTextMapPropagator;
        }

    }

}
