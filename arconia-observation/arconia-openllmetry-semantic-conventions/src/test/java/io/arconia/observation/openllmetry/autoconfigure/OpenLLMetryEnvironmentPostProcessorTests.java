package io.arconia.observation.openllmetry.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link OpenLLMetryEnvironmentPostProcessor}.
 */
class OpenLLMetryEnvironmentPostProcessorTests {

    private final OpenLLMetryEnvironmentPostProcessor processor = new OpenLLMetryEnvironmentPostProcessor();

    @Test
    void postProcessEnvironmentShouldThrowExceptionWhenEnvironmentIsNull() {
        assertThatThrownBy(() -> processor.postProcessEnvironment(null, new SpringApplication()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("environment cannot be null");
    }

    @Test
    void postProcessEnvironmentShouldAddPropertySourceFirst() {
        var environment = new MockEnvironment();
        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getPropertySources().stream().findFirst())
            .hasValueSatisfying(propertySource -> assertThat(propertySource.getName()).isEqualTo("openllmetry-environment-variable-specification"));
    }

    @Test
    void postProcessEnvironmentShouldMapTraceProperties() {
        var environment = new MockEnvironment()
            .withProperty("TRACELOOP_TRACE_CONTENT", "false");

        processor.postProcessEnvironment(environment, new SpringApplication());

        String prefix = OpenLLMetryProperties.CONFIG_PREFIX;
        assertThat(environment.getProperty(prefix + ".trace-content")).isEqualTo("false");
    }

    @Test
    void getOrderShouldReturnLowestPrecedence() {
        assertThat(processor.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
    }

}
