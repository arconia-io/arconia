package io.arconia.core.bootstrap.profiles;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.springframework.util.Assert;

/**
 * Loads the Arconia bootstrap configuration file, holding the few properties that must be
 * available before the application configuration is loaded: the mode-specific profile settings.
 * <p>
 * Unlike regular application configuration, the file is read once during the environment
 * preparation, so there are no profile-specific variants of it.
 * The bootstrap mode itself is not supported here: it describes the launch context, whereas
 * the file is static and shipped with the packaged application.
 */
final class BootstrapConfigurationFile {

    static final String LOCATION = "META-INF/arconia-bootstrap.properties";

    /**
     * Whether to activate profiles based on the bootstrap mode.
     */
    static final String PROFILES_ENABLED_PROPERTY = "arconia.bootstrap.profiles.enabled";

    /**
     * Names of the profiles to activate in dev mode.
     */
    static final String DEV_PROFILES_PROPERTY = "arconia.dev.profiles";

    /**
     * Names of the profiles to activate in test mode.
     */
    static final String TEST_PROFILES_PROPERTY = "arconia.test.profiles";

    /**
     * The properties supported in the bootstrap configuration file.
     */
    static final List<String> SUPPORTED_PROPERTIES = List.of(
            PROFILES_ENABLED_PROPERTY, DEV_PROFILES_PROPERTY, TEST_PROFILES_PROPERTY);

    private BootstrapConfigurationFile() {}

    /**
     * Loads the bootstrap configuration file from the classpath. When multiple files are present
     * (e.g. contributed by different jars), the first one found on the classpath is used.
     * Returns empty properties when the file is not present.
     */
    static Properties load(ClassLoader classLoader) throws IOException {
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
