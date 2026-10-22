package io.arconia.dev.services.redis;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import io.arconia.dev.services.tests.DisabledIfDockerUnavailable;
import io.arconia.testcontainers.redis.RedisContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Redis Dev Service in a test slice: the slice picks up the dev service
 * with no further configuration.
 */
@DataRedisTest
@DisabledIfDockerUnavailable
class RedisDevServicesSliceIT {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedisContainer container;

    @Test
    void sliceConnectsToTheDevService() {
        redisTemplate.opsForValue().set("greeting", "hello");

        assertThat(container.isRunning()).isTrue();
        assertThat(redisTemplate.opsForValue().get("greeting")).isEqualTo("hello");
    }

    /**
     * The application the slice is taken from, as a test would have one in the project.
     */
    @SpringBootApplication(proxyBeanMethods = false)
    static class TestApplication {}

}
