package io.arconia.observation.opentelemetry.autoconfigure;

import io.micrometer.tracing.otel.bridge.OtelSpan;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.restclient.observation.ObservationRestClientCustomizer;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.observation.ClientRequestObservationConvention;
import org.springframework.http.client.observation.DefaultClientRequestObservationConvention;
import org.springframework.http.server.observation.OpenTelemetryServerRequestObservationConvention;
import org.springframework.http.server.observation.ServerRequestObservationConvention;

import io.arconia.observation.opentelemetry.instrumentation.http.ExtendedOpenTelemetryServerRequestObservationConvention;
import io.arconia.observation.opentelemetry.instrumentation.http.OpenTelemetryClientRequestObservationConvention;
import io.arconia.observation.opentelemetry.instrumentation.http.OpenTelemetryHttpSpanStatusObservationHandler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link HttpConventionsAutoConfiguration}.
 */
class HttpConventionsAutoConfigurationTests {

    private final WebApplicationContextRunner webContextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(HttpConventionsAutoConfiguration.class));

    private final ApplicationContextRunner nonWebContextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(HttpConventionsAutoConfiguration.class));

    // Activation / deactivation

    @Test
    void activatesInServletWebContext() {
        webContextRunner.run(context -> {
            assertThat(context).hasSingleBean(ExtendedOpenTelemetryServerRequestObservationConvention.class);
            assertThat(context).hasSingleBean(OpenTelemetryClientRequestObservationConvention.class);
        });
    }

    @Test
    void activatesOnlyClientConventionInNonWebContext() {
        nonWebContextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(ServerRequestObservationConvention.class);
            assertThat(context).hasSingleBean(OpenTelemetryClientRequestObservationConvention.class);
        });
    }

    @Test
    void doesNotActivateWhenDisabled() {
        webContextRunner
                .withPropertyValues("arconia.observations.conventions.opentelemetry.http.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ServerRequestObservationConvention.class);
                    assertThat(context).doesNotHaveBean(ClientRequestObservationConvention.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryHttpSpanStatusObservationHandler.class);
                });
    }

    @Test
    void doesNotActivateWhenSpringWebNotOnClasspath() {
        webContextRunner
                .withClassLoader(new FilteredClassLoader(OpenTelemetryServerRequestObservationConvention.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ServerRequestObservationConvention.class);
                    assertThat(context).doesNotHaveBean(ClientRequestObservationConvention.class);
                });
    }

    @Test
    void activatesOnlyServerConventionWhenSpringBootRestClientNotOnClasspath() {
        webContextRunner
                .withClassLoader(new FilteredClassLoader(ObservationRestClientCustomizer.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(ExtendedOpenTelemetryServerRequestObservationConvention.class);
                    assertThat(context).doesNotHaveBean(ClientRequestObservationConvention.class);
                });
    }

    @Test
    void registersSpanStatusHandler() {
        nonWebContextRunner.run(context ->
                assertThat(context).hasSingleBean(OpenTelemetryHttpSpanStatusObservationHandler.class));
    }

    @Test
    void doesNotRegisterSpanStatusHandlerWhenOpenTelemetryTracingBridgeNotOnClasspath() {
        webContextRunner
                .withClassLoader(new FilteredClassLoader(OtelSpan.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(OpenTelemetryHttpSpanStatusObservationHandler.class);
                    assertThat(context).hasSingleBean(ExtendedOpenTelemetryServerRequestObservationConvention.class);
                });
    }

    // Custom bean precedence

    @Test
    void customServerRequestObservationConventionTakesPrecedence() {
        webContextRunner
                .withUserConfiguration(CustomServerRequestConventionConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ServerRequestObservationConvention.class);
                    assertThat(context).doesNotHaveBean(ExtendedOpenTelemetryServerRequestObservationConvention.class);
                });
    }

    @Test
    void customClientRequestObservationConventionTakesPrecedence() {
        nonWebContextRunner
                .withUserConfiguration(CustomClientRequestConventionConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ClientRequestObservationConvention.class);
                    assertThat(context).doesNotHaveBean(OpenTelemetryClientRequestObservationConvention.class);
                });
    }

    // Custom bean configurations

    @Configuration(proxyBeanMethods = false)
    static class CustomServerRequestConventionConfig {
        @Bean
        ServerRequestObservationConvention serverRequestObservationConvention() {
            return new OpenTelemetryServerRequestObservationConvention();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomClientRequestConventionConfig {
        @Bean
        ClientRequestObservationConvention clientRequestObservationConvention() {
            return new DefaultClientRequestObservationConvention();
        }
    }

}
