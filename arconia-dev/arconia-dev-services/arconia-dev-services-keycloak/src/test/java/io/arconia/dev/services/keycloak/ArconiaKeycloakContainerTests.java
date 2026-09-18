package io.arconia.dev.services.keycloak;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.Transferable;

import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.util.ReflectionUtils;

import io.arconia.dev.services.api.registration.DevServiceLinkDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ArconiaKeycloakContainer}.
 */
@ExtendWith(OutputCaptureExtension.class)
class ArconiaKeycloakContainerTests {

    private static ArconiaKeycloakContainer container(KeycloakDevServicesProperties properties) {
        return new ArconiaKeycloakContainer(properties);
    }

    @Test
    void whenExposedPortsAreNotConfigured() {
        var container = container(new KeycloakDevServicesProperties());
        container.configure();
        assertThat(container.getPortBindings()).isEmpty();
    }

    @Test
    void whenExposedPortsAreConfigured() {
        var properties = new KeycloakDevServicesProperties();
        properties.setPort(1234);

        var container = container(properties);
        container.configure();

        assertThat(container.getPortBindings())
                .anyMatch(binding -> binding.startsWith(
                        properties.getPort() + ":" + ArconiaKeycloakContainer.HTTP_PORT));
    }

    @Test
    void shouldGenerateARealmWhenNoImportPathIsConfigured() {
        var container = container(new KeycloakDevServicesProperties());

        assertThat(container.getGeneratedRealmJson()).isNotNull();
        assertThat(container.getRealm()).isEqualTo(KeycloakDevServicesProperties.DEFAULT_REALM);
        // Nothing is copied from disk: the generated realm is transferred as content.
        assertThat(container.getCopyToFileContainerPathMap()).isEmpty();
    }

    @Test
    void shouldNotGenerateARealmWhenImportPathsAreConfigured() {
        var properties = new KeycloakDevServicesProperties();
        properties.getRealm().setImportPaths(List.of("classpath:keycloak/test-realm.json"));

        var container = container(properties);

        assertThat(container.getGeneratedRealmJson()).isNull();
    }

    @Test
    void shouldCopyNothingWhenRealmCreationIsDisabledAndNothingIsImported() {
        // Keycloak is left with only its master realm, for an application configuring
        // Spring Security itself.
        var properties = new KeycloakDevServicesProperties();
        properties.getRealm().setCreate(false);

        var container = container(properties);

        assertThat(container.getGeneratedRealmJson()).isNull();
        assertThat(container.getCopyToFileContainerPathMap()).isEmpty();
        assertThat(container.getRealm()).isEqualTo(KeycloakRealmMetadata.MASTER_REALM);
    }

    @Test
    void shouldStillImportRealmFilesWhenRealmCreationIsDisabled() {
        // Disabling generation turns off exactly that, leaving configured realm files alone.
        var properties = new KeycloakDevServicesProperties();
        properties.getRealm().setCreate(false);
        properties.getRealm().setImportPaths(List.of("classpath:keycloak/test-realm.json"));

        var container = container(properties);

        assertThat(container.getGeneratedRealmJson()).isNull();
        assertThat(container.getCopyToFileContainerPathMap().values())
                .containsExactly(ArconiaKeycloakContainer.IMPORT_DIRECTORY + "test-realm-realm.json");
    }

    @Test
    void shouldCopyRealmImportFilesNamedAfterTheRealmTheyDeclare(@TempDir Path tempDir) throws IOException {
        // Keycloak imports a directory by convention and refuses to start unless each file is
        // named "<realm>-realm.json", so the source file name is deliberately not preserved.
        Path fromDisk = Files.writeString(tempDir.resolve("anything-at-all.json"), "{\"realm\": \"from-disk\"}");
        var properties = new KeycloakDevServicesProperties();
        properties.getRealm().setImportPaths(List.of("classpath:keycloak/test-realm.json", fromDisk.toString()));

        var container = container(properties);

        assertThat(container.getCopyToFileContainerPathMap().values())
                .containsExactlyInAnyOrder(
                        ArconiaKeycloakContainer.IMPORT_DIRECTORY + "test-realm-realm.json",
                        ArconiaKeycloakContainer.IMPORT_DIRECTORY + "from-disk-realm.json");
        // No realm is generated alongside a configured one.
        assertThat(container.getGeneratedRealmJson()).isNull();
        assertThat(container.getRealm()).isEqualTo("test-realm");
    }

