package io.arconia.dev.services.redis;

import com.redis.testcontainers.RedisContainer;
import com.redis.testcontainers.RedisStackContainer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RedisDevServicesAutoConfiguration}.
 */
class RedisDevServicesAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(RestartScope.class))
            .withConfiguration(AutoConfigurations.of(RedisDevServicesAutoConfiguration.class));

    @Test
    void autoConfigurationNotActivatedWhenDisabled() {
        contextRunner
            .withPropertyValues("arconia.dev.services.redis.enabled=false")
            .run(context -> {
                assertThat(context).doesNotHaveBean(RedisContainer.class);
                assertThat(context).doesNotHaveBean(RedisStackContainer.class);
            });
    }

    @Test
    void redisCommunityContainerAvailableWithDefaultConfiguration() {
        contextRunner
            .withPropertyValues("spring.devtools.restart.enabled=false")
            .run(context -> {
                assertThat(context).hasSingleBean(RedisContainer.class);
                RedisContainer container = context.getBean(RedisContainer.class);
                assertThat(container.getDockerImageName()).contains("redis");
                assertThat(container.isShouldBeReused()).isFalse();
            });
    }

    @Test
    void redisCommunityContainerConfigurationApplied() {
        contextRunner
            .withPropertyValues(
                "spring.devtools.restart.enabled=false",
                "arconia.dev.services.redis.community.image-name=redis:7.2-alpine",
                "arconia.dev.services.redis.community.reusable=false"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(RedisContainer.class);
                RedisContainer container = context.getBean(RedisContainer.class);
                assertThat(container.getDockerImageName()).contains("redis:7.2-alpine");
                assertThat(container.isShouldBeReused()).isFalse();
            });
    }

    @Test
    void redisStackContainerAvailableWithStackEdition() {
        contextRunner
            .withPropertyValues(
                "spring.devtools.restart.enabled=false",
                "arconia.dev.services.redis.edition=stack"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(RedisStackContainer.class);
                RedisStackContainer container = context.getBean(RedisStackContainer.class);
                assertThat(container.getDockerImageName()).contains("redis/redis-stack-server");
                assertThat(container.isShouldBeReused()).isFalse();
            });
    }

    @Test
    void redisStackContainerConfigurationApplied() {
        contextRunner
            .withPropertyValues(
                "spring.devtools.restart.enabled=false",
                "arconia.dev.services.redis.edition=stack",
                "arconia.dev.services.redis.stack.image-name=redis/redis-stack-server:7.2.0-v0",
                "arconia.dev.services.redis.stack.reusable=false"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(RedisStackContainer.class);
                RedisStackContainer container = context.getBean(RedisStackContainer.class);
                assertThat(container.getDockerImageName()).contains("redis/redis-stack-server:7.2.0-v0");
                assertThat(container.isShouldBeReused()).isFalse();
            });
    }

}
