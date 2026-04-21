package io.arconia.multitenancy.core.tenantdetails;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Loads tenant-specific data. It is used throughout the framework as a tenant DAO.
 */
public interface TenantDetailsService {

    /**
     * Loads all tenants.
     */
    List<? extends TenantDetails> loadAllTenants();

    /**
     * Loads a tenant by identifier.
     */
    @Nullable
    TenantDetails loadTenantByIdentifier(String identifier);

}
