package io.arconia.opentelemetry.autoconfigure.sdk.logs;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OnOpenTelemetryLoggingCondition}.
 */
class OnOpenTelemetryLoggingConditionTests {

    private final OnOpenTelemetryLoggingCondition condition = new OnOpenTelemetryLoggingCondition();

    private final MockEnvironment environment = new MockEnvironment();

    private final ConditionContext context = mock(ConditionContext.class);

    private final AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);

    @Test
    void shouldMatchWhenLoggingIsEnabled() {
        environment.setProperty("arconia.otel.logs.enabled", "true");
        when(context.getEnvironment()).thenReturn(environment);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage())
                .contains("arconia.otel.logs.enabled is true");
    }

    @Test
    void shouldNotMatchWhenLoggingIsDisabled() {
        environment.setProperty("arconia.otel.logs.enabled", "false");
        when(context.getEnvironment()).thenReturn(environment);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage())
                .contains("arconia.otel.logs.enabled is false");
    }

    @Test
    void shouldMatchByDefaultWhenPropertyIsNotSet() {
        when(context.getEnvironment()).thenReturn(environment);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage())
                .contains("OpenTelemetry Logs are enabled by default");
    }

}
