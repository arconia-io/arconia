package io.arconia.dev.services.mongodb;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.mongodb.MongoDBContainer;

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
            MongoDBContainer container = context.getBean(MongoDBContainer.class);
            assertThat(container.getDockerImageName()).contains("mongo");
            assertThat(container.getEnv()).isEmpty();
            assertThat(container.isShouldBeReused()).isFalse();
        });
    }

    @Test
    void containerConfigurationApplied() {
        contextRunner
            .withPropertyValues(
                "arconia.dev.services.mongodb.environment.MONGO_INITDB_ROOT_USERNAME=mongodb",
                "arconia.dev.services.mongodb.shared=never",
                "arconia.dev.services.mongodb.startup-timeout=90s"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(MongoDBContainer.class);
                MongoDBContainer container = context.getBean(MongoDBContainer.class);
                assertThat(container.getEnv()).contains("MONGO_INITDB_ROOT_USERNAME=mongodb");
                assertThat(container.isShouldBeReused()).isFalse();
            });
    }

}
