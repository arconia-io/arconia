package io.arconia.opentelemetry.autoconfigure.sdk;

import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OnEnabledOpenTelemetryCondition}.
 */
class OnEnabledOpenTelemetryConditionTests {

    private final OnEnabledOpenTelemetryCondition condition = new OnEnabledOpenTelemetryCondition();

    @Test
    void matchWhenPropertyTrueAndAnnotationTrue() {
        ConditionOutcome outcome = getMatchOutcome(true, true);
        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage()).contains("arconia.otel.enabled is true and annotation requested enabled to be true");
    }

    @Test
    void noMatchWhenPropertyTrueAndAnnotationFalse() {
        ConditionOutcome outcome = getMatchOutcome(true, false);
        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage()).contains("arconia.otel.enabled is true and annotation requested enabled to be false");
    }

    @Test
    void noMatchWhenPropertyFalseAndAnnotationTrue() {
        ConditionOutcome outcome = getMatchOutcome(false, true);
        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage()).contains("arconia.otel.enabled is false and annotation requested enabled to be true");
    }

    @Test
    void matchWhenPropertyFalseAndAnnotationFalse() {
        ConditionOutcome outcome = getMatchOutcome(false, false);
        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage()).contains("arconia.otel.enabled is false and annotation requested enabled to be false");
    }

    @Test
    void matchWhenPropertyNotSetAndAnnotationTrue() {
        ConditionOutcome outcome = getMatchOutcome(null, true);
        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage()).contains("OpenTelemetry is enabled by default");
    }

    @Test
    void noMatchWhenPropertyNotSetAndAnnotationFalse() {
        ConditionOutcome outcome = getMatchOutcome(null, false);
        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage()).contains("OpenTelemetry is disabled because annotation requested enabled to be false");
    }

    private ConditionOutcome getMatchOutcome(@Nullable Boolean propertyValue, boolean annotationValue) {
        ConditionContext context = mock(ConditionContext.class);
        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        MockEnvironment environment = new MockEnvironment();

        if (propertyValue != null) {
            environment.setProperty("arconia.otel.enabled", propertyValue.toString());
        }

        when(context.getEnvironment()).thenReturn(environment);
        when(metadata.getAnnotationAttributes(ConditionalOnOpenTelemetry.class.getName()))
                .thenReturn(Map.of("enabled", annotationValue));

        return condition.getMatchOutcome(context, metadata);
    }

}
