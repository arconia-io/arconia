package io.arconia.multitenancy.web.resolution;

import java.util.Arrays;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;
import io.arconia.multitenancy.core.resolution.InvalidTenantIdentifierException;

/**
 * Strategy used to resolve the current tenant from a cookie in an HTTP request.
 */
@Incubating
public final class CookieTenantResolver implements HttpRequestTenantResolver {

    private static final String DEFAULT_COOKIE_NAME = "TENANT-ID";

    private final String tenantCookieName;

    private CookieTenantResolver(String tenantCookieName) {
        Assert.hasText(tenantCookieName, "tenantCookieName cannot be null or empty");
        this.tenantCookieName = tenantCookieName;
    }

    /**
     * Name of the HTTP cookie from which the current tenant is resolved.
     */
    public String getTenantCookieName() {
        return tenantCookieName;
    }

    @Override
    @Nullable
    public String resolveTenantIdentifier(HttpServletRequest request) {
        Assert.notNull(request, "request cannot be null");
        var cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        var values = Arrays.stream(cookies)
            .filter(cookie -> cookie.getName().equals(tenantCookieName))
            .map(Cookie::getValue)
            .toList();
        if (values.size() > 1) {
            throw new InvalidTenantIdentifierException(
                    "The request must not contain more than one '%s' cookie".formatted(tenantCookieName));
        }
        return values.isEmpty() ? null : values.getFirst();
    }

    /**
     * Creates a new builder for {@link CookieTenantResolver}.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String tenantCookieName = DEFAULT_COOKIE_NAME;

        private Builder() {}

        /**
         * Name of the HTTP cookie from which to resolve the current tenant.
         */
        public Builder tenantCookieName(String tenantCookieName) {
            this.tenantCookieName = tenantCookieName;
            return this;
        }

        /**
         * Builds the {@link CookieTenantResolver} instance.
         */
        public CookieTenantResolver build() {
            return new CookieTenantResolver(tenantCookieName);
        }

    }

}
