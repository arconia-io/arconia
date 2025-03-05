package io.arconia.opentelemetry.autoconfigure.instrumentation.logback;

import ch.qos.logback.core.Appender;
import io.opentelemetry.api.OpenTelemetry;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationListener;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LogbackAppenderInstrumentationAutoConfiguration}.
 */
class LogbackAppenderInstrumentationAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LogbackAppenderInstrumentationAutoConfiguration.class))
            .withBean(OpenTelemetry.class, () -> OpenTelemetry.noop());

    @Test
    void autoConfigurationNotActivatedWhenAppenderClassMissing() {
        contextRunner.withClassLoader(new FilteredClassLoader(Appender.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ApplicationListener.class);
                    assertThat(context).doesNotHaveBean("logbackAppenderOnReady");
                    assertThat(context).doesNotHaveBean("logbackAppenderOnFailed");
                });
    }

    @Test
    void autoConfigurationNotActivatedWhenOpenTelemetryDisabled() {
        contextRunner
            .withPropertyValues("arconia.otel.enabled=false")
            .run(context -> {
                assertThat(context).doesNotHaveBean("logbackAppenderOnReady");
                assertThat(context).doesNotHaveBean("logbackAppenderOnFailed");
            });
    }

    @Test
    void autoConfigurationNotActivatedWhenLoggingDisabled() {
        contextRunner
            .withPropertyValues("arconia.otel.logs.enabled=false")
            .run(context -> {
                assertThat(context).doesNotHaveBean("logbackAppenderOnReady");
                assertThat(context).doesNotHaveBean("logbackAppenderOnFailed");
            });
    }

    @Test
    void autoConfigurationNotActivatedWhenInstrumentationDisabled() {
        contextRunner
            .withPropertyValues("arconia.otel.instrumentation.logback-appender.enabled=false")
            .run(context -> {
                assertThat(context).doesNotHaveBean("logbackAppenderOnReady");
                assertThat(context).doesNotHaveBean("logbackAppenderOnFailed");
            });
    }

    @Test
    void autoConfigurationNotActivatedWhenLoggingExportDisabled() {
        contextRunner
            .withPropertyValues("arconia.otel.logs.exporter.type=none")
            .run(context -> {
                assertThat(context).doesNotHaveBean("logbackAppenderOnReady");
                assertThat(context).doesNotHaveBean("logbackAppenderOnFailed");
            });
    }

    @Test
    void listenersAvailableWithDefaultConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(OpenTelemetry.class);
            assertThat(context).hasBean("logbackAppenderOnReady");
            assertThat(context).hasBean("logbackAppenderOnFailed");
            assertThat(context.getBean("logbackAppenderOnReady"))
                .isInstanceOf(ApplicationListener.class);
            assertThat(context.getBean("logbackAppenderOnFailed"))
                .isInstanceOf(ApplicationListener.class);
        });
    }

    @Test
    void listenersAvailableWithOtlpExporter() {
        contextRunner
            .withPropertyValues("arconia.otel.logs.exporter.type=otlp")
            .run(context -> {
                assertThat(context).hasBean("logbackAppenderOnReady");
                assertThat(context).hasBean("logbackAppenderOnFailed");
            });
    }

    @Test
    void listenersNotAvailableWhenOpenTelemetryMissing() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LogbackAppenderInstrumentationAutoConfiguration.class))
            .run(context -> {
                assertThat(context).doesNotHaveBean("logbackAppenderOnReady");
                assertThat(context).doesNotHaveBean("logbackAppenderOnFailed");
            });
    }

}
