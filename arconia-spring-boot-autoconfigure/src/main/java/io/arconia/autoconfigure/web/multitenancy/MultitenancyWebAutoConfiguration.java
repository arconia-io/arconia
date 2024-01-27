package io.arconia.autoconfigure.web.multitenancy;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;

import io.arconia.autoconfigure.core.multitenancy.MultitenancyCoreAutoConfiguration;

/**
 * Auto-configuration for web multitenancy.
 *
 * @author Thomas Vitale
 */
@AutoConfiguration(after = MultitenancyCoreAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import(HttpTenantResolutionConfiguration.class)
public class MultitenancyWebAutoConfiguration {

}
