package io.arconia.dev.services.floci;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.floci.testcontainers.FlociContainer;

import io.arconia.dev.services.api.registration.ContainerInfo;
import io.arconia.dev.services.core.registration.DiscoveredContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the connection details of a Floci dev service running in a container
 * discovered from another application.
 */
class FlociDiscoveredConnectionDetailsTests {

    private static final String HOST = "docker.host";

    private static final int MAPPED_PORT = 49152;

    @Test
    void connectionDetailsUseTheDefaultRegion() {
        var connectionDetails = new FlociDiscoveredConnectionDetails(discoveredContainer(), new FlociDevServicesProperties());

        assertThat(connectionDetails.getEndpoint()).hasToString("http://docker.host:49152");
        assertThat(connectionDetails.getRegion()).isEqualTo("us-east-1");
    }

    @Test
    void connectionDetailsUseTheRegionConfiguredThroughTheEnvironment() {
        var properties = new FlociDevServicesProperties();
        properties.setEnvironment(Map.of("FLOCI_DEFAULT_REGION", "eu-west-1"));

        var connectionDetails = new FlociDiscoveredConnectionDetails(discoveredContainer(), properties);

        assertThat(connectionDetails.getRegion()).isEqualTo("eu-west-1");
    }

    private static DiscoveredContainer discoveredContainer() {
        return DiscoveredContainer.builder().containerInfo(ContainerInfo.builder()
                .id("abc123")
                .imageName("floci/floci:latest")
                .ports(List.of(new ContainerInfo.ContainerPort("0.0.0.0", FlociContainer.PORT, MAPPED_PORT, "tcp")))
                .state("running")
                .status("running")
                .build()).host(HOST).build();
    }

}
