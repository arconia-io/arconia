package io.arconia.dev.services.ollama;

import org.junit.jupiter.api.Test;
import org.springframework.ai.model.ollama.autoconfigure.OllamaConnectionProperties;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.support.SimpleThreadScope;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.ollama.OllamaContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OllamaDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class OllamaDevServicesAutoConfigurationIT {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(RestartScope.class, OllamaConnectionProperties.class))
            .withConfiguration(AutoConfigurations.of(OllamaDevServicesAutoConfiguration.class));

    @Test
    void autoConfigurationNotActivatedWhenGloballyDisabled() {
        contextRunner
                .withPropertyValues("arconia.dev.services.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(OllamaContainer.class));
    }

    @Test
    void autoConfigurationActivatedWhenDefault() {
        contextRunner
                .run(context -> assertThat(context).hasSingleBean(OllamaContainer.class));
    }

    @Test
    void autoConfigurationNotActivatedWhenDisabled() {
        contextRunner
                .withPropertyValues("arconia.dev.services.ollama.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(OllamaContainer.class));
    }

    @Test
    void containerActivatedWhenEnabled() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .run(context -> {
                    assertThat(context).hasSingleBean(OllamaContainer.class);
                    OllamaContainer container = context.getBean(OllamaContainer.class);
                    assertThat(container.getDockerImageName()).contains("ollama/ollama");
                    assertThat(container.getEnv()).isEmpty();
                    assertThat(container.getNetworkAliases()).hasSize(1);
                    assertThat(container.isShouldBeReused()).isTrue();

                    String[] beanNames = context.getBeanFactory().getBeanNamesForType(OllamaContainer.class);
                    assertThat(beanNames).hasSize(1);
                    assertThat(context.getBeanFactory().getBeanDefinition(beanNames[0]).getScope()).isEqualTo("singleton");
                });
    }

    @Test
    void containerConfigurationApplied() {
        contextRunner
                .withPropertyValues(
                        "arconia.dev.services.ollama.environment.KEY=value",
                        "arconia.dev.services.ollama.network-aliases=network1",
                        "arconia.dev.services.ollama.resources[0].source-path=test-resource.txt",
                        "arconia.dev.services.ollama.resources[0].container-path=/tmp/test-resource.txt"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(OllamaContainer.class);
                    OllamaContainer container = context.getBean(OllamaContainer.class);
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
                .withClassLoader(new FilteredClassLoader(OllamaConnectionProperties.class))
                .withInitializer(context -> {
                    context.getBeanFactory().registerScope("restart", new SimpleThreadScope());
                })
                .run(context -> {
                    assertThat(context).hasSingleBean(OllamaContainer.class);
                    String[] beanNames = context.getBeanFactory().getBeanNamesForType(OllamaContainer.class);
                    assertThat(beanNames).hasSize(1);
                    assertThat(context.getBeanFactory().getBeanDefinition(beanNames[0]).getScope())
                            .isEqualTo("restart");
                });
    }

}
