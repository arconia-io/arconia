package io.arconia.dev.services.core.container;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

import io.arconia.dev.services.api.config.BaseDevServicesProperties;
import io.arconia.dev.services.api.config.JdbcDevServicesProperties;
import io.arconia.dev.services.api.config.ResourceMapping;
import io.arconia.dev.services.api.config.VolumeMapping;
import io.arconia.dev.services.core.TestDevServicesProperties;
import io.arconia.dev.services.core.TestJdbcDevServicesProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ContainerConfigurer}.
 */
class ContainerConfigurerTests {

    @Test
    void applyShouldConfigureTheContainerFromTheProperties() {
        GenericContainer<?> container = new GenericContainer<>("alpine:latest");
        container.waitingFor(Wait.defaultWaitStrategy());
        BaseDevServicesProperties properties = new TestDevServicesProperties()
                .withEnvironment(Map.of("KEY1", "VALUE1", "KEY2", "VALUE2"))
                .withNetworkAliases(List.of("alias1", "alias2"))
                .withStartupTimeout(Duration.ofMinutes(2))
                .withResources(List.of(
                        new ResourceMapping("classpath:test-resource.txt", "/etc/config/test1.txt"),
                        new ResourceMapping("test-resource.txt", "/etc/config/test2.txt")))
                .withVolumes(List.of(new VolumeMapping("/host/path", "/container/path")));

        ContainerConfigurer.apply(container, properties);

        assertThat(container.getEnvMap()).containsEntry("KEY1", "VALUE1").containsEntry("KEY2", "VALUE2");
        assertThat(container.getNetworkAliases()).contains("alias1", "alias2");
        assertThat(getStartupTimeout(container)).isEqualTo(Duration.ofMinutes(2));
        assertThat(container.getCopyToFileContainerPathMap().values()).contains("/etc/config/test1.txt", "/etc/config/test2.txt");
        assertThat(container.getBinds()).singleElement().satisfies(bind -> {
            assertThat(bind.getPath()).isEqualTo("/host/path");
            assertThat(bind.getVolume().getPath()).isEqualTo("/container/path");
            assertThat(bind.getAccessMode().toString()).isEqualTo("rw");
        });
    }

    @Test
    void applyShouldFailWhenAResourceIsNotFound() {
        GenericContainer<?> container = new GenericContainer<>("alpine:latest");
        BaseDevServicesProperties properties = new TestDevServicesProperties()
                .withResources(List.of(new ResourceMapping("non-existent-resource.txt", "/etc/config/test.txt")));

        assertThatThrownBy(() -> ContainerConfigurer.apply(container, properties))
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

        ContainerConfigurer.apply(container, properties);

        assertThat(container.getUsername()).isEqualTo("testuser");
        assertThat(container.getPassword()).isEqualTo("testpass");
        assertThat(container.getDatabaseName()).isEqualTo("testdb");
        assertThat(ReflectionTestUtils.getField(container, "initScriptPaths")).asInstanceOf(InstanceOfAssertFactories.LIST)
                .containsExactly("init1.sql", "init2.sql");
    }

    @Test
    void applyShouldPropagateTheStartupTimeoutToTheJdbcConnectionPolling() {
        JdbcDatabaseContainer<?> container = new PostgreSQLContainer("postgres:latest");
        JdbcDevServicesProperties properties = new TestJdbcDevServicesProperties()
                .withUsername("testuser")
                .withStartupTimeout(Duration.ofSeconds(45));

        ContainerConfigurer.apply(container, properties);

        assertThat(ReflectionTestUtils.getField(container, "startupTimeoutSeconds")).isEqualTo(45);
        assertThat(getStartupTimeout(container)).isEqualTo(Duration.ofSeconds(45));
    }

    @Test
    void applyShouldRoundTheJdbcConnectionPollingTimeoutUpToWholeSeconds() {
        JdbcDatabaseContainer<?> container = new PostgreSQLContainer("postgres:latest");
        JdbcDevServicesProperties properties = new TestJdbcDevServicesProperties()
                .withStartupTimeout(Duration.ofMillis(500));

        ContainerConfigurer.apply(container, properties);

        // Truncating to 0 seconds would skip the connection polling altogether.
        assertThat(ReflectionTestUtils.getField(container, "startupTimeoutSeconds")).isEqualTo(1);
    }

    private static Duration getStartupTimeout(GenericContainer<?> container) {
        WaitStrategy waitStrategy = ReflectionTestUtils.invokeMethod(container, "getWaitStrategy");
        assertThat(waitStrategy).isNotNull();
        return (Duration) ReflectionTestUtils.getField(waitStrategy, "startupTimeout");
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
        MountableFile mountableFile = ContainerConfigurer.resolveMountableFile("test-resource.txt", 0644);

        assertThat(mountableFile).isNotNull();
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
        Path file = Files.writeString(tempDir.resolve("realm.json"), """
                {"realm":"arconia"}
                """);

        Resource resource = ContainerConfigurer.resolveResource(file.toString());

        assertThat(resource).isInstanceOf(FileSystemResource.class);
        assertThat(resource.getContentAsString(StandardCharsets.UTF_8)).isEqualTo("""
                {"realm":"arconia"}
                """);
    }

    @Test
    void resolveResourceShouldPreferClasspathOverFilesystem() {
        assertThat(ContainerConfigurer.resolveResource("test-resource.txt"))
                .isInstanceOf(ClassPathResource.class);
    }

    @Test
    void resolveResourceShouldThrowExceptionWhenResourceNotFound() {
        assertThatThrownBy(() -> ContainerConfigurer.resolveResource("non-existent-resource.txt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Resource not found");
    }

}
