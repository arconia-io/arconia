package io.arconia.multitenancy.web.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;

import io.arconia.multitenancy.core.autoconfigure.MultitenancyAutoConfiguration;

/**
 * Auto-configuration for web multitenancy.
 */
@AutoConfiguration(after = MultitenancyAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import({ HttpTenantResolutionConfiguration.class, WebMvcConfiguration.class })
public final class MultitenancyWebAutoConfiguration {

    private MultitenancyWebAutoConfiguration() {}

}
