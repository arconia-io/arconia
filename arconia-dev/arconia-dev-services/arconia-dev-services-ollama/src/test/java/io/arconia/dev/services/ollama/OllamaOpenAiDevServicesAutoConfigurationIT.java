package io.arconia.dev.services.ollama;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.ollama.OllamaContainer;

import io.arconia.boot.bootstrap.BootstrapMode;
import io.arconia.dev.services.tests.DevServiceContainers;
import io.arconia.dev.services.tests.DisabledIfDockerUnavailable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OllamaOpenAiDevServicesAutoConfiguration}.
 */
@DisabledIfDockerUnavailable
class OllamaOpenAiDevServicesAutoConfigurationIT {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
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
                    String baseUrl = environment.getProperty("spring.ai.openai.base-url");
                    assertThat(baseUrl).isEqualTo(container.getEndpoint() + "/v1");
                    assertThat(environment.getProperty("spring.ai.openai.api-key"))
                            .isEqualTo("ollama");
                    // The OpenAI client appends the operation path to the base URL, as for the model list.
                    assertThat(get(baseUrl + "/models")).isEqualTo(200);

                    container.stop();
                });
    }

    @Test
    void openAiBaseUrlResolvedFromDiscoveredContainer() {
        OllamaDevServicesProperties properties = new OllamaDevServicesProperties();
        try (OllamaContainer discoveredContainer = DevServiceContainers.discoverable(
                new ArconiaOllamaContainer(properties), properties, "ollama", "another-application")) {
            discoveredContainer.start();

            contextRunner
                    .withSystemProperties("arconia.bootstrap.mode=dev")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(OllamaContainer.class);

                        var environment = context.getEnvironment();
                        assertThat(environment.getProperty("spring.ai.openai.base-url"))
                                .isEqualTo("http://%s:%d/v1".formatted(
                                        discoveredContainer.getHost(), discoveredContainer.getMappedPort(ArconiaOllamaContainer.OLLAMA_PORT)));
                        assertThat(environment.getProperty("spring.ai.openai.api-key"))
                                .isEqualTo("ollama");
                    });
        }
    }

    private static int get(String url) throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.discarding()).statusCode();
        }
    }

}
