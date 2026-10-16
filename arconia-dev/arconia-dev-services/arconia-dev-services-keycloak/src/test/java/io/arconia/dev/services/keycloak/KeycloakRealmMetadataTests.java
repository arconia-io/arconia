package io.arconia.dev.services.keycloak;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link KeycloakRealmMetadata}.
 */
class KeycloakRealmMetadataTests {

    private static KeycloakDevServicesProperties importing(String... paths) {
        KeycloakDevServicesProperties properties = new KeycloakDevServicesProperties();
        properties.getRealm().setImportPaths(List.of(paths));
        return properties;
    }

    @Nested
    class GeneratedRealm {

        @Test
        void shouldUseDefaultRealmWhenNothingIsConfigured() {
            KeycloakDevServicesProperties properties = new KeycloakDevServicesProperties();

            assertThat(KeycloakRealmMetadata.resolveRealmName(properties)).isEqualTo(KeycloakDevServicesProperties.DEFAULT_REALM);
            assertThat(KeycloakRealmMetadata.resolveRealmNames(properties)).containsExactly(KeycloakDevServicesProperties.DEFAULT_REALM);
        }

        @Test
        void shouldUseTheConfiguredRealmName() {
            KeycloakDevServicesProperties properties = new KeycloakDevServicesProperties();
            properties.getRealm().setName("my-realm");

            assertThat(KeycloakRealmMetadata.resolveRealmName(properties)).isEqualTo("my-realm");
            assertThat(KeycloakRealmMetadata.resolveRealmNames(properties)).containsExactly("my-realm");
        }

    }

    @Nested
    class ImportedRealms {

        @Test
        void shouldReadRealmNameFromClasspathImportFile() {
            KeycloakDevServicesProperties properties = importing("classpath:keycloak/test.json");

            assertThat(KeycloakRealmMetadata.resolveRealmName(properties)).isEqualTo("test-realm");
            assertThat(KeycloakRealmMetadata.resolveRealmNames(properties)).containsExactly("test-realm");
        }

        @Test
        void shouldReadRealmNameFromFilesystemImportFile(@TempDir Path tempDir) throws IOException {
            Path realmFile = Files.writeString(tempDir.resolve("realm.json"), """
                {"realm":"from-disk", "enabled": true}""");

            assertThat(KeycloakRealmMetadata.resolveRealmName(importing("file:" + realmFile))).isEqualTo("from-disk");
        }

        @Test
        void shouldReadEveryRealmDeclaredByAListOfRealms() {
            assertThat(KeycloakRealmMetadata.readRealmNames("classpath:keycloak/tenants.json", "arconia"))
                    .containsExactly("alpha", "beta");
        }

        @Test
        void shouldCollectTheRealmsOfEveryImportFileInOrder() {
            KeycloakDevServicesProperties properties = importing("classpath:keycloak/gamma-realm.json", "classpath:keycloak/tenants.json");

            assertThat(KeycloakRealmMetadata.resolveRealmNames(properties)).containsExactly("gamma", "alpha", "beta");
            assertThat(KeycloakRealmMetadata.resolveRealmName(properties)).isEqualTo("gamma");
        }

        @Test
        void shouldDeclareARealmOnceAcrossFiles(@TempDir Path tempDir) throws IOException {
            Path usersFile = Files.writeString(tempDir.resolve("gamma-users-0.json"), """
                {"realm":"gamma", "users": []}""");

            assertThat(KeycloakRealmMetadata.resolveRealmNames(importing("classpath:keycloak/gamma-realm.json", usersFile.toString())))
                    .containsExactly("gamma");
        }

        @Test
        void shouldConnectToTheFirstImportedRealmOtherThanMaster(@TempDir Path tempDir) throws IOException {
            Path realmFile = Files.writeString(tempDir.resolve("export.json"), """
                [{"realm":"master"}, {"realm":"dukes"}, {"realm":"beans"}]""");

            assertThat(KeycloakRealmMetadata.resolveRealmName(importing(realmFile.toString()))).isEqualTo("dukes");
        }

        @Test
        void shouldConnectToTheConfiguredRealmAmongTheImportedOnes() {
            KeycloakDevServicesProperties properties = importing("classpath:keycloak/tenants.json");
            properties.getRealm().setName("beta");

            assertThat(KeycloakRealmMetadata.resolveRealmName(properties)).isEqualTo("beta");
        }

