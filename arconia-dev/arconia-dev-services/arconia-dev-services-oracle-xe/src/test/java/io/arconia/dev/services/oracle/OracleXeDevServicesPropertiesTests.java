package io.arconia.dev.services.oracle;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OracleXeDevServicesProperties}.
 */
class OracleXeDevServicesPropertiesTests {

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        OracleXeDevServicesProperties properties = new OracleXeDevServicesProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getImageName()).contains("gvenzl/oracle-xe");
        assertThat(properties.getEnvironment()).isEmpty();
        assertThat(properties.getNetworkAliases()).isEmpty();
        assertThat(properties.getPort()).isEqualTo(0);
        assertThat(properties.isShared()).isFalse();
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(2));

        assertThat(properties.getUsername()).isEqualTo("test");
        assertThat(properties.getPassword()).isEqualTo("test");
        assertThat(properties.getDbName()).isEqualTo("test");
        assertThat(properties.getInitScriptPaths()).isEmpty();
    }

    @Test
    void shouldUpdateValues() {
        OracleXeDevServicesProperties properties = new OracleXeDevServicesProperties();

        properties.setEnabled(false);
        properties.setImageName("gvenzl/oracle-xe:latest");
        properties.setEnvironment(Map.of("KEY", "value"));
        properties.setNetworkAliases(List.of("network1", "network2"));
        properties.setPort(ArconiaOracleXeContainer.ORACLE_PORT);
        properties.setShared(true);
        properties.setStartupTimeout(Duration.ofMinutes(1));

        properties.setUsername("mytest");
        properties.setPassword("mytest");
        properties.setDbName("mytest");
        properties.setInitScriptPaths(List.of("init.sql"));

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getImageName()).isEqualTo("gvenzl/oracle-xe:latest");
        assertThat(properties.getEnvironment()).containsEntry("KEY", "value");
        assertThat(properties.getNetworkAliases()).containsExactly("network1", "network2");
        assertThat(properties.getPort()).isEqualTo(ArconiaOracleXeContainer.ORACLE_PORT);
        assertThat(properties.isShared()).isTrue();
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(1));

        assertThat(properties.getUsername()).isEqualTo("mytest");
        assertThat(properties.getPassword()).isEqualTo("mytest");
        assertThat(properties.getDbName()).isEqualTo("mytest");
        assertThat(properties.getInitScriptPaths()).containsExactly("init.sql");
    }

}
