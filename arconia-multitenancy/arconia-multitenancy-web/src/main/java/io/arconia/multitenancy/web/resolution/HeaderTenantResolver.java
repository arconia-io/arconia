package io.arconia.multitenancy.web.resolution;

import java.util.Collections;

import jakarta.servlet.http.HttpServletRequest;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;
import io.arconia.multitenancy.core.resolution.InvalidTenantIdentifierException;

/**
 * Strategy used to resolve the current tenant from a header in an HTTP request.
 */
@Incubating
public final class HeaderTenantResolver implements HttpRequestTenantResolver {

    private static final String DEFAULT_HEADER_NAME = "X-TenantId";

    private final String tenantHeaderName;

    private HeaderTenantResolver(String tenantHeaderName) {
        Assert.hasText(tenantHeaderName, "tenantHeaderName cannot be null or empty");
        this.tenantHeaderName = tenantHeaderName;
    }

    /**
     * Name of the HTTP header from which the current tenant is resolved.
     */
    public String getTenantHeaderName() {
        return tenantHeaderName;
    }

    @Override
    @Nullable
    public String resolveTenantIdentifier(HttpServletRequest request) {
        Assert.notNull(request, "request cannot be null");
        var headers = request.getHeaders(tenantHeaderName);
        if (headers == null) {
            return null;
        }
        var values = Collections.list(headers);
        if (values.size() > 1) {
            throw new InvalidTenantIdentifierException(
                    "The request must not contain more than one '%s' header".formatted(tenantHeaderName));
        }
        return values.isEmpty() ? null : values.getFirst();
    }

    /**
     * Creates a new builder for {@link HeaderTenantResolver}.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String tenantHeaderName = DEFAULT_HEADER_NAME;

        private Builder() {}

        /**
         * Name of the HTTP header from which to resolve the current tenant.
         */
        public Builder tenantHeaderName(String tenantHeaderName) {
            this.tenantHeaderName = tenantHeaderName;
            return this;
        }

        /**
         * Builds the {@link HeaderTenantResolver} instance.
         */
        public HeaderTenantResolver build() {
            return new HeaderTenantResolver(tenantHeaderName);
        }

    }

}
