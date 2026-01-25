package io.arconia.dev.services.core.autoconfigure;

import java.util.HashMap;
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
 * Unit tests for {@link OnDevServicesEnabledCondition}.
 */
class OnDevServicesEnabledConditionTests {

    private final OnDevServicesEnabledCondition condition = new OnDevServicesEnabledCondition();

    private final MockEnvironment environment = new MockEnvironment();

    private final ConditionContext context = mock(ConditionContext.class);

    @Test
    void shouldMatchWhenGloballyEnabledAndSpecificDevServiceEnabled() {
        environment.setProperty("arconia.dev.services.enabled", "true");
        environment.setProperty("arconia.dev.services.test-service.enabled", "true");
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", "test-service");
        when(metadata.getAnnotationAttributes(ConditionalOnDevServicesEnabled.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage())
                .contains("arconia.dev.services.test-service.enabled is set to true");
    }

    @Test
    void shouldNotMatchWhenGloballyEnabledButSpecificDevServiceDisabled() {
        environment.setProperty("arconia.dev.services.enabled", "true");
        environment.setProperty("arconia.dev.services.test-service.enabled", "false");
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", "test-service");
        when(metadata.getAnnotationAttributes(ConditionalOnDevServicesEnabled.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage())
                .contains("arconia.dev.services.test-service.enabled is set to false");
    }

    @Test
    void shouldNotMatchWhenGloballyDisabledAndSpecificDevServiceEnabled() {
        environment.setProperty("arconia.dev.services.enabled", "false");
        environment.setProperty("arconia.dev.services.test-service.enabled", "true");
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", "test-service");
        when(metadata.getAnnotationAttributes(ConditionalOnDevServicesEnabled.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage())
                .contains("arconia.dev.services.enabled is set to false");
    }

    @Test
    void shouldNotMatchWhenGloballyDisabledAndSpecificDevServiceDisabled() {
        environment.setProperty("arconia.dev.services.enabled", "false");
        environment.setProperty("arconia.dev.services.test-service.enabled", "false");
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", "test-service");
        when(metadata.getAnnotationAttributes(ConditionalOnDevServicesEnabled.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage())
                .contains("arconia.dev.services.enabled is set to false");
    }

    @Test
    void shouldMatchByDefaultWhenPropertiesAreNotSet() {
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", "test-service");
        when(metadata.getAnnotationAttributes(ConditionalOnDevServicesEnabled.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage())
                .contains("enabled by default (arconia.dev.services.test-service.enabled is not set)");
    }

    @Test
    void shouldNotMatchWhenDevServicesNameIsEmpty() {
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", "");
        when(metadata.getAnnotationAttributes(ConditionalOnDevServicesEnabled.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage())
                .contains("a valid dev services name is not specified");
    }

    @Test
    void shouldNotMatchWhenDevServicesNameIsBlank() {
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", "   ");
        when(metadata.getAnnotationAttributes(ConditionalOnDevServicesEnabled.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage())
                .contains("a valid dev services name is not specified");
    }

    @Test
    void shouldNotMatchWhenDevServicesNameIsNull() {
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", null);
        when(metadata.getAnnotationAttributes(ConditionalOnDevServicesEnabled.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage())
                .contains("a valid dev services name is not specified");
    }

    @Test
    void shouldNotMatchWhenAnnotationAttributesAreNull() {
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        when(metadata.getAnnotationAttributes(ConditionalOnDevServicesEnabled.class.getName()))
                .thenReturn(null);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage())
                .contains("a valid dev services name is not specified");
    }

    @Test
    void shouldMatchWhenOnlyGlobalPropertyIsSetToTrue() {
        environment.setProperty("arconia.dev.services.enabled", "true");
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", "test-service");
        when(metadata.getAnnotationAttributes(ConditionalOnDevServicesEnabled.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage())
                .contains("enabled by default (arconia.dev.services.test-service.enabled is not set)");
    }

    @Test
    void shouldMatchWhenOnlyGlobalPropertyIsSetToFalse() {
        environment.setProperty("arconia.dev.services.enabled", "false");
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", "test-service");
        when(metadata.getAnnotationAttributes(ConditionalOnDevServicesEnabled.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage())
                .contains("arconia.dev.services.enabled is set to false");
    }

    @Test
    void shouldMatchWhenOnlySpecificPropertyIsSetToTrue() {
        environment.setProperty("arconia.dev.services.test-service.enabled", "true");
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", "test-service");
        when(metadata.getAnnotationAttributes(ConditionalOnDevServicesEnabled.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage())
                .contains("arconia.dev.services.test-service.enabled is set to true");
    }

}
