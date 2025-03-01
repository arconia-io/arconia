package io.arconia.opentelemetry.autoconfigure.sdk.traces;

import io.micrometer.tracing.otel.bridge.OtelTracer;
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
import org.springframework.boot.actuate.autoconfigure.tracing.SdkTracerProviderBuilderCustomizer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
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
            .withPropertyValues("arconia.otel.enabled=true")
            .withBean(Clock.class, Clock::getDefault)
            .withBean(Resource.class, Resource::empty)
            .withBean(OpenTelemetry.class, () -> mock(OpenTelemetry.class));

    @Test
    void autoConfigurationNotActivatedWhenOpenTelemetryDisabled() {
        contextRunner
            .withPropertyValues("arconia.otel.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(SdkTracerProvider.class));
    }

    @Test
    void autoConfigurationNotActivatedWhenTracingDisabled() {
        contextRunner
            .withPropertyValues("arconia.otel.traces.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(SdkTracerProvider.class));
    }

    @Test
    void autoConfigurationNotActivatedWhenTracerProviderClassMissing() {
        contextRunner.withClassLoader(new FilteredClassLoader(SdkTracerProvider.class))
                .run(context -> assertThat(context).doesNotHaveBean(SdkTracerProvider.class));
    }

    @Test
    void tracerProviderAvailableWithDefaultConfiguration() {
        contextRunner.withUserConfiguration(org.springframework.boot.actuate.autoconfigure.tracing.OpenTelemetryTracingAutoConfiguration.class)
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
                "arconia.otel.traces.sampling.probability=0.5"
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
                "arconia.otel.traces.sampling.probability=0.5"
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
                "arconia.otel.traces.span-limits.max-number-of-attributes=10",
                "arconia.otel.traces.span-limits.max-number-of-events=20",
                "arconia.otel.traces.span-limits.max-number-of-links=30",
                "arconia.otel.traces.span-limits.max-number-of-attributes-per-event=40",
                "arconia.otel.traces.span-limits.max-number-of-attributes-per-link=50",
                "arconia.otel.traces.span-limits.max-attribute-value-length=60"
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
    void batchSpanProcessorConfigurationApplied() {
        contextRunner.withClassLoader(new FilteredClassLoader(OtelTracer.class))
            .withUserConfiguration(CustomSpanExporterConfiguration.class)
            .withPropertyValues(
                "arconia.otel.traces.processor.export-timeout=10s",
                "arconia.otel.traces.processor.schedule-delay=5s",
                "arconia.otel.traces.processor.max-export-batch-size=512",
                "arconia.otel.traces.processor.max-queue-size=2048",
                "arconia.otel.traces.processor.metrics=true"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(BatchSpanProcessor.class);
                assertThat(context).hasSingleBean(SpanExporter.class);
            });
    }

    @Test
    void customTracerProviderBuilderCustomizerApplied() {
        contextRunner
            .withUserConfiguration(CustomTracerProviderConfiguration.class)
            .run(context -> {
                assertThat(context).hasSingleBean(SdkTracerProvider.class);
                assertThat(context).hasSingleBean(SdkTracerProviderBuilderCustomizer.class);
            });
    }

    @Test
    void customContextPropagatorsAvailable() {
        contextRunner
            .withUserConfiguration(CustomContextPropagatorsConfiguration.class)
            .run(context -> {
                assertThat(context).hasSingleBean(ContextPropagators.class);
                assertThat(context).hasSingleBean(TextMapPropagator.class);
            });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomTracerProviderConfiguration {

        private final SdkTracerProviderBuilderCustomizer customizer = mock(SdkTracerProviderBuilderCustomizer.class);

        @Bean
        SdkTracerProviderBuilderCustomizer customTracerProviderBuilderCustomizer() {
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
    static class CustomContextPropagatorsConfiguration {

        private final TextMapPropagator customTextMapPropagator = mock(TextMapPropagator.class);

        @Bean
        TextMapPropagator customTextMapPropagator() {
            return customTextMapPropagator;
        }

    }

}
