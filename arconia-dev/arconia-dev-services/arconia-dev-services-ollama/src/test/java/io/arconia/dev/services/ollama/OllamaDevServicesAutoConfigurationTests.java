package io.arconia.dev.services.ollama;

import org.junit.jupiter.api.Test;
import org.springframework.ai.model.ollama.autoconfigure.OllamaConnectionDetails;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testcontainers.beans.TestcontainerBeanDefinition;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.ollama.OllamaContainer;

import io.arconia.dev.services.api.registration.DevServiceLabels;
import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationTests;
import io.arconia.dev.services.tests.DisabledIfDockerUnavailable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OllamaDevServicesAutoConfiguration}.
 */
@DisabledIfDockerUnavailable
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
    void withoutTheSpringAiOllamaModuleThenNoServiceConnectionNorDiscovery() {
        getContextRunner()
                .withClassLoader(new FilteredClassLoader(OllamaConnectionDetails.class))
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    String[] containerBeanNames = context.getBeanNamesForType(getContainerClass());
                    assertThat(containerBeanNames).hasSize(1);
                    var beanDefinition = (TestcontainerBeanDefinition) context.getBeanFactory().getBeanDefinition(containerBeanNames[0]);
                    assertThat(beanDefinition.getAnnotations().isPresent(ServiceConnection.class)).isFalse();
                    // The framework reuse strategy, the default, needs the connection details of the module.
                    assertThat(context.getBean(getContainerClass()).getLabels()).doesNotContainKey(DevServiceLabels.DISCOVERABLE);
                });
    }

    @Test
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
