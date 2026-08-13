package io.arconia.multitenancy.core.autoconfigure;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;
import io.arconia.multitenancy.core.tenantdetails.Tenant;
import io.arconia.multitenancy.core.tenantdetails.TenantDetails;
import io.arconia.multitenancy.core.tenantdetails.TenantDetailsService;

/**
 * An implementation of {@link TenantDetailsService} that uses application properties as
 * the source for the tenant details.
 */
@Incubating
public final class PropertiesTenantDetailsService implements TenantDetailsService {

    private final TenantDetailsProperties tenantDetailsProperties;

    public PropertiesTenantDetailsService(TenantDetailsProperties tenantDetailsProperties) {
        Assert.notNull(tenantDetailsProperties, "tenantDetailsProperties cannot be null");
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
            .filter(tenantConfig -> identifier.equals(tenantConfig.getIdentifier()))
            .findFirst()
            .map(this::toTenant)
            .orElse(null);
    }

    private Tenant toTenant(TenantDetailsProperties.TenantConfig tenantConfig) {
        return Tenant.builder()
            .identifier(tenantConfig.getIdentifier())
            .enabled(tenantConfig.isEnabled())
            .attributes(tenantConfig.getAttributes())
            .build();
    }

}
