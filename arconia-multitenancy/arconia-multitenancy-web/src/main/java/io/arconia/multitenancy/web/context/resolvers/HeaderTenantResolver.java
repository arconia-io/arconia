package io.arconia.multitenancy.web.context.resolvers;

import jakarta.servlet.http.HttpServletRequest;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;

/**
 * Strategy used to resolve the current tenant from a header in an HTTP request.
 */
@Incubating(since = "0.1.0")
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
