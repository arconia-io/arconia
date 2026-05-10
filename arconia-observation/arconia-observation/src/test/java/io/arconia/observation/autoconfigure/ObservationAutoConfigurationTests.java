package io.arconia.observation.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import io.arconia.observation.conventions.AiObservationConventionsProvider;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ObservationAutoConfiguration}.
 */
class ObservationAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ObservationAutoConfiguration.class));

    @Test
    void noFailureWithNoConventionsProviders() {
        contextRunner.run(context ->
                assertThat(context).hasNotFailed());
    }

    @Test
    void noFailureWithSingleConventionsProvider() {
        contextRunner
            .withBean("provider", AiObservationConventionsProvider.class, () -> AiObservationConventionsProvider.of("openinference"))
            .run(context ->
                assertThat(context).hasNotFailed());
    }

    @Test
    void failsWithMultipleConventionsProviders() {
        contextRunner
            .withBean("provider1", AiObservationConventionsProvider.class, () -> AiObservationConventionsProvider.of("openinference"))
            .withBean("provider2", AiObservationConventionsProvider.class, () -> AiObservationConventionsProvider.of("opentelemetry"))
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                        .isInstanceOf(MultipleAiObservationConventionsException.class)
                        .hasMessageContaining("openinference")
                        .hasMessageContaining("opentelemetry");
            });
    }

}
