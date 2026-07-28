package io.arconia.dev.services.rabbitmq;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.amqp.autoconfigure.RabbitConnectionDetails;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.TestcontainersConfiguration;

import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.core.container.DevServiceLabels;
import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationIT;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link RabbitMqDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class RabbitMqDevServicesAutoConfigurationIT extends BaseDevServicesAutoConfigurationIT {

    private final ApplicationContextRunner contextRunner = defaultContextRunner(RabbitMqDevServicesAutoConfiguration.class);

    @Override
    protected ApplicationContextRunner getContextRunner() {
        return contextRunner;
    }

    @Override
    protected Class<?> getAutoConfigurationClass() {
        return RabbitMqDevServicesAutoConfiguration.class;
    }

    @Override
    protected Class<? extends GenericContainer<?>> getContainerClass() {
        return RabbitMQContainer.class;
    }

    @Override
    protected String getServiceName() {
        return "rabbitmq";
    }

    @Override
    protected Class<?> getConnectionDetailsClass() {
        return RabbitConnectionDetails.class;
    }

    @Test
    void containerAvailableInDevMode() {
        getContextRunner()
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .run(context -> {
                    assertThat(context).hasSingleBean(getContainerClass());
                    var container = (RabbitMQContainer) context.getBean(getContainerClass());
                    assertThat(container.getDockerImageName()).contains(ArconiaRabbitMqContainer.COMPATIBLE_IMAGE_NAME);
                    assertThat(container.getEnv()).isEmpty();
                    assertThat(container.getNetworkAliases()).hasSize(1);
                    assertThat(container.isShouldBeReused()).isFalse();
                    assertThat(container.getAdminUsername()).isEqualTo(RabbitMqDevServicesProperties.DEFAULT_USERNAME);
                    assertThat(container.getAdminPassword()).isEqualTo(RabbitMqDevServicesProperties.DEFAULT_PASSWORD);
                    assertThat(container.getLabels())
                            .containsEntry(DevServiceLabels.NAME, "rabbitmq")
                            .containsEntry(DevServiceLabels.SHARED, "true")
                            .containsEntry(DevServiceLabels.OWNER, DevServiceLabels.ownerId());

                    assertThatHasSingletonScope(context);
                });
    }

    @Test
    void containerReusedWhenReuseEnabled() {
        getContextRunner()
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .withPropertyValues("arconia.dev.services.rabbitmq.reuse=true")
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
                .withPropertyValues("arconia.dev.services.rabbitmq.shared=false")
                .run(context -> {
                    var container = context.getBean(getContainerClass());
                    assertThat(container.getLabels()).containsEntry(DevServiceLabels.SHARED, "false");
                });
    }

    @Test
    void sharedContainerDiscoveredWhenStartedByAnotherApplication() {
        RabbitMqDevServicesProperties properties = new RabbitMqDevServicesProperties();
        try (RabbitMQContainer sharedContainer = new RabbitMQContainer(properties.getImageName())
                .withAdminUser(properties.getUsername())
                .withAdminPassword(properties.getPassword())
                .withLabel(DevServiceLabels.NAME, "rabbitmq")
                .withLabel(DevServiceLabels.SHARED, "true")
                .withLabel(DevServiceLabels.OWNER, "another-application")) {
            sharedContainer.start();

            getContextRunner()
                    .withSystemProperties("arconia.bootstrap.mode=dev")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(getContainerClass());
                        assertThat(context).hasSingleBean(RabbitConnectionDetails.class);

                        RabbitConnectionDetails connectionDetails = context.getBean(RabbitConnectionDetails.class);
                        assertThat(connectionDetails.getFirstAddress().port())
                                .isEqualTo(sharedContainer.getMappedPort(ArconiaRabbitMqContainer.AMQP_PORT));
                        assertThat(connectionDetails.getUsername()).isEqualTo(properties.getUsername());
                        assertThat(connectionDetails.getPassword()).isEqualTo(properties.getPassword());

                        assertThat(context).hasSingleBean(DevServiceRegistration.class);
                        DevServiceRegistration registration = context.getBean(DevServiceRegistration.class);
                        assertThat(registration.origin()).isEqualTo(DevServiceRegistration.Origin.DISCOVERED);
                        assertThat(registration.containerInfo().get().id()).isEqualTo(sharedContainer.getContainerId());
                    });
        }
    }

    @Test
    void oldestSharedContainerDiscoveredWhenMultipleAvailable() throws Exception {
        RabbitMqDevServicesProperties properties = new RabbitMqDevServicesProperties();
        try (RabbitMQContainer olderContainer = new RabbitMQContainer(properties.getImageName())
                .withLabel(DevServiceLabels.NAME, "rabbitmq")
                .withLabel(DevServiceLabels.SHARED, "true")
                .withLabel(DevServiceLabels.OWNER, "another-application");
             RabbitMQContainer newerContainer = new RabbitMQContainer(properties.getImageName())
                .withLabel(DevServiceLabels.NAME, "rabbitmq")
                .withLabel(DevServiceLabels.SHARED, "true")
                .withLabel(DevServiceLabels.OWNER, "yet-another-application")) {
            olderContainer.start();
            // Container creation timestamps have second granularity.
            Thread.sleep(1100);
            newerContainer.start();

            getContextRunner()
                    .withSystemProperties("arconia.bootstrap.mode=dev")
                    .run(context -> {
                        DevServiceRegistration registration = context.getBean(DevServiceRegistration.class);
                        assertThat(registration.containerInfo().get().id()).isEqualTo(olderContainer.getContainerId());
                    });
        }
    }

    @Test
    void pausedSharedContainerNotDiscovered() {
        RabbitMqDevServicesProperties properties = new RabbitMqDevServicesProperties();
        try (RabbitMQContainer pausedContainer = new RabbitMQContainer(properties.getImageName())
                .withLabel(DevServiceLabels.NAME, "rabbitmq")
                .withLabel(DevServiceLabels.SHARED, "true")
                .withLabel(DevServiceLabels.OWNER, "another-application")) {
            pausedContainer.start();
            DockerClientFactory.lazyClient().pauseContainerCmd(pausedContainer.getContainerId()).exec();

            try {
                getContextRunner()
                        .withSystemProperties("arconia.bootstrap.mode=dev")
                        .run(context -> {
                            assertThat(context).hasSingleBean(getContainerClass());
                            assertThat(context.getBean(DevServiceRegistration.class).origin())
                                    .isEqualTo(DevServiceRegistration.Origin.OWNED);
                        });
            } finally {
                DockerClientFactory.lazyClient().unpauseContainerCmd(pausedContainer.getContainerId()).exec();
            }
        }
    }

    @Test
    void ownSharedContainerNotDiscovered() {
        RabbitMqDevServicesProperties properties = new RabbitMqDevServicesProperties();
        try (RabbitMQContainer ownContainer = new RabbitMQContainer(properties.getImageName())
                .withLabel(DevServiceLabels.NAME, "rabbitmq")
                .withLabel(DevServiceLabels.SHARED, "true")
                .withLabel(DevServiceLabels.OWNER, DevServiceLabels.ownerId())) {
            ownContainer.start();

            getContextRunner()
                    .withSystemProperties("arconia.bootstrap.mode=dev")
                    .run(context -> {
                        assertThat(context).hasSingleBean(getContainerClass());
                        assertThat(context.getBean(DevServiceRegistration.class).origin())
                                .isEqualTo(DevServiceRegistration.Origin.OWNED);
                    });
        }
    }

    @Test
    void containerConfigurationApplied() {
        String[] properties = ArrayUtils.addAll(commonConfigurationProperties(),
                "arconia.dev.services.%s.username=myusername".formatted(getServiceName()),
                "arconia.dev.services.%s.password=mypassword".formatted(getServiceName())
        );

        getContextRunner()
            .withPropertyValues(properties)
            .run(context -> {
                var container = (RabbitMQContainer) context.getBean(getContainerClass());
                container.start();
                assertThatConfigurationIsApplied(container);
                assertThat(container.getAdminUsername()).isEqualTo("myusername");
                assertThat(container.getAdminPassword()).isEqualTo("mypassword");
                container.stop();
            });
    }

}
