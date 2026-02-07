package io.arconia.dev.services.core.container;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.Assert;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.utility.MountableFile;

import io.arconia.boot.bootstrap.BootstrapMode;
import io.arconia.dev.services.api.config.BaseDevServicesProperties;
import io.arconia.dev.services.api.config.JdbcDevServicesProperties;

/**
 * Utility class for configuring Dev Service containers.
 */
public final class ContainerConfigurer {

    private static final String RESOURCE_PREFIX_CLASSPATH = "classpath:";
    private static final String RESOURCE_PREFIX_FILE = "file:";

    /**
     * Configures base container configuration for Dev Services.
     */
    public static void base(GenericContainer<?> container, BaseDevServicesProperties properties) {
        container
                .withEnv(properties.getEnvironment())
                .withNetworkAliases(properties.getNetworkAliases().toArray(new String[]{}))
                .withStartupTimeout(properties.getStartupTimeout())
                .withReuse(isDevMode() && properties.isShared());

        resources(container, properties);
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

    private static MountableFile resolveMountableFile(String resourcePath) {
        // 1. Handle explicit prefixes.
        if (resourcePath.startsWith(RESOURCE_PREFIX_CLASSPATH)) {
            String path = resourcePath.substring(RESOURCE_PREFIX_CLASSPATH.length());
            return MountableFile.forClasspathResource(path);
        }

        if (resourcePath.startsWith(RESOURCE_PREFIX_FILE)) {
            String path = resourcePath.substring(RESOURCE_PREFIX_FILE.length());
            return MountableFile.forHostPath(path);
        }

        // 2. When no prefixes, try classpath first.
        ClassPathResource classpathResource = new ClassPathResource(resourcePath);
        if (classpathResource.exists()) {
            return MountableFile.forClasspathResource(resourcePath);
        }

        // 3. If not found, try filesystem.
        FileSystemResource fileResource = new FileSystemResource(resourcePath);
        if (fileResource.exists()) {
            return MountableFile.forHostPath(resourcePath);
        }

        // 4. If still not found, throw exception.
        throw new IllegalArgumentException("Resource not found in classpath or filesystem: " + resourcePath);
    }

    /**
     * Configures JDBC common settings for Dev Services.
     */
    public static void jdbc(JdbcDatabaseContainer<?> container, JdbcDevServicesProperties properties) {
        container
                .withUsername(properties.getUsername())
                .withPassword(properties.getPassword())
                .withDatabaseName(properties.getDbName())
                .withInitScripts(properties.getInitScriptPaths());
    }

    private static boolean isDevMode() {
        return BootstrapMode.DEV.equals(BootstrapMode.detect());
    }

}
