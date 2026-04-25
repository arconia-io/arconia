package io.arconia.observation.opentelemetry.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.observation.OpenTelemetryServerRequestObservationConvention;
import org.springframework.http.server.observation.ServerRequestObservationConvention;

import io.arconia.observation.opentelemetry.instrumentation.http.ExtendedOpenTelemetryServerRequestObservationConvention;

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
        webContextRunner.run(context ->
                assertThat(context).hasSingleBean(ExtendedOpenTelemetryServerRequestObservationConvention.class));
    }

    @Test
    void doesNotActivateInNonWebContext() {
        nonWebContextRunner.run(context ->
                assertThat(context).doesNotHaveBean(ServerRequestObservationConvention.class));
    }

    @Test
    void doesNotActivateWhenDisabled() {
        webContextRunner
                .withPropertyValues("arconia.observations.conventions.opentelemetry.http.enabled=false")
                .run(context ->
                        assertThat(context).doesNotHaveBean(ServerRequestObservationConvention.class));
    }

    @Test
    void doesNotActivateWhenConventionTypeSetToDifferentValue() {
        webContextRunner
                .withPropertyValues("arconia.observations.conventions.type=micrometer")
                .run(context ->
                        assertThat(context).doesNotHaveBean(ServerRequestObservationConvention.class));
    }

    @Test
    void doesNotActivateWhenSpringWebNotOnClasspath() {
        webContextRunner
                .withClassLoader(new FilteredClassLoader(OpenTelemetryServerRequestObservationConvention.class))
                .run(context ->
                        assertThat(context).doesNotHaveBean(ServerRequestObservationConvention.class));
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

    // Custom bean configurations

    @Configuration(proxyBeanMethods = false)
    static class CustomServerRequestConventionConfig {
        @Bean
        ServerRequestObservationConvention serverRequestObservationConvention() {
            return new OpenTelemetryServerRequestObservationConvention();
        }
    }

}
