package io.arconia.observation.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ObservationAutoConfiguration}.
 */
class ObservationAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ObservationAutoConfiguration.class));

    @Test
    void observationPropertiesWhenDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ObservationProperties.class);
        });
    }

    @Test
    void observationPropertiesDefaultValues() {
        contextRunner.run(context -> {
            var properties = context.getBean(ObservationProperties.class);
            assertThat(properties.getConventions().getType()).isEqualTo("micrometer");
        });
    }

    @Test
    void observationPropertiesCustomValues() {
        contextRunner.withPropertyValues("arconia.observations.conventions.type=openinference").run(context -> {
            var properties = context.getBean(ObservationProperties.class);
            assertThat(properties.getConventions().getType()).isEqualTo("openinference");
        });
    }

}
