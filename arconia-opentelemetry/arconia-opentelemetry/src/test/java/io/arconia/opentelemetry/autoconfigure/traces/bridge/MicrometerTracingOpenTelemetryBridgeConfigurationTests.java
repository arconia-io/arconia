package io.arconia.opentelemetry.autoconfigure.traces.bridge;

import io.micrometer.tracing.otel.bridge.OtelPropagator;
import io.micrometer.tracing.propagation.Propagator;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.resources.Resource;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.arconia.opentelemetry.autoconfigure.traces.OpenTelemetryTracingAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link MicrometerTracingOpenTelemetryBridgeConfiguration}.
 */
class MicrometerTracingOpenTelemetryBridgeConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OpenTelemetryTracingAutoConfiguration.class))
            .withBean(Clock.class, Clock::getDefault)
            .withBean(Resource.class, Resource::empty)
            .withBean(OpenTelemetry.class, OpenTelemetry::noop);

    @Test
    void propagatorAvailableByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(Propagator.class);
            assertThat(context.getBean(Propagator.class)).isInstanceOf(OtelPropagator.class);
        });
    }

    @Test
    void customPropagatorTakesPrecedence() {
        contextRunner
            .withUserConfiguration(CustomPropagatorConfiguration.class)
            .run(context -> {
                assertThat(context).hasSingleBean(Propagator.class);
                assertThat(context).hasBean("customPropagator");
                assertThat(context).doesNotHaveBean(OtelPropagator.class);
            });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomPropagatorConfiguration {

        @Bean
        Propagator customPropagator() {
            return mock(Propagator.class);
        }

    }

}
