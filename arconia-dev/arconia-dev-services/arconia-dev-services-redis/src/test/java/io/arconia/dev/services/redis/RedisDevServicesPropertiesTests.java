package io.arconia.dev.services.redis;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RedisDevServicesProperties}.
 */
class RedisDevServicesPropertiesTests {

    @Test
    void shouldHaveCorrectConfigPrefix() {
        assertThat(RedisDevServicesProperties.CONFIG_PREFIX)
                .isEqualTo("arconia.dev.services.redis");
    }

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        RedisDevServicesProperties properties = new RedisDevServicesProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getEdition()).isEqualTo(RedisDevServicesProperties.Edition.COMMUNITY);

        // Community edition defaults
        assertThat(properties.getCommunity().getImageName()).isEqualTo("redis:7.4-alpine");
        assertThat(properties.getCommunity().getEnvironment()).isEmpty();
        assertThat(properties.getCommunity().isReusable()).isFalse();

        // Stack edition defaults
        assertThat(properties.getStack().getImageName()).contains("redis/redis-stack-server");
        assertThat(properties.getStack().getEnvironment()).isEmpty();
        assertThat(properties.getStack().isReusable()).isFalse();
    }

    @Test
    void shouldUpdateCommunityEditionValues() {
        RedisDevServicesProperties properties = new RedisDevServicesProperties();

        properties.setEnabled(false);
        properties.setEdition(RedisDevServicesProperties.Edition.COMMUNITY);
        properties.getCommunity().setImageName("redis:latest");
        properties.getCommunity().setEnvironment(Map.of("REDIS_PASSWORD", "redis"));
        properties.getCommunity().setReusable(true);

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getEdition()).isEqualTo(RedisDevServicesProperties.Edition.COMMUNITY);
        assertThat(properties.getCommunity().getImageName()).isEqualTo("redis:latest");
        assertThat(properties.getCommunity().getEnvironment()).containsEntry("REDIS_PASSWORD", "redis");
        assertThat(properties.getCommunity().isReusable()).isTrue();
    }

    @Test
    void shouldUpdateStackEditionValues() {
        RedisDevServicesProperties properties = new RedisDevServicesProperties();

        properties.setEnabled(false);
        properties.setEdition(RedisDevServicesProperties.Edition.STACK);
        properties.getStack().setImageName("redis/redis-stack:latest");
        properties.getStack().setEnvironment(Map.of("REDIS_PASSWORD", "redis"));
        properties.getStack().setReusable(true);

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getEdition()).isEqualTo(RedisDevServicesProperties.Edition.STACK);
        assertThat(properties.getStack().getImageName()).isEqualTo("redis/redis-stack:latest");
        assertThat(properties.getStack().getEnvironment()).containsEntry("REDIS_PASSWORD", "redis");
        assertThat(properties.getStack().isReusable()).isTrue();
    }

}
