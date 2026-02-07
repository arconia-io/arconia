package io.arconia.dev.services.oracle;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.api.config.ResourceMapping;

import static io.arconia.dev.services.oracle.OracleDevServicesProperties.DEFAULT_DB_NAME;
import static io.arconia.dev.services.oracle.OracleDevServicesProperties.DEFAULT_PASSWORD;
import static io.arconia.dev.services.oracle.OracleDevServicesProperties.DEFAULT_USERNAME;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OracleDevServicesProperties}.
 */
class OracleDevServicesPropertiesTests {

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        OracleDevServicesProperties properties = new OracleDevServicesProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getImageName()).contains("gvenzl/oracle-free");
        assertThat(properties.getEnvironment()).isEmpty();
        assertThat(properties.getNetworkAliases()).isEmpty();
        assertThat(properties.getPort()).isEqualTo(0);
        assertThat(properties.getResources()).isEmpty();
        assertThat(properties.isShared()).isFalse();
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(2));

        assertThat(properties.getUsername()).isEqualTo(DEFAULT_USERNAME);
        assertThat(properties.getPassword()).isEqualTo(DEFAULT_PASSWORD);
        assertThat(properties.getDbName()).isEqualTo(DEFAULT_DB_NAME);
        assertThat(properties.getInitScriptPaths()).isEmpty();
    }

    @Test
    void shouldUpdateValues() {
        OracleDevServicesProperties properties = new OracleDevServicesProperties();

        properties.setEnabled(false);
        properties.setImageName("gvenzl/oracle-free:latest");
        properties.setEnvironment(Map.of("KEY", "value"));
        properties.setNetworkAliases(List.of("network1", "network2"));
        properties.setPort(ArconiaOracleContainer.ORACLE_PORT);
        properties.setResources(List.of(new ResourceMapping("test-resource.txt", "/tmp/test-resource.txt")));
        properties.setShared(true);
        properties.setStartupTimeout(Duration.ofMinutes(1));

        properties.setUsername("mytest");
        properties.setPassword("mytest");
        properties.setDbName("mytest");
        properties.setInitScriptPaths(List.of("init.sql"));

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getImageName()).isEqualTo("gvenzl/oracle-free:latest");
        assertThat(properties.getEnvironment()).containsEntry("KEY", "value");
        assertThat(properties.getNetworkAliases()).containsExactly("network1", "network2");
        assertThat(properties.getPort()).isEqualTo(ArconiaOracleContainer.ORACLE_PORT);
        assertThat(properties.getResources()).hasSize(1);
        assertThat(properties.getResources().getFirst().getSourcePath()).isEqualTo("test-resource.txt");
        assertThat(properties.getResources().getFirst().getContainerPath()).isEqualTo("/tmp/test-resource.txt");
        assertThat(properties.isShared()).isTrue();
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(1));

        assertThat(properties.getUsername()).isEqualTo("mytest");
        assertThat(properties.getPassword()).isEqualTo("mytest");
        assertThat(properties.getDbName()).isEqualTo("mytest");
        assertThat(properties.getInitScriptPaths()).containsExactly("init.sql");
    }

}
