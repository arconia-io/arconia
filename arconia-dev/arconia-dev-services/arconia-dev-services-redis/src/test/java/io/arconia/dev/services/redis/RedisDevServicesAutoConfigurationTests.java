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
                "arconia.dev.services.redis.community.image-name=docker.io/redis",
                "arconia.dev.services.redis.community.environment.REDIS_PASSWORD=redis",
                "arconia.dev.services.redis.community.reusable=false"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(RedisContainer.class);
                RedisContainer container = context.getBean(RedisContainer.class);
                assertThat(container.getDockerImageName()).contains("docker.io/redis");
                assertThat(container.getEnv()).contains("REDIS_PASSWORD=redis");
                assertThat(container.isShouldBeReused()).isFalse();
            });
    }

    @Test
    void redisStackContainerAvailableWithStackEdition() {
        contextRunner
            .withPropertyValues(
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
                "arconia.dev.services.redis.edition=stack",
                "arconia.dev.services.redis.stack.image-name=docker.io/redis/redis-stack-server",
                "arconia.dev.services.redis.stack.environment.REDIS_PASSWORD=redis",
                "arconia.dev.services.redis.stack.reusable=false"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(RedisStackContainer.class);
                RedisStackContainer container = context.getBean(RedisStackContainer.class);
                assertThat(container.getDockerImageName()).contains("docker.io/redis/redis-stack-server");
                assertThat(container.getEnv()).contains("REDIS_PASSWORD=redis");
                assertThat(container.isShouldBeReused()).isFalse();
            });
    }

}
