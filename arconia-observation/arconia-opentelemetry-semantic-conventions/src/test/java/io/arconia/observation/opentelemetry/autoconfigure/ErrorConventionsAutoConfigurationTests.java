package io.arconia.observation.opentelemetry.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import io.arconia.observation.opentelemetry.instrumentation.error.OpenTelemetryErrorTypeObservationFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ErrorConventionsAutoConfiguration}.
 */
class ErrorConventionsAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ErrorConventionsAutoConfiguration.class));

    @Test
    void activatesByDefault() {
        contextRunner.run(context ->
                assertThat(context).hasSingleBean(OpenTelemetryErrorTypeObservationFilter.class));
    }

    @Test
    void doesNotActivateWhenDisabled() {
        contextRunner
                .withPropertyValues("arconia.observations.conventions.opentelemetry.error.enabled=false")
                .run(context ->
                        assertThat(context).doesNotHaveBean(OpenTelemetryErrorTypeObservationFilter.class));
    }

}
