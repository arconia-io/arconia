package io.arconia.multitenancy.details.jdbc.autoconfigure;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.sql.init.DatabaseInitializationMode;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.ResourceUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JdbcTenantDetailsProperties}.
 */
class JdbcTenantDetailsPropertiesTests {

    private static final String PLATFORM_PLACEHOLDER = "@@platform@@";

    @Test
    void defaultValues() {
        var properties = new JdbcTenantDetailsProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getSchema()).isEqualTo(JdbcTenantDetailsProperties.DEFAULT_SCHEMA_LOCATION);
        assertThat(properties.getPlatform()).isNull();
        assertThat(properties.getInitializeSchema()).isEqualTo(DatabaseInitializationMode.EMBEDDED);
        assertThat(properties.isContinueOnError()).isTrue();
    }

    @Test
    void defaultSchemaLocationResolvesForBundledPlatforms() throws IOException {
        var properties = new JdbcTenantDetailsProperties();
        var resourceLoader = new DefaultResourceLoader();
        var platforms = bundledPlatforms();

        assertThat(platforms).contains("h2", "postgresql");

        for (var platform : platforms) {
            var location = properties.getSchema().replace(PLATFORM_PLACEHOLDER, platform);
            assertThat(resourceLoader.getResource(location).isReadable())
                .as("schema script for platform '%s' at %s", platform, location)
                .isTrue();
        }
    }

    @Test
    void bundledPlatformsHaveDropScript() throws IOException {
        var properties = new JdbcTenantDetailsProperties();
        var resourceLoader = new DefaultResourceLoader();

        for (var platform : bundledPlatforms()) {
            var location = properties.getSchema().replace("schema-" + PLATFORM_PLACEHOLDER, "schema-drop-" + platform);
            assertThat(resourceLoader.getResource(location).isReadable())
                .as("drop script for platform '%s' at %s", platform, location)
                .isTrue();
        }
    }

    private List<String> bundledPlatforms() throws IOException {
        var location = new JdbcTenantDetailsProperties().getSchema().replace(PLATFORM_PLACEHOLDER, "*");
        var pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
                + location.substring(ResourceUtils.CLASSPATH_URL_PREFIX.length());
        var scripts = new PathMatchingResourcePatternResolver().getResources(pattern);
        return Arrays.stream(scripts)
            .map(Resource::getFilename)
            .filter(filename -> filename != null && !filename.startsWith("schema-drop-"))
            .map(filename -> filename.substring("schema-".length(), filename.length() - ".sql".length()))
            .toList();
    }

}
