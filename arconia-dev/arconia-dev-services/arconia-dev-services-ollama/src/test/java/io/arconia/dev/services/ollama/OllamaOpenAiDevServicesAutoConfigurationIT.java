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
 * Integration tests for {@link OllamaOpenAiDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class OllamaOpenAiDevServicesAutoConfigurationIT {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(RestartScope.class))
            .withConfiguration(AutoConfigurations.of(
                    OllamaDevServicesAutoConfiguration.class,
                    OllamaOpenAiDevServicesAutoConfiguration.class))
            .withPropertyValues("arconia.dev.services.ollama.ignore-native-service=true");

    @Test
    void openAiBaseUrlResolvedFromContainer() {
        contextRunner
                .run(context -> {
                    assertThat(context).hasSingleBean(OllamaContainer.class);

                    var container = context.getBean(OllamaContainer.class);
                    container.start();

                    var environment = context.getEnvironment();
                    assertThat(environment.getProperty("spring.ai.openai.base-url"))
                            .isEqualTo(container.getEndpoint());
                    assertThat(environment.getProperty("spring.ai.openai.api-key"))
                            .isEqualTo("ollama");

                    container.stop();
                });
    }

}
