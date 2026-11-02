package io.arconia.opentelemetry.autoconfigure.metrics.exporter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotationMetadata;
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
    void metricsExporterType() {
        environment.setProperty("arconia.otel.exporter.type", "otlp");
        environment.setProperty("arconia.otel.metrics.exporter.type", "console");

        ConditionOutcome outcome = outcome(ConsoleExporter.class);

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage()).contains("arconia.otel.metrics.exporter.type is set to CONSOLE");
    }

    @Test
    void commonExporterType() {
        environment.setProperty("arconia.otel.exporter.type", "console");

        ConditionOutcome outcome = outcome(ConsoleExporter.class);

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage()).contains("arconia.otel.exporter.type is set to CONSOLE");
    }

    private ConditionOutcome outcome(Class<?> annotatedClass) {
        when(context.getEnvironment()).thenReturn(environment);
        return condition.getMatchOutcome(context, AnnotationMetadata.introspect(annotatedClass));
    }

    @ConditionalOnOpenTelemetryMetricsExporter("console")
    static class ConsoleExporter {
    }

}
