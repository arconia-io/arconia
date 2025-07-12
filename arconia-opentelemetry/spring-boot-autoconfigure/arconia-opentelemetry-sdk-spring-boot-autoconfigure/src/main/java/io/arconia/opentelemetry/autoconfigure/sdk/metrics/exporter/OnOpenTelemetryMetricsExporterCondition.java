package io.arconia.opentelemetry.autoconfigure.sdk.metrics.exporter;

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
 * Determines if a certain exporter type is enabled for OpenTelemetry metrics.
 */
class OnOpenTelemetryMetricsExporterCondition extends SpringBootCondition {

    private static final String GENERAL_EXPORTER_TYPE = OpenTelemetryExporterProperties.CONFIG_PREFIX + ".type";
    private static final String METRICS_EXPORTER_TYPE = OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".type";

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnOpenTelemetryMetricsExporter.class.getName());
        String requestedExporterType = attributes != null ? (String) attributes.get("value") : null;

        if (!StringUtils.hasText(requestedExporterType)) {
            return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnOpenTelemetryMetricsExporter.class)
                    .because("a valid exporter type is not specified"));
        }

        String generalExporterTypeString = context.getEnvironment().getProperty(GENERAL_EXPORTER_TYPE, "otlp");
        ExporterType generalExporterType = StringUtils.hasText(generalExporterTypeString) ? ExporterType.valueOf(generalExporterTypeString.toUpperCase()) : null;

        String metricsExporterTypeString = context.getEnvironment().getProperty(METRICS_EXPORTER_TYPE, String.class);
        ExporterType metricsExporterType = StringUtils.hasText(metricsExporterTypeString) ? ExporterType.valueOf(metricsExporterTypeString.toUpperCase()) : null;

        if (metricsExporterType != null) {
            if (metricsExporterType.toString().equalsIgnoreCase(requestedExporterType)) {
                return ConditionOutcome.match(ConditionMessage.forCondition(ConditionalOnOpenTelemetryMetricsExporter.class)
                        .because(METRICS_EXPORTER_TYPE + " is set to " + metricsExporterType));
            } else {
                return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnOpenTelemetryMetricsExporter.class)
                        .because(METRICS_EXPORTER_TYPE + " is set to " + metricsExporterType + ", but requested " + requestedExporterType));
            }
        }

        if (generalExporterType != null) {
            if (generalExporterType.toString().equalsIgnoreCase(requestedExporterType)) {
                return ConditionOutcome.match(ConditionMessage.forCondition(ConditionalOnOpenTelemetryMetricsExporter.class)
                        .because(GENERAL_EXPORTER_TYPE + " is set to " + generalExporterType));
            } else {
                return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnOpenTelemetryMetricsExporter.class)
                        .because(GENERAL_EXPORTER_TYPE + " is set to " + generalExporterType + ", but requested " + requestedExporterType));
            }
        }

        return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnOpenTelemetryMetricsExporter.class)
                .because("exporter type not enabled: " + requestedExporterType));
    }

}
