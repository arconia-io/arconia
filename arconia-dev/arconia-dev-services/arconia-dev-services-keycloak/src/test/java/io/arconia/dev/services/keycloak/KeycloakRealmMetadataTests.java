package io.arconia.dev.services.keycloak;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link KeycloakRealmMetadata}.
 */
class KeycloakRealmMetadataTests {

    @Test
    void shouldUseDefaultRealmWhenNothingIsConfigured() {
        assertThat(KeycloakRealmMetadata.resolveRealmName(new KeycloakDevServicesProperties()))
                .isEqualTo(KeycloakDevServicesProperties.DEFAULT_REALM);
    }

    @Test
    void shouldUseTheConfiguredRealmName() {
        KeycloakDevServicesProperties properties = new KeycloakDevServicesProperties();
        properties.getRealm().setName("my-realm");

        assertThat(KeycloakRealmMetadata.resolveRealmName(properties)).isEqualTo("my-realm");
    }

    @Test
    void shouldReadRealmNameFromClasspathImportFile() {
        KeycloakDevServicesProperties properties = new KeycloakDevServicesProperties();
        properties.getRealm().setImportPath("classpath:keycloak/test-realm.json");

        assertThat(KeycloakRealmMetadata.resolveRealmName(properties)).isEqualTo("test-realm");
    }

    @Test
    void shouldReadRealmNameFromFilesystemImportFile(@TempDir Path tempDir) throws IOException {
        Path realmFile = Files.writeString(tempDir.resolve("realm.json"), """
            {"realm":"from-disk", "enabled": true}""");
        KeycloakDevServicesProperties properties = new KeycloakDevServicesProperties();
        properties.getRealm().setImportPath("file:" + realmFile);

        assertThat(KeycloakRealmMetadata.resolveRealmName(properties)).isEqualTo("from-disk");
    }

    @Test
    void shouldFailWhenImportFileHasNoRealmField(@TempDir Path tempDir) throws IOException {
        Path realmFile = Files.writeString(tempDir.resolve("realm.json"), """
            {"enabled": true}""");

        assertThatThrownBy(() -> KeycloakRealmMetadata.readRealmName(realmFile.toString()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No 'realm' field");
    }

    @Test
    void shouldFailWhenImportFileIsNotValidJson(@TempDir Path tempDir) throws IOException {
        Path realmFile = Files.writeString(tempDir.resolve("realm.json"), "not json at all");

        assertThatThrownBy(() -> KeycloakRealmMetadata.readRealmName(realmFile.toString()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to parse");
    }

    @Test
    void shouldFailWhenImportFileDoesNotExist() {
        assertThatThrownBy(() -> KeycloakRealmMetadata.readRealmName("classpath:no/such/realm.json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Resource not found");
    }

}
