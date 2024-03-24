package io.arconia.core.multitenancy.cache;

import java.lang.reflect.Method;

import org.springframework.cache.interceptor.SimpleKeyGenerator;

import io.arconia.core.multitenancy.context.TenantContextHolder;

/**
 * An implementation of {@link TenantKeyGenerator} that generates cache keys combining the
 * current tenant identifier with the given method and parameters.
 */
public final class DefaultTenantKeyGenerator implements TenantKeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        return SimpleKeyGenerator.generateKey(TenantContextHolder.getRequiredTenantIdentifier(), params);
    }

}
