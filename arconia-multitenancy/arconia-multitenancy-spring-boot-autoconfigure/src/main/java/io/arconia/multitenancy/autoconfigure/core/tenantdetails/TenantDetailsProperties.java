package io.arconia.multitenancy.autoconfigure.core.tenantdetails;

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
     * The source of tenant details.
     */
    private Source source = Source.NONE;

    /**
     * List of tenant details.
     */
    private List<TenantConfig> tenants = new ArrayList<>();

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public List<TenantConfig> getTenants() {
        return tenants;
    }

    public void setTenants(List<TenantConfig> tenants) {
        this.tenants = tenants;
    }

    public enum Source {

        NONE, PROPERTIES

    }

    public static class TenantConfig {

        private String identifier;

        private boolean enabled = true;

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
