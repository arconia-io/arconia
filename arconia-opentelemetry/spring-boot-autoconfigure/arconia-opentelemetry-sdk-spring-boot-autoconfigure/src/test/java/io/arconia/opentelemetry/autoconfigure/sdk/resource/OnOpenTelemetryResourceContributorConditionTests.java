package io.arconia.opentelemetry.autoconfigure.sdk.resource;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.mock.env.MockEnvironment;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OnOpenTelemetryResourceContributorCondition}.
 */
class OnOpenTelemetryResourceContributorConditionTests {

    private final OnOpenTelemetryResourceContributorCondition condition = new OnOpenTelemetryResourceContributorCondition();

    private final MockEnvironment environment = new MockEnvironment();

    private final ConditionContext context = mock(ConditionContext.class);

    @Test
    void shouldMatchWhenContributorIsEnabled() {
        environment.setProperty("arconia.otel.resource.contributors.test-contributor.enabled", "true");
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", "test-contributor");
        attributes.put("matchIfMissing", false);
        when(metadata.getAnnotationAttributes(ConditionalOnOpenTelemetryResourceContributor.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage())
                .contains("arconia.otel.resource.contributors.test-contributor.enabled is true");
    }

    @Test
    void shouldNotMatchWhenContributorIsDisabled() {
        environment.setProperty("arconia.otel.resource.contributors.test-contributor.enabled", "false");
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", "test-contributor");
        attributes.put("matchIfMissing", false);
        when(metadata.getAnnotationAttributes(ConditionalOnOpenTelemetryResourceContributor.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage())
                .contains("arconia.otel.resource.contributors.test-contributor.enabled is false");
    }

    @Test
    void shouldNotMatchByDefaultWhenPropertyIsNotSet() {
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", "test-contributor");
        attributes.put("matchIfMissing", false);
        when(metadata.getAnnotationAttributes(ConditionalOnOpenTelemetryResourceContributor.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage())
                .contains("resource contributor is disabled by default");
    }

    @Test
    void shouldNotMatchByDefaultWhenContributorNameIsEmpty() {
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", "");
        attributes.put("matchIfMissing", false);
        when(metadata.getAnnotationAttributes(ConditionalOnOpenTelemetryResourceContributor.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage())
                .contains("resource contributor is disabled by default");
    }

    @Test
    void shouldNotMatchByDefaultWhenContributorNameIsBlank() {
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", "   ");
        attributes.put("matchIfMissing", false);
        when(metadata.getAnnotationAttributes(ConditionalOnOpenTelemetryResourceContributor.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage())
                .contains("resource contributor is disabled by default");
    }

    @Test
    void shouldNotMatchByDefaultWhenAnnotationAttributesAreNull() {
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        when(metadata.getAnnotationAttributes(ConditionalOnOpenTelemetryResourceContributor.class.getName()))
                .thenReturn(null);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage())
                .contains("resource contributor is disabled by default");
    }

    @Test
    void shouldNotMatchByDefaultWhenContributorNameIsNull() {
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", null);
        attributes.put("matchIfMissing", false);
        when(metadata.getAnnotationAttributes(ConditionalOnOpenTelemetryResourceContributor.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage())
                .contains("resource contributor is disabled by default");
    }

    @Test
    void shouldMatchByDefaultWhenPropertyIsNotSetAndMatchIfMissingIsTrue() {
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", "test-contributor");
        attributes.put("matchIfMissing", true);
        when(metadata.getAnnotationAttributes(ConditionalOnOpenTelemetryResourceContributor.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage())
                .contains("resource contributor is enabled by default");
    }

    @Test
    void shouldNotMatchByDefaultWhenPropertyIsNotSetAndMatchIfMissingIsFalse() {
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", "test-contributor");
        attributes.put("matchIfMissing", false);
        when(metadata.getAnnotationAttributes(ConditionalOnOpenTelemetryResourceContributor.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage())
                .contains("resource contributor is disabled by default");
    }

}
