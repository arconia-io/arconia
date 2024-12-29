package io.arconia.multitenancy.core.cache;

import org.springframework.cache.interceptor.KeyGenerator;

/**
 * Cache key generator. Used for creating a tenant-aware key based on the given method
 * (used as context) and its parameters.
 */
@FunctionalInterface
public interface TenantKeyGenerator extends KeyGenerator {

}
