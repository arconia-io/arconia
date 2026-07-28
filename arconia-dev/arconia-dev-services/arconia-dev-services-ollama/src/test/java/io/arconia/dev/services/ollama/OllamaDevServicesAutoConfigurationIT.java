package io.arconia.dev.services.ollama;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;
import org.springframework.ai.model.ollama.autoconfigure.OllamaConnectionDetails;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.utility.TestcontainersConfiguration;

import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.core.container.DevServiceLabels;
import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationIT;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OllamaDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class OllamaDevServicesAutoConfigurationIT extends BaseDevServicesAutoConfigurationIT {

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
    void containerReusedWhenReuseEnabled() {
        getContextRunner()
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .withPropertyValues("arconia.dev.services.ollama.reuse=true")
                .run(context -> {
                    var container = context.getBean(getContainerClass());
                    assertThat(container.isShouldBeReused()).isTrue();
                    // Sharing and reuse compose: a reused container is still advertised as shared.
                    assertThat(container.getLabels()).containsEntry(DevServiceLabels.SHARED, "true");
                    // The owner label is omitted for reusable containers (user labels contribute
                    // to the Testcontainers reuse hash), but only when the environment actually
                    // supports reuse; otherwise it's kept to protect against self-discovery.
                    if (TestcontainersConfiguration.getInstance().environmentSupportsReuse()) {
                        assertThat(container.getLabels()).doesNotContainKey(DevServiceLabels.OWNER);
                    } else {
                        assertThat(container.getLabels()).containsKey(DevServiceLabels.OWNER);
                    }
                });
    }

    @Test
    void containerNotSharedWhenSharingDisabled() {
        getContextRunner()
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .withPropertyValues("arconia.dev.services.ollama.shared=false")
                .run(context -> {
                    var container = context.getBean(getContainerClass());
                    assertThat(container.getLabels()).containsEntry(DevServiceLabels.SHARED, "false");
                });
    }

    @Test
    void sharedContainerDiscoveredWhenStartedByAnotherApplication() {
        OllamaDevServicesProperties properties = new OllamaDevServicesProperties();
        try (OllamaContainer sharedContainer = new OllamaContainer(properties.getImageName())
                .withLabel(DevServiceLabels.NAME, "ollama")
                .withLabel(DevServiceLabels.SHARED, "true")
                .withLabel(DevServiceLabels.OWNER, "another-application")) {
            sharedContainer.start();

            getContextRunner()
                    .withSystemProperties("arconia.bootstrap.mode=dev")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(getContainerClass());
                        assertThat(context).hasSingleBean(OllamaConnectionDetails.class);

                        OllamaConnectionDetails connectionDetails = context.getBean(OllamaConnectionDetails.class);
                        assertThat(connectionDetails.getBaseUrl()).isEqualTo("http://%s:%d".formatted(
                                sharedContainer.getHost(), sharedContainer.getMappedPort(ArconiaOllamaContainer.OLLAMA_PORT)));

                        assertThat(context).hasSingleBean(DevServiceRegistration.class);
                        DevServiceRegistration registration = context.getBean(DevServiceRegistration.class);
                        assertThat(registration.origin()).isEqualTo(DevServiceRegistration.Origin.DISCOVERED);
                        assertThat(registration.containerInfo().get().id()).isEqualTo(sharedContainer.getContainerId());
                    });
        }
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

}
