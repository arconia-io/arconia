package io.arconia.multitenancy.core.autoconfigure;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

/**
 * Configuration properties for tenant details.
 */
@ConfigurationProperties(prefix = TenantDetailsProperties.CONFIG_PREFIX)
public class TenantDetailsProperties implements InitializingBean {

    public static final String CONFIG_PREFIX = "arconia.multitenancy.details";

    /**
     * List of tenant details.
     */
    private final List<TenantConfig> tenants = new ArrayList<>();

    /**
     * Caching of the tenant details looked up to verify each request's tenant.
     */
    private final Cache cache = new Cache();

    public List<TenantConfig> getTenants() {
        return tenants;
    }

    public Cache getCache() {
        return cache;
    }

    @Override
    public void afterPropertiesSet() {
        validate();
    }

    public void validate() {
        for (int i = 0; i < tenants.size(); i++) {
            if (tenants.get(i).getIdentifier() == null) {
                throw new IllegalStateException("The '%s.tenants[%d].identifier' property must be set".formatted(CONFIG_PREFIX, i));
            }
        }
    }

    public static class Cache {

        /**
         * Time after which a cached tenant is loaded again. Setting it enables caching,
         * which requires Caffeine on the classpath. A change to a tenant, such as
         * disabling it, takes effect once its entry expires.
         */
        @Nullable
        private Duration timeToLive;

        /**
         * Maximum number of tenants held in the cache.
         */
        private long maxSize = 1000;

        @Nullable
        public Duration getTimeToLive() {
            return timeToLive;
        }

        public void setTimeToLive(@Nullable Duration timeToLive) {
            this.timeToLive = timeToLive;
        }

        public long getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(long maxSize) {
            this.maxSize = maxSize;
        }

    }

    public static class TenantConfig {

        /**
         * Identifier for the tenant.
         */
        @Nullable
        private String identifier;

        /**
         * Whether the tenant is enabled.
         */
        private boolean enabled = true;

        /**
         * Additional information about the tenant.
         */
        private Map<String, Object> attributes = Map.of();

        @Nullable
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
