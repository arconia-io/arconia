package io.arconia.core.bootstrap.autoconfigure;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.arconia.core.bootstrap.BootstrapMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Keeps the hand-maintained configuration metadata of the bootstrap mode in sync with the code.
 * The metadata cannot be generated, since the mode is not bound to a
 * {@code @ConfigurationProperties} class.
 */
class BootstrapConfigurationMetadataTests {

    // Read from the sources, since other jars on the test classpath have a metadata file with the same name.
    private static final Path METADATA_FILE = Path.of("src/main/resources/META-INF/spring-configuration-metadata.json");

    private static DocumentContext metadata;

    @BeforeAll
    static void readMetadata() throws IOException {
        metadata = JsonPath.parse(METADATA_FILE.toFile());
    }

    @Test
    void shouldDescribeTheBootstrapMode() {
        List<String> names = metadata.read("$.properties[*].name");
        List<String> types = metadata.read("$.properties[?(@.name == '%s')].type".formatted(BootstrapMode.SYSTEM_PROPERTY));

        assertThat(names).containsExactly(BootstrapMode.SYSTEM_PROPERTY);
        assertThat(types).containsExactly(BootstrapMode.class.getName());
    }

}
