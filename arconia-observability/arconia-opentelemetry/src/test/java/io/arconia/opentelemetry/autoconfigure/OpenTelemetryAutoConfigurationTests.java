package io.arconia.opentelemetry.autoconfigure;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.trace.SdkTracerProvider;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link OpenTelemetryAutoConfiguration}.
 */
class OpenTelemetryAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OpenTelemetryAutoConfiguration.class));

    @Test
    void openTelemetryWhenDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(OpenTelemetry.class);
            assertThat(context.getBean(OpenTelemetry.class)).isInstanceOf(OpenTelemetrySdk.class);
        });
    }

    @Test
    void openTelemetryWhenEnabled() {
        contextRunner.withPropertyValues("arconia.otel.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(OpenTelemetry.class);
                    assertThat(context.getBean(OpenTelemetry.class)).isInstanceOf(OpenTelemetrySdk.class);
                });
    }

    @Test
    void openTelemetryWhenDisabled() {
        contextRunner.withPropertyValues("arconia.otel.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(OpenTelemetry.class);
                    assertThat(context.getBean(OpenTelemetry.class)).isSameAs(OpenTelemetry.noop());
                });
    }

    @Test
    void customOpenTelemetryTakesPrecedence() {
        contextRunner.withUserConfiguration(CustomOpenTelemetryConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(OpenTelemetry.class);
                    assertThat(context.getBean(OpenTelemetry.class))
                            .isSameAs(context.getBean(CustomOpenTelemetryConfiguration.class).customOpenTelemetry());
                });
    }

    @Test
    void optionalDependenciesAreConfigured() {
        contextRunner.withUserConfiguration(OptionalDependenciesConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(OpenTelemetry.class);
                    OpenTelemetrySdk sdk = context.getBean(OpenTelemetrySdk.class);

                    SdkLoggerProvider loggerProvider = context.getBean(SdkLoggerProvider.class);
                    SdkMeterProvider meterProvider = context.getBean(SdkMeterProvider.class);
                    SdkTracerProvider tracerProvider = context.getBean(SdkTracerProvider.class);
                    ContextPropagators propagators = context.getBean(ContextPropagators.class);

                    assertThat(sdk.getSdkLoggerProvider()).isSameAs(loggerProvider);
                    assertThat(sdk.getSdkMeterProvider()).isSameAs(meterProvider);
                    assertThat(sdk.getSdkTracerProvider()).isSameAs(tracerProvider);
                    assertThat(sdk.getPropagators()).isSameAs(propagators);
                });
    }

    @Test
    void clockIsAvailable() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(Clock.class);
            assertThat(context.getBean(Clock.class)).isEqualTo(Clock.getDefault());
        });
    }

    @Test
    void customClockTakesPrecedence() {
        contextRunner.withUserConfiguration(CustomClockConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(Clock.class);
                    assertThat(context.getBean(Clock.class))
                            .isSameAs(context.getBean(CustomClockConfiguration.class).customClock());
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomOpenTelemetryConfiguration {

        @Bean
        OpenTelemetry customOpenTelemetry() {
            return OpenTelemetry.noop();
        }

    }

    @Configuration(proxyBeanMethods = false)
    static class CustomClockConfiguration {

        private final Clock customClock = mock(Clock.class);

        @Bean
        Clock customClock() {
            return customClock;
        }

    }

    @Configuration(proxyBeanMethods = false)
    static class OptionalDependenciesConfiguration {

        private final SdkLoggerProvider loggerProvider = mock(SdkLoggerProvider.class);
        private final SdkMeterProvider meterProvider = mock(SdkMeterProvider.class);
        private final SdkTracerProvider tracerProvider = mock(SdkTracerProvider.class);
        private final ContextPropagators propagators = mock(ContextPropagators.class);

        @Bean
        SdkLoggerProvider loggerProvider() {
            return loggerProvider;
        }

        @Bean
        SdkMeterProvider meterProvider() {
            return meterProvider;
        }

        @Bean
        SdkTracerProvider tracerProvider() {
            return tracerProvider;
        }

        @Bean
        ContextPropagators propagators() {
            return propagators;
        }

    }

}
