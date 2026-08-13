package io.arconia.multitenancy.core.autoconfigure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

/**
 * Configuration properties for tenant details.
 */
@ConfigurationProperties(prefix = TenantDetailsProperties.CONFIG_PREFIX)
public class TenantDetailsProperties {

    public static final String CONFIG_PREFIX = "arconia.multitenancy.details";

    /**
     * List of tenant details.
     */
    private final List<TenantConfig> tenants = new ArrayList<>();

    public List<TenantConfig> getTenants() {
        return tenants;
    }

    public static class TenantConfig {

        /**
         * Identifier for the tenant.
         */
        private String identifier;

        /**
         * Whether the tenant is enabled.
         */
        private boolean enabled = true;

        /**
         * Additional information about the tenant.
         */
        private Map<String, Object> attributes = Map.of();

        public String getIdentifier() {
            return identifier;
        }

        public void setIdentifier(String identifier) {
            Assert.hasText(identifier, "identifier cannot be null or empty");
            this.identifier = identifier;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Map<String, Object> getAttributes() {
            return attributes;
        }

        public void setAttributes(Map<String, Object> attributes) {
            Assert.notNull(attributes, "attributes cannot be null");
            this.attributes = attributes;
        }

    }

}
