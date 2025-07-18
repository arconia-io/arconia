package io.arconia.opentelemetry.autoconfigure.metrics;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OnOpenTelemetryMetricsCondition}.
 */
class OnOpenTelemetryMetricsConditionTests {

    private final OnOpenTelemetryMetricsCondition condition = new OnOpenTelemetryMetricsCondition();

    private final MockEnvironment environment = new MockEnvironment();

    private final ConditionContext context = mock(ConditionContext.class);

    private final AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);

    @Test
    void shouldMatchWhenMetricsIsEnabled() {
        environment.setProperty("arconia.otel.metrics.enabled", "true");
        when(context.getEnvironment()).thenReturn(environment);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage())
                .contains("arconia.otel.metrics.enabled is true");
    }

    @Test
    void shouldNotMatchWhenMetricsIsDisabled() {
        environment.setProperty("arconia.otel.metrics.enabled", "false");
        when(context.getEnvironment()).thenReturn(environment);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage())
                .contains("arconia.otel.metrics.enabled is false");
    }

    @Test
    void shouldMatchByDefaultWhenPropertyIsNotSet() {
        when(context.getEnvironment()).thenReturn(environment);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage())
                .contains("OpenTelemetry Metrics are enabled by default");
    }

}
