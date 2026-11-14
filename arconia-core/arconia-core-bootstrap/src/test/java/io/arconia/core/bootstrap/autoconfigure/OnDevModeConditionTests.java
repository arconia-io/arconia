package io.arconia.core.bootstrap.autoconfigure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OnDevModeCondition}.
 */
class OnDevModeConditionTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void shouldMatchWhenDevMode() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .run(context -> assertThat(context).hasBean("devBean"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "test", "prod" })
    void shouldNotMatchWhenNotDevMode(String mode) {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=" + mode)
                .run(context -> assertThat(context).doesNotHaveBean("devBean"));
    }

    @Test
    void shouldNotMatchWhenModeIsDetected() {
        // The test is running in a JUnit context, so the test mode is detected.
        contextRunner.run(context -> assertThat(context).doesNotHaveBean("devBean"));
    }

    @Configuration(proxyBeanMethods = false)
    static class TestConfig {

        @Bean
        @ConditionalOnDevMode
        String devBean() {
            return "dev";
        }

    }

}
