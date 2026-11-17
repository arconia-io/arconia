package io.arconia.dev.services.floci;

import java.net.URI;
import java.util.List;

import io.awspring.cloud.autoconfigure.core.AwsConnectionDetails;
import io.floci.testcontainers.FlociContainer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.testcontainers.containers.GenericContainer;

import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.core.container.DevServiceContainerCustomizer;
import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationTests;
import io.arconia.dev.services.tests.DisabledIfDockerUnavailable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FlociDevServicesAutoConfiguration}.
 */
class FlociDevServicesAutoConfigurationTests extends BaseDevServicesAutoConfigurationTests {

    FlociDevServicesAutoConfigurationTests() {
        super(FlociDevServicesAutoConfiguration.class, "floci", FlociContainer.class, AwsConnectionDetails.class);
    }

    @Override
    protected GenericContainer<?> createDiscoverableContainer(String ownerId) {
        var properties = new FlociDevServicesProperties();
        return asDiscoverableContainer(FlociDevServicesAutoConfiguration.createContainer(properties), properties, ownerId);
    }

    @Override
    protected void assertDiscoveredConnectionDetails(AssertableApplicationContext context, GenericContainer<?> discoveredContainer) {
        FlociContainer flociContainer = (FlociContainer) discoveredContainer;
        AwsConnectionDetails connectionDetails = context.getBean(AwsConnectionDetails.class);
        assertThat(connectionDetails.getEndpoint()).isEqualTo(URI.create(flociContainer.getEndpoint()));
        assertThat(connectionDetails.getRegion()).isEqualTo(flociContainer.getRegion());
        assertThat(connectionDetails.getAccessKey()).isEqualTo(flociContainer.getAccessKey());
        assertThat(connectionDetails.getSecretKey()).isEqualTo(flociContainer.getSecretKey());
    }

    @Test
    @DisabledIfDockerUnavailable
    void selinuxLabelsDisabledWhenDockerSocketIsMounted() {
        contextRunnerWithContainerLifecycle().run(context -> {
            var hostConfig = context.getBean(FlociContainer.class).getContainerInfo().getHostConfig();
            assertThat(hostConfig.getBinds()).anyMatch(bind -> bind.getVolume().getPath().equals("/var/run/docker.sock"));
            assertThat(hostConfig.getSecurityOpts()).contains("label=disable");
        });
    }

    @Test
    @DisabledIfDockerUnavailable
    void selinuxLabelsNotDisabledWithoutDockerSocket() {
        contextRunnerWithContainerLifecycle()
                .withBean(DockerSocketCustomizer.class)
                .run(context -> {
                    var hostConfig = context.getBean(FlociContainer.class).getContainerInfo().getHostConfig();
                    assertThat(hostConfig.getSecurityOpts()).isNullOrEmpty();
                });
    }

    @Test
    @DisabledIfDockerUnavailable
    void devServiceLinksExposeFlociUi() {
        contextRunnerWithContainerLifecycle().run(context -> {
            var container = context.getBean(getContainerClass());
            List<DevServiceLink> links = context.getBean(DevServiceRegistration.class).getLinks();
            assertThat(links).extracting(DevServiceLink::label).containsExactly("Floci UI");
            assertThat(links).extracting(DevServiceLink::url).containsExactly(
                    "http://" + container.getHost() + ":" + container.getMappedPort(FlociContainer.PORT) + "/_floci/ui");
        });
    }

    static class DockerSocketCustomizer implements DevServiceContainerCustomizer<FlociContainer> {

        @Override
        public void customize(FlociContainer container) {
            container.withDockerSocket(false);
        }

    }

}
