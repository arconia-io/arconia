package io.arconia.opentelemetry.autoconfigure.metrics.exporter;

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
 * Unit tests for {@link OnOpenTelemetryMetricsExporterCondition}.
 */
class OnOpenTelemetryMetricsExporterConditionTests {

    private final OnOpenTelemetryMetricsExporterCondition condition = new OnOpenTelemetryMetricsExporterCondition();

    private final MockEnvironment environment = new MockEnvironment();

    private final ConditionContext context = mock(ConditionContext.class);

    @Test
    void shouldMatchWhenMetricsExporterTypeMatches() {
        environment.setProperty("arconia.otel.metrics.exporter.type", "console");
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", "console");
        when(metadata.getAnnotationAttributes(ConditionalOnOpenTelemetryMetricsExporter.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage())
                .contains("arconia.otel.metrics.exporter.type is set to CONSOLE");
    }

    @Test
    void shouldMatchWhenGeneralExporterTypeMatches() {
        environment.setProperty("arconia.otel.exporter.type", "otlp");
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", "otlp");
        when(metadata.getAnnotationAttributes(ConditionalOnOpenTelemetryMetricsExporter.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage())
                .contains("arconia.otel.exporter.type is set to OTLP");
    }

    @Test
    void shouldMatchOtlpByDefault() {
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", "otlp");
        when(metadata.getAnnotationAttributes(ConditionalOnOpenTelemetryMetricsExporter.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage())
                .contains("arconia.otel.exporter.type is set to OTLP");
    }

    @Test
    void shouldNotMatchWhenExporterTypeDoesNotMatch() {
        environment.setProperty("arconia.otel.metrics.exporter.type", "console");
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", "none");
        when(metadata.getAnnotationAttributes(ConditionalOnOpenTelemetryMetricsExporter.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage())
                .contains("arconia.otel.metrics.exporter.type is set to CONSOLE, but requested none");
    }

    @Test
    void shouldNotMatchWhenExporterTypeIsEmpty() {
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", "");
        when(metadata.getAnnotationAttributes(ConditionalOnOpenTelemetryMetricsExporter.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage())
                .contains("a valid exporter type is not specified");
    }

    @Test
    void shouldNotMatchWhenExporterTypeIsBlank() {
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", "   ");
        when(metadata.getAnnotationAttributes(ConditionalOnOpenTelemetryMetricsExporter.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage())
                .contains("a valid exporter type is not specified");
    }

    @Test
    void shouldNotMatchWhenExporterTypeIsNull() {
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", null);
        when(metadata.getAnnotationAttributes(ConditionalOnOpenTelemetryMetricsExporter.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage())
                .contains("a valid exporter type is not specified");
    }

    @Test
    void shouldNotMatchWhenAnnotationAttributesAreNull() {
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        when(metadata.getAnnotationAttributes(ConditionalOnOpenTelemetryMetricsExporter.class.getName()))
                .thenReturn(null);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage())
                .contains("a valid exporter type is not specified");
    }

    @Test
    void shouldPrioritizeMetricsExporterTypeOverGeneralExporterType() {
        environment.setProperty("arconia.otel.exporter.type", "console");
        environment.setProperty("arconia.otel.metrics.exporter.type", "otlp");
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", "otlp");
        when(metadata.getAnnotationAttributes(ConditionalOnOpenTelemetryMetricsExporter.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage())
                .contains("arconia.otel.metrics.exporter.type is set to OTLP");
    }

    @Test
    void shouldMatchCaseInsensitively() {
        environment.setProperty("arconia.otel.metrics.exporter.type", "console");
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", "Console");
        when(metadata.getAnnotationAttributes(ConditionalOnOpenTelemetryMetricsExporter.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage())
                .contains("arconia.otel.metrics.exporter.type is set to CONSOLE");
    }

    @Test
    void shouldMatchOtlpCaseInsensitively() {
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", "OTLP");
        when(metadata.getAnnotationAttributes(ConditionalOnOpenTelemetryMetricsExporter.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage())
                .contains("arconia.otel.exporter.type is set to OTLP");
    }

}
