package io.arconia.core.multitenancy.cache;

import org.springframework.cache.interceptor.KeyGenerator;

/**
 * Cache key generator. Used for creating a tenant-aware key based on the given method
 * (used as context) and its parameters.
 *
 * @author Thomas Vitale
 */
@FunctionalInterface
public interface TenantKeyGenerator extends KeyGenerator {

}
