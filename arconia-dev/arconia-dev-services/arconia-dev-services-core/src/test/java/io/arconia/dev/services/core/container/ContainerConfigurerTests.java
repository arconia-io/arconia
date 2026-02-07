package io.arconia.dev.services.core.container;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import io.arconia.core.support.Incubating;

import org.junit.jupiter.api.Test;
import org.springframework.util.ReflectionUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import io.arconia.dev.services.api.config.BaseDevServicesProperties;
import io.arconia.dev.services.api.config.JdbcDevServicesProperties;
import io.arconia.dev.services.api.config.ResourceMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ContainerConfigurer}.
 */
@Incubating
class ContainerConfigurerTests {

    @Test
    void baseConfigurationShouldApplyEnvironmentVariables() {
        GenericContainer<?> container = new GenericContainer<>("alpine:latest");
        BaseDevServicesProperties properties = new TestBaseDevServicesProperties()
                .withEnvironment(Map.of("KEY1", "VALUE1", "KEY2", "VALUE2"));

        ContainerConfigurer.base(container, properties);

        assertThat(container.getEnvMap())
                .containsEntry("KEY1", "VALUE1")
                .containsEntry("KEY2", "VALUE2");
    }

    @Test
    void baseConfigurationShouldApplyNetworkAliases() {
        GenericContainer<?> container = new GenericContainer<>("alpine:latest");
        BaseDevServicesProperties properties = new TestBaseDevServicesProperties()
                .withNetworkAliases(List.of("alias1", "alias2", "alias3"));

        ContainerConfigurer.base(container, properties);

        assertThat(container.getNetworkAliases())
                .contains("alias1", "alias2", "alias3");
    }

    @Test
    void baseConfigurationShouldApplyStartupTimeout() {
        GenericContainer<?> container = new GenericContainer<>("alpine:latest");
        Duration customTimeout = Duration.ofMinutes(2);
        BaseDevServicesProperties properties = new TestBaseDevServicesProperties()
                .withStartupTimeout(customTimeout);

        ContainerConfigurer.base(container, properties);

        WaitStrategy waitStrategy = getWaitStrategy(container);
        Duration actualTimeout = getStartupTimeout(waitStrategy);

        assertThat(actualTimeout).isEqualTo(customTimeout);
    }

    /**
     * Helper method to extract the WaitStrategy from a GenericContainer using reflection.
     */
    private WaitStrategy getWaitStrategy(GenericContainer<?> container) {
        Field waitStrategyField = ReflectionUtils.findField(GenericContainer.class, "waitStrategy");
        assertThat(waitStrategyField).isNotNull();
        ReflectionUtils.makeAccessible(waitStrategyField);
        return (WaitStrategy) ReflectionUtils.getField(waitStrategyField, container);
    }

    /**
     * Helper method to extract the startup timeout from a WaitStrategy using reflection.
     */
    private Duration getStartupTimeout(WaitStrategy waitStrategy) {
        Field startupTimeoutField = ReflectionUtils.findField(waitStrategy.getClass(), "startupTimeout");
        assertThat(startupTimeoutField).isNotNull();
        ReflectionUtils.makeAccessible(startupTimeoutField);
        return (Duration) ReflectionUtils.getField(startupTimeoutField, waitStrategy);
    }

    @Test
    void baseConfigurationShouldApplyEmptyEnvironmentVariables() {
        GenericContainer<?> container = new GenericContainer<>("alpine:latest");
        BaseDevServicesProperties properties = new TestBaseDevServicesProperties()
                .withEnvironment(Map.of());

        ContainerConfigurer.base(container, properties);

        assertThat(container.getEnvMap()).isEmpty();
    }

    @Test
    void baseConfigurationShouldApplyEmptyNetworkAliases() {
        GenericContainer<?> container = new GenericContainer<>("alpine:latest");
        BaseDevServicesProperties properties = new TestBaseDevServicesProperties()
                .withNetworkAliases(List.of());

        ContainerConfigurer.base(container, properties);

        assertThat(container.getNetworkAliases()).hasSize(1); // default alias added by Testcontainers
    }

    @Test
    void resourcesConfigurationShouldCopyClasspathResourceWithExplicitPrefix() {
        GenericContainer<?> container = new GenericContainer<>("alpine:latest");
        BaseDevServicesProperties properties = new TestBaseDevServicesProperties()
                .withResources(List.of(
                        new ResourceMapping("classpath:test-resource.txt", "/etc/config/test.txt")
                ));

        ContainerConfigurer.resources(container, properties);

        // Then
        assertThat(container.getCopyToFileContainerPathMap()).isNotEmpty();
        assertThat(container.getCopyToFileContainerPathMap().values())
                .contains("/etc/config/test.txt");
    }

