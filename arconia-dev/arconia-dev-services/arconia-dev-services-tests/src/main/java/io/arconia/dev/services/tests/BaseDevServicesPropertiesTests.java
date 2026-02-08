package io.arconia.dev.services.tests;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.api.config.BaseDevServicesProperties;
import io.arconia.dev.services.api.config.ResourceMapping;
import io.arconia.dev.services.api.config.VolumeMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base test class for testing {@link BaseDevServicesProperties} implementations.
 *
 * @param <T> the specific {@link BaseDevServicesProperties} implementation type
 */
public abstract class BaseDevServicesPropertiesTests<T extends BaseDevServicesProperties> {

    private static final String TEST_IMAGE_NAME = "test-image:latest";
    private static final int TEST_PORT = 9999;
    private static final Map<String, String> TEST_ENVIRONMENT = Map.of("KEY", "value");
    private static final List<String> TEST_NETWORK_ALIASES = List.of("network1", "network2");
    private static final Duration TEST_STARTUP_TIMEOUT = Duration.ofMinutes(1);
    private static final ResourceMapping TEST_RESOURCE =
            new ResourceMapping("test-resource.txt", "/tmp/test-resource.txt");
    private static final VolumeMapping TEST_VOLUME =
            new VolumeMapping("/host/path", "/container/path");

    /**
     * Create a new instance of the properties class to test.
     */
    protected abstract T createProperties();

    /**
     * Get the expected default values for properties that differ between implementations,
     * such as the image name and startup timeout.
     */
    protected abstract DefaultValues getExpectedDefaults();

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        T properties = createProperties();
        DefaultValues defaults = getExpectedDefaults();

        if (defaults.imageName().isEmpty()) {
            assertThat(properties.getImageName()).isEmpty();
        } else {
            assertThat(properties.getImageName()).contains(defaults.imageName());
        }

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getEnvironment()).isEmpty();
        assertThat(properties.getNetworkAliases()).isEmpty();
        assertThat(properties.getPort()).isEqualTo(0);
        assertThat(properties.getResources()).isEmpty();
        assertThat(properties.isShared()).isEqualTo(defaults.shared());
        assertThat(properties.getStartupTimeout()).isEqualTo(defaults.startupTimeout());
        assertThat(properties.getVolumes()).isEmpty();
    }

    @Test
    void shouldUpdateCommonProperties() {
        T properties = createProperties();
        DefaultValues defaults = getExpectedDefaults();

        properties.setEnabled(false);
        assertThat(properties.isEnabled()).isFalse();

        properties.setEnvironment(TEST_ENVIRONMENT);
        assertThat(properties.getEnvironment()).containsEntry("KEY", "value");

        properties.setImageName(TEST_IMAGE_NAME);
        assertThat(properties.getImageName()).isEqualTo(TEST_IMAGE_NAME);

        properties.setNetworkAliases(TEST_NETWORK_ALIASES);
        assertThat(properties.getNetworkAliases()).containsExactly("network1", "network2");

        properties.setPort(TEST_PORT);
        assertThat(properties.getPort()).isEqualTo(TEST_PORT);

        properties.setResources(List.of(TEST_RESOURCE));
        assertThat(properties.getResources()).hasSize(1);
        assertThat(properties.getResources().getFirst().getSourcePath()).isEqualTo("test-resource.txt");
        assertThat(properties.getResources().getFirst().getContainerPath()).isEqualTo("/tmp/test-resource.txt");

        properties.setShared(!defaults.shared());
        assertThat(properties.isShared()).isEqualTo(!defaults.shared());

        properties.setStartupTimeout(TEST_STARTUP_TIMEOUT);
        assertThat(properties.getStartupTimeout()).isEqualTo(TEST_STARTUP_TIMEOUT);

        properties.setVolumes(List.of(TEST_VOLUME));
        assertThat(properties.getVolumes()).hasSize(1);
        assertThat(properties.getVolumes().getFirst().getHostPath()).isEqualTo("/host/path");
        assertThat(properties.getVolumes().getFirst().getContainerPath()).isEqualTo("/container/path");
    }

    /**
     * Holds expected default values for a specific implementation.
     *
     * @param imageName the expected image name (or substring for contains check, or empty string)
     * @param shared the expected default value for the shared property
     * @param startupTimeout the expected default value for the startupTimeout property
     */
    public record DefaultValues(
            String imageName,
            boolean shared,
            Duration startupTimeout
    ) {

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {

            private String imageName = "";
            private boolean shared = false;
            private Duration startupTimeout = Duration.ofSeconds(30);

            public Builder imageName(String imageName) {
                this.imageName = imageName;
                return this;
            }

            public Builder shared(boolean shared) {
                this.shared = shared;
                return this;
            }

            public Builder startupTimeout(Duration startupTimeout) {
                this.startupTimeout = startupTimeout;
                return this;
            }

            public DefaultValues build() {
                return new DefaultValues(imageName, shared, startupTimeout);
            }

        }

    }

    /**
     * Check if a property is actually implemented (not just using interface default).
     * We do this by checking if the getter's declaring class is the implementation,
     * not the interface.
     * <p>
     * The reason is to be able to add new common properties without forcing
     * right away all modules to implement them.
     */
    private boolean isPropertyImplemented(T properties, String propertyName) {
        try {
            String getterName = "get" + propertyName.substring(0, 1).toUpperCase()
                    + propertyName.substring(1);
            if (propertyName.equals("enabled") || propertyName.equals("shared")) {
                getterName = "is" + propertyName.substring(0, 1).toUpperCase()
                        + propertyName.substring(1);
            }

            Method getter = properties.getClass().getMethod(getterName);
            // If the method is declared in the concrete class (not just inherited from interface)
            return getter.getDeclaringClass() != BaseDevServicesProperties.class;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

}
