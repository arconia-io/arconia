package io.arconia.multitenancy.core.autoconfigure;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Determines if any tenant is configured via the application configuration.
 */
class OnTenantsConfiguredCondition extends SpringBootCondition {

    private static final String TENANTS_PROPERTY = TenantDetailsProperties.CONFIG_PREFIX + ".tenants";

    private static final Bindable<List<TenantDetailsProperties.TenantConfig>> TENANT_LIST =
            Bindable.listOf(TenantDetailsProperties.TenantConfig.class);

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        ConditionMessage.Builder message = ConditionMessage.forCondition("Tenants Configured Condition");
        List<TenantDetailsProperties.TenantConfig> tenants = getTenants(context.getEnvironment());
        if (!tenants.isEmpty()) {
            return ConditionOutcome.match(message.foundExactly("configured tenants " + tenants.stream()
                .map(TenantDetailsProperties.TenantConfig::getIdentifier)
                .collect(Collectors.joining(", "))));
        }
        return ConditionOutcome.noMatch(message.notAvailable("configured tenants"));
    }

    private List<TenantDetailsProperties.TenantConfig> getTenants(Environment environment) {
        // Bind via the Binder so that invalid values fail loudly instead of being
        // silently interpreted as no tenant being configured.
        return Binder.get(environment).bind(TENANTS_PROPERTY, TENANT_LIST).orElse(List.of());
    }

}
