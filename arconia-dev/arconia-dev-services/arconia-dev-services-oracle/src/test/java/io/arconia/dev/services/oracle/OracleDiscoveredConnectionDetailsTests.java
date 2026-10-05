package io.arconia.dev.services.oracle;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.api.registration.ContainerInfo;
import io.arconia.dev.services.core.container.ContainerConfigurer;
import io.arconia.dev.services.core.registration.DiscoveredContainer;
import io.arconia.dev.services.oracle.OracleDevServicesAutoConfiguration.OracleDevServicesRegistrar;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the connection details of an Oracle dev service running in a
 * container discovered from another application.
 */
class OracleDiscoveredConnectionDetailsTests {

    private static final String HOST = "docker.host";

    private static final int MAPPED_PORT = 49152;

    @Test
    void connectionDetailsMatchTheOnesTheContainerReports() {
        assertThatConnectionDetailsMatchTheContainer(new OracleDevServicesProperties());
    }

    @Test
    void connectionDetailsMatchTheOnesTheContainerReportsWithCustomConfiguration() {
        var properties = new OracleDevServicesProperties();
        properties.setUsername("myuser");
        properties.setPassword("mypassword");
        properties.setDbName("mydatabase");

        assertThatConnectionDetailsMatchTheContainer(properties);
    }

    private static void assertThatConnectionDetailsMatchTheContainer(OracleDevServicesProperties properties) {
        OracleContainer container = reachableContainer(properties);

        JdbcConnectionDetails connectionDetails = OracleDevServicesRegistrar
                .discoveredConnectionDetails(discoveredContainer(), properties);

        assertThat(connectionDetails.getJdbcUrl()).isEqualTo(container.getJdbcUrl());
        assertThat(connectionDetails.getUsername()).isEqualTo(container.getUsername());
        assertThat(connectionDetails.getPassword()).isEqualTo(container.getPassword());
    }

    /**
     * A container configured as the dev service would configure it, reporting the address
     * a running container would be reachable at.
     */
    private static OracleContainer reachableContainer(OracleDevServicesProperties properties) {
        var container = new OracleContainer(DockerImageName.parse(properties.getImageName())
                .asCompatibleSubstituteFor(ArconiaOracleContainer.COMPATIBLE_IMAGE_NAME)) {

            @Override
            public String getHost() {
                return HOST;
            }

            @Override
            public Integer getOraclePort() {
                return MAPPED_PORT;
            }

        };
        ContainerConfigurer.jdbc(container, properties);
        return container;
    }

    private static DiscoveredContainer discoveredContainer() {
        return new DiscoveredContainer(ContainerInfo.builder()
                .id("abc123")
                .imageName("oracle:latest")
                .exposedPorts(List.of(new ContainerInfo.ContainerPort("0.0.0.0", ArconiaOracleContainer.ORACLE_PORT, MAPPED_PORT, "tcp")))
                .status("running")
                .build(), HOST);
    }

}
