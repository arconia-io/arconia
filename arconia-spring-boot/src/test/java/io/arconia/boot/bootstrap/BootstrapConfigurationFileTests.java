package io.arconia.boot.bootstrap;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link BootstrapConfigurationFile}.
 */
class BootstrapConfigurationFileTests {

    @Test
    void shouldLoadPropertiesWhenFilePresent() {
        var classLoader = classLoaderWithBootstrapFile("""
                arconia.dev.profiles=local,debug
                arconia.test.profiles=integration
                """);
        var properties = BootstrapConfigurationFile.load(classLoader);
        assertThat(properties.getProperty(BootstrapConfigurationFile.DEV_PROFILES_PROPERTY)).isEqualTo("local,debug");
        assertThat(properties.getProperty(BootstrapConfigurationFile.TEST_PROFILES_PROPERTY)).isEqualTo("integration");
    }

    @Test
    void shouldReturnEmptyPropertiesWhenFileAbsent() {
        assertThat(BootstrapConfigurationFile.load(getClass().getClassLoader())).isEmpty();
    }

    @Test
    void shouldFallBackToContextClassLoaderWhenNull() {
        assertThat(BootstrapConfigurationFile.load(null)).isEmpty();
    }

    static ClassLoader classLoaderWithBootstrapFile(String content) {
        return new ClassLoader(BootstrapConfigurationFileTests.class.getClassLoader()) {
            @Override
            @Nullable
            public InputStream getResourceAsStream(String name) {
                if (BootstrapConfigurationFile.LOCATION.equals(name)) {
                    return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
                }
                return super.getResourceAsStream(name);
            }
        };
    }

}
