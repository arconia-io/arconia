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
    void whenSameTenantAndParametersThenSameCacheKey() {
        var objectToCache = new Object[] { "something" };

        TenantContext.where("tenant1").run(() -> {
            assertThat(generateCacheKey(objectToCache)).isEqualTo(generateCacheKey(objectToCache));
        });
    }

    @Test
    void whenDifferentTenantThenDifferentCacheKey() {
        var objectToCache = new Object[] { "something" };

        TenantContext.where("tenant1").run(() -> {
            Object key1 = generateCacheKey(objectToCache);

            TenantContext.where("tenant2").run(() -> {
                assertThat(generateCacheKey(objectToCache)).isNotEqualTo(key1);
            });
        });
    }

    @Test
    void whenDifferentParametersThenDifferentCacheKey() {
        TenantContext.where("tenant1").run(() -> {
            assertThat(generateCacheKey(new Object[] { "something" }))
                .isNotEqualTo(generateCacheKey(new Object[] { "something else" }));
        });
    }

    @Test
    void whenTenantContextNotDefinedThenThrow() {
        assertThatThrownBy(() -> generateCacheKey(new Object[] { "something" }))
            .isInstanceOf(TenantNotFoundException.class)
            .hasMessageContaining("A tenant-aware cache key cannot be generated outside a tenant context");
    }

    private Object generateCacheKey(Object[] arguments) {
        var method = ReflectionUtils.findMethod(this.getClass(), "generateCacheKey", Object[].class);
        assertThat(method).isNotNull();
        return keyGenerator.generate(this, method, arguments);
    }

}
