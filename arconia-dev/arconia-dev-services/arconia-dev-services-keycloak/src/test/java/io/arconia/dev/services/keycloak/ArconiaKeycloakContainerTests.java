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
import org.junit.jupiter.api.io.TempDir;
import org.springframework.util.ReflectionUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.images.builder.Transferable;

import io.arconia.dev.services.core.container.ContainerConfigurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ArconiaKeycloakContainer}.
 */
class ArconiaKeycloakContainerTests {

    private static ArconiaKeycloakContainer container(KeycloakDevServicesProperties properties) {
        var container = new ArconiaKeycloakContainer(properties);
        ContainerConfigurer.apply(container, properties);
        return container;
    }

    @Test
    @SuppressWarnings("deprecation")
    void shouldCopyTheGeneratedRealmUnderTheNameKeycloakExpects() {
        var container = container(new KeycloakDevServicesProperties());

        assertThat(container.getRealm()).isEqualTo(KeycloakDevServicesProperties.DEFAULT_REALM);
        assertThat(transferableDestinations(container))
                .containsExactly(ArconiaKeycloakContainer.IMPORT_DIRECTORY
                        + KeycloakDevServicesProperties.DEFAULT_REALM + "-realm.json");
        assertThat(container.getCopyToFileContainerPathMap()).isEmpty();
    }

    @Test
    @SuppressWarnings("deprecation")
    void shouldCopyTheImportedRealmFilesUnderTheirOwnNames(@TempDir Path tempDir) throws IOException {
        Path realmFile = Files.writeString(tempDir.resolve("anything-at-all.json"), """
            {"realm":"from-disk"}""");
        var properties = new KeycloakDevServicesProperties();
        properties.getRealm().setImportPaths(List.of(realmFile.toString(), "classpath:keycloak/gamma-realm.json"));

        var container = container(properties);

        assertThat(container.getRealm()).isEqualTo("from-disk");
        assertThat(container.getCopyToFileContainerPathMap().values())
                .containsExactlyInAnyOrder(ArconiaKeycloakContainer.IMPORT_DIRECTORY + "anything-at-all.json",
                        ArconiaKeycloakContainer.IMPORT_DIRECTORY + "gamma-realm.json");
        assertThat(transferableDestinations(container)).isEmpty();
    }

    @Test
    void shouldFailWhenTwoImportedRealmFilesShareAName(@TempDir Path tempDir) throws IOException {
        Path realmFile = Files.writeString(tempDir.resolve("test.json"), """
            {"realm":"from-disk"}""");
        var properties = new KeycloakDevServicesProperties();
        properties.getRealm().setImportPaths(List.of(realmFile.toString(), "classpath:keycloak/test.json"));

        assertThatThrownBy(() -> container(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("'test.json'");
    }

    @Test
    @SuppressWarnings("deprecation")
    void shouldCopyRealmFilesReadableByTheKeycloakUser() {
        var properties = new KeycloakDevServicesProperties();
        properties.getRealm().setImportPaths(List.of("classpath:keycloak/test.json"));

        var container = container(properties);

        assertThat(container.getCopyToFileContainerPathMap().keySet())
                .singleElement()
                .satisfies(file -> assertThat(file.getFileMode() & 0777).isEqualTo(0644));
    }

    @Test
    void shouldFailWhenTheRealmImportPathDoesNotExist() {
        var properties = new KeycloakDevServicesProperties();
        properties.getRealm().setImportPaths(List.of("classpath:no/such/realm.json"));

        assertThatThrownBy(() -> container(properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Resource not found");
    }

    @Test
    @SuppressWarnings("deprecation")
    void shouldIgnoreGeneratedRealmUsersWhenARealmIsImported() {
        var properties = new KeycloakDevServicesProperties();
        properties.getRealm().setImportPaths(List.of("classpath:keycloak/test.json"));
        properties.getRealm().setUsers(Map.of("alice", new KeycloakDevServicesProperties.Realm.User()));

        var container = container(properties);

        assertThat(container.getRealm()).isEqualTo("test-realm");
        assertThat(container.getCopyToFileContainerPathMap().values())
                .containsExactly(ArconiaKeycloakContainer.IMPORT_DIRECTORY + "test.json");
        assertThat(transferableDestinations(container)).isEmpty();
    }

    @Test
    void shouldApplyTheStartupTimeoutToItsOwnWaitStrategy() {
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
        Map<Transferable, String> transferables = (Map<Transferable, String>) ReflectionUtils.invokeMethod(accessor, container);
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
    void shouldNotPullAnImageThatIsAvailableLocally() {
        // The container library defaults to an age-based policy measured from the date the image
        // was built, which pulls on every start and fails offline.
        var container = container(new KeycloakDevServicesProperties());

        Field pullPolicy = ReflectionUtils.findField(container.getClass(), "imagePullPolicy");
        assertThat(pullPolicy).isNotNull();
        ReflectionUtils.makeAccessible(pullPolicy);
        assertThat(ReflectionUtils.getField(pullPolicy, container))
                .isInstanceOf(PullPolicy.defaultPolicy().getClass());
    }

    @Test
    void exposesTheHttpPort() {
        var container = container(new KeycloakDevServicesProperties());

        container.configure();

        assertThat(container.getExposedPorts()).contains(ArconiaKeycloakContainer.HTTP_PORT);
    }

}
