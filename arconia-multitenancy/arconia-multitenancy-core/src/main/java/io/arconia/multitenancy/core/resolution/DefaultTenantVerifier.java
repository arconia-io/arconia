package io.arconia.multitenancy.core.resolution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import io.arconia.core.annotations.Incubating;
import io.arconia.multitenancy.core.tenantdetails.TenantDetailsService;

/**
 * Default {@link TenantVerifier} implementation that checks the tenant exists and is
 * enabled using a {@link TenantDetailsService}. The loaded tenant must carry exactly the
 * requested identifier.
 */
@Incubating
public final class DefaultTenantVerifier implements TenantVerifier {

    private static final Logger logger = LoggerFactory.getLogger(DefaultTenantVerifier.class);

    private final TenantDetailsService tenantDetailsService;

    public DefaultTenantVerifier(TenantDetailsService tenantDetailsService) {
        Assert.notNull(tenantDetailsService, "tenantDetailsService cannot be null");
        this.tenantDetailsService = tenantDetailsService;
    }

    @Override
    public void verify(String tenantIdentifier) {
        Assert.hasText(tenantIdentifier, "tenantIdentifier cannot be null or empty");
        logger.trace("Verifying tenant: {}", tenantIdentifier);
        var tenant = tenantDetailsService.loadTenantByIdentifier(tenantIdentifier);
        // Identifiers are compared exactly, so that a service matching case-insensitively,
        // such as one backed by a case-insensitive collation, cannot admit "ACME" as an
        // alias of "acme" and split one tenant across data sources and cache keys.
        if (tenant == null || !tenant.enabled() || !tenantIdentifier.equals(tenant.identifier())) {
            throw new TenantVerificationException("The resolved tenant is invalid or disabled");
        }
    }

}
