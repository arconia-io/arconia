package io.arconia.opentelemetry.autoconfigure.sdk.traces;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OnOpenTelemetryTracingCondition}.
 */
class OnOpenTelemetryTracingConditionTests {

    private final OnOpenTelemetryTracingCondition condition = new OnOpenTelemetryTracingCondition();

    private final MockEnvironment environment = new MockEnvironment();

    private final ConditionContext context = mock(ConditionContext.class);

    private final AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);

    @Test
    void shouldMatchWhenBothPropertiesAreEnabled() {
        environment.setProperty("arconia.otel.traces.enabled", "true");
        environment.setProperty("management.tracing.enabled", "true");
        when(context.getEnvironment()).thenReturn(environment);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage())
                .contains("arconia.otel.traces.enabled is true")
                .contains("management.tracing.enabled is true");
    }

    @Test
    void shouldNotMatchWhenArconiaTracingIsDisabled() {
        environment.setProperty("arconia.otel.traces.enabled", "false");
        environment.setProperty("management.tracing.enabled", "true");
        when(context.getEnvironment()).thenReturn(environment);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage())
                .contains("arconia.otel.traces.enabled is false")
                .contains("management.tracing.enabled is true");
    }

    @Test
    void shouldNotMatchWhenManagementTracingIsDisabled() {
        environment.setProperty("arconia.otel.traces.enabled", "true");
        environment.setProperty("management.tracing.enabled", "false");
        when(context.getEnvironment()).thenReturn(environment);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage())
                .contains("arconia.otel.traces.enabled is true")
                .contains("management.tracing.enabled is false");
    }

    @Test
    void shouldMatchByDefaultWhenNoPropertiesAreSet() {
        when(context.getEnvironment()).thenReturn(environment);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage())
                .contains("OpenTelemetry tracing is enabled by default");
    }

    @Test
    void shouldMatchByDefaultWhenOnlyArconiaTracingIsSet() {
        environment.setProperty("arconia.otel.traces.enabled", "true");
        when(context.getEnvironment()).thenReturn(environment);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage())
                .contains("OpenTelemetry tracing is enabled by default");
    }

    @Test
    void shouldMatchByDefaultWhenOnlyManagementTracingIsSet() {
        environment.setProperty("management.tracing.enabled", "true");
        when(context.getEnvironment()).thenReturn(environment);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage())
                .contains("OpenTelemetry tracing is enabled by default");
    }

}
