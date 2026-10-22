package io.arconia.dev.services.lldap;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.api.registration.ContainerInfo;
import io.arconia.dev.services.core.registration.DiscoveredContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the connection details of an LLDAP dev service running in a container
 * discovered from another application.
 */
class LldapDiscoveredConnectionDetailsTests {

    private static final String HOST = "docker.host";

    private static final int MAPPED_PORT = 49152;

    @Test
    void connectionDetailsUseTheDefaultConfiguration() {
        var connectionDetails = new LldapDiscoveredConnectionDetails(discoveredContainer(), new LldapDevServicesProperties());

        assertThat(connectionDetails.getUrls()).containsExactly("ldap://docker.host:49152");
        assertThat(connectionDetails.getBase()).isEqualTo("dc=example,dc=com");
        assertThat(connectionDetails.getPassword()).isEqualTo(LldapDevServicesAutoConfiguration.ADMIN_PASSWORD);
    }

    @Test
    void connectionDetailsUseTheConfigurationSetThroughTheEnvironment() {
        var properties = new LldapDevServicesProperties();
        properties.setEnvironment(Map.of("LLDAP_LDAP_BASE_DN", "dc=acme,dc=org", "LLDAP_LDAP_USER_PASS", "secret-password"));

        var connectionDetails = new LldapDiscoveredConnectionDetails(discoveredContainer(), properties);

        assertThat(connectionDetails.getBase()).isEqualTo("dc=acme,dc=org");
        assertThat(connectionDetails.getUsername()).isEqualTo("cn=admin,ou=people,dc=acme,dc=org");
        assertThat(connectionDetails.getPassword()).isEqualTo("secret-password");
    }

    private static DiscoveredContainer discoveredContainer() {
        return DiscoveredContainer.builder().containerInfo(ContainerInfo.builder()
                .id("abc123")
                .imageName("lldap/lldap:latest")
                .ports(List.of(new ContainerInfo.ContainerPort("0.0.0.0", LldapDevServicesAutoConfiguration.LDAP_PORT, MAPPED_PORT, "tcp")))
                .state("running")
                .status("running")
                .build()).host(HOST).build();
    }

}
