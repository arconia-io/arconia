package io.arconia.opentelemetry.autoconfigure.sdk;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Determines if OpenTelemetry SDK support should be enabled.
 */
class OnEnabledOpenTelemetryCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnOpenTelemetry.class.getName());
        boolean checkForEnabled = attributes != null && (boolean) attributes.get("enabled");

        String openTelemetryEnabledKey = OpenTelemetryProperties.CONFIG_PREFIX + ".enabled";
        Boolean openTelemetryEnabled = context.getEnvironment().getProperty(openTelemetryEnabledKey, Boolean.class);

        if (openTelemetryEnabled != null) {
            boolean enabled = checkForEnabled == openTelemetryEnabled;
            return new ConditionOutcome(enabled,
                    ConditionMessage.forCondition(ConditionalOnOpenTelemetry.class)
                            .because(openTelemetryEnabledKey + " is " + openTelemetryEnabled
                                    + " and annotation requested enabled to be " + checkForEnabled));
        }

        if (checkForEnabled) {
            return ConditionOutcome.match(ConditionMessage.forCondition(ConditionalOnOpenTelemetry.class)
                    .because("OpenTelemetry is enabled by default"));
        } else {
            return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnOpenTelemetry.class)
                    .because("OpenTelemetry is disabled because annotation requested enabled to be false"));
        }
    }

}
