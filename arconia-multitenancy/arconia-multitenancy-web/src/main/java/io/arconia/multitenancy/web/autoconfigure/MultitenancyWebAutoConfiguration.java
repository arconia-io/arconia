package io.arconia.multitenancy.web.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;

import io.arconia.multitenancy.core.autoconfigure.MultitenancyCoreAutoConfiguration;

/**
 * Auto-configuration for web multitenancy.
 */
@AutoConfiguration(after = MultitenancyCoreAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import({ HttpTenantResolutionConfiguration.class, WebMvcConfiguration.class })
public final class MultitenancyWebAutoConfiguration {

    private MultitenancyWebAutoConfiguration() {}

}
