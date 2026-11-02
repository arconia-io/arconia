package io.arconia.multitenancy.web.resolution;

import java.util.Collections;

import jakarta.servlet.http.HttpServletRequest;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import io.arconia.core.annotations.Incubating;
import io.arconia.multitenancy.core.resolution.InvalidTenantIdentifierException;

/**
 * Strategy used to resolve the current tenant from a header in an HTTP request.
 */
@Incubating
public final class HeaderTenantResolver implements HttpRequestTenantResolver {

    private static final String DEFAULT_HEADER_NAME = "X-TenantId";

    private final String headerName;

    private HeaderTenantResolver(String headerName) {
        Assert.hasText(headerName, "headerName cannot be null or empty");
        this.headerName = headerName;
    }

    /**
     * Name of the HTTP header from which the current tenant is resolved.
     */
    public String getHeaderName() {
        return headerName;
    }

    @Override
    @Nullable
    public String resolveTenantIdentifier(HttpServletRequest request) {
        Assert.notNull(request, "request cannot be null");
        var headers = request.getHeaders(headerName);
        if (headers == null) {
            return null;
        }
        var values = Collections.list(headers);
        if (values.size() > 1) {
            throw new InvalidTenantIdentifierException(
                    "The request must not contain more than one '%s' header".formatted(headerName));
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

        private String headerName = DEFAULT_HEADER_NAME;

        private Builder() {}

        /**
         * Name of the HTTP header from which to resolve the current tenant.
         */
        public Builder headerName(String headerName) {
            this.headerName = headerName;
            return this;
        }

        /**
         * Builds the {@link HeaderTenantResolver} instance.
         */
        public HeaderTenantResolver build() {
            return new HeaderTenantResolver(headerName);
        }

    }

}
