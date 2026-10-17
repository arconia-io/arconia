package io.arconia.multitenancy.web.resolution;

import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;

import org.jspecify.annotations.Nullable;
import org.springframework.http.server.RequestPath;
import org.springframework.util.Assert;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import io.arconia.core.support.Incubating;

/**
 * Strategy used to resolve the current tenant from the path of an HTTP request.
 * <p>
 * The path within the application is matched against a pattern containing a
 * {@code {tenant}} variable, such as {@code /tenants/{tenant}/**}. A request to
 * {@code /tenants/acme/products} resolves the tenant {@code acme} with that pattern.
 * Requests whose path does not match the pattern resolve no tenant. The resolver only
 * extracts the identifier: it never rewrites the path, so handler mappings must still
 * account for the tenant segment.
 * <p>
 * Patterns are matched against the path within the application, so they keep working
 * when the application is deployed under a context path.
 */
@Incubating
public final class PathTenantResolver implements HttpRequestTenantResolver {

    /**
     * Name of the pattern variable that captures the tenant identifier.
     */
    public static final String TENANT_VARIABLE = "tenant";

    private static final Pattern TENANT_VARIABLE_PATTERN = Pattern.compile("\\{" + TENANT_VARIABLE + "(:[^}]*)?}");

    private final String pattern;

    private final PathPattern pathPattern;

    private PathTenantResolver(String pattern) {
        Assert.hasText(pattern, "pattern cannot be null or empty");
        Assert.isTrue(TENANT_VARIABLE_PATTERN.matcher(pattern).find(),
                "pattern must contain a {" + TENANT_VARIABLE + "} variable");
        this.pattern = pattern;
        var parser = new PathPatternParser();
        this.pathPattern = parser.parse(parser.initFullPathPattern(pattern));
    }

    /**
     * Pattern matched against the request path to resolve the current tenant.
     */
    public String getPattern() {
        return pattern;
    }

    @Override
    @Nullable
    public String resolveTenantIdentifier(HttpServletRequest request) {
        Assert.notNull(request, "request cannot be null");
        var path = RequestPath.parse(request.getRequestURI(), request.getContextPath()).pathWithinApplication();
        var match = pathPattern.matchAndExtract(path);
        if (match == null) {
            return null;
        }
        return match.getUriVariables().get(TENANT_VARIABLE);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        @Nullable
        private String pattern;

        private Builder() {}

        /**
         * Pattern matched against the request path, containing a {@code {tenant}}
         * variable that captures the tenant identifier, such as
         * {@code /tenants/{tenant}/**}.
         */
        public Builder pattern(@Nullable String pattern) {
            this.pattern = pattern;
            return this;
        }

        /**
         * Builds the {@link PathTenantResolver} instance.
         */
        public PathTenantResolver build() {
            Assert.hasText(pattern, "pattern cannot be null or empty");
            return new PathTenantResolver(pattern);
        }

    }

}