    @Test
    void shouldFailWhenRealmImportPathsDeclareTheSameRealm(@TempDir Path tempDir) throws IOException {
        // Files are named after the realms they declare, so two files declaring the same realm
        // would overwrite each other and silently drop one.
        Path first = Files.writeString(tempDir.resolve("first.json"), "{\"realm\": \"duplicated\"}");
        Path second = Files.writeString(tempDir.resolve("second.json"), "{\"realm\": \"duplicated\"}");
        var properties = new KeycloakDevServicesProperties();
        properties.getRealm().setImportPaths(List.of(first.toString(), second.toString()));

        assertThatThrownBy(() -> container(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("duplicated");
    }

    @Test
    void shouldFailWhenARealmImportFileDeclaresNoRealm(@TempDir Path tempDir) throws IOException {
        Path realmFile = Files.writeString(tempDir.resolve("realm.json"), "{\"enabled\": true}");
        var properties = new KeycloakDevServicesProperties();
        properties.getRealm().setImportPaths(List.of(realmFile.toString()));

        assertThatThrownBy(() -> container(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No 'realm' field");
    }

    @Test
    void shouldFailWhenARealmImportPathDoesNotExist() {
        var properties = new KeycloakDevServicesProperties();
        properties.getRealm().setImportPaths(List.of("classpath:no/such/realm.json"));

        assertThatThrownBy(() -> container(properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Resource not found");
    }

    @Test
    void shouldWarnThatConfiguredUsersAndRolesAreIgnoredWhenARealmIsImported(CapturedOutput output) {
        // A realm file is a complete description of a realm, so it wins outright and nothing is
        // generated beside it. Configured users and roles are then simply absent, and a login
        // failing for a user that configuration clearly declares is a puzzling way to find out.
        var properties = new KeycloakDevServicesProperties();
        properties.getRealm().setImportPaths(List.of("classpath:keycloak/test-realm.json"));
        properties.getRealm().setUsers(Map.of("alice", KeycloakDevServicesProperties.Realm.User.of("s3cret")));
        properties.getRealm().setRoles(Map.of("editor", List.of("alice")));

        container(properties);

        assertThat(output).contains(KeycloakDevServicesProperties.CONFIG_PREFIX + ".realm.users")
                .contains(KeycloakDevServicesProperties.CONFIG_PREFIX + ".realm.roles");
    }

    @Test
    void shouldNotWarnWhenNoUsersOrRolesAreConfiguredAlongsideAnImportedRealm(CapturedOutput output) {
        var properties = new KeycloakDevServicesProperties();
        properties.getRealm().setImportPaths(List.of("classpath:keycloak/test-realm.json"));

        container(properties);

        assertThat(output).doesNotContain("are ignored").doesNotContain("is ignored");
    }

    @Test
    void shouldFailWhenTheConfiguredRealmIsNotOneOfTheImportedOnes() {
        // Nothing later would say so: the issuer URI would point at a realm that does not exist,
        // and the discovery endpoint would answer 404 without ever mentioning the realm.
        var properties = new KeycloakDevServicesProperties();
        properties.getRealm().setImportPaths(List.of("classpath:keycloak/test-realm.json"));
        properties.getRealm().setName("not-imported");

        assertThatThrownBy(() -> container(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not-imported")
                .hasMessageContaining("test-realm")
                .hasMessageContaining(KeycloakDevServicesProperties.CONFIG_PREFIX + ".realm.name");
    }

    @Test
    void shouldAcceptAConfiguredRealmThatIsOneOfTheImportedOnes(@TempDir Path tempDir) throws IOException {
        // Naming one of several imported realms explicitly is how an application says which of
        // them it connects to, rather than taking the first.
        Path second = Files.writeString(tempDir.resolve("second.json"), "{\"realm\": \"chosen\"}");
        var properties = new KeycloakDevServicesProperties();
        properties.getRealm().setImportPaths(List.of("classpath:keycloak/test-realm.json", second.toString()));
        properties.getRealm().setName("chosen");

        assertThat(container(properties).getRealm()).isEqualTo("chosen");
    }

    @Test
    void shouldNotValidateTheConfiguredRealmWhenNothingIsImported() {
        // With no realm files, the name is free to point at a realm another application imported
        // into a shared dev service, which is exactly what realm.name is for.
        var properties = new KeycloakDevServicesProperties();
        properties.getRealm().setName("imported-by-someone-else");

        assertThat(container(properties).getRealm()).isEqualTo("imported-by-someone-else");
    }

    @Test
    void shouldCopyTheGeneratedRealmUnderTheNameKeycloakExpects() {
        // The content is covered by KeycloakRealmFactoryTests; what matters here is that it lands
        // at the path Keycloak imports from, named after the realm it declares.
        var container = container(new KeycloakDevServicesProperties());

        assertThat(transferableDestinations(container))
                .containsExactly(ArconiaKeycloakContainer.IMPORT_DIRECTORY
                        + KeycloakDevServicesProperties.DEFAULT_REALM + "-realm.json");
    }

    @Test
    void shouldCopyRealmFilesReadableByTheKeycloakUser() {
        // A resource extracted from a jar carries no usable permissions, and Keycloak runs as a
        // non-root user that would otherwise be unable to read the realm it is told to import.
        var properties = new KeycloakDevServicesProperties();
        properties.getRealm().setImportPaths(List.of("classpath:keycloak/test-realm.json"));

        var container = container(properties);

        assertThat(container.getCopyToFileContainerPathMap().keySet())
                .singleElement()
                // Masked to the permission bits: the mode also carries the file type, so a
                // regular file reads as 0100644 rather than 0644.
                .satisfies(file -> assertThat(file.getFileMode() & 0777).isEqualTo(0644));
    }

    @Test
    void shouldUseTheDefaultImage() {
        var container = container(new KeycloakDevServicesProperties());

        assertThat(container.getDockerImageName()).isEqualTo("quay.io/keycloak/keycloak:26.7");
    }

    @Test
    void shouldApplyTheStartupTimeoutToItsOwnWaitStrategy() {
        // Load-bearing, and entirely dependent on the container library: ExtendableKeycloakContainer
        // overrides withStartupTimeout(Duration) to keep the value for itself. Without that
        // override, ContainerConfigurer.base would stamp this timeout onto
        // GenericContainer.DEFAULT_WAIT_STRATEGY — a static instance every container that hasn't
        // declared its own wait strategy shares — and every other dev service in the JVM would
        // silently inherit Keycloak's two-minute timeout.
        var properties = new KeycloakDevServicesProperties();
        properties.setStartupTimeout(Duration.ofSeconds(97));

        var container = container(properties);
        container.configure();

        assertThat(container.getStartupTimeout()).isEqualTo(Duration.ofSeconds(97));
        assertThat(sharedDefaultWaitStrategyTimeout()).isNotEqualTo(Duration.ofSeconds(97));
    }

    /**
     * The destinations of the files copied as content rather than from disk, which is how the
     * generated realm is transferred. Testcontainers keeps them in a package-private map with no
     * public accessor, so they are read reflectively rather than exposed by the container itself.
     */
    private static Collection<String> transferableDestinations(ArconiaKeycloakContainer container) {
        Method accessor = ReflectionUtils.findMethod(GenericContainer.class, "getCopyToTransferableContainerPathMap");
        assertThat(accessor).isNotNull();
        ReflectionUtils.makeAccessible(accessor);
        @SuppressWarnings("unchecked")
        Map<Transferable, String> transferables =
                (Map<Transferable, String>) ReflectionUtils.invokeMethod(accessor, container);
        assertThat(transferables).isNotNull();
        return transferables.values();
    }

    /**
     * The startup timeout of the wait strategy instance Testcontainers shares across every
     * container that has not declared one of its own.
     */
    private static Duration sharedDefaultWaitStrategyTimeout() {
        Field defaultWaitStrategy = ReflectionUtils.findField(GenericContainer.class, "DEFAULT_WAIT_STRATEGY");
        assertThat(defaultWaitStrategy).isNotNull();
        ReflectionUtils.makeAccessible(defaultWaitStrategy);
        Object waitStrategy = ReflectionUtils.getField(defaultWaitStrategy, null);
        assertThat(waitStrategy).isNotNull();

        Field startupTimeout = ReflectionUtils.findField(waitStrategy.getClass(), "startupTimeout");
        assertThat(startupTimeout).isNotNull();
        ReflectionUtils.makeAccessible(startupTimeout);
        return (Duration) ReflectionUtils.getField(startupTimeout, waitStrategy);
    }

    @Test
    void shouldApplyAdminCredentials() {
        var properties = new KeycloakDevServicesProperties();
        properties.setAdminUsername("keycloak");
        properties.setAdminPassword("s3cret");

        var container = container(properties);

        assertThat(container.getAdminUsername()).isEqualTo("keycloak");
        assertThat(container.getAdminPassword()).isEqualTo("s3cret");
    }

    @Test
    void shouldNotExposeHostPortsWhenNoneAreConfigured() {
        var container = container(new KeycloakDevServicesProperties());

        container.configure();

        assertThat(container.getExposedPorts()).contains(ArconiaKeycloakContainer.HTTP_PORT);
    }

    @Test
    void shouldExposeTheAdminConsoleLink() {
        var container = container(new KeycloakDevServicesProperties());

        List<DevServiceLinkDefinition> links = container.devServiceLinkDefinitions();

        assertThat(links).singleElement().satisfies(link -> {
            assertThat(link.id()).isEqualTo("keycloak");
            assertThat(link.label()).isEqualTo("Keycloak Admin Console");
            assertThat(link.port()).isEqualTo(ArconiaKeycloakContainer.HTTP_PORT);
            assertThat(link.path()).isEqualTo("/admin");
        });
    }

}
