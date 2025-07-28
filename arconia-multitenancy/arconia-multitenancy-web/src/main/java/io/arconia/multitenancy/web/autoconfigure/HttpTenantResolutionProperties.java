package io.arconia.multitenancy.web.autoconfigure;

import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

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

    public static class Filter {

        /**
         * Whether the HTTP filter resolving the current tenant is enabled.
         */
        private boolean enabled = true;

        /**
         * Comma-separated list of HTTP request paths for which the tenant resolution will
         * not be performed.
         */
        private Set<String> ignorePaths = Set.of("/actuator/**", "/webjars/**", "/css/**", "/js/**", ".ico");

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
        HEADER;

    }

}