    @Test
    void resourcesConfigurationShouldCopyClasspathResourceWithoutPrefix() {
        GenericContainer<?> container = new GenericContainer<>("alpine:latest");
        BaseDevServicesProperties properties = new TestBaseDevServicesProperties()
                .withResources(List.of(
                        new ResourceMapping("test-resource.txt", "/etc/config/test.txt")
                ));

        ContainerConfigurer.resources(container, properties);

        assertThat(container.getCopyToFileContainerPathMap()).isNotEmpty();
        assertThat(container.getCopyToFileContainerPathMap().values())
                .contains("/etc/config/test.txt");
    }

    @Test
    void resourcesConfigurationShouldCopyMultipleResources() {
        GenericContainer<?> container = new GenericContainer<>("alpine:latest");
        BaseDevServicesProperties properties = new TestBaseDevServicesProperties()
                .withResources(List.of(
                        new ResourceMapping("classpath:test-resource.txt", "/etc/config/test1.txt"),
                        new ResourceMapping("test-resource.txt", "/etc/config/test2.txt")
                ));

        ContainerConfigurer.resources(container, properties);

        assertThat(container.getCopyToFileContainerPathMap()).hasSize(2);
        assertThat(container.getCopyToFileContainerPathMap().values())
                .contains("/etc/config/test1.txt", "/etc/config/test2.txt");
    }

