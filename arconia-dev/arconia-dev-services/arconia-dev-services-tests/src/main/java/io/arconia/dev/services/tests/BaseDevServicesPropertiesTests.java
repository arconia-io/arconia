package io.arconia.dev.services.tests;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import io.arconia.dev.services.api.config.BaseDevServicesProperties;
import io.arconia.dev.services.api.config.ReuseStrategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Abstract base test class for testing {@link BaseDevServicesProperties} implementations.
 * <p>
 * Values are applied via Spring Boot's {@link Binder} so that the actual configuration
 * binding path is exercised: a property class missing a setter fails these tests instead
 * of silently discarding user configuration.
 *
 * @param <T> the specific {@link BaseDevServicesProperties} implementation type
 */
public abstract class BaseDevServicesPropertiesTests<T extends BaseDevServicesProperties> {

    private static final String TEST_IMAGE_NAME = "test-image:latest";
    private static final int TEST_PORT = 9999;
    private static final Duration TEST_STARTUP_TIMEOUT = Duration.ofMinutes(1);

    /**
     * Create a new instance of the properties class to test.
     */
    protected abstract T createProperties();

    /**
     * Get the expected default values for properties that differ between implementations,
     * such as the image name and startup timeout.
     */
    protected abstract DefaultValues getExpectedDefaults();

    /**
     * Bind the given configuration values (relative to the properties prefix)
     * onto the given properties instance via Spring Boot's {@link Binder}.
     */
    protected static <P> P bind(P properties, Map<String, String> values) {
        return new Binder(new MapConfigurationPropertySource(values))
                .bind("", Bindable.ofInstance(properties))
                .orElse(properties);
    }

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
        assertThat(properties.getReuseStrategy()).isEqualTo(defaults.reuseStrategy());
        assertThat(properties.getStartupTimeout()).isEqualTo(defaults.startupTimeout());
        assertThat(properties.getVolumes()).isEmpty();
    }

    @Test
    void shouldBindCommonProperties() {
        T properties = createProperties();
        DefaultValues defaults = getExpectedDefaults();

        Map<String, String> values = new HashMap<>();
        values.put("enabled", "false");
        values.put("environment.KEY", "value");
        values.put("image-name", TEST_IMAGE_NAME);
        values.put("network-aliases[0]", "network1");
        values.put("network-aliases[1]", "network2");
        values.put("port", String.valueOf(TEST_PORT));
        values.put("resources[0].source-path", "test-resource.txt");
        values.put("resources[0].container-path", "/tmp/test-resource.txt");
        values.put("reuse-strategy", "testcontainers");
        values.put("startup-timeout", TEST_STARTUP_TIMEOUT.toString());
        values.put("volumes[0].host-path", "/host/path");
        values.put("volumes[0].container-path", "/container/path");

        bind(properties, values);

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getEnvironment()).containsEntry("KEY", "value");
        assertThat(properties.getImageName()).isEqualTo(TEST_IMAGE_NAME);
        assertThat(properties.getNetworkAliases()).containsExactly("network1", "network2");
        assertThat(properties.getPort()).isEqualTo(TEST_PORT);
        assertThat(properties.getResources()).hasSize(1);
        assertThat(properties.getResources().getFirst().sourcePath()).isEqualTo("test-resource.txt");
        assertThat(properties.getResources().getFirst().containerPath()).isEqualTo("/tmp/test-resource.txt");
        assertThat(properties.getReuseStrategy()).isEqualTo(ReuseStrategy.TESTCONTAINERS);
        assertThat(properties.getStartupTimeout()).isEqualTo(TEST_STARTUP_TIMEOUT);
        assertThat(properties.getVolumes()).hasSize(1);
        assertThat(properties.getVolumes().getFirst().hostPath()).isEqualTo("/host/path");
        assertThat(properties.getVolumes().getFirst().containerPath()).isEqualTo("/container/path");
    }

    @Test
    void shouldRejectBlankPathsWhenBinding() {
        T properties = createProperties();

        // A mapping validates its paths when created, so a blank one fails while binding.
        assertThatThrownBy(() -> bind(properties, Map.of(
                "resources[0].source-path", "",
                "resources[0].container-path", "/tmp/test-resource.txt")))
                .hasRootCauseMessage("sourcePath cannot be null or empty");
        assertThatThrownBy(() -> bind(properties, Map.of(
                "volumes[0].host-path", "/host/path",
                "volumes[0].container-path", "")))
                .hasRootCauseMessage("containerPath cannot be null or empty");
    }

    /**
     * Holds expected default values for a specific implementation.
     *
     * @param imageName the expected image name (or substring for contains check, or empty string)
     * @param reuseStrategy the expected default value for the reuseStrategy property
     * @param startupTimeout the expected default value for the startupTimeout property
     */
    public record DefaultValues(
            String imageName,
            ReuseStrategy reuseStrategy,
            Duration startupTimeout
    ) {

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {

            private String imageName = "";
            private ReuseStrategy reuseStrategy = ReuseStrategy.NONE;
            private Duration startupTimeout = Duration.ofSeconds(30);

            public Builder imageName(String imageName) {
                this.imageName = imageName;
                return this;
            }

            public Builder reuseStrategy(ReuseStrategy reuseStrategy) {
                this.reuseStrategy = reuseStrategy;
                return this;
            }

            public Builder startupTimeout(Duration startupTimeout) {
                this.startupTimeout = startupTimeout;
                return this;
            }

            public DefaultValues build() {
                return new DefaultValues(imageName, reuseStrategy, startupTimeout);
            }

        }

    }

}
