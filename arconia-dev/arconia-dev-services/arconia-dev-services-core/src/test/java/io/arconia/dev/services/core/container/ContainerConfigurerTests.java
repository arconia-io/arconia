package io.arconia.dev.services.core.container;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.ReflectionUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

import io.arconia.core.support.Incubating;
import io.arconia.dev.services.api.config.BaseDevServicesProperties;
import io.arconia.dev.services.api.config.JdbcDevServicesProperties;
import io.arconia.dev.services.api.config.ResourceMapping;
import io.arconia.dev.services.api.config.ReuseStrategy;
import io.arconia.dev.services.api.config.VolumeMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ContainerConfigurer}.
 */
@Incubating
class ContainerConfigurerTests {

    @Test
    void applyShouldConfigureTheContainerFromTheProperties() {
        GenericContainer<?> container = new GenericContainer<>("alpine:latest");
        // The container must own its wait strategy before the startup timeout is applied to it:
        // see the Javadoc of ContainerConfigurer.apply.
        container.waitingFor(Wait.defaultWaitStrategy());
        BaseDevServicesProperties properties = new TestBaseDevServicesProperties()
                .withEnvironment(Map.of("KEY1", "VALUE1", "KEY2", "VALUE2"))
                .withNetworkAliases(List.of("alias1", "alias2"))
                .withStartupTimeout(Duration.ofMinutes(2))
                .withResources(List.of(
                        new ResourceMapping("classpath:test-resource.txt", "/etc/config/test1.txt"),
                        new ResourceMapping("test-resource.txt", "/etc/config/test2.txt")))
                .withVolumes(List.of(new VolumeMapping("/host/path", "/container/path")));

        ContainerConfigurer.apply(container, properties, true);

        assertThat(container.getEnvMap()).containsEntry("KEY1", "VALUE1").containsEntry("KEY2", "VALUE2");
        assertThat(container.getNetworkAliases()).contains("alias1", "alias2");
        assertThat(getStartupTimeout(getWaitStrategy(container))).isEqualTo(Duration.ofMinutes(2));
        assertThat(container.getCopyToFileContainerPathMap().values()).contains("/etc/config/test1.txt", "/etc/config/test2.txt");
        assertThat(container.getBinds()).singleElement().satisfies(bind -> {
            assertThat(bind.getPath()).isEqualTo("/host/path");
            assertThat(bind.getVolume().getPath()).isEqualTo("/container/path");
            assertThat(bind.getAccessMode().toString()).isEqualTo("rw");
        });
        assertThat(container.isShouldBeReused()).isTrue();
    }

