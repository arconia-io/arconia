package io.arconia.dev.services.mongodb;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MongoDbDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class MongoDbDevServicesAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(RestartScope.class))
            .withConfiguration(AutoConfigurations.of(MongoDbDevServicesAutoConfiguration.class));

    @Test
    void autoConfigurationNotActivatedWhenDisabled() {
        contextRunner
            .withPropertyValues("arconia.dev.services.mongodb.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(MongoDBContainer.class));
    }

    @Test
    void containerAvailableWithDefaultConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(MongoDBContainer.class);
            MongoDBContainer<?> container = context.getBean(MongoDBContainer.class);
            assertThat(container.getDockerImageName()).contains("mongodb");
            assertThat(container.getEnv()).isEmpty();
            assertThat(container.isShouldBeReused()).isFalse();
        });
    }

    @Test
    void containerConfigurationApplied() {
        contextRunner
            .withPropertyValues(
                "arconia.dev.services.mongodb.image-name=docker.io/mongodb",
                "arconia.dev.services.mongodb.environment.MONGODB_USER=mongodb",
                "arconia.dev.services.mongodb.shared=never",
                "arconia.dev.services.mongodb.startup-timeout=90s"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(MongoDBContainer.class);
                MongoDBContainer<?> container = context.getBean(MongoDBContainer.class);
                assertThat(container.getDockerImageName()).contains("docker.io/mongodb");
                assertThat(container.getEnv()).contains("MONGODB_USER=mongodb");
                assertThat(container.isShouldBeReused()).isFalse();
            });
    }

}
