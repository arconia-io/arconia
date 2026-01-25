package io.arconia.dev.services.ollama;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OnOllamaNativeUnavailable}.
 */
class OnOllamaNativeUnavailableTests {

    private static final MockEnvironment environment = new MockEnvironment();

    private static final ConditionContext context = mock(ConditionContext.class);

    private static final AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);

    @BeforeAll
    static void setup() {
        when(context.getEnvironment()).thenReturn(environment);
    }

    @Test
    void shouldMatchWhenNotInDevMode() {
        OnOllamaNativeUnavailable spyCondition = spy(new OnOllamaNativeUnavailable());
        when(spyCondition.isDevMode()).thenReturn(false);

        ConditionOutcome outcome = spyCondition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage()).contains("Dev mode is not enabled");
    }

    @Test
    void shouldMatchWhenInDevModeButOllamaNativeConnectionIsNotAvailable() {
        OnOllamaNativeUnavailable spyCondition = spy(new OnOllamaNativeUnavailable());
        when(spyCondition.isDevMode()).thenReturn(true);
        when(spyCondition.isOllamaNativeConnection(anyString())).thenReturn(false);

        ConditionOutcome outcome = spyCondition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage()).contains("Ollama native connection is not available");
    }

    @Test
    void shouldNotMatchWhenInDevModeAndOllamaNativeConnectionIsAvailable() {
        OnOllamaNativeUnavailable spyCondition = spy(new OnOllamaNativeUnavailable());
        when(spyCondition.isDevMode()).thenReturn(true);
        when(spyCondition.isOllamaNativeConnection(anyString())).thenReturn(true);

        ConditionOutcome outcome = spyCondition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage())
                .contains("Dev mode is enabled and Ollama native connection detected at");
    }

    @Test
    void shouldNotMatchWhenInDevModeAndOllamaNativeConnectionIsAvailableWithCustomBaseUrl() {
        environment.setProperty("spring.ai.ollama.base-url", "http://custom-host:8080");
        OnOllamaNativeUnavailable spyCondition = spy(new OnOllamaNativeUnavailable());
        when(spyCondition.isDevMode()).thenReturn(true);
        when(spyCondition.isOllamaNativeConnection("http://custom-host:8080")).thenReturn(true);

        ConditionOutcome outcome = spyCondition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage())
                .contains("Dev mode is enabled and Ollama native connection detected at http://custom-host:8080");
    }

    @Test
    void shouldMatchWhenInDevModeButOllamaNativeConnectionIsNotAvailableWithCustomBaseUrl() {
        environment.setProperty("spring.ai.ollama.base-url", "http://custom-host:8080");
        OnOllamaNativeUnavailable spyCondition = spy(new OnOllamaNativeUnavailable());
        when(spyCondition.isDevMode()).thenReturn(true);
        when(spyCondition.isOllamaNativeConnection("http://custom-host:8080")).thenReturn(false);

        ConditionOutcome outcome = spyCondition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage()).contains("Ollama native connection is not available");
    }

    @Test
    void shouldMatchWhenExceptionOccursDuringEvaluation() {
        ConditionContext nullContext = mock(ConditionContext.class);
        when(nullContext.getEnvironment()).thenReturn(null);

        OnOllamaNativeUnavailable condition = new OnOllamaNativeUnavailable();

        ConditionOutcome outcome = condition.getMatchOutcome(nullContext, metadata);

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage()).contains("Failed to evaluate Ollama condition");
    }

    @Test
    void isOllamaNativeConnectionShouldReturnFalseWhenConnectionFails() {
        OnOllamaNativeUnavailable condition = new OnOllamaNativeUnavailable();

        boolean result = condition.isOllamaNativeConnection("http://non-existent-host:11434");

        assertThat(result).isFalse();
    }

    @Test
    void isOllamaNativeConnectionShouldReturnFalseWithInvalidUrl() {
        OnOllamaNativeUnavailable condition = new OnOllamaNativeUnavailable();

        boolean result = condition.isOllamaNativeConnection("invalid-url");

        assertThat(result).isFalse();
    }

}
