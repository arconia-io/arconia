package io.arconia.dev.services.ollama;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;
import org.springframework.ai.model.ollama.autoconfigure.OllamaConnectionDetails;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.ollama.OllamaContainer;

import io.arconia.dev.services.api.registration.DevServiceLabels;
import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationIT;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OllamaDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class OllamaDevServicesAutoConfigurationIT extends BaseDevServicesAutoConfigurationIT {

    /**
     * Two variants of a very small embedding model, keeping the pulls cheap
     * while still exercising more than one model.
     */
    private static final String TEST_MODEL = "all-minilm:22m";

    private static final String OTHER_TEST_MODEL = "all-minilm:33m";

    private final ApplicationContextRunner contextRunner = defaultContextRunner(OllamaDevServicesAutoConfiguration.class)
            .withPropertyValues("arconia.dev.services.ollama.ignore-native-service=true");

    @Override
    protected ApplicationContextRunner getContextRunner() {
        return contextRunner;
    }

    @Override
    protected Class<?> getAutoConfigurationClass() {
        return OllamaDevServicesAutoConfiguration.class;
    }

    @Override
    protected Class<? extends GenericContainer<?>> getContainerClass() {
        return OllamaContainer.class;
    }

    @Override
    protected String getServiceName() {
        return "ollama";
    }

    @Override
    protected Class<?> getConnectionDetailsClass() {
        return OllamaConnectionDetails.class;
    }

    @Override
    protected boolean supportsSharing() {
        return true;
    }

    @Override
    protected GenericContainer<?> createSharedContainer(String ownerId) {
        OllamaDevServicesProperties properties = new OllamaDevServicesProperties();
        return withSharedLabels(new OllamaContainer(properties.getImageName()), ownerId);
    }

    @Override
    protected void assertDiscoveredConnectionDetails(AssertableApplicationContext context, GenericContainer<?> sharedContainer) {
        OllamaConnectionDetails connectionDetails = context.getBean(OllamaConnectionDetails.class);
        assertThat(connectionDetails.getBaseUrl()).isEqualTo("http://%s:%d".formatted(
                sharedContainer.getHost(), sharedContainer.getMappedPort(ArconiaOllamaContainer.OLLAMA_PORT)));
    }

    @Test
    void containerActivatedWhenEnabled() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .run(context -> {
                    assertThat(context).hasSingleBean(getContainerClass());
                    var container = context.getBean(getContainerClass());
                    assertThat(container.getDockerImageName()).contains("ollama/ollama");
                    assertThat(container.getEnv()).isEmpty();
                    assertThat(container.getNetworkAliases()).hasSize(1);
                    assertThat(container.isShouldBeReused()).isFalse();
                    assertThat(container.getLabels())
                            .containsEntry(DevServiceLabels.NAME, "ollama")
                            .containsEntry(DevServiceLabels.SHARED, "true")
                            .containsEntry(DevServiceLabels.OWNER, DevServiceLabels.ownerId());

                    assertThatHasSingletonScope(context);
                });
    }

    @Test
    void containerConfigurationApplied() {
        String[] properties = ArrayUtils.addAll(commonConfigurationProperties());

        contextRunner
                .withPropertyValues(properties)
                .run(context -> {
                    var container = context.getBean(getContainerClass());
                    container.start();
                    assertThatConfigurationIsApplied(container);
                    container.stop();
                });
    }

    @Test
    void modelsPulledWhenConfigured() {
        contextRunner
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
        contextRunner
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
