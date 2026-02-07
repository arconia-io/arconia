package io.arconia.dev.services.mongodb.atlas;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.support.SimpleThreadScope;
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
    void autoConfigurationNotActivatedWhenGloballyDisabled() {
        contextRunner
                .withPropertyValues("arconia.dev.services.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(MongoDBAtlasLocalContainer.class));
    }

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
            MongoDBAtlasLocalContainer container = context.getBean(MongoDBAtlasLocalContainer.class);
            assertThat(container.getDockerImageName()).contains("mongodb/mongodb-atlas-local");
            assertThat(container.getEnv()).isEmpty();
            assertThat(container.getNetworkAliases()).hasSize(1);
            assertThat(container.isShouldBeReused()).isFalse();

            String[] beanNames = context.getBeanFactory().getBeanNamesForType(MongoDBAtlasLocalContainer.class);
            assertThat(beanNames).hasSize(1);
            assertThat(context.getBeanFactory().getBeanDefinition(beanNames[0]).getScope())
                    .isEqualTo("singleton");
        });
    }

    @Test
    void containerConfigurationApplied() {
        contextRunner
                .withPropertyValues(
                        "arconia.dev.services.mongodb-atlas.environment.KEY=value",
                        "arconia.dev.services.mongodb-atlas.network-aliases=network1",
                        "arconia.dev.services.mongodb-atlas.resources[0].source-path=test-resource.txt",
                        "arconia.dev.services.mongodb-atlas.resources[0].container-path=/tmp/test-resource.txt"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(MongoDBAtlasLocalContainer.class);
                    MongoDBAtlasLocalContainer container = context.getBean(MongoDBAtlasLocalContainer.class);
                    assertThat(container.getEnv()).contains("KEY=value");
                    assertThat(container.getNetworkAliases()).contains("network1");
                    container.start();
                    assertThat(container.getCurrentContainerInfo().getState().getStatus()).isEqualTo("running");
                    assertThat(container.execInContainer("ls", "/tmp").getStdout()).contains("test-resource.txt");
                    container.stop();
                });
    }

    @Test
    void containerWithRestartScope() {
        contextRunner
                .withClassLoader(this.getClass().getClassLoader())
                .withInitializer(context -> {
                    context.getBeanFactory().registerScope("restart", new SimpleThreadScope());
                })
                .run(context -> {
                    assertThat(context).hasSingleBean(MongoDBAtlasLocalContainer.class);
                    String[] beanNames = context.getBeanFactory().getBeanNamesForType(MongoDBAtlasLocalContainer.class);
                    assertThat(beanNames).hasSize(1);
                    assertThat(context.getBeanFactory().getBeanDefinition(beanNames[0]).getScope())
                            .isEqualTo("restart");
                });
    }

}
