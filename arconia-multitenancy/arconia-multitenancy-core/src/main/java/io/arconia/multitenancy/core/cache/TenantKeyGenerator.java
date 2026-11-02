package io.arconia.multitenancy.core.cache;

import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKeyGenerator;

import io.arconia.core.annotations.Incubating;
import io.arconia.multitenancy.core.context.TenantContext;
import io.arconia.multitenancy.core.context.TenantNotFoundException;

/**
 * A {@link KeyGenerator} producing cache keys that are scoped to the current tenant, so
 * that entries cached for one tenant are never served to another. Keys combine the
 * current tenant identifier with the method parameters. The target object and the method
 * are ignored, so keys are unique only within a given cache name.
 * <p>
 * Generation fails when no tenant is bound to the current context. Cache data that is
 * genuinely tenant-independent belongs in a separate cache using the default key generator.
 */
@Incubating
public final class TenantKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, @Nullable Object... params) {
        String tenantIdentifier = TenantContext.getTenantIdentifier();
        if (tenantIdentifier == null) {
            throw new TenantNotFoundException("No tenant found in the current context. A tenant-aware cache key cannot be generated outside a tenant context, such as from a scheduled task or during application startup");
        }
        return SimpleKeyGenerator.generateKey(tenantIdentifier, params);
    }

}