    @Test
    void resourcesConfigurationShouldThrowExceptionWhenSourcePathIsNull() {
        GenericContainer<?> container = new GenericContainer<>("alpine:latest");
        BaseDevServicesProperties properties = new TestBaseDevServicesProperties()
                .withResources(List.of(
                        new ResourceMapping(null, "/etc/config/test.txt")
                ));

        assertThatThrownBy(() -> ContainerConfigurer.resources(container, properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("source path");
    }

    @Test
    void resourcesConfigurationShouldThrowExceptionWhenSourcePathIsEmpty() {
        GenericContainer<?> container = new GenericContainer<>("alpine:latest");
        BaseDevServicesProperties properties = new TestBaseDevServicesProperties()
                .withResources(List.of(
                        new ResourceMapping("", "/etc/config/test.txt")
                ));

        assertThatThrownBy(() -> ContainerConfigurer.resources(container, properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("source path");
    }

    @Test
    void resourcesConfigurationShouldThrowExceptionWhenContainerPathIsNull() {
        GenericContainer<?> container = new GenericContainer<>("alpine:latest");
        BaseDevServicesProperties properties = new TestBaseDevServicesProperties()
                .withResources(List.of(
                        new ResourceMapping("test-resource.txt", null)
                ));

        assertThatThrownBy(() -> ContainerConfigurer.resources(container, properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("container path");
    }

    @Test
    void resourcesConfigurationShouldThrowExceptionWhenContainerPathIsEmpty() {
        GenericContainer<?> container = new GenericContainer<>("alpine:latest");
        BaseDevServicesProperties properties = new TestBaseDevServicesProperties()
                .withResources(List.of(
                        new ResourceMapping("test-resource.txt", "")
                ));

        assertThatThrownBy(() -> ContainerConfigurer.resources(container, properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("container path");
    }

    @Test
    void resourcesConfigurationShouldThrowExceptionWhenResourceNotFound() {
        GenericContainer<?> container = new GenericContainer<>("alpine:latest");
        BaseDevServicesProperties properties = new TestBaseDevServicesProperties()
                .withResources(List.of(
                        new ResourceMapping("non-existent-resource.txt", "/etc/config/test.txt")
                ));

        assertThatThrownBy(() -> ContainerConfigurer.resources(container, properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Resource not found");
    }

    @Test
    void resourcesConfigurationShouldHandleEmptyResourcesList() {
        GenericContainer<?> container = new GenericContainer<>("alpine:latest");
        BaseDevServicesProperties properties = new TestBaseDevServicesProperties()
                .withResources(List.of());

        ContainerConfigurer.resources(container, properties);

        assertThat(container.getCopyToFileContainerPathMap()).isEmpty();
    }

    @Test
    void jdbcConfigurationShouldApplyUsername() {
        JdbcDatabaseContainer<?> container = new PostgreSQLContainer<>("postgres:latest");
        JdbcDevServicesProperties properties = new TestJdbcDevServicesProperties()
                .withUsername("testuser");

        ContainerConfigurer.jdbc(container, properties);

        assertThat(container.getUsername()).isEqualTo("testuser");
    }

    @Test
    void jdbcConfigurationShouldApplyPassword() {
        JdbcDatabaseContainer<?> container = new PostgreSQLContainer<>("postgres:latest");
        JdbcDevServicesProperties properties = new TestJdbcDevServicesProperties()
                .withPassword("testpassword");

        ContainerConfigurer.jdbc(container, properties);

        assertThat(container.getPassword()).isEqualTo("testpassword");
    }

    @Test
    void jdbcConfigurationShouldApplyDatabaseName() {
        JdbcDatabaseContainer<?> container = new PostgreSQLContainer<>("postgres:latest");
        JdbcDevServicesProperties properties = new TestJdbcDevServicesProperties()
                .withDbName("testdb");

        ContainerConfigurer.jdbc(container, properties);

        assertThat(container.getDatabaseName()).isEqualTo("testdb");
    }

    @Test
    void jdbcConfigurationShouldApplyInitScripts() {
        JdbcDatabaseContainer<?> container = new PostgreSQLContainer<>("postgres:latest");
        JdbcDevServicesProperties properties = new TestJdbcDevServicesProperties()
                .withInitScriptPaths(List.of("init1.sql", "init2.sql"));

        ContainerConfigurer.jdbc(container, properties);

        String[] initScripts = getInitScripts(container);
        assertThat(initScripts)
                .containsExactly("init1.sql", "init2.sql");
    }
    @Test
    void jdbcConfigurationShouldHandleEmptyInitScripts() {
        JdbcDatabaseContainer<?> container = new PostgreSQLContainer<>("postgres:latest");
        JdbcDevServicesProperties properties = new TestJdbcDevServicesProperties()
                .withInitScriptPaths(List.of());

        assertThatCode(() -> ContainerConfigurer.jdbc(container, properties))
                .doesNotThrowAnyException();

        String[] initScripts = getInitScripts(container);
        assertThat(initScripts).isEmpty();
    }

    /**
     * Helper method to extract the init scripts from a JdbcDatabaseContainer using reflection.
     */
    private String[] getInitScripts(JdbcDatabaseContainer<?> container) {
        Field initScriptsField = ReflectionUtils.findField(JdbcDatabaseContainer.class, "initScriptPaths");
        assertThat(initScriptsField).isNotNull();
        ReflectionUtils.makeAccessible(initScriptsField);
        @SuppressWarnings("unchecked")
        List<String> scripts = (List<String>) ReflectionUtils.getField(initScriptsField, container);
        return scripts != null ? scripts.toArray(new String[0]) : new String[0];
    }

    private static class TestBaseDevServicesProperties implements BaseDevServicesProperties {
        private Map<String, String> environment = Map.of();
        private List<String> networkAliases = List.of();
        private Duration startupTimeout = Duration.ofSeconds(30);
        private List<ResourceMapping> resources = List.of();
        private boolean shared = false;

        @Override
        public String getImageName() {
            return "test-image:latest";
        }

        @Override
        public Map<String, String> getEnvironment() {
            return environment;
        }

        public TestBaseDevServicesProperties withEnvironment(Map<String, String> environment) {
            this.environment = environment;
            return this;
        }

        @Override
        public List<String> getNetworkAliases() {
            return networkAliases;
        }

        public TestBaseDevServicesProperties withNetworkAliases(List<String> networkAliases) {
            this.networkAliases = networkAliases;
            return this;
        }

        @Override
        public Duration getStartupTimeout() {
            return startupTimeout;
        }

        public TestBaseDevServicesProperties withStartupTimeout(Duration startupTimeout) {
            this.startupTimeout = startupTimeout;
            return this;
        }

        @Override
        public List<ResourceMapping> getResources() {
            return resources;
        }

        public TestBaseDevServicesProperties withResources(List<ResourceMapping> resources) {
            this.resources = resources;
            return this;
        }

        @Override
        public boolean isShared() {
            return shared;
        }

        public TestBaseDevServicesProperties withShared(boolean shared) {
            this.shared = shared;
            return this;
        }
    }

    private static class TestJdbcDevServicesProperties implements JdbcDevServicesProperties {
        private String username = "user";
        private String password = "password";
        private String dbName = "testdb";
        private List<String> initScriptPaths = List.of();

        @Override
        public String getImageName() {
            return "test-db:latest";
        }

        @Override
        public String getUsername() {
            return username;
        }

        public TestJdbcDevServicesProperties withUsername(String username) {
            this.username = username;
            return this;
        }

        @Override
        public String getPassword() {
            return password;
        }

        public TestJdbcDevServicesProperties withPassword(String password) {
            this.password = password;
            return this;
        }

        @Override
        public String getDbName() {
            return dbName;
        }

        public TestJdbcDevServicesProperties withDbName(String dbName) {
            this.dbName = dbName;
            return this;
        }

        @Override
        public List<String> getInitScriptPaths() {
            return initScriptPaths;
        }

        public TestJdbcDevServicesProperties withInitScriptPaths(List<String> initScriptPaths) {
            this.initScriptPaths = initScriptPaths;
            return this;
        }
    }

}
