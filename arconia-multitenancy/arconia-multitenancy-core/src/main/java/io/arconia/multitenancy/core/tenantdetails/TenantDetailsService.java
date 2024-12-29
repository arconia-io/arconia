package io.arconia.multitenancy.core.tenantdetails;

import java.util.List;

import org.springframework.lang.Nullable;

/**
 * Loads tenant-specific data. It is used throughout the framework as a tenant DAO.
 */
public interface TenantDetailsService {

    List<? extends TenantDetails> loadAllTenants();

    @Nullable
    TenantDetails loadTenantByIdentifier(String identifier);

}
