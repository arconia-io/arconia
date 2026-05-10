package io.arconia.observation.opentelemetry.ai.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link OpenTelemetryAiConventionsEnvironmentPostProcessor}.
 * Verifies that flavor-specific defaults are injected at lowest priority and can be overridden.
 */
class OpenTelemetryAiConventionsEnvironmentPostProcessorTests {

    private final OpenTelemetryAiConventionsEnvironmentPostProcessor processor =
            new OpenTelemetryAiConventionsEnvironmentPostProcessor();

    private static final String CAPTURE_CONTENT_KEY =
            OpenTelemetryAiConventionsProperties.CONFIG_PREFIX + ".capture-content";

    private static final String INCLUDE_TOOL_DEFINITIONS_KEY =
            OpenTelemetryAiConventionsProperties.CONFIG_PREFIX + ".include-tool-definitions";

    private static final String TOOL_EXECUTION_INCLUDE_CONTENT_KEY =
            OpenTelemetryAiConventionsProperties.CONFIG_PREFIX + ".include-tool-call-content";

    @Test
    void postProcessEnvironmentShouldThrowExceptionWhenEnvironmentIsNull() {
        assertThatThrownBy(() -> processor.postProcessEnvironment(null, new SpringApplication()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("environment cannot be null");
    }

    @Test
    void getOrderShouldReturnLowestPrecedence() {
        assertThat(processor.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
    }

    @Test
    void defaultFlavorShouldHaveConservativeDefaults() {
        var environment = new MockEnvironment();
        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty(CAPTURE_CONTENT_KEY)).isEqualTo("NONE");
        assertThat(environment.getProperty(INCLUDE_TOOL_DEFINITIONS_KEY)).isEqualTo("false");
        assertThat(environment.getProperty(TOOL_EXECUTION_INCLUDE_CONTENT_KEY)).isEqualTo("false");
    }

    @Test
    void opentelemetryFlavorShouldHaveConservativeDefaults() {
        var environment = new MockEnvironment()
                .withProperty(OpenTelemetryAiConventionsProperties.CONFIG_PREFIX + ".flavor", "opentelemetry");
        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty(CAPTURE_CONTENT_KEY)).isEqualTo("NONE");
        assertThat(environment.getProperty(INCLUDE_TOOL_DEFINITIONS_KEY)).isEqualTo("false");
        assertThat(environment.getProperty(TOOL_EXECUTION_INCLUDE_CONTENT_KEY)).isEqualTo("false");
    }

    @Test
    void openllmetryFlavorShouldHaveContentDefaults() {
        var environment = new MockEnvironment()
                .withProperty(OpenTelemetryAiConventionsProperties.CONFIG_PREFIX + ".flavor", "openllmetry");
        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty(CAPTURE_CONTENT_KEY)).isEqualTo("SPAN_ATTRIBUTES");
        assertThat(environment.getProperty(INCLUDE_TOOL_DEFINITIONS_KEY)).isEqualTo("true");
        assertThat(environment.getProperty(TOOL_EXECUTION_INCLUDE_CONTENT_KEY)).isEqualTo("true");
    }

    @Test
    void langsmithFlavorShouldHaveContentDefaults() {
        var environment = new MockEnvironment()
                .withProperty(OpenTelemetryAiConventionsProperties.CONFIG_PREFIX + ".flavor", "langsmith");
        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty(CAPTURE_CONTENT_KEY)).isEqualTo("SPAN_EVENTS");
        assertThat(environment.getProperty(INCLUDE_TOOL_DEFINITIONS_KEY)).isEqualTo("true");
        assertThat(environment.getProperty(TOOL_EXECUTION_INCLUDE_CONTENT_KEY)).isEqualTo("true");
    }

    @Test
    void flavorMatchingShouldBeCaseInsensitive() {
        var environment = new MockEnvironment()
                .withProperty(OpenTelemetryAiConventionsProperties.CONFIG_PREFIX + ".flavor", "OpenLLMetry");
        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty(CAPTURE_CONTENT_KEY)).isEqualTo("SPAN_ATTRIBUTES");
        assertThat(environment.getProperty(INCLUDE_TOOL_DEFINITIONS_KEY)).isEqualTo("true");
        assertThat(environment.getProperty(TOOL_EXECUTION_INCLUDE_CONTENT_KEY)).isEqualTo("true");
    }

    @Test
    void userValueShouldOverrideOpenllmetryDefault() {
        // EPP adds at lowest priority (addLast), user's explicit property takes precedence
        var environment = new MockEnvironment()
                .withProperty(OpenTelemetryAiConventionsProperties.CONFIG_PREFIX + ".flavor", "openllmetry")
                .withProperty(CAPTURE_CONTENT_KEY, "NONE");
        processor.postProcessEnvironment(environment, new SpringApplication());

        // MockEnvironment.withProperty adds to a higher-priority source than addLast,
        // so the user's "NONE" wins over the EPP's "SPAN_ATTRIBUTES".
        assertThat(environment.getProperty(CAPTURE_CONTENT_KEY)).isEqualTo("NONE");
    }

}
