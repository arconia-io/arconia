package io.arconia.opentelemetry.autoconfigure.sdk.logs.exporter;

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
 * Determines if a certain exporter type is enabled for OpenTelemetry logs.
 */
class OnOpenTelemetryLoggingExporterCondition extends SpringBootCondition {

    private static final String GENERAL_EXPORTER_TYPE = OpenTelemetryExporterProperties.CONFIG_PREFIX + ".type";
    private static final String LOGS_EXPORTER_TYPE = OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".type";

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnOpenTelemetryLoggingExporter.class.getName());
        String requestedExporterType = attributes != null ? (String) attributes.get("value") : null;

        if (!StringUtils.hasText(requestedExporterType)) {
            return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnOpenTelemetryLoggingExporter.class)
                    .because("a valid exporter type is not specified"));
        }

        String generalExporterTypeString = context.getEnvironment().getProperty(GENERAL_EXPORTER_TYPE, "otlp");
        ExporterType generalExporterType = StringUtils.hasText(generalExporterTypeString) ? ExporterType.valueOf(generalExporterTypeString.toUpperCase()) : null;

        String logsExporterTypeString = context.getEnvironment().getProperty(LOGS_EXPORTER_TYPE, String.class);
        ExporterType logsExporterType = StringUtils.hasText(logsExporterTypeString) ? ExporterType.valueOf(logsExporterTypeString.toUpperCase()) : null;

        if (logsExporterType != null) {
            if (logsExporterType.toString().equalsIgnoreCase(requestedExporterType)) {
                return ConditionOutcome.match(ConditionMessage.forCondition(ConditionalOnOpenTelemetryLoggingExporter.class)
                        .because(LOGS_EXPORTER_TYPE + " is set to " + logsExporterType));
            } else {
                return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnOpenTelemetryLoggingExporter.class)
                        .because(LOGS_EXPORTER_TYPE + " is set to " + logsExporterType + ", but requested " + requestedExporterType));
            }
        }

        if (generalExporterType != null) {
            if (generalExporterType.toString().equalsIgnoreCase(requestedExporterType)) {
                return ConditionOutcome.match(ConditionMessage.forCondition(ConditionalOnOpenTelemetryLoggingExporter.class)
                        .because(GENERAL_EXPORTER_TYPE + " is set to " + generalExporterType));
            } else {
                return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnOpenTelemetryLoggingExporter.class)
                        .because(GENERAL_EXPORTER_TYPE + " is set to " + generalExporterType + ", but requested " + requestedExporterType));
            }
        }

        return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnOpenTelemetryLoggingExporter.class)
                .because("exporter type not enabled: " + requestedExporterType));
    }

}
