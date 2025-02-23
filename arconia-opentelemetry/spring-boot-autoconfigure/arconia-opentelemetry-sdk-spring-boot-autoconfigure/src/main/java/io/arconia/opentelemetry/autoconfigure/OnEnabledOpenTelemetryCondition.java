package io.arconia.opentelemetry.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Determines if OpenTelemetry support should be enabled.
 */
public class OnEnabledOpenTelemetryCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Boolean openTelemetryEnabled = context.getEnvironment().getProperty(OpenTelemetryProperties.CONFIG_PREFIX + ".enabled", Boolean.class);

        if (openTelemetryEnabled != null) {
            return new ConditionOutcome(openTelemetryEnabled,
                    ConditionMessage.forCondition(ConditionalOnEnabledOpenTelemetry.class)
                            .because(OpenTelemetryProperties.CONFIG_PREFIX + ".enabled is " + openTelemetryEnabled));
        }

        return ConditionOutcome.match(ConditionMessage.forCondition(ConditionalOnEnabledOpenTelemetry.class)
                .because("OpenTelemetry is enabled by default"));
    }

}
