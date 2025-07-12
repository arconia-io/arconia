package io.arconia.opentelemetry.autoconfigure.sdk.traces.exporter;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

import io.arconia.opentelemetry.autoconfigure.sdk.exporter.ExporterType;
import io.arconia.opentelemetry.autoconfigure.sdk.exporter.OpenTelemetryExporterProperties;

/**
 * Determines if a certain exporter type is enabled for OpenTelemetry traces.
 */
class OnOpenTelemetryTracingExporterCondition extends SpringBootCondition {

    private static final String GENERAL_EXPORTER_TYPE = OpenTelemetryExporterProperties.CONFIG_PREFIX + ".type";
    private static final String TRACES_EXPORTER_TYPE = OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".type";

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnOpenTelemetryTracingExporter.class.getName());
        String requestedExporterType = attributes != null ? (String) attributes.get("value") : null;

        if (!StringUtils.hasText(requestedExporterType)) {
            return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnOpenTelemetryTracingExporter.class)
                    .because("a valid exporter type is not specified"));
        }

        String generalExporterTypeString = context.getEnvironment().getProperty(GENERAL_EXPORTER_TYPE, "otlp");
        ExporterType generalExporterType = StringUtils.hasText(generalExporterTypeString) ? ExporterType.valueOf(generalExporterTypeString.toUpperCase()) : null;

        String tracesExporterTypeString = context.getEnvironment().getProperty(TRACES_EXPORTER_TYPE, String.class);
        ExporterType tracesExporterType = StringUtils.hasText(tracesExporterTypeString) ? ExporterType.valueOf(tracesExporterTypeString.toUpperCase()) : null;

        if (tracesExporterType != null) {
            if (tracesExporterType.toString().equalsIgnoreCase(requestedExporterType)) {
                return ConditionOutcome.match(ConditionMessage.forCondition(ConditionalOnOpenTelemetryTracingExporter.class)
                        .because(TRACES_EXPORTER_TYPE + " is set to " + tracesExporterType));
            } else {
                return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnOpenTelemetryTracingExporter.class)
                        .because(TRACES_EXPORTER_TYPE + " is set to " + tracesExporterType + ", but requested " + requestedExporterType));
            }
        }

        if (generalExporterType != null) {
            if (generalExporterType.toString().equalsIgnoreCase(requestedExporterType)) {
                return ConditionOutcome.match(ConditionMessage.forCondition(ConditionalOnOpenTelemetryTracingExporter.class)
                        .because(GENERAL_EXPORTER_TYPE + " is set to " + generalExporterType));
            } else {
                return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnOpenTelemetryTracingExporter.class)
                        .because(GENERAL_EXPORTER_TYPE + " is set to " + generalExporterType + ", but requested " + requestedExporterType));
            }
        }

        return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnOpenTelemetryTracingExporter.class)
                .because("exporter type not enabled: " + requestedExporterType));

    }

}
