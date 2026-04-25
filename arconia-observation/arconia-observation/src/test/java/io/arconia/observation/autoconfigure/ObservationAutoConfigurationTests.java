package io.arconia.observation.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import io.arconia.observation.conventions.ObservationConventionsProvider;

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
            assertThat(properties.getConventions().getType()).isNull();
        });
    }

    @Test
    void observationPropertiesCustomValues() {
        contextRunner.withPropertyValues("arconia.observations.conventions.type=openinference").run(context -> {
            var properties = context.getBean(ObservationProperties.class);
            assertThat(properties.getConventions().getType()).isEqualTo("openinference");
        });
    }

    @Test
    void noFailureWithSingleConventionsProvider() {
        contextRunner
            .withBean("provider", ObservationConventionsProvider.class, () -> () -> "openinference")
            .run(context -> {
                assertThat(context).hasNotFailed();
            });
    }

    @Test
    void failsWithMultipleConventionsProvidersAndNoTypeProperty() {
        contextRunner
            .withBean("provider1", ObservationConventionsProvider.class, () -> () -> "openinference")
            .withBean("provider2", ObservationConventionsProvider.class, () -> () -> "opentelemetry")
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                        .isInstanceOf(MultipleObservationConventionsException.class)
                        .hasMessageContaining("openinference")
                        .hasMessageContaining("opentelemetry");
            });
    }

    @Test
    void noFailureWithMultipleConventionsProvidersWhenTypePropertySet() {
        contextRunner
            .withPropertyValues("arconia.observations.conventions.type=openinference")
            .withBean("provider1", ObservationConventionsProvider.class, () -> () -> "openinference")
            .withBean("provider2", ObservationConventionsProvider.class, () -> () -> "opentelemetry")
            .run(context -> {
                assertThat(context).hasNotFailed();
            });
    }

    @Test
    void noFailureWithNoConventionsProviders() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
        });
    }

}
