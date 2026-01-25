package io.arconia.dev.services.lldap;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LldapDevServicesProperties}.
 */
class LldapDevServicesPropertiesTests {

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        LldapDevServicesProperties properties = new LldapDevServicesProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getImageName()).contains("lldap/lldap");
        assertThat(properties.getEnvironment()).isEmpty();
        assertThat(properties.getNetworkAliases()).isEmpty();
        assertThat(properties.getPort()).isEqualTo(0);
        assertThat(properties.isShared()).isFalse();
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(properties.getManagementConsolePort()).isEqualTo(0);
    }

    @Test
    void shouldUpdateValues() {
        LldapDevServicesProperties properties = new LldapDevServicesProperties();

        properties.setEnabled(false);
        properties.setImageName("lldap/lldap:latest");
        properties.setEnvironment(Map.of("KEY", "value"));
        properties.setNetworkAliases(List.of("network1", "network2"));
        properties.setPort(ArconiaLldapContainer.LDAP_PORT);
        properties.setShared(true);
        properties.setStartupTimeout(Duration.ofMinutes(1));
        properties.setManagementConsolePort(ArconiaLldapContainer.UI_PORT);

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getImageName()).isEqualTo("lldap/lldap:latest");
        assertThat(properties.getEnvironment()).containsEntry("KEY", "value");
        assertThat(properties.getNetworkAliases()).containsExactly("network1", "network2");
        assertThat(properties.getPort()).isEqualTo(ArconiaLldapContainer.LDAP_PORT);
        assertThat(properties.isShared()).isTrue();
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(1));
        assertThat(properties.getManagementConsolePort()).isEqualTo(ArconiaLldapContainer.UI_PORT);
    }

}
