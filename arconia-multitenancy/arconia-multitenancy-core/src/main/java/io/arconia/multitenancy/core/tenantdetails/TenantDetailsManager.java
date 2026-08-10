package io.arconia.multitenancy.core.tenantdetails;

import java.util.Map;

/**
 * handle creation and storage of {@link TenantDetails}
 */
public interface TenantDetailsManager
 extends TenantDetailsService
{
    TenantDetails createTenant(String identifier,
                               boolean enable,
                               Map<String, Object> attributes);

}
