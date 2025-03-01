package io.arconia.opentelemetry.autoconfigure.sdk.traces;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Determines if OpenTelemetry tracing support should be enabled.
 */
class OnOpenTelemetryTracingCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Boolean openTelemetryTracesEnabled = context.getEnvironment().getProperty(OpenTelemetryTracingProperties.CONFIG_PREFIX + ".enabled", Boolean.class);

        if (openTelemetryTracesEnabled != null) {
            return new ConditionOutcome(openTelemetryTracesEnabled,
                    ConditionMessage.forCondition(ConditionalOnOpenTelemetryTracing.class)
                            .because(OpenTelemetryTracingProperties.CONFIG_PREFIX + ".enabled is " + openTelemetryTracesEnabled));
        }

        return ConditionOutcome.match(ConditionMessage.forCondition(ConditionalOnOpenTelemetryTracing.class)
                .because("OpenTelemetry tracing is enabled by default"));
    }

}
