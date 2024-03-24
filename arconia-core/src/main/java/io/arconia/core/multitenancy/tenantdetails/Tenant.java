package io.arconia.core.multitenancy.tenantdetails;

import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * Default implementation to hold tenant details.
 */
public class Tenant implements TenantDetails {

    private final String identifier;

    private final boolean enabled;

    private final Map<String, Object> attributes = new HashMap<>();

    public Tenant(String identifier, boolean enabled, Map<String, Object> attributes) {
        Assert.hasText(identifier, "identifier cannot be null or empty");
        Assert.notNull(attributes, "attributes cannot be null");

        this.identifier = identifier;
        this.enabled = enabled;
        this.attributes = attributes;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static Builder create() {
        return new Builder();
    }

    public static class Builder {

        private String identifier;

        private boolean enabled;

        private Map<String, Object> attributes = new HashMap<>();

        public Builder identifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder attributes(Map<String, Object> attributes) {
            this.attributes = attributes;
            return this;
        }

        public Builder addAttribute(String key, Object value) {
            attributes.put(key, value);
            return this;
        }

        public Tenant build() {
            return new Tenant(identifier, enabled, attributes);
        }

    }

}
