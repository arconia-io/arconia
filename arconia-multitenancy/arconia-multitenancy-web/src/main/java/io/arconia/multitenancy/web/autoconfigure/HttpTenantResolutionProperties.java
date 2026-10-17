package io.arconia.multitenancy.web.autoconfigure;

import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;

/**
 * Configuration properties for HTTP tenant resolution.
 */
@ConfigurationProperties(prefix = HttpTenantResolutionProperties.CONFIG_PREFIX)
public class HttpTenantResolutionProperties {

    public static final String CONFIG_PREFIX = "arconia.multitenancy.resolution.http";

    /**
     * Whether an HTTP tenant resolution strategy should be used.
     */
    private boolean enabled = true;

    /**
     * Modes of HTTP resolution, consulted in order until one resolves a tenant. The
     * "fixed" mode always resolves a tenant, so it can only be the last one.
     */
    private List<HttpResolutionMode> resolutionMode = List.of(HttpResolutionMode.HEADER);

    /**
     * Configuration for HTTP header tenant resolution.
     */
    private final Header header = new Header();

    /**
     * Configuration for HTTP cookie tenant resolution.
     */
    private final Cookie cookie = new Cookie();

    /**
     * Configuration for HTTP path tenant resolution.
     */
    private final Path path = new Path();

    /**
     * Configuration for HTTP subdomain tenant resolution.
     */
    private final Subdomain subdomain = new Subdomain();

    /**
     * Configuration for OAuth2 tenant resolution.
     */
    private final OAuth2 oauth2 = new OAuth2();

    /**
     * Configuration for HTTP filter resolving the current tenant.
     */
    private final Filter filter = new Filter();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<HttpResolutionMode> getResolutionMode() {
        return resolutionMode;
    }

    public void setResolutionMode(List<HttpResolutionMode> resolutionMode) {
        this.resolutionMode = resolutionMode;
    }

    public Header getHeader() {
        return header;
    }

    public Cookie getCookie() {
        return cookie;
    }

    public Path getPath() {
        return path;
    }

    public Subdomain getSubdomain() {
        return subdomain;
    }

    public OAuth2 getOauth2() {
        return oauth2;
    }

    public Filter getFilter() {
        return filter;
    }

    public static class Header {

        /**
         * Name of the HTTP header from which to resolve the current tenant.
         */
        private String headerName = "X-TenantId";

        public String getHeaderName() {
            return headerName;
        }

        public void setHeaderName(String headerName) {
            this.headerName = headerName;
        }

    }

    public static class Cookie {

        /**
         * Name of the HTTP cookie from which to resolve the current tenant.
         */
        private String cookieName = "TENANT-ID";

        public String getCookieName() {
            return cookieName;
        }

        public void setCookieName(String cookieName) {
            this.cookieName = cookieName;
        }

    }

    public static class Path {

        /**
         * Pattern matched against the request path, containing a "{tenant}" variable that
         * captures the tenant identifier, such as "/tenants/{tenant}/**" for tenants served
         * at "/tenants/acme/...". Required when the "path" resolution mode is used.
         */
        @Nullable
        private String pattern;

        @Nullable
        public String getPattern() {
            return pattern;
        }

        public void setPattern(@Nullable String pattern) {
            this.pattern = pattern;
        }

    }

    public static class Subdomain {

        /**
         * Pattern matched against the request host, containing a "{tenant}" variable that
         * captures the tenant identifier, such as "{tenant}.example.com" for tenants served
         * at "acme.example.com". Required when the "subdomain" resolution mode is used.
         */
        @Nullable
        private String pattern;

        @Nullable
        public String getPattern() {
            return pattern;
        }

        public void setPattern(@Nullable String pattern) {
            this.pattern = pattern;
        }

    }

    public static class OAuth2 {

        /**
         * Name of the OAuth2 token claim from which to resolve the current tenant.
         */
        private String claimName = "tenant_id";

        public String getClaimName() {
            return claimName;
        }

        public void setClaimName(String claimName) {
            this.claimName = claimName;
        }

    }

    public static class Filter {

        /**
         * Whether the HTTP filter resolving the current tenant is enabled.
         */
        private boolean enabled = true;

        /**
         * Order of the HTTP filter resolving the current tenant. By default, the filter
         * runs after the Spring Security filter chain, which is required when resolving
         * the tenant from an OAuth2 token and keeps unauthenticated callers from probing
         * which tenants exist.
         */
        private int order = Ordered.LOWEST_PRECEDENCE;

        /**
         * Comma-separated list of HTTP request paths for which the tenant resolution will
         * not be performed.
         */
        private Set<String> ignorePaths = Set.of("/actuator/**", "/livez", "/readyz", "/webjars/**", "/css/**", "/js/**",
                "/**/*.ico", "/login", "/oauth2/authorization/**", "/login/oauth2/code/**");

        /**
         * Additional comma-separated list of HTTP request paths for which the tenant
         * resolution will not be performed.
         */
        private Set<String> additionalIgnorePaths = Set.of();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getOrder() {
            return order;
        }

        public void setOrder(int order) {
            this.order = order;
        }

        public Set<String> getIgnorePaths() {
            return ignorePaths;
        }

        public void setIgnorePaths(Set<String> ignorePaths) {
            this.ignorePaths = ignorePaths;
        }

        public Set<String> getAdditionalIgnorePaths() {
            return additionalIgnorePaths;
        }

        public void setAdditionalIgnorePaths(Set<String> additionalIgnorePaths) {
            this.additionalIgnorePaths = additionalIgnorePaths;
        }

    }

    /**
     * Sources from which the current tenant can be resolved.
     */
    public enum HttpResolutionMode {

        /**
         * A cookie of the request.
         */
        COOKIE,

        /**
         * A fixed tenant identifier, regardless of the request. Always resolves a tenant,
         * so it acts as the default tenant when placed last in the chain.
         */
        FIXED,

        /**
         * A header of the request.
         */
        HEADER,

        /**
         * A claim of the OAuth2 token that authenticated the request.
         */
        OAUTH2,

        /**
         * A segment of the request path.
         */
        PATH,

        /**
         * A label of the request host.
         */
        SUBDOMAIN;

    }

}
