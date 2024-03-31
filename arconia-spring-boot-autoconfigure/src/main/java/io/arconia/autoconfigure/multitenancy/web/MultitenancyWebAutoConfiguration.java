package io.arconia.autoconfigure.multitenancy.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;

import io.arconia.autoconfigure.multitenancy.core.MultitenancyCoreAutoConfiguration;

/**
 * Auto-configuration for web multitenancy.
 */
@AutoConfiguration(after = MultitenancyCoreAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import({ HttpTenantResolutionConfiguration.class, WebMvcConfiguration.class })
public class MultitenancyWebAutoConfiguration {

}
