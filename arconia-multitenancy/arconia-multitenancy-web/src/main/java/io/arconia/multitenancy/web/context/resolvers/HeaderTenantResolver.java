package io.arconia.multitenancy.web.context.resolvers;

import io.arconia.core.support.Incubating;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

/**
 * Strategy used to resolve the current tenant from a header in an HTTP request.
 */
@Incubating
public final class HeaderTenantResolver implements HttpRequestTenantResolver {

    public static final String DEFAULT_HEADER_NAME = "X-TenantId";

    private final String tenantHeaderName;

    public HeaderTenantResolver() {
        this.tenantHeaderName = DEFAULT_HEADER_NAME;
    }

    public HeaderTenantResolver(String tenantHeaderName) {
        Assert.hasText(tenantHeaderName, "tenantHeaderName cannot be null or empty");
        this.tenantHeaderName = tenantHeaderName;
    }

    @Override
    @Nullable
    public String resolveTenantIdentifier(HttpServletRequest request) {
        Assert.notNull(request, "request cannot be null");
        return request.getHeader(tenantHeaderName);
    }

}
