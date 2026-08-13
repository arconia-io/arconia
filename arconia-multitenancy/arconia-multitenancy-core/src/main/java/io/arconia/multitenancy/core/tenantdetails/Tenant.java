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

    /**
     * Creates a new builder for {@link Tenant}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Tenant}.
     */
    public static final class Builder {

        private String identifier;

        private boolean enabled = true;

        private Map<String, Object> attributes = new HashMap<>();

        private Builder() {}

        /**
         * Identifier for the tenant.
         */
        public Builder identifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        /**
         * Whether the tenant is enabled.
         */
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        /**
         * Additional information about the tenant.
         */
        public Builder attributes(Map<String, Object> attributes) {
            Assert.notNull(attributes, "attributes cannot be null");
            this.attributes = new HashMap<>(attributes);
            return this;
        }

        /**
         * Adds a single attribute to the additional information about the tenant.
         */
        public Builder addAttribute(String key, Object value) {
            attributes.put(key, value);
            return this;
        }

        /**
         * Builds the {@link Tenant} instance.
         */
        public Tenant build() {
            return new Tenant(identifier, enabled, attributes);
        }

    }

}
