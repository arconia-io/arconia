package io.arconia.dev.services.keycloak;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link KeycloakRealmMetadata}.
 */
class KeycloakRealmMetadataTests {

    @Test
    void shouldUseDefaultRealmWhenNoImportPathIsConfigured() {
        assertThat(KeycloakRealmMetadata.resolveRealmName(new KeycloakDevServicesProperties()))
                .isEqualTo(KeycloakDevServicesProperties.DEFAULT_REALM);
    }

    @Test
    void shouldDeriveRealmNameFromClasspathImportFile() {
        KeycloakDevServicesProperties properties = new KeycloakDevServicesProperties();
        properties.getRealm().setImportPaths(List.of("classpath:keycloak/test-realm.json"));

        assertThat(KeycloakRealmMetadata.resolveRealmName(properties)).isEqualTo("test-realm");
    }

    @Test
    void shouldDeriveRealmNameFromFilesystemImportFile(@TempDir Path tempDir) throws IOException {
        Path realmFile = Files.writeString(tempDir.resolve("realm.json"), "{\"realm\": \"from-disk\", \"enabled\": true}");
        KeycloakDevServicesProperties properties = new KeycloakDevServicesProperties();
        properties.getRealm().setImportPaths(List.of("file:" + realmFile));

        assertThat(KeycloakRealmMetadata.resolveRealmName(properties)).isEqualTo("from-disk");
    }

    @Test
    void shouldDeriveRealmNameFromTheFirstImportFile(@TempDir Path tempDir) throws IOException {
        Path first = Files.writeString(tempDir.resolve("first.json"), "{\"realm\": \"first-realm\"}");
        Path second = Files.writeString(tempDir.resolve("second.json"), "{\"realm\": \"second-realm\"}");
        KeycloakDevServicesProperties properties = new KeycloakDevServicesProperties();
        properties.getRealm().setImportPaths(List.of(first.toString(), second.toString()));

        assertThat(KeycloakRealmMetadata.resolveRealmName(properties)).isEqualTo("first-realm");
    }

    @Test
    void shouldPreferConfiguredRealmNameOverImportFile() {
        KeycloakDevServicesProperties properties = new KeycloakDevServicesProperties();
        properties.getRealm().setImportPaths(List.of("classpath:keycloak/test-realm.json"));
        properties.getRealm().setName("explicit-realm");

        assertThat(KeycloakRealmMetadata.resolveRealmName(properties)).isEqualTo("explicit-realm");
    }

    @Test
    void shouldNotReadImportFileWhenRealmNameIsConfigured() {
        // An application joining a shared dev service configures only the realm name, so
        // resolution must short-circuit before touching any file at all.
        KeycloakDevServicesProperties properties = new KeycloakDevServicesProperties();
        properties.getRealm().setImportPaths(List.of("classpath:no/such/realm.json"));
        properties.getRealm().setName("shared-realm");

        assertThat(KeycloakRealmMetadata.resolveRealmName(properties)).isEqualTo("shared-realm");
    }

    @Test
    void shouldFailWhenImportFileHasNoRealmField(@TempDir Path tempDir) throws IOException {
        Path realmFile = Files.writeString(tempDir.resolve("realm.json"), "{\"enabled\": true}");
        KeycloakDevServicesProperties properties = new KeycloakDevServicesProperties();
        properties.getRealm().setImportPaths(List.of(realmFile.toString()));

        assertThatThrownBy(() -> KeycloakRealmMetadata.resolveRealmName(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No 'realm' field");
    }

    @Test
    void shouldFailWhenImportFileIsNotValidJson(@TempDir Path tempDir) throws IOException {
        Path realmFile = Files.writeString(tempDir.resolve("realm.json"), "not json at all");
        KeycloakDevServicesProperties properties = new KeycloakDevServicesProperties();
        properties.getRealm().setImportPaths(List.of(realmFile.toString()));

        assertThatThrownBy(() -> KeycloakRealmMetadata.resolveRealmName(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to parse");
    }

    @Test
    void shouldReadRealmNameOfAnyImportFile(@TempDir Path tempDir) throws IOException {
        // Each file's realm name is needed to copy it under the name Keycloak expects, so
        // reading it must work independently of which file is first.
        Path realmFile = Files.writeString(tempDir.resolve("anything-at-all.json"), "{\"realm\": \"named\"}");

        assertThat(KeycloakRealmMetadata.readRealmName(realmFile.toString())).isEqualTo("named");
    }

    @Test
    void shouldFailWhenImportFileDoesNotExist() {
        KeycloakDevServicesProperties properties = new KeycloakDevServicesProperties();
        properties.getRealm().setImportPaths(List.of("classpath:no/such/realm.json"));

        assertThatThrownBy(() -> KeycloakRealmMetadata.resolveRealmName(properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Resource not found");
    }

    @Test
    void shouldResolveMasterRealmWhenNoRealmIsGeneratedOrImported() {
        // Nothing else exists in the container, so naming the realm that would have been
        // generated would point every consumer of the connection details at a missing realm.
        KeycloakDevServicesProperties properties = new KeycloakDevServicesProperties();
        properties.getRealm().setCreate(false);

        assertThat(KeycloakRealmMetadata.resolveRealmName(properties)).isEqualTo(KeycloakRealmMetadata.MASTER_REALM);
    }

    @Test
    void shouldPreferConfiguredRealmNameOverMasterRealm() {
        KeycloakDevServicesProperties properties = new KeycloakDevServicesProperties();
        properties.getRealm().setCreate(false);
        properties.getRealm().setName("explicit-realm");

        assertThat(KeycloakRealmMetadata.resolveRealmName(properties)).isEqualTo("explicit-realm");
    }

    @Test
    void shouldHaveRealmWhenOneIsGenerated() {
        assertThat(KeycloakRealmMetadata.hasRealm(new KeycloakDevServicesProperties().getRealm())).isTrue();
    }

    @Test
    void shouldHaveRealmWhenOneIsImportedEvenIfGenerationIsDisabled() {
        KeycloakDevServicesProperties properties = new KeycloakDevServicesProperties();
        properties.getRealm().setCreate(false);
        properties.getRealm().setImportPaths(List.of("classpath:keycloak/test-realm.json"));

        assertThat(KeycloakRealmMetadata.hasRealm(properties.getRealm())).isTrue();
    }

    @Test
    void shouldHaveNoRealmWhenGenerationIsDisabledAndNothingIsImported() {
        KeycloakDevServicesProperties properties = new KeycloakDevServicesProperties();
        properties.getRealm().setCreate(false);

        assertThat(KeycloakRealmMetadata.hasRealm(properties.getRealm())).isFalse();
    }

}
