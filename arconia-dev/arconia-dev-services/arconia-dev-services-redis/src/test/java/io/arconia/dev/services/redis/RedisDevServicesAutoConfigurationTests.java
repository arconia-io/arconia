package io.arconia.dev.services.redis;

import com.redis.testcontainers.RedisContainer;

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
            .run(context -> assertThat(context).doesNotHaveBean(RedisContainer.class));
    }

    @Test
    void containerAvailableWithDefaultConfiguration() {
        contextRunner
            .run(context -> {
                assertThat(context).hasSingleBean(RedisContainer.class);
                RedisContainer container = context.getBean(RedisContainer.class);
                assertThat(container.getDockerImageName()).contains("redis");
                assertThat(container.isShouldBeReused()).isFalse();
            });
    }

    @Test
    void containerConfigurationApplied() {
        contextRunner
            .withPropertyValues(
                "arconia.dev.services.redis.image-name=docker.io/redis",
                "arconia.dev.services.redis.environment.REDIS_PASSWORD=redis",
                "arconia.dev.services.redis.shared=never"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(RedisContainer.class);
                RedisContainer container = context.getBean(RedisContainer.class);
                assertThat(container.getDockerImageName()).contains("docker.io/redis");
                assertThat(container.getEnv()).contains("REDIS_PASSWORD=redis");
                assertThat(container.isShouldBeReused()).isFalse();
            });
    }

}
