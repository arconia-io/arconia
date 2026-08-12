package io.arconia.multitenancy.core.cache;

import org.junit.jupiter.api.Test;
import org.springframework.util.ReflectionUtils;

import io.arconia.multitenancy.core.context.TenantContext;
import io.arconia.multitenancy.core.exceptions.TenantNotFoundException;

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

        TenantContext.where("tenant1").run(() -> {
            Object key1 = generateCacheKey(objectToCache);
            Object key2 = generateCacheKey(objectToCache);
            assertThat(key1.hashCode()).isEqualTo(key2.hashCode());

            TenantContext.where("tenant2").run(() -> {
                Object key3 = generateCacheKey(objectToCache);
                assertThat(key1.hashCode()).isNotEqualTo(key3.hashCode());
            });
        });
    }

    @Test
    void whenTenantContextNotDefinedThenThrow() {
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
