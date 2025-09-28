package io.arconia.dev.services.ollama;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.ollama.OllamaContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OllamaDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class OllamaDevServicesAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(RestartScope.class))
            .withConfiguration(AutoConfigurations.of(OllamaDevServicesAutoConfiguration.class));

    @Test
    void autoConfigurationNotActivatedWhenDefault() {
        contextRunner
                .run(context -> assertThat(context).doesNotHaveBean(OllamaContainer.class));
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
            .withPropertyValues("arconia.dev.services.ollama.enabled=true")
            .run(context -> {
                assertThat(context).hasSingleBean(OllamaContainer.class);
                OllamaContainer container = context.getBean(OllamaContainer.class);
                assertThat(container.getDockerImageName()).contains("ollama/ollama");
                assertThat(container.getEnv()).isEmpty();
                assertThat(container.isShouldBeReused()).isTrue();
            });
    }

    @Test
    void containerConfigurationApplied() {
        contextRunner
            .withPropertyValues(
                "arconia.dev.services.ollama.enabled=true",
                "arconia.dev.services.ollama.environment.OLLAMA_NUM_PARALLEL=4",
                "arconia.dev.services.ollama.shared=never",
                "arconia.dev.services.ollama.startup-timeout=90s"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(OllamaContainer.class);
                OllamaContainer container = context.getBean(OllamaContainer.class);
                assertThat(container.getEnv()).contains("OLLAMA_NUM_PARALLEL=4");
                assertThat(container.isShouldBeReused()).isFalse();
            });
    }

}
