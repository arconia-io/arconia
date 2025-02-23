package io.arconia.opentelemetry.autoconfigure.sdk.logs;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Determines if OpenTelemetry logging support should be enabled.
 */
class OnEnabledOpenTelemetryLoggingCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Boolean openTelemetryLogsEnabled = context.getEnvironment().getProperty(OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".enabled", Boolean.class);

        if (openTelemetryLogsEnabled != null) {
            return new ConditionOutcome(openTelemetryLogsEnabled,
                    ConditionMessage.forCondition(ConditionalOnEnabledOpenTelemetryLogging.class)
                            .because(OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".enabled is " + openTelemetryLogsEnabled));
        }

        return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnEnabledOpenTelemetryLogging.class)
                .because("OpenTelemetry Logs are disabled by default"));
    }

}
