package io.arconia.multitenancy.web.autoconfigure;

import java.util.Set;

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
     * Mode of HTTP resolution.
     */
    private HttpResolutionMode resolutionMode = HttpResolutionMode.HEADER;

    /**
     * Configuration for HTTP header tenant resolution.
     */
    private final Header header = new Header();

    /**
     * Configuration for HTTP cookie tenant resolution.
     */
    private final Cookie cookie = new Cookie();

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

    public HttpResolutionMode getResolutionMode() {
        return resolutionMode;
    }

    public void setResolutionMode(HttpResolutionMode resolutionMode) {
        this.resolutionMode = resolutionMode;
    }

    public Header getHeader() {
        return header;
    }

    public Cookie getCookie() {
        return cookie;
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
         * the tenant from an OAuth2 token.
         */
        private int order = Ordered.LOWEST_PRECEDENCE;

        /**
         * Comma-separated list of HTTP request paths for which the tenant resolution will
         * not be performed.
         */
        private Set<String> ignorePaths = Set.of("/actuator/**", "/webjars/**", "/css/**", "/js/**", "/**/*.ico",
                "/login", "/oauth2/authorization/**", "/login/oauth2/code/**");

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

    public enum HttpResolutionMode {

        COOKIE,
        HEADER,
        OAUTH2;

    }

}
