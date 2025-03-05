package io.arconia.opentelemetry.autoconfigure.instrumentation;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OnOpenTelemetryInstrumentationCondition}.
 */
class OnOpenTelemetryInstrumentationConditionTests {

    private final OnOpenTelemetryInstrumentationCondition condition = new OnOpenTelemetryInstrumentationCondition();

    @Test
    void matchWhenSpecificInstrumentationEnabled() {
        ConditionOutcome outcome = getMatchOutcome("test-instrumentation", true, null);
        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage()).contains("arconia.otel.instrumentation.test-instrumentation.enabled is true");
    }

    @Test
    void noMatchWhenSpecificInstrumentationDisabled() {
        ConditionOutcome outcome = getMatchOutcome("test-instrumentation", false, null);
        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage()).contains("arconia.otel.instrumentation.test-instrumentation.enabled is false");
    }

    @Test
    void matchWhenGlobalInstrumentationEnabled() {
        ConditionOutcome outcome = getMatchOutcome(null, null, true);
        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage()).contains("arconia.otel.instrumentation.enabled is true");
    }

    @Test
    void noMatchWhenGlobalInstrumentationDisabled() {
        ConditionOutcome outcome = getMatchOutcome(null, null, false);
        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage()).contains("arconia.otel.instrumentation.enabled is false");
    }

    @Test
    void matchWhenNoPropertiesSet() {
        ConditionOutcome outcome = getMatchOutcome(null, null, null);
        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage()).contains("instrumentation is enabled by default");
    }

    @Test
    void specificInstrumentationOverridesGlobalSetting() {
        ConditionOutcome outcome = getMatchOutcome("test-instrumentation", false, true);
        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage()).contains("arconia.otel.instrumentation.test-instrumentation.enabled is false");
    }

    private ConditionOutcome getMatchOutcome(String instrumentationName, Boolean specificPropertyValue, Boolean globalPropertyValue) {
        ConditionContext context = mock(ConditionContext.class);
        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        MockEnvironment environment = new MockEnvironment();

        if (specificPropertyValue != null && instrumentationName != null) {
            environment.setProperty("arconia.otel.instrumentation." + instrumentationName + ".enabled", specificPropertyValue.toString());
        }

        if (globalPropertyValue != null) {
            environment.setProperty("arconia.otel.instrumentation.enabled", globalPropertyValue.toString());
        }

        when(context.getEnvironment()).thenReturn(environment);
        
        if (instrumentationName != null) {
            when(metadata.getAnnotationAttributes(ConditionalOnOpenTelemetryInstrumentation.class.getName()))
                    .thenReturn(Map.of("value", instrumentationName));
        } else {
            when(metadata.getAnnotationAttributes(ConditionalOnOpenTelemetryInstrumentation.class.getName()))
                    .thenReturn(Map.of("value", ""));
        }

        return condition.getMatchOutcome(context, metadata);
    }

}
