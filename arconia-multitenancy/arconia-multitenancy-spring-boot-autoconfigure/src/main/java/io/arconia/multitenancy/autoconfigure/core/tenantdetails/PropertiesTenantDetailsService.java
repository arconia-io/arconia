package io.arconia.multitenancy.autoconfigure.core.tenantdetails;

import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import io.arconia.multitenancy.core.tenantdetails.Tenant;
import io.arconia.multitenancy.core.tenantdetails.TenantDetails;
import io.arconia.multitenancy.core.tenantdetails.TenantDetailsService;

/**
 * An implementation of {@link TenantDetailsService} that uses application properties as
 * the source for the tenant details.
 */
public class PropertiesTenantDetailsService implements TenantDetailsService {

    private final TenantDetailsProperties tenantDetailsProperties;

    public PropertiesTenantDetailsService(TenantDetailsProperties tenantDetailsProperties) {
        this.tenantDetailsProperties = tenantDetailsProperties;
    }

    @Override
    public List<? extends TenantDetails> loadAllTenants() {
        return tenantDetailsProperties.getTenants().stream().map(this::toTenant).toList();
    }

    @Nullable
    @Override
    public TenantDetails loadTenantByIdentifier(String identifier) {
        Assert.hasText(identifier, "identifier cannot be null or empty");
        return tenantDetailsProperties.getTenants()
            .stream()
            .map(this::toTenant)
            .filter(tenant -> tenant.getIdentifier().equals(identifier))
            .findFirst()
            .orElse(null);
    }

    private Tenant toTenant(TenantDetailsProperties.TenantConfig tenantConfig) {
        return Tenant.create()
            .identifier(tenantConfig.getIdentifier())
            .enabled(tenantConfig.isEnabled())
            .attributes(tenantConfig.getAttributes())
            .build();
    }

}
