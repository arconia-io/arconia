package io.arconia.autoconfigure.multitenancy.core.tenantdetails;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.arconia.core.multitenancy.tenantdetails.Tenant;

/**
 * Configuration properties for tenant details.
 */
@ConfigurationProperties(prefix = TenantDetailsProperties.CONFIG_PREFIX)
public class TenantDetailsProperties {

    public static final String CONFIG_PREFIX = "arconia.multitenancy.details";

    /**
     * The source of tenant details.
     */
    private Source source = Source.PROPERTIES;

    /**
     * List of tenant details.
     */
    private List<Tenant> tenants = new ArrayList<>();

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public List<Tenant> getTenants() {
        return tenants;
    }

    public void setTenants(List<Tenant> tenants) {
        this.tenants = tenants;
    }

    public enum Source {

        HTTP, JDBC, PROPERTIES

    }

}
