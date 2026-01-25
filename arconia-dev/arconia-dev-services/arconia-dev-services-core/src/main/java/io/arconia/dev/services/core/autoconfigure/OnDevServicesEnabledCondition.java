package io.arconia.dev.services.core.autoconfigure;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * Determines if a certain dev service is enabled.
 *
 * @see ConditionalOnDevServicesEnabled
 */
class OnDevServicesEnabledCondition extends SpringBootCondition {

    private static final String GLOBAL_PROPERTY = DevServicesProperties.CONFIG_PREFIX + ".enabled";

    private static final String DEV_SERVICES_PROPERTY = "arconia.dev.services.%s.enabled";

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnDevServicesEnabled.class.getName());
        String devServicesName = attributes != null ? (String) attributes.get("value") : null;

        if (!StringUtils.hasText(devServicesName)) {
            return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnDevServicesEnabled.class)
                    .because("a valid dev services name is not specified"));
        }

        var globalProperty = context.getEnvironment().getProperty(GLOBAL_PROPERTY);
        var devServiceProperty = context.getEnvironment().getProperty(DEV_SERVICES_PROPERTY.formatted(devServicesName));

        boolean areDevServicesGloballyDisabled = globalProperty != null && !Boolean.parseBoolean(globalProperty);
        boolean isSpecificDevServiceEnabled = Boolean.parseBoolean(devServiceProperty);

        if (areDevServicesGloballyDisabled) {
            return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnDevServicesEnabled.class)
                    .because(GLOBAL_PROPERTY + " is set to false"));
        }

        if (isSpecificDevServiceEnabled) {
            return ConditionOutcome.match(ConditionMessage.forCondition(ConditionalOnDevServicesEnabled.class)
                    .because(DEV_SERVICES_PROPERTY.formatted(devServicesName) + " is set to true"));
        }

        if (devServiceProperty == null) {
            return ConditionOutcome.match(ConditionMessage.forCondition(ConditionalOnDevServicesEnabled.class)
                    .because("enabled by default (" + DEV_SERVICES_PROPERTY.formatted(devServicesName) + " is not set)"));
        }

        return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnDevServicesEnabled.class)
                .because(DEV_SERVICES_PROPERTY.formatted(devServicesName) + " is set to false"));

    }

}
