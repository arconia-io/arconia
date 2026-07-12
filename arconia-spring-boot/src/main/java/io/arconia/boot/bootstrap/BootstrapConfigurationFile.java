package io.arconia.boot.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.arconia.core.support.Internal;

/**
 * Loads the Arconia bootstrap configuration file, holding the few properties that must be
 * available before the application configuration is loaded: the mode-specific profile settings.
 * <p>
 * Unlike regular application configuration, the file is read once during the environment
 * preparation, so it only supports plain values (no placeholders, no profile-specific variants).
 * The bootstrap mode itself is not supported here: it describes the launch context, whereas
 * the file is static and shipped with the packaged application.
 */
@Internal
public final class BootstrapConfigurationFile {

    public static final String LOCATION = "META-INF/arconia-bootstrap.properties";

    /**
     * Whether the profiles are enabled based on the application mode.
     */
    public static final String PROFILES_ENABLED_PROPERTY = "arconia.bootstrap.profiles.enabled";

    /**
     * Name of the profiles to activate in development mode.
     */
    public static final String DEV_PROFILES_PROPERTY = "arconia.dev.profiles";

    /**
     * Name of the profiles to activate in test mode.
     */
    public static final String TEST_PROFILES_PROPERTY = "arconia.test.profiles";

    private static final Logger logger = LoggerFactory.getLogger(BootstrapConfigurationFile.class);

    private BootstrapConfigurationFile() {}

    /**
     * Loads the bootstrap configuration file from the classpath. When multiple files are present
     * (e.g. contributed by different jars), the first one found on the classpath is used.
     * Returns empty properties when the file is not present.
     */
    public static Properties load(@Nullable ClassLoader classLoader) {
        ClassLoader classLoaderToUse = classLoader;
        if (classLoaderToUse == null) {
            classLoaderToUse = Thread.currentThread().getContextClassLoader();
        }
        if (classLoaderToUse == null) {
            classLoaderToUse = BootstrapConfigurationFile.class.getClassLoader();
        }

        Properties properties = new Properties();
        try (InputStream inputStream = classLoaderToUse.getResourceAsStream(LOCATION)) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException ex) {
            logger.warn("Failed to load the bootstrap configuration file from {}", LOCATION, ex);
        }
        return properties;
    }

}
