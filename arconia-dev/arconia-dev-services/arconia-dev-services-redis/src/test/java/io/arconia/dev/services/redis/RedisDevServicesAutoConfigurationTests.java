package io.arconia.dev.services.redis;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RedisDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class RedisDevServicesAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(RestartScope.class))
            .withConfiguration(AutoConfigurations.of(RedisDevServicesAutoConfiguration.class));

    @Test
    void autoConfigurationNotActivatedWhenDisabled() {
        contextRunner
            .withPropertyValues("arconia.dev.services.redis.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(GenericContainer.class));
    }

    @Test
    void containerAvailableWithDefaultConfiguration() {
        contextRunner
            .run(context -> {
                assertThat(context).hasSingleBean(GenericContainer.class);
                GenericContainer<?> container = context.getBean(GenericContainer.class);
                assertThat(container.getDockerImageName()).contains("redis");
                assertThat(container.getEnv()).isEmpty();
                assertThat(container.isShouldBeReused()).isFalse();
            });
    }

    @Test
    void containerConfigurationApplied() {
        contextRunner
            .withPropertyValues(
                "arconia.dev.services.redis.environment.REDIS_PASSWORD=redis",
                "arconia.dev.services.redis.shared=never",
                "arconia.dev.services.redis.startup-timeout=90s"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(GenericContainer.class);
                GenericContainer<?> container = context.getBean(GenericContainer.class);
                assertThat(container.getEnv()).contains("REDIS_PASSWORD=redis");
                assertThat(container.isShouldBeReused()).isFalse();
            });
    }

}
