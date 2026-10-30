package io.arconia.observation.opentelemetry.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenTelemetryConventionsAutoConfiguration}.
 */
class OpenTelemetryConventionsAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OpenTelemetryConventionsAutoConfiguration.class));

    @Test
    void observationGroupsEnabledByDefault() {
        contextRunner.run(context -> {
            OpenTelemetryConventionsProperties properties = context.getBean(OpenTelemetryConventionsProperties.class);
            assertThat(properties.getHttp().isEnabled()).isTrue();
            assertThat(properties.getJvm().isEnabled()).isTrue();
            assertThat(properties.getError().isEnabled()).isTrue();
        });
    }

}
