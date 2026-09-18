package io.arconia.dev.services.core.container;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.utility.MountableFile;

import io.arconia.boot.bootstrap.BootstrapMode;
import io.arconia.core.support.Incubating;
import io.arconia.dev.services.api.config.BaseDevServicesProperties;
import io.arconia.dev.services.api.config.JdbcDevServicesProperties;
import io.arconia.dev.services.api.config.VolumeMapping;

/**
 * Utility class for configuring Dev Service containers.
 */
@Incubating
public final class ContainerConfigurer {

    private static final String RESOURCE_PREFIX_CLASSPATH = "classpath:";
    private static final String RESOURCE_PREFIX_FILE = "file:";

    private ContainerConfigurer() {}

    /**
     * Configures base container configuration for Dev Services.
     * <p>
     * The container must already have a wait strategy of its own when this method is called,
     * since the configured startup timeout is applied to it. Containers that don't declare one
     * rely on a wait strategy instance that Testcontainers shares across all containers, which
     * would make the startup timeout apply to every other container relying on it as well.
     */
    public static void base(GenericContainer<?> container, BaseDevServicesProperties properties) {
        container
                .withEnv(properties.getEnvironment())
                .withNetworkAliases(properties.getNetworkAliases().toArray(new String[]{}))
                .withStartupTimeout(properties.getStartupTimeout());

        resources(container, properties);
        volumes(container, properties);
        reuse(container, properties);
    }

    /**
     * Configures whether the container is reused across application restarts, relying on the
     * Testcontainers reusable containers feature. Reuse only takes effect in dev mode and
     * additionally requires enabling the feature in the {@code ~/.testcontainers.properties} file.
     */
    public static void reuse(GenericContainer<?> container, BaseDevServicesProperties properties) {
        container.withReuse(isDevMode() && properties.isReuse());
    }

    private static boolean isDevMode() {
        return BootstrapMode.DEV.equals(BootstrapMode.detect());
    }

    /**
     * Configures mapped resources to be loaded into the container.
     */
    public static void resources(GenericContainer<?> container, BaseDevServicesProperties properties) {
        for (var resource : properties.getResources()) {
            Assert.hasText(resource.getSourcePath(), "the source path in a resource mapping cannot be null or empty.");
            Assert.hasText(resource.getContainerPath(), "the container path in a resource mapping cannot be null or empty.");

            MountableFile mountableFile = resolveMountableFile(resource.getSourcePath());
            container.withCopyFileToContainer(mountableFile, resource.getContainerPath());
        }
    }

    /**
     * Resolve a source path from the classpath or the host filesystem into a mountable file,
     * applying the same rules as the {@code resources} property: an explicit
     * {@code classpath:} or {@code file:} prefix selects the location, otherwise the classpath
     * is tried first and the host filesystem second.
     *
     * @throws IllegalArgumentException if the resource exists in neither location.
     */
    public static MountableFile resolveMountableFile(String sourcePath) {
        ResourceLocation location = resolveLocation(sourcePath);
        return location.classpath()
                ? MountableFile.forClasspathResource(location.path())
                : MountableFile.forHostPath(location.path());
    }

    /**
     * Resolve a source path into a mountable file with the given POSIX file mode, applying the
     * same rules as {@link #resolveMountableFile(String)}.
     * <p>
     * A mode is needed whenever the container process reads the copied file as a non-root user:
     * a resource extracted from a jar does not carry usable permissions of its own.
     *
     * @throws IllegalArgumentException if the resource exists in neither location.
     */
    public static MountableFile resolveMountableFile(String sourcePath, int mode) {
        ResourceLocation location = resolveLocation(sourcePath);
        return location.classpath()
                ? MountableFile.forClasspathResource(location.path(), mode)
                : MountableFile.forHostPath(location.path(), mode);
    }

    /**
     * Resolve a source path into a {@link Resource}, applying the same rules as
     * {@link #resolveMountableFile(String)}, so that a dev service can read a mapped resource
     * (for example to derive configuration from it) using the very lookup that will later copy
     * it into the container.
     *
     * @throws IllegalArgumentException if the resource exists in neither location.
     */
    public static Resource resolveResource(String sourcePath) {
        ResourceLocation location = resolveLocation(sourcePath);
        return location.classpath()
                ? new ClassPathResource(location.path())
                : new FileSystemResource(location.path());
    }

    /**
     * Where a source path resolves to, with the prefix (if any) stripped. Shared by every
     * {@code resolve*} method so the lookup rules cannot drift between them.
     */
    private record ResourceLocation(boolean classpath, String path) {}

    private static ResourceLocation resolveLocation(String resourcePath) {
        Assert.hasText(resourcePath, "resourcePath cannot be null or empty");

        // 1. Handle explicit prefixes.
        if (resourcePath.startsWith(RESOURCE_PREFIX_CLASSPATH)) {
            String path = resourcePath.substring(RESOURCE_PREFIX_CLASSPATH.length());
            return requireExisting(new ResourceLocation(true, path), resourcePath);
        }

        if (resourcePath.startsWith(RESOURCE_PREFIX_FILE)) {
            String path = resourcePath.substring(RESOURCE_PREFIX_FILE.length());
            return requireExisting(new ResourceLocation(false, path), resourcePath);
        }

        // 2. When no prefixes, try classpath first.
        if (new ClassPathResource(resourcePath).exists()) {
            return new ResourceLocation(true, resourcePath);
        }

        // 3. If not found, try filesystem.
        if (new FileSystemResource(resourcePath).exists()) {
            return new ResourceLocation(false, resourcePath);
        }

        // 4. If still not found, throw exception.
        throw new IllegalArgumentException("Resource not found in classpath or filesystem: " + resourcePath);
    }

    /**
     * Validate a prefixed path, so that a missing resource is reported the same way whether or
     * not the location was stated explicitly, and names the path as the user wrote it.
     */
    private static ResourceLocation requireExisting(ResourceLocation location, String resourcePath) {
        Resource resource = location.classpath()
                ? new ClassPathResource(location.path())
                : new FileSystemResource(location.path());
        if (!resource.exists()) {
            throw new IllegalArgumentException("Resource not found in %s: %s"
                    .formatted(location.classpath() ? "classpath" : "filesystem", resourcePath));
        }
        return location;
    }

    /**
     * Configures mapped volumes to be bound with read-write access to the container.
     */
    public static void volumes(GenericContainer<?> container, BaseDevServicesProperties properties) {
        for (VolumeMapping mapping : properties.getVolumes()) {
            Assert.hasText(mapping.getHostPath(), "the host path in a volume mapping cannot be null or empty.");
            Assert.hasText(mapping.getContainerPath(), "the container path in a volume mapping cannot be null or empty.");

            container.withFileSystemBind(mapping.getHostPath(), mapping.getContainerPath(), BindMode.READ_WRITE);
        }
    }

    /**
     * Configures JDBC common settings for Dev Services.
     */
    public static void jdbc(JdbcDatabaseContainer<?> container, JdbcDevServicesProperties properties) {
        container
                .withUsername(properties.getUsername())
                .withPassword(properties.getPassword())
                .withDatabaseName(properties.getDbName())
                .withInitScripts(properties.getInitScriptPaths())
                // The startup timeout is applied again here because JdbcDatabaseContainer does not wait
                // on the container's wait strategy: it polls the database until it answers a test query,
                // bounded by its own startupTimeoutSeconds (120 seconds by default). Without this,
                // the configured startup timeout would have no effect on JDBC dev services.
                .withStartupTimeoutSeconds((int) properties.getStartupTimeout().toSeconds());
    }

}
