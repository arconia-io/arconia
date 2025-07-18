package io.arconia.opentelemetry.autoconfigure.traces;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Determines if OpenTelemetry tracing support should be enabled.
 */
class OnOpenTelemetryTracingCondition extends SpringBootCondition {

    private static final String TRACING_ENABLED_PROPERTY = "management.tracing.enabled";

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Boolean openTelemetryTracesEnabled = context.getEnvironment().getProperty(OpenTelemetryTracingProperties.CONFIG_PREFIX + ".enabled", Boolean.class);
        Boolean tracingEnabled = context.getEnvironment().getProperty(TRACING_ENABLED_PROPERTY, Boolean.class);

        if (openTelemetryTracesEnabled != null && tracingEnabled != null) {
            return new ConditionOutcome(openTelemetryTracesEnabled && tracingEnabled,
                    ConditionMessage.forCondition(ConditionalOnOpenTelemetryTracing.class)
                            .because(OpenTelemetryTracingProperties.CONFIG_PREFIX + ".enabled is " + openTelemetryTracesEnabled)
                            .append(TRACING_ENABLED_PROPERTY + " is " + tracingEnabled));
        }

        return ConditionOutcome.match(ConditionMessage.forCondition(ConditionalOnOpenTelemetryTracing.class)
                .because("OpenTelemetry tracing is enabled by default"));
    }

}
