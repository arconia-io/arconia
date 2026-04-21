package io.arconia.multitenancy.core.tenantdetails;

import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;

/**
 * Default implementation of {@link TenantDetails}.
 */
@Incubating
public record Tenant(String identifier, boolean enabled, Map<String, Object> attributes) implements TenantDetails {

    public Tenant {
        Assert.hasText(identifier, "identifier cannot be null or empty");
        Assert.notNull(attributes, "attributes cannot be null");
        Assert.noNullElements(attributes.keySet(), "attributes keys cannot contain null values");
        attributes = Map.copyOf(attributes);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String identifier;

        private boolean enabled = true;

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
