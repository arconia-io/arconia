package io.arconia.opentelemetry.autoconfigure;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Determines if OpenTelemetry support should be enabled.
 */
class OnEnabledOpenTelemetryCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnOpenTelemetry.class.getName());
        boolean checkForEnabled = attributes != null && (boolean) attributes.get("enabled");

        String openTelemetryEnabledKey = OpenTelemetryProperties.CONFIG_PREFIX + ".enabled";
        boolean openTelemetryEnabled = context.getEnvironment().getProperty(openTelemetryEnabledKey, boolean.class, true);

        boolean enabled = checkForEnabled == openTelemetryEnabled;
        return new ConditionOutcome(enabled,
                ConditionMessage.forCondition(ConditionalOnOpenTelemetry.class)
                        .because(openTelemetryEnabledKey + " is " + openTelemetryEnabled
                                + " and annotation requested enabled to be " + checkForEnabled));
    }

}
