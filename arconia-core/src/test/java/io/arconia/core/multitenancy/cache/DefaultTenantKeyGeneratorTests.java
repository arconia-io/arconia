package io.arconia.core.multitenancy.cache;

import org.junit.jupiter.api.Test;
import org.springframework.util.ReflectionUtils;

import io.arconia.core.multitenancy.context.TenantContextHolder;
import io.arconia.core.multitenancy.exceptions.TenantNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DefaultTenantKeyGenerator}.
 */
class DefaultTenantKeyGeneratorTests {

    private final DefaultTenantKeyGenerator keyGenerator = new DefaultTenantKeyGenerator();

    @Test
    void whenTenantContextDefinedThenGenerateCacheKey() {
        var objectToCache = new Object[] { "something" };

        TenantContextHolder.setTenantIdentifier("tenant1");
        Object key1 = generateCacheKey(objectToCache);
        Object key2 = generateCacheKey(objectToCache);
        TenantContextHolder.clear();

        TenantContextHolder.setTenantIdentifier("tenant2");
        Object key3 = generateCacheKey(objectToCache);
        TenantContextHolder.clear();

        assertThat(key1.hashCode()).isEqualTo(key2.hashCode());
        assertThat(key1.hashCode()).isNotEqualTo(key3.hashCode());
    }

    @Test
    void whenTenantContextNotDefinedThenThrow() {
        TenantContextHolder.clear();
        assertThatThrownBy(() -> generateCacheKey(new Object[] { "something" }))
            .isInstanceOf(TenantNotFoundException.class)
            .hasMessageContaining("No tenant found in the current context");
    }

    private Object generateCacheKey(Object[] arguments) {
        var method = ReflectionUtils.findMethod(this.getClass(), "generateCacheKey", Object[].class);
        assertThat(method).isNotNull();
        return keyGenerator.generate(this, method, arguments);
    }

}
