package io.arconia.dev.services.oracle.xe;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.api.registration.ContainerInfo;
import io.arconia.dev.services.core.container.ContainerConfigurer;
import io.arconia.dev.services.core.registration.DiscoveredContainer;
import io.arconia.dev.services.oracle.xe.OracleXeDevServicesAutoConfiguration.OracleXeDevServicesRegistrar;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the connection details of an Oracle XE dev service running in a
 * container discovered from another application.
 */
class OracleXeDiscoveredConnectionDetailsTests {

    private static final String HOST = "docker.host";

    private static final int MAPPED_PORT = 49152;

    @Test
    void connectionDetailsMatchTheOnesTheContainerReports() {
        assertThatConnectionDetailsMatchTheContainer(new OracleXeDevServicesProperties());
    }

    @Test
    void connectionDetailsMatchTheOnesTheContainerReportsWithCustomConfiguration() {
        var properties = new OracleXeDevServicesProperties();
        properties.setUsername("myuser");
        properties.setPassword("mypassword");
        properties.setDbName("mydatabase");

        assertThatConnectionDetailsMatchTheContainer(properties);
    }

    private static void assertThatConnectionDetailsMatchTheContainer(OracleXeDevServicesProperties properties) {
        OracleContainer container = reachableContainer(properties);

        JdbcConnectionDetails connectionDetails = OracleXeDevServicesRegistrar
                .discoveredConnectionDetails(discoveredContainer(), properties);

        assertThat(connectionDetails.getJdbcUrl()).isEqualTo(container.getJdbcUrl());
        assertThat(connectionDetails.getUsername()).isEqualTo(container.getUsername());
        assertThat(connectionDetails.getPassword()).isEqualTo(container.getPassword());
    }

    /**
     * A container configured as the dev service would configure it, reporting the address
     * a running container would be reachable at.
     */
    private static OracleContainer reachableContainer(OracleXeDevServicesProperties properties) {
        var container = new OracleContainer(DockerImageName.parse(properties.getImageName())
                .asCompatibleSubstituteFor(ArconiaOracleXeContainer.COMPATIBLE_IMAGE_NAME)) {

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
                .exposedPorts(List.of(new ContainerInfo.ContainerPort("0.0.0.0", ArconiaOracleXeContainer.ORACLE_PORT, MAPPED_PORT, "tcp")))
                .status("running")
                .build(), HOST);
    }

}
