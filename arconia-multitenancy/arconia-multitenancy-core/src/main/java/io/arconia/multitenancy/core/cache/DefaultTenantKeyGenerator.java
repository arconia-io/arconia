package io.arconia.multitenancy.core.cache;

import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;
import org.springframework.cache.interceptor.SimpleKeyGenerator;

import io.arconia.core.support.Incubating;
import io.arconia.multitenancy.core.context.TenantContext;

/**
 * An implementation of {@link TenantKeyGenerator} that generates cache keys combining the
 * current tenant identifier with the given method and parameters.
 */
@Incubating
public final class DefaultTenantKeyGenerator implements TenantKeyGenerator {

    @Override
    public Object generate(Object target, Method method, @Nullable Object... params) {
        return SimpleKeyGenerator.generateKey(TenantContext.getRequiredTenantIdentifier(), params);
    }

}
