package io.arconia.core.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.springframework.util.Assert;

import io.arconia.core.annotations.Internal;

/**
 * Loads the Arconia bootstrap configuration file, holding the few properties that must be
 * available before the application configuration is loaded: the mode-specific profile settings.
 * <p>
 * Unlike regular application configuration, the file is read once during the environment
 * preparation, so there are no profile-specific variants of it.
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

    private BootstrapConfigurationFile() {}

    /**
     * Loads the bootstrap configuration file from the classpath. When multiple files are present
     * (e.g. contributed by different jars), the first one found on the classpath is used.
     * Returns empty properties when the file is not present.
     */
    public static Properties load(ClassLoader classLoader) throws IOException {
        Assert.notNull(classLoader, "classLoader cannot be null");
        Properties properties = new Properties();
        try (InputStream inputStream = classLoader.getResourceAsStream(LOCATION)) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        }
        return properties;
    }

}
