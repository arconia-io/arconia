package io.arconia.observation.openllmetry.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link OpenLLMetryEnvironmentPropertyAdapters}.
 */
class OpenLLMetryEnvironmentPropertyAdaptersTests {

    @Test
    void tracesShouldMapProperties() {
        var environment = new MockEnvironment()
                .withProperty("TRACELOOP_TRACE_CONTENT", "false");

        var adapter = OpenLLMetryEnvironmentPropertyAdapters.traces(environment);

        String prefix = OpenLLMetryProperties.CONFIG_PREFIX;
        assertThat(adapter.getArconiaProperties().get(prefix + ".trace-content")).isEqualTo(false);
    }

    @Test
    void tracesShouldHandleInvalidBooleanValues() {
        var environment = new MockEnvironment()
                .withProperty("TRACELOOP_TRACE_CONTENT", "not-a-boolean");

        var adapter = OpenLLMetryEnvironmentPropertyAdapters.traces(environment);

        String prefix = OpenLLMetryProperties.CONFIG_PREFIX;
        assertThat(adapter.getArconiaProperties().containsKey(prefix + ".trace-content")).isTrue();
        assertThat(adapter.getArconiaProperties().get(prefix + ".trace-content")).isEqualTo(false);
    }

    @Test
    void tracesShouldThrowExceptionWhenEnvironmentIsNull() {
        assertThatThrownBy(() -> OpenLLMetryEnvironmentPropertyAdapters.traces(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("environment cannot be null");
    }

}
