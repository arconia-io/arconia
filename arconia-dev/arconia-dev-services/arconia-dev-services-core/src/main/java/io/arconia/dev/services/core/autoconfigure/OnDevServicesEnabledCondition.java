package io.arconia.dev.services.core.autoconfigure;

import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import io.arconia.boot.bootstrap.BootstrapMode;

/**
 * Determines if a certain dev service is enabled.
 *
 * @see ConditionalOnDevServicesEnabled
 */
class OnDevServicesEnabledCondition extends SpringBootCondition {

    private static final String GLOBAL_PROPERTY = DevServicesProperties.CONFIG_PREFIX + ".enabled";

    private static final String DEV_SERVICES_PROPERTY = "%s.%s.enabled";

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnDevServicesEnabled.class.getName());
        String devServicesName = getAttribute(attributes, "name", "value");
        String customPrefix = getAttribute(attributes, "prefix");

        Assert.state(!StringUtils.hasText(customPrefix) || StringUtils.hasText(devServicesName),
                "@ConditionalOnDevServicesEnabled requires a name when a custom prefix is specified");

        if (BootstrapMode.detect() == BootstrapMode.PROD) {
            return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnDevServicesEnabled.class)
                    .because("dev services are only available in dev and test mode, but the application is running in prod mode"));
        }

        // Bind boolean properties via the Binder so that invalid values fail loudly
        // instead of being silently interpreted as false.
        Binder binder = Binder.get(context.getEnvironment());

        boolean areDevServicesGloballyEnabled = binder.bind(GLOBAL_PROPERTY, Boolean.class).orElse(true);

        if (!areDevServicesGloballyEnabled) {
            return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnDevServicesEnabled.class)
                    .because(GLOBAL_PROPERTY + " is set to false"));
        }

        if (!StringUtils.hasText(devServicesName)) {
            return ConditionOutcome.match(ConditionMessage.forCondition(ConditionalOnDevServicesEnabled.class)
                    .because("dev services are globally enabled and no specific dev services name is specified"));
        }

        String prefix = StringUtils.hasText(customPrefix) ? customPrefix : DevServicesProperties.CONFIG_PREFIX;
        String enabledProperty = DEV_SERVICES_PROPERTY.formatted(prefix, devServicesName);

        BindResult<Boolean> devServiceEnabled = binder.bind(enabledProperty, Boolean.class);

        if (!devServiceEnabled.isBound()) {
            return ConditionOutcome.match(ConditionMessage.forCondition(ConditionalOnDevServicesEnabled.class)
                    .because("enabled by default (" + enabledProperty + " is not set)"));
        }

        if (devServiceEnabled.get()) {
            return ConditionOutcome.match(ConditionMessage.forCondition(ConditionalOnDevServicesEnabled.class)
                    .because(enabledProperty + " is set to true"));
        }

        return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnDevServicesEnabled.class)
                .because(enabledProperty + " is set to false"));

    }

    /**
     * Get the first non-blank attribute value among the given attribute names.
     * The name attributes are aliased, but blank fallbacks are still needed when
     * the attributes come from sources that don't resolve aliases.
     */
    @Nullable
    private static String getAttribute(@Nullable Map<String, Object> attributes, String... names) {
        if (attributes == null) {
            return null;
        }
        for (String name : names) {
            String value = (String) attributes.get(name);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

}
