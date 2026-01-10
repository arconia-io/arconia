package io.arconia.dev.services.redis;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import io.arconia.testcontainers.redis.RedisContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link RedisDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class RedisDevServicesAutoConfigurationIT {

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
                assertThat(container.getEnv()).isEmpty();
                assertThat(container.isShouldBeReused()).isFalse();
            });
    }

    @Test
    void containerConfigurationApplied() {
        contextRunner
            .withPropertyValues(
                "arconia.dev.services.redis.environment.KEY=value",
                "arconia.dev.services.redis.shared=never",
                "arconia.dev.services.redis.startup-timeout=90s"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(RedisContainer.class);
                RedisContainer container = context.getBean(RedisContainer.class);
                assertThat(container.getEnv()).contains("KEY=value");
                assertThat(container.isShouldBeReused()).isFalse();
            });
    }

    @Test
    void containerWithRestartScope() {
        contextRunner
                .withClassLoader(this.getClass().getClassLoader())
                .run(context -> {
                    assertThat(context).hasSingleBean(RedisContainer.class);
                    String[] beanNames = context.getBeanFactory().getBeanNamesForType(RedisContainer.class);
                    assertThat(beanNames).hasSize(1);
                    assertThat(context.getBeanFactory().getBeanDefinition(beanNames[0]).getScope())
                            .isEqualTo("restart");
                });
    }

}
