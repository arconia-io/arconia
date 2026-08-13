package io.arconia.multitenancy.core.cache;

import org.springframework.cache.interceptor.KeyGenerator;

import io.arconia.core.support.Incubating;

/**
 * Cache key generator producing keys that are scoped to the current tenant, so that
 * entries cached for one tenant are never served to another.
 */
@Incubating
@FunctionalInterface
public interface TenantKeyGenerator extends KeyGenerator {}
