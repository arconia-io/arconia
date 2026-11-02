package io.arconia.opentelemetry.autoconfigure.exporter;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OnOpenTelemetryExporterCondition}.
 */
class OnOpenTelemetryExporterConditionTests {

    private static final String SIGNAL_TYPE = "arconia.otel.test.exporter.type";

    private static final String COMMON_TYPE = "arconia.otel.exporter.type";

    private final TestExporterCondition condition = new TestExporterCondition();

    private final MockEnvironment environment = new MockEnvironment();

    private final ConditionContext context = mock(ConditionContext.class);

    @Test
    void whenAnnotationTypeNullThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new OnOpenTelemetryExporterCondition(null, "arconia.otel.test.exporter") {})
                .withMessage("annotationType cannot be null");
    }

    @Test
    void whenSignalExporterPrefixEmptyThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new OnOpenTelemetryExporterCondition(ConditionalOnTestExporter.class, "") {})
                .withMessage("signalExporterPrefix cannot be null or empty");
    }

    @Test
    void otlpByDefault() {
        ConditionOutcome outcome = outcome(OtlpExporter.class);

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage()).contains(COMMON_TYPE + " is set to OTLP");
        assertThat(outcome(ConsoleExporter.class).isMatch()).isFalse();
    }

    @Test
    void signalExporterType() {
        environment.setProperty(SIGNAL_TYPE, "console");

        ConditionOutcome outcome = outcome(ConsoleExporter.class);

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage()).contains(SIGNAL_TYPE + " is set to CONSOLE");
    }

    @Test
    void commonExporterType() {
        environment.setProperty(COMMON_TYPE, "console");

        ConditionOutcome outcome = outcome(ConsoleExporter.class);

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage()).contains(COMMON_TYPE + " is set to CONSOLE");
    }

    @Test
    void signalExporterTypeTakesPrecedenceOverCommonExporterType() {
        environment.setProperty(COMMON_TYPE, "console");
        environment.setProperty(SIGNAL_TYPE, "otlp");

        assertThat(outcome(OtlpExporter.class).isMatch()).isTrue();
        assertThat(outcome(ConsoleExporter.class).isMatch()).isFalse();
    }

    @Test
    void differentExporterType() {
        environment.setProperty(SIGNAL_TYPE, "console");

        ConditionOutcome outcome = outcome(OtlpExporter.class);

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage()).contains(SIGNAL_TYPE + " is set to CONSOLE, but requested otlp");
    }

    @Test
    void exporterTypeBoundLikeConfigurationProperties() {
        environment.setProperty(SIGNAL_TYPE, "Console");

        assertThat(outcome(ConsoleExporter.class).isMatch()).isTrue();
    }

    @Test
    void requestedExporterTypeMatchedCaseInsensitively() {
        environment.setProperty(SIGNAL_TYPE, "console");

        assertThat(outcome(UppercaseConsoleExporter.class).isMatch()).isTrue();
    }

    @Test
    void emptySignalExporterTypeFallsBackToCommonExporterType() {
        environment.setProperty(COMMON_TYPE, "console");
        environment.setProperty(SIGNAL_TYPE, "");

        assertThat(outcome(ConsoleExporter.class).isMatch()).isTrue();
    }

    @Test
    void emptyCommonExporterTypeDisablesExporters() {
        environment.setProperty(COMMON_TYPE, "");

        ConditionOutcome outcome = outcome(OtlpExporter.class);

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage()).contains(COMMON_TYPE + " is empty");
    }

    @Test
    void invalidExporterTypeReportsProperty() {
        environment.setProperty(SIGNAL_TYPE, "zipkin");

        assertThatExceptionOfType(BindException.class)
                .isThrownBy(() -> outcome(OtlpExporter.class))
                .withMessageContaining(SIGNAL_TYPE);
    }

    @Test
    void noMatchWhenRequestedExporterTypeIsBlank() {
        ConditionOutcome outcome = outcome(BlankExporter.class);

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage()).contains("a valid exporter type is not specified");
    }

    @Test
    void noMatchWhenAnnotationIsMissing() {
        ConditionOutcome outcome = outcome(NotAnnotated.class);

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage()).contains("a valid exporter type is not specified");
    }

    private ConditionOutcome outcome(Class<?> annotatedClass) {
        when(context.getEnvironment()).thenReturn(environment);
        return condition.getMatchOutcome(context, AnnotationMetadata.introspect(annotatedClass));
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface ConditionalOnTestExporter {

        String value();

    }

    static class TestExporterCondition extends OnOpenTelemetryExporterCondition {

        TestExporterCondition() {
            super(ConditionalOnTestExporter.class, "arconia.otel.test.exporter");
        }

    }

    @ConditionalOnTestExporter("otlp")
    static class OtlpExporter {
    }

    @ConditionalOnTestExporter("console")
    static class ConsoleExporter {
    }

    @ConditionalOnTestExporter("CONSOLE")
    static class UppercaseConsoleExporter {
    }

    @ConditionalOnTestExporter("  ")
    static class BlankExporter {
    }

    static class NotAnnotated {
    }

}
