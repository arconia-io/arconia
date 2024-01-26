package io.arconia.web.multitenancy.context.resolvers;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Strategy used to resolve the current tenant from a header in an HTTP request.
 *
 * @author Thomas Vitale
 */
public final class HeaderTenantResolver implements HttpRequestTenantResolver {

    public static final String DEFAULT_HEADER_NAME = "X-TenantId";

    private final String tenantHeaderName;

    public HeaderTenantResolver() {
        this.tenantHeaderName = DEFAULT_HEADER_NAME;
    }

    public HeaderTenantResolver(String tenantHeaderName) {
        Assert.hasText(tenantHeaderName, "tenantHeaderName cannot be empty");
        this.tenantHeaderName = tenantHeaderName;
    }

    @Override
    @Nullable
    public String resolveTenantId(HttpServletRequest request) {
        Assert.notNull(request, "request cannot be null");
        return request.getHeader(tenantHeaderName);
    }

    /**
     * The name of the HTTP Header containing the tenant identifier.
     */
    public String getTenantHeaderName() {
        return tenantHeaderName;
    }

}