    @Test
    void applyShouldFailWhenAResourceIsNotFound() {
        GenericContainer<?> container = new GenericContainer<>("alpine:latest");
        BaseDevServicesProperties properties = new TestBaseDevServicesProperties()
                .withResources(List.of(new ResourceMapping("non-existent-resource.txt", "/etc/config/test.txt")));

        assertThatThrownBy(() -> ContainerConfigurer.apply(container, properties, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Resource not found");
    }

    @Test
    void applyShouldConfigureAJdbcContainerFromJdbcProperties() {
        JdbcDatabaseContainer<?> container = new PostgreSQLContainer("postgres:latest");
        JdbcDevServicesProperties properties = new TestJdbcDevServicesProperties()
                .withUsername("testuser")
                .withPassword("testpass")
                .withDbName("testdb")
                .withInitScriptPaths(List.of("init1.sql", "init2.sql"));

        ContainerConfigurer.apply(container, properties, false);

        assertThat(container.getUsername()).isEqualTo("testuser");
        assertThat(container.getPassword()).isEqualTo("testpass");
        assertThat(container.getDatabaseName()).isEqualTo("testdb");
        assertThat(getInitScripts(container)).containsExactly("init1.sql", "init2.sql");
    }

    @Test
    void reuseShouldFollowTheGivenDecision() {
        GenericContainer<?> container = new GenericContainer<>("alpine:latest");
        BaseDevServicesProperties properties = new TestBaseDevServicesProperties();

        ContainerConfigurer.apply(container, properties, true);
        assertThat(container.isShouldBeReused()).isTrue();

        ContainerConfigurer.apply(container, properties, false);
        assertThat(container.isShouldBeReused()).isFalse();
    }

    /**
     * Helper method to extract the WaitStrategy from a GenericContainer using reflection.
     */
    private WaitStrategy getWaitStrategy(GenericContainer<?> container) {
        Method waitStrategyMethod = ReflectionUtils.findMethod(GenericContainer.class, "getWaitStrategy");
        assertThat(waitStrategyMethod).isNotNull();
        ReflectionUtils.makeAccessible(waitStrategyMethod);
        return (WaitStrategy) ReflectionUtils.invokeMethod(waitStrategyMethod, container);
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
    void resolveMountableFileShouldResolveClasspathResourceWithExplicitPrefix() {
        assertThat(ContainerConfigurer.resolveMountableFile("classpath:test-resource.txt"))
                .isNotNull()
                .extracting(MountableFile::getFilesystemPath).asString()
                .endsWith("test-resource.txt");
    }

    @Test
    void resolveMountableFileShouldResolveClasspathResourceWithoutPrefix() {
        assertThat(ContainerConfigurer.resolveMountableFile("test-resource.txt"))
                .isNotNull()
                .extracting(MountableFile::getFilesystemPath).asString()
                .endsWith("test-resource.txt");
    }

    @Test
    void resolveMountableFileShouldResolveFilesystemResourceWithExplicitPrefix(@TempDir Path tempDir) throws IOException {
        Path file = Files.writeString(tempDir.resolve("realm.json"), "{}");

        assertThat(ContainerConfigurer.resolveMountableFile("file:" + file))
                .isNotNull()
                .extracting(MountableFile::getFilesystemPath).asString()
                .endsWith("realm.json");
    }

    @Test
    void resolveMountableFileShouldResolveFilesystemResourceWithoutPrefix(@TempDir Path tempDir) throws IOException {
        Path file = Files.writeString(tempDir.resolve("realm.json"), "{}");

        assertThat(ContainerConfigurer.resolveMountableFile(file.toString()))
                .isNotNull()
                .extracting(MountableFile::getFilesystemPath).asString()
                .endsWith("realm.json");
    }

    @Test
    void resolveMountableFileShouldApplyTheGivenFileMode() {
        // A resource extracted from a jar carries no usable permissions, so the mode override is
        // what makes a copied file readable by a container process running as a non-root user.
        MountableFile mountableFile = ContainerConfigurer.resolveMountableFile("test-resource.txt", 0644);

        assertThat(mountableFile).isNotNull();
        // getFileMode() ORs in the file-type bits, so compare only the permission bits.
        assertThat(mountableFile.getFileMode() & 0777).isEqualTo(0644);
    }

    @Test
    void resolveMountableFileShouldThrowExceptionWhenResourceNotFound() {
        assertThatThrownBy(() -> ContainerConfigurer.resolveMountableFile("non-existent-resource.txt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Resource not found");
    }

    @Test
    void resolveMountableFileShouldThrowExceptionWhenPrefixedResourceNotFound() {
        assertThatThrownBy(() -> ContainerConfigurer.resolveMountableFile("classpath:non-existent-resource.txt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("classpath")
                .hasMessageContaining("classpath:non-existent-resource.txt");

        assertThatThrownBy(() -> ContainerConfigurer.resolveMountableFile("file:/no/such/realm.json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("filesystem")
                .hasMessageContaining("file:/no/such/realm.json");
    }

    @Test
    void resolveResourceShouldReadClasspathResource() throws IOException {
        Resource resource = ContainerConfigurer.resolveResource("classpath:test-resource.txt");

        assertThat(resource).isInstanceOf(ClassPathResource.class);
        assertThat(resource.exists()).isTrue();
        assertThat(resource.getContentAsString(StandardCharsets.UTF_8)).isNotEmpty();
    }

    @Test
    void resolveResourceShouldReadFilesystemResource(@TempDir Path tempDir) throws IOException {
        Path file = Files.writeString(tempDir.resolve("realm.json"), "{\"realm\":\"arconia\"}");

        Resource resource = ContainerConfigurer.resolveResource(file.toString());

        assertThat(resource).isInstanceOf(FileSystemResource.class);
        assertThat(resource.getContentAsString(StandardCharsets.UTF_8)).isEqualTo("{\"realm\":\"arconia\"}");
    }

    @Test
    void resolveResourceShouldPreferClasspathOverFilesystem() {
        // Same lookup order as the resources property: classpath first, filesystem second.
        assertThat(ContainerConfigurer.resolveResource("test-resource.txt"))
                .isInstanceOf(ClassPathResource.class);
    }

    @Test
    void resolveResourceShouldThrowExceptionWhenResourceNotFound() {
        assertThatThrownBy(() -> ContainerConfigurer.resolveResource("non-existent-resource.txt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Resource not found");
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
        private List<VolumeMapping> volumes = List.of();
        private ReuseStrategy reuseStrategy = ReuseStrategy.NONE;

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
        public List<VolumeMapping> getVolumes() {
            return volumes;
        }

        public TestBaseDevServicesProperties withVolumes(List<VolumeMapping> volumes) {
            this.volumes = volumes;
            return this;
        }

        @Override
        public ReuseStrategy getReuseStrategy() {
            return reuseStrategy;
        }

        public TestBaseDevServicesProperties withReuseStrategy(ReuseStrategy reuseStrategy) {
            this.reuseStrategy = reuseStrategy;
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
