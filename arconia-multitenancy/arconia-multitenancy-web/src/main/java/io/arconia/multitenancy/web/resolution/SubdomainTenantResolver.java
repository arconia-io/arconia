package io.arconia.multitenancy.web.resolution;

import java.util.Locale;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;

import org.jspecify.annotations.Nullable;
import org.springframework.http.server.PathContainer;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import io.arconia.core.support.Incubating;

/**
 * Strategy used to resolve the current tenant from the subdomain of an HTTP request.
 * <p>
 * The host of the request is matched against a pattern containing a {@code {tenant}}
 * variable, such as {@code {tenant}.example.com} or {@code {tenant}-admin.example.com}.
 * A request to {@code acme.example.com} resolves the tenant {@code acme} with the first
 * pattern. Requests whose host does not match the pattern resolve no tenant. The variable
 * matches a single host label, so nested subdomains never match.
 * <p>
 * Host names are case-insensitive, so the host is lowercased before matching, and the
 * resolved tenant identifier is always lowercase.
 * <p>
 * The host is read from {@link HttpServletRequest#getServerName()}, so it honours
 * forwarded headers when the application is configured to process them.
 */
@Incubating
public final class SubdomainTenantResolver implements HttpRequestTenantResolver {

    /**
     * Name of the pattern variable that captures the tenant identifier.
     */
    public static final String TENANT_VARIABLE = "tenant";

    private static final Pattern TENANT_VARIABLE_PATTERN = Pattern.compile("\\{" + TENANT_VARIABLE + "(:[^}]*)?}");

    private static final PathContainer.Options HOST_OPTIONS = PathContainer.Options.create('.', false);

    private final String pattern;

    private final PathPattern hostPattern;

    private SubdomainTenantResolver(String pattern) {
        Assert.hasText(pattern, "pattern cannot be null or empty");
        Assert.isTrue(TENANT_VARIABLE_PATTERN.matcher(pattern).find(),
                "pattern must contain a {" + TENANT_VARIABLE + "} variable");
        this.pattern = pattern;
        var parser = new PathPatternParser();
        parser.setPathOptions(HOST_OPTIONS);
        parser.setCaseSensitive(false);
        this.hostPattern = parser.parse(pattern);
    }

    /**
     * Pattern matched against the request host to resolve the current tenant.
     */
    public String getPattern() {
        return pattern;
    }

    @Override
    @Nullable
    public String resolveTenantIdentifier(HttpServletRequest request) {
        Assert.notNull(request, "request cannot be null");
        var host = request.getServerName();
        if (!StringUtils.hasText(host)) {
            return null;
        }
        host = host.toLowerCase(Locale.ROOT);
        var match = hostPattern.matchAndExtract(PathContainer.parsePath(host, HOST_OPTIONS));
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
         * Pattern matched against the request host, containing a {@code {tenant}} variable
         * that captures the tenant identifier, such as {@code {tenant}.example.com}.
         */
        public Builder pattern(@Nullable String pattern) {
            this.pattern = pattern;
            return this;
        }

        /**
         * Builds the {@link SubdomainTenantResolver} instance.
         */
        public SubdomainTenantResolver build() {
            Assert.hasText(pattern, "pattern cannot be null or empty");
            return new SubdomainTenantResolver(pattern);
        }

    }

}
