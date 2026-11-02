package io.arconia.opentelemetry.autoconfigure.logs.exporter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OnOpenTelemetryLoggingExporterCondition}.
 */
class OnOpenTelemetryLoggingExporterConditionTests {

    private final OnOpenTelemetryLoggingExporterCondition condition = new OnOpenTelemetryLoggingExporterCondition();

    private final MockEnvironment environment = new MockEnvironment();

    private final ConditionContext context = mock(ConditionContext.class);

    @Test
    void logsExporterType() {
        environment.setProperty("arconia.otel.exporter.type", "otlp");
        environment.setProperty("arconia.otel.logs.exporter.type", "console");

        ConditionOutcome outcome = outcome(ConsoleExporter.class);

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage()).contains("arconia.otel.logs.exporter.type is set to CONSOLE");
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

    @ConditionalOnOpenTelemetryLoggingExporter("console")
    static class ConsoleExporter {
    }

}
