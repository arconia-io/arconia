package io.arconia.autoconfigure.web.multitenancy;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.arconia.web.multitenancy.context.resolvers.CookieTenantResolver;
import io.arconia.web.multitenancy.context.resolvers.HeaderTenantResolver;

/**
 * Configuration properties for HTTP tenant resolution.
 *
 * @author Thomas Vitale
 */
@ConfigurationProperties(prefix = HttpTenantResolutionProperties.CONFIG_PREFIX)
public class HttpTenantResolutionProperties {

    public static final String CONFIG_PREFIX = "arconia.multitenancy.resolution.http";

    /**
     * Whether an HTTP tenant resolution strategy should be used.
     */
    private boolean enabled = true;

    /**
     * The HTTP type of resolution.
     */
    private HttpResolutionMode type = HttpResolutionMode.HEADER;

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

    public HttpResolutionMode getType() {
        return type;
    }

    public void setType(HttpResolutionMode type) {
        this.type = type;
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
         * The name of the HTTP header from which to resolve the current tenant.
         */
        private String headerName = HeaderTenantResolver.DEFAULT_HEADER_NAME;

        public String getHeaderName() {
            return headerName;
        }

        public void setHeaderName(String headerName) {
            this.headerName = headerName;
        }

    }

    public static class Cookie {

        /**
         * The name of the HTTP cookie from which to resolve the current tenant.
         */
        private String cookieName = CookieTenantResolver.DEFAULT_COOKIE_NAME;

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
         * A list of HTTP request paths for which the tenant resolution will not be
         * performed.
         */
        private List<String> ignorePaths = List.of("/actuator/**", "/webjars/**", "/css/**", "/js/**", ".ico");

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getIgnorePaths() {
            return ignorePaths;
        }

        public void setIgnorePaths(List<String> ignorePaths) {
            this.ignorePaths = ignorePaths;
        }

    }

    public enum HttpResolutionMode {

        COOKIE, HEADER

    }

}
