package io.arconia.dev.services.ollama;

import org.junit.jupiter.api.Test;
import org.springframework.ai.model.ollama.autoconfigure.OllamaConnectionDetails;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.ollama.OllamaContainer;

import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationTests;
import io.arconia.dev.services.tests.DisabledIfDockerUnavailable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OllamaDevServicesAutoConfiguration}.
 */
class OllamaDevServicesAutoConfigurationTests extends BaseDevServicesAutoConfigurationTests {

    /**
     * Two variants of a very small embedding model, keeping the pulls cheap
     * while still exercising more than one model.
     */
    private static final String TEST_MODEL = "all-minilm:22m";

    private static final String OTHER_TEST_MODEL = "all-minilm:33m";

    OllamaDevServicesAutoConfigurationTests() {
        super(OllamaDevServicesAutoConfiguration.class, "ollama", OllamaContainer.class, OllamaConnectionDetails.class);
    }

    @Override
    protected ApplicationContextRunner getContextRunner() {
        return super.getContextRunner().withPropertyValues("arconia.dev.services.ollama.ignore-native-service=true");
    }

    @Override
    protected GenericContainer<?> createDiscoverableContainer(String ownerId) {
        var properties = new OllamaDevServicesProperties();
        return asDiscoverableContainer(new ArconiaOllamaContainer(properties), properties, ownerId);
    }

    @Override
    protected void assertDiscoveredConnectionDetails(AssertableApplicationContext context, GenericContainer<?> discoveredContainer) {
        OllamaConnectionDetails connectionDetails = context.getBean(OllamaConnectionDetails.class);
        assertThat(connectionDetails.getBaseUrl()).isEqualTo(((OllamaContainer) discoveredContainer).getEndpoint());
    }

    @Test
    @DisabledIfDockerUnavailable
    void modelsPulledWhenConfigured() {
        getContextRunner()
                .withPropertyValues("arconia.dev.services.ollama.models=%s,%s".formatted(TEST_MODEL, OTHER_TEST_MODEL))
                .run(context -> {
                    var container = context.getBean(getContainerClass());
                    container.start();
                    try {
                        var result = container.execInContainer("ollama", "list");
                        assertThat(result.getExitCode()).isZero();
                        assertThat(result.getStdout()).contains(TEST_MODEL, OTHER_TEST_MODEL);
                    }
                    finally {
                        container.stop();
                    }
                });
    }

    @Test
    @DisabledIfDockerUnavailable
    void noModelPulledWhenNotConfigured() {
        getContextRunner()
                .run(context -> {
                    var container = context.getBean(getContainerClass());
                    container.start();
                    try {
                        var result = container.execInContainer("ollama", "list");
                        assertThat(result.getExitCode()).isZero();
                        assertThat(result.getStdout()).doesNotContain("all-minilm");
                    }
                    finally {
                        container.stop();
                    }
                });
    }

}
