package io.arconia.multitenancy.web.context.resolvers;

import java.util.Arrays;

import io.arconia.core.support.Incubating;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

/**
 * Strategy used to resolve the current tenant from a cookie in an HTTP request.
 */
@Incubating
public final class CookieTenantResolver implements HttpRequestTenantResolver {

    public static final String DEFAULT_COOKIE_NAME = "TENANT-ID";

    private final String tenantCookieName;

    public CookieTenantResolver() {
        this.tenantCookieName = DEFAULT_COOKIE_NAME;
    }

    public CookieTenantResolver(String tenantCookieName) {
        Assert.hasText(tenantCookieName, "tenantCookieName cannot be null or empty");
        this.tenantCookieName = tenantCookieName;
    }

    @Override
    @Nullable
    public String resolveTenantIdentifier(HttpServletRequest request) {
        Assert.notNull(request, "request cannot be null");
        return Arrays.stream(request.getCookies())
            .filter(cookie -> cookie.getName().equals(tenantCookieName))
            .map(Cookie::getValue)
            .findFirst()
            .orElse(null);
    }

}
