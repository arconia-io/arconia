package io.arconia.dev.services.core.container;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.utility.MountableFile;

import io.arconia.core.support.Incubating;
import io.arconia.dev.services.api.config.BaseDevServicesProperties;
import io.arconia.dev.services.api.config.JdbcDevServicesProperties;
import io.arconia.dev.services.api.config.ResourceMapping;
import io.arconia.dev.services.api.config.VolumeMapping;

/**
 * Applies the common dev service properties to a container, and resolves the resources they
 * refer to. The properties are applied by the dev services registry when it creates the
 * container, so a dev service must not apply them itself.
 */
@Incubating
public final class ContainerConfigurer {

    private static final String RESOURCE_PREFIX_CLASSPATH = "classpath:";

    private static final String RESOURCE_PREFIX_FILE = "file:";

    private static final String JDBC_DATABASE_CONTAINER_CLASS = "org.testcontainers.containers.JdbcDatabaseContainer";

    private ContainerConfigurer() {}

    /**
     * Apply the common dev service properties to the given container: environment, network
     * aliases, startup timeout, resources, volumes, and whether Testcontainers reuses the
     * container. A {@link JdbcDatabaseContainer} configured with {@link JdbcDevServicesProperties}
     * also gets its credentials, database name, and init scripts.
     * <p>
     * The startup timeout is applied to the wait strategy of the container, which must therefore
     * be its own. A container that doesn't declare one relies on a wait strategy instance that
     * Testcontainers shares across all containers, and the timeout would apply to every other
     * container relying on it as well.
     */
    public static void apply(GenericContainer<?> container, BaseDevServicesProperties properties, boolean testcontainersReuse) {
        Assert.notNull(container, "container cannot be null");
        Assert.notNull(properties, "properties cannot be null");

        container
                .withEnv(properties.getEnvironment())
                .withNetworkAliases(properties.getNetworkAliases().toArray(new String[]{}))
                .withStartupTimeout(properties.getStartupTimeout())
                .withReuse(testcontainersReuse);
        for (ResourceMapping resource : properties.getResources()) {
            container.withCopyFileToContainer(resolveMountableFile(resource.sourcePath()), resource.containerPath());
        }
        volumes(container, properties);

        if (properties instanceof JdbcDevServicesProperties jdbcProperties
                && ClassUtils.isPresent(JDBC_DATABASE_CONTAINER_CLASS, ContainerConfigurer.class.getClassLoader())) {
            jdbc(container, jdbcProperties);
        }
    }

    @SuppressWarnings("deprecation")
    private static void volumes(GenericContainer<?> container, BaseDevServicesProperties properties) {
        for (VolumeMapping volume : properties.getVolumes()) {
            container.withFileSystemBind(volume.hostPath(), volume.containerPath(), BindMode.READ_WRITE);
        }
    }

    private static void jdbc(GenericContainer<?> container, JdbcDevServicesProperties properties) {
        if (!(container instanceof JdbcDatabaseContainer<?> jdbcContainer)) {
            return;
        }
        jdbcContainer
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


}
