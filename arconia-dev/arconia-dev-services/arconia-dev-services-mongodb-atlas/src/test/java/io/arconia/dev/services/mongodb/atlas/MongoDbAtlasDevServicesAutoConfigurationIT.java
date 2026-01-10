package io.arconia.dev.services.mongodb.atlas;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.mongodb.MongoDBAtlasLocalContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link MongoDbAtlasDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class MongoDbAtlasDevServicesAutoConfigurationIT {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(RestartScope.class))
            .withConfiguration(AutoConfigurations.of(MongoDbAtlasDevServicesAutoConfiguration.class));

    @Test
    void autoConfigurationNotActivatedWhenDisabled() {
        contextRunner
            .withPropertyValues("arconia.dev.services.mongodb-atlas.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(MongoDBAtlasLocalContainer.class));
    }

    @Test
    void containerAvailableWithDefaultConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(MongoDBAtlasLocalContainer.class);
            MongoDBAtlasLocalContainer atlasLocalContainer = context.getBean(MongoDBAtlasLocalContainer.class);
            assertThat(atlasLocalContainer.getDockerImageName()).contains("mongodb/mongodb-atlas-local");
            assertThat(atlasLocalContainer.getEnv()).isEmpty();
            assertThat(atlasLocalContainer.isShouldBeReused()).isFalse();
        });
    }

    @Test
    void containerConfigurationApplied() {
        contextRunner
            .withPropertyValues(
                "arconia.dev.services.mongodb-atlas.environment.KEY=value",
                "arconia.dev.services.mongodb-atlas.shared=never",
                "arconia.dev.services.mongodb-atlas.startup-timeout=90s"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(MongoDBAtlasLocalContainer.class);
                MongoDBAtlasLocalContainer atlasLocalContainer = context.getBean(MongoDBAtlasLocalContainer.class);
                assertThat(atlasLocalContainer.getEnv()).contains("KEY=value");
                assertThat(atlasLocalContainer.isShouldBeReused()).isFalse();
            });
    }

    @Test
    void containerWithRestartScope() {
        contextRunner
                .withClassLoader(this.getClass().getClassLoader())
                .run(context -> {
                    assertThat(context).hasSingleBean(MongoDBAtlasLocalContainer.class);
                    String[] beanNames = context.getBeanFactory().getBeanNamesForType(MongoDBAtlasLocalContainer.class);
                    assertThat(beanNames).hasSize(1);
                    assertThat(context.getBeanFactory().getBeanDefinition(beanNames[0]).getScope())
                            .isEqualTo("restart");
                });
    }

}
