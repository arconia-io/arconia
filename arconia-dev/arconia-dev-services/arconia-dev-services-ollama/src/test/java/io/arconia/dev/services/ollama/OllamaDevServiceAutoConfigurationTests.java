package io.arconia.dev.services.ollama;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.ollama.OllamaContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OllamaDevServiceAutoConfiguration}.
 */
class OllamaDevServiceAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withPropertyValues("spring.devtools.restart.enabled=false")
            .withConfiguration(AutoConfigurations.of(OllamaDevServiceAutoConfiguration.class));

    @Test
    void autoConfigurationNotActivatedWhenDefault() {
        contextRunner
                .withPropertyValues("arconia.dev.services.ollama.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(OllamaContainer.class));
    }

    @Test
    void autoConfigurationNotActivatedWhenDisabled() {
        contextRunner
            .withPropertyValues("arconia.dev.services.ollama.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(OllamaContainer.class));
    }

    @Test
    void ollamaContainerActivatedWhenEnabled() {
        contextRunner
            .withPropertyValues("arconia.dev.services.ollama.enabled=true")
            .run(context -> {
                assertThat(context).hasSingleBean(OllamaContainer.class);
                OllamaContainer container = context.getBean(OllamaContainer.class);
                assertThat(container.getDockerImageName()).contains("ollama/ollama");
                assertThat(container.isShouldBeReused()).isTrue();
            });
    }

    @Test
    void ollamaContainerConfigurationApplied() {
        contextRunner
            .withPropertyValues(
                "arconia.dev.services.ollama.enabled=true",
                "arconia.dev.services.ollama.image-name=ollama/ollama",
                "arconia.dev.services.ollama.reusable=false"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(OllamaContainer.class);
                OllamaContainer container = context.getBean(OllamaContainer.class);
                assertThat(container.getDockerImageName()).contains("ollama/ollama");
                assertThat(container.isShouldBeReused()).isFalse();
            });
    }

}
