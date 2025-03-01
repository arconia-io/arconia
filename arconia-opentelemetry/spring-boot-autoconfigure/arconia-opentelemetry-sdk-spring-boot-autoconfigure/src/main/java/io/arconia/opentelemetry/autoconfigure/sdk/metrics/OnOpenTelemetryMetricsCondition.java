package io.arconia.opentelemetry.autoconfigure.sdk.metrics;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Determines if OpenTelemetry metrics support should be enabled.
 */
class OnOpenTelemetryMetricsCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Boolean openTelemetryMetricsEnabled = context.getEnvironment().getProperty(OpenTelemetryMetricsProperties.CONFIG_PREFIX + ".enabled", Boolean.class);

        if (openTelemetryMetricsEnabled != null) {
            return new ConditionOutcome(openTelemetryMetricsEnabled,
                    ConditionMessage.forCondition(ConditionalOnOpenTelemetryMetrics.class)
                            .because(OpenTelemetryMetricsProperties.CONFIG_PREFIX + ".enabled is " + openTelemetryMetricsEnabled));
        }

        return ConditionOutcome.match(ConditionMessage.forCondition(ConditionalOnOpenTelemetryMetrics.class)
                .because("OpenTelemetry Metrics are enabled by default"));
    }

}
