package io.arconia.dev.services.redis;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RedisDevServiceProperties}.
 */
class RedisDevServicePropertiesTests {

    @Test
    void shouldHaveCorrectConfigPrefix() {
        assertThat(RedisDevServiceProperties.CONFIG_PREFIX)
                .isEqualTo("arconia.dev.services.redis");
    }

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        RedisDevServiceProperties properties = new RedisDevServiceProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getEdition()).isEqualTo(RedisDevServiceProperties.Edition.COMMUNITY);

        // Community edition defaults
        assertThat(properties.getCommunity().getImageName()).isEqualTo("redis:7.4-alpine");
        assertThat(properties.getCommunity().isReusable()).isFalse();

        // Stack edition defaults
        assertThat(properties.getStack().getImageName()).isEqualTo("redis/redis-stack-server:7.4.0-v3");
        assertThat(properties.getStack().isReusable()).isFalse();
    }

    @Test
    void shouldUpdateCommunityEditionValues() {
        RedisDevServiceProperties properties = new RedisDevServiceProperties();

        properties.setEnabled(false);
        properties.setEdition(RedisDevServiceProperties.Edition.COMMUNITY);
        properties.getCommunity().setImageName("redis:latest");
        properties.getCommunity().setReusable(true);

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getEdition()).isEqualTo(RedisDevServiceProperties.Edition.COMMUNITY);
        assertThat(properties.getCommunity().getImageName()).isEqualTo("redis:latest");
        assertThat(properties.getCommunity().isReusable()).isTrue();
    }

    @Test
    void shouldUpdateStackEditionValues() {
        RedisDevServiceProperties properties = new RedisDevServiceProperties();

        properties.setEnabled(false);
        properties.setEdition(RedisDevServiceProperties.Edition.STACK);
        properties.getStack().setImageName("redis/redis-stack:latest");
        properties.getStack().setReusable(true);

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getEdition()).isEqualTo(RedisDevServiceProperties.Edition.STACK);
        assertThat(properties.getStack().getImageName()).isEqualTo("redis/redis-stack:latest");
        assertThat(properties.getStack().isReusable()).isTrue();
    }

}