        @Test
        void shouldFailWhenTheConfiguredRealmIsNotImported() {
            KeycloakDevServicesProperties properties = importing("classpath:keycloak/tenants.json");
            properties.getRealm().setName("delta");

            assertThatThrownBy(() -> KeycloakRealmMetadata.resolveRealmName(properties))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("[alpha, beta]")
                    .hasMessageContaining("'delta'");
        }

        @Test
        void shouldImportAMasterRealmWithItsOwnUsers(@TempDir Path tempDir) throws IOException {
            Path realmFile = Files.writeString(tempDir.resolve("export.json"), """
                [{"realm":"master","users":[{"username":"admin"}]}, {"realm":"dukes"}]""");

            assertThat(KeycloakRealmMetadata.resolveRealmNames(importing(realmFile.toString()))).containsExactly("master", "dukes");
        }

        @Test
        void shouldFailWhenTheImportedMasterRealmAlreadyHasTheAdministrator(@TempDir Path tempDir) throws IOException {
            Path realmFile = Files.writeString(tempDir.resolve("export.json"), """
                [{"realm":"master","users":[{"username":"admin"},{"username":"Arconia"}]}, {"realm":"dukes"}]""");

            assertThatThrownBy(() -> KeycloakRealmMetadata.resolveRealmNames(importing(realmFile.toString())))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("'Arconia'")
                    .hasMessageContaining("admin-username");
        }

        @Test
        void shouldImportAMasterRealmWhenTheAdministratorIsRenamed(@TempDir Path tempDir) throws IOException {
            Path realmFile = Files.writeString(tempDir.resolve("export.json"), """
                [{"realm":"master","users":[{"username":"arconia"}]}, {"realm":"dukes"}]""");
            KeycloakDevServicesProperties properties = importing(realmFile.toString());
            properties.setAdminUsername("root");

            assertThat(KeycloakRealmMetadata.resolveRealmNames(properties)).containsExactly("master", "dukes");
        }

        @Test
        void shouldFailWhenOnlyMasterIsImported(@TempDir Path tempDir) throws IOException {
            Path realmFile = Files.writeString(tempDir.resolve("export.json"), """
                [{"realm":"master"}]""");

            assertThatThrownBy(() -> KeycloakRealmMetadata.resolveRealmName(importing(realmFile.toString())))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("realm.name");
        }

    }

    @Nested
    class RealmFiles {

        @Test
        void shouldFailWhenAFileNamedAfterARealmDeclaresAnotherOne(@TempDir Path tempDir) throws IOException {
            // Keycloak itself refuses to start on this mismatch, so it is reported before the container starts.
            Path realmFile = Files.writeString(tempDir.resolve("my-realm.json"), """
                {"realm":"test-realm"}""");

            assertThatThrownBy(() -> KeycloakRealmMetadata.readRealmNames(realmFile.toString(), "arconia"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("'my-realm.json'")
                    .hasMessageContaining("single realm 'my'")
                    .hasMessageContaining("[test-realm]");
        }

        @Test
        void shouldFailWhenImportFileHasNoRealmField(@TempDir Path tempDir) throws IOException {
            Path realmFile = Files.writeString(tempDir.resolve("realm.json"), """
                {"enabled": true}""");

            assertThatThrownBy(() -> KeycloakRealmMetadata.readRealmNames(realmFile.toString(), "arconia"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No 'realm' field");
        }

        @Test
        void shouldFailWhenAListedRealmHasNoRealmField(@TempDir Path tempDir) throws IOException {
            Path realmFile = Files.writeString(tempDir.resolve("realms.json"), """
                [{"realm":"alpha"}, {"enabled": true}]""");

            assertThatThrownBy(() -> KeycloakRealmMetadata.readRealmNames(realmFile.toString(), "arconia"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No 'realm' field");
        }

        @Test
        void shouldFailWhenImportFileDeclaresNoRealm(@TempDir Path tempDir) throws IOException {
            Path realmFile = Files.writeString(tempDir.resolve("realms.json"), "[]");

            assertThatThrownBy(() -> KeycloakRealmMetadata.readRealmNames(realmFile.toString(), "arconia"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("declares no realm");
        }

        @Test
        void shouldFailWhenImportFileIsNotValidJson(@TempDir Path tempDir) throws IOException {
            Path realmFile = Files.writeString(tempDir.resolve("realm.json"), "not json at all");

            assertThatThrownBy(() -> KeycloakRealmMetadata.readRealmNames(realmFile.toString(), "arconia"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Failed to parse");
        }

        @Test
        void shouldFailWhenImportFileDoesNotExist() {
            assertThatThrownBy(() -> KeycloakRealmMetadata.readRealmNames("classpath:no/such/realm.json", "arconia"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Resource not found");
        }

    }

}
