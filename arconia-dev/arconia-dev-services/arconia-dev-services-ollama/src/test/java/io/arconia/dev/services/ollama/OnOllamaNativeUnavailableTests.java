package io.arconia.dev.services.ollama;

import org.junit.jupiter.api.BeforeEach;
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

    private MockEnvironment environment;

    private ConditionContext context;

    private AnnotatedTypeMetadata metadata;

    @BeforeEach
    void setup() {
        environment = new MockEnvironment();
        context = mock(ConditionContext.class);
        metadata = mock(AnnotatedTypeMetadata.class);
        when(context.getEnvironment()).thenReturn(environment);
    }

    @Test
    void shouldMatchWhenOllamaNativeConnectionIsNotAvailable() {
        OnOllamaNativeUnavailable spyCondition = spy(new OnOllamaNativeUnavailable());
        when(spyCondition.isOllamaNativeConnection(anyString())).thenReturn(false);

        ConditionOutcome outcome = spyCondition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage()).contains("Ollama native connection is not available");
    }

    @Test
    void shouldNotMatchWhenOllamaNativeConnectionIsAvailable() {
        OnOllamaNativeUnavailable spyCondition = spy(new OnOllamaNativeUnavailable());
        when(spyCondition.isOllamaNativeConnection(anyString())).thenReturn(true);

        ConditionOutcome outcome = spyCondition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage())
                .contains("Ollama native connection detected at");
    }

    @Test
    void shouldNotMatchWhenOllamaNativeConnectionIsAvailableWithCustomBaseUrl() {
        environment.setProperty("spring.ai.ollama.base-url", "http://custom-host:8080");
        OnOllamaNativeUnavailable spyCondition = spy(new OnOllamaNativeUnavailable());
        when(spyCondition.isOllamaNativeConnection("http://custom-host:8080")).thenReturn(true);

        ConditionOutcome outcome = spyCondition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage())
                .contains("Ollama native connection detected at http://custom-host:8080");
    }

    @Test
    void shouldMatchWhenOllamaNativeConnectionIsNotAvailableWithCustomBaseUrl() {
        environment.setProperty("spring.ai.ollama.base-url", "http://custom-host:8080");
        OnOllamaNativeUnavailable spyCondition = spy(new OnOllamaNativeUnavailable());
        when(spyCondition.isOllamaNativeConnection("http://custom-host:8080")).thenReturn(false);

        ConditionOutcome outcome = spyCondition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage()).contains("Ollama native connection is not available");
    }

    @Test
    void shouldMatchWhenIgnoreNativeServiceIsTrue() {
        environment.setProperty("arconia.dev.services.ollama.ignore-native-service", "true");
        OnOllamaNativeUnavailable condition = new OnOllamaNativeUnavailable();

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage()).contains("Usage of Ollama native service is ignored");
        assertThat(outcome.getMessage()).contains("ignore-native-service");
    }

    @Test
    void shouldCheckConnectionWhenIgnoreNativeServiceIsFalse() {
        environment.setProperty("arconia.dev-services.ollama.ignore-native-service", "false");
        OnOllamaNativeUnavailable spyCondition = spy(new OnOllamaNativeUnavailable());
        when(spyCondition.isOllamaNativeConnection(anyString())).thenReturn(false);

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
