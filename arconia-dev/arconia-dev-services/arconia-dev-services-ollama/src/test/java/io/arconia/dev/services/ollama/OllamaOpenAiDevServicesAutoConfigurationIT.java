package io.arconia.dev.services.ollama;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.ollama.OllamaContainer;

import io.arconia.boot.bootstrap.BootstrapMode;
import io.arconia.dev.services.api.registration.DevServiceLabels;

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

    @BeforeEach
    void setUp() {
        BootstrapMode.clear();
    }

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

    @Test
    void openAiBaseUrlResolvedFromDiscoveredContainer() {
        OllamaDevServicesProperties properties = new OllamaDevServicesProperties();
        try (OllamaContainer sharedContainer = new OllamaContainer(properties.getImageName())
                .withLabel(DevServiceLabels.NAME, "ollama")
                .withLabel(DevServiceLabels.SHARED, "true")
                .withLabel(DevServiceLabels.OWNER, "another-application")) {
            sharedContainer.start();

            contextRunner
                    .withSystemProperties("arconia.bootstrap.mode=dev")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(OllamaContainer.class);

                        var environment = context.getEnvironment();
                        assertThat(environment.getProperty("spring.ai.openai.base-url"))
                                .isEqualTo("http://%s:%d".formatted(
                                        sharedContainer.getHost(), sharedContainer.getMappedPort(ArconiaOllamaContainer.OLLAMA_PORT)));
                        assertThat(environment.getProperty("spring.ai.openai.api-key"))
                                .isEqualTo("ollama");
                    });
        }
    }

}
