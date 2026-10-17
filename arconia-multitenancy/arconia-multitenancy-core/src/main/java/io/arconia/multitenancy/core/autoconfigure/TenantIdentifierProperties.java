package io.arconia.multitenancy.core.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the tenant identifier policy.
 */
@ConfigurationProperties(prefix = TenantIdentifierProperties.CONFIG_PREFIX)
public class TenantIdentifierProperties {

    public static final String CONFIG_PREFIX = "arconia.multitenancy.identifier";

    /**
     * Maximum number of characters accepted in a tenant identifier.
     */
    private int maxLength = 64;

    /**
     * Regular expression a tenant identifier must match in full. The default accepts
     * alphanumeric characters, dashes, and underscores, which are safe to use in logs,
     * cache keys, and schema or database names.
     */
    private String pattern = "[a-zA-Z0-9_-]+";

    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

}
