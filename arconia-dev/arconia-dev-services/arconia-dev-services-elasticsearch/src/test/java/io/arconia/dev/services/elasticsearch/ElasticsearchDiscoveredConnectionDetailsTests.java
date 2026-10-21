package io.arconia.dev.services.elasticsearch;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchConnectionDetails;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import io.arconia.dev.services.api.registration.ContainerInfo;
import io.arconia.dev.services.core.registration.DiscoveredContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ElasticsearchDiscoveredConnectionDetails}.
 */
class ElasticsearchDiscoveredConnectionDetailsTests {

    private static final String HOST = "docker.host";

    private static final int MAPPED_PORT = 49152;

    @Test
    void connectionDetailsUseTheDefaultPassword() {
        ElasticsearchConnectionDetails connectionDetails = new ElasticsearchDiscoveredConnectionDetails(discoveredContainer(), new ElasticsearchDevServicesProperties());

        assertThat(connectionDetails.getUsername()).isEqualTo("elastic");
        assertThat(connectionDetails.getPassword()).isEqualTo(ElasticsearchContainer.ELASTICSEARCH_DEFAULT_PASSWORD);
        assertThat(connectionDetails.getNodes()).singleElement().satisfies(node -> {
            assertThat(node.hostname()).isEqualTo(HOST);
            assertThat(node.port()).isEqualTo(MAPPED_PORT);
            assertThat(node.protocol()).isEqualTo(ElasticsearchConnectionDetails.Node.Protocol.HTTP);
        });
    }

    @Test
    void connectionDetailsUseThePasswordConfiguredThroughTheEnvironment() {
        var properties = new ElasticsearchDevServicesProperties();
        properties.setEnvironment(Map.of("ELASTIC_PASSWORD", "secret"));

        ElasticsearchConnectionDetails connectionDetails = new ElasticsearchDiscoveredConnectionDetails(discoveredContainer(), properties);

        assertThat(connectionDetails.getPassword()).isEqualTo("secret");
    }

    private static DiscoveredContainer discoveredContainer() {
        return DiscoveredContainer.builder().containerInfo(ContainerInfo.builder()
                .id("abc123")
                .imageName("elasticsearch:latest")
                .ports(List.of(new ContainerInfo.ContainerPort("0.0.0.0", ElasticsearchDevServicesAutoConfiguration.ELASTICSEARCH_DEFAULT_PORT, MAPPED_PORT, "tcp")))
                .state("running")
                .status("running")
                .build()).host(HOST).build();
    }

}
