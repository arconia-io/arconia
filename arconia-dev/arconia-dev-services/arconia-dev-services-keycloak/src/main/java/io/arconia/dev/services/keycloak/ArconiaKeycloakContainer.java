package io.arconia.dev.services.keycloak;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import org.springframework.util.StringUtils;

import io.arconia.dev.services.api.registration.DevServiceLinkDefinition;
import io.arconia.dev.services.api.registration.DevServiceLinkProvider;
import io.arconia.dev.services.core.container.ContainerConfigurer;
import io.arconia.dev.services.core.util.ContainerUtils;

/**
 * A {@link KeycloakContainer} configured for use with Arconia Dev Services.
 */
final class ArconiaKeycloakContainer extends KeycloakContainer implements DevServiceLinkProvider {

    private static final Logger logger = LoggerFactory.getLogger(ArconiaKeycloakContainer.class);

    private final KeycloakDevServicesProperties properties;

    private final String realm;

    @Nullable
    private String generatedRealmJson;

    static final String COMPATIBLE_IMAGE_NAME = "quay.io/keycloak/keycloak";

    static final int HTTP_PORT = 8080;

    static final String IMPORT_DIRECTORY = "/opt/keycloak/data/import/";

    /**
     * Realm files are copied with an explicit file mode: a resource extracted from a jar
     * carries no usable permissions, and Keycloak runs as a non-root user that would
     * otherwise be unable to read the imported realm.
     */
    private static final int REALM_FILE_MODE = 0644;

    ArconiaKeycloakContainer(KeycloakDevServicesProperties properties) {
        super(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        this.properties = properties;
        // Every configured realm file is read once, here, and the names carried through: each is
        // needed both to resolve the realm to connect to and to name the file in the container.
        Map<String, String> importedRealms = KeycloakRealmMetadata.readRealmNames(properties.getRealm().getImportPaths());
        this.realm = KeycloakRealmMetadata.resolveRealmName(properties, importedRealms.values());

        this.withAdminUsername(properties.getAdminUsername());
        this.withAdminPassword(properties.getAdminPassword());

        validateConfiguredRealmIsImported(importedRealms.values());
        copyRealmImportFiles(importedRealms);

        ContainerConfigurer.base(this, properties);
    }

    private void validateConfiguredRealmIsImported(Collection<String> importedRealms) {
        String configuredRealm = properties.getRealm().getName();
        if (importedRealms.isEmpty() || !StringUtils.hasText(configuredRealm)
                || importedRealms.contains(configuredRealm)) {
            return;
        }
        throw new IllegalStateException(
                "The realm '%s' configured in '%s.realm.name' is not declared by any of the configured realm import paths, which declare: %s."
                        .formatted(configuredRealm, KeycloakDevServicesProperties.CONFIG_PREFIX, importedRealms));
    }

    private void copyRealmImportFiles(Map<String, String> importedRealms) {
        if (importedRealms.isEmpty()) {
            if (properties.getRealm().isCreate()) {
                copyGeneratedRealm();
            }
            return;
        }

        warnAboutIgnoredRealmConfiguration();
        importedRealms.forEach(this::copyRealmImportFile);
    }

    private void warnAboutIgnoredRealmConfiguration() {
        List<String> ignored = new ArrayList<>();
        if (!properties.getRealm().getUsers().isEmpty()) {
            ignored.add("realm.users");
        }
        if (!properties.getRealm().getRoles().isEmpty()) {
            ignored.add("realm.roles");
        }
        if (ignored.isEmpty()) {
            return;
        }
        logger.warn("Keycloak Dev Service: {} realm import path(s) are configured, so the realm is imported as it is and {} {} ignored. Declare the users and roles in the realm file itself, or remove the import paths to have a realm generated from configuration.",
                properties.getRealm().getImportPaths().size(),
                ignored.stream().map(name -> "'%s.%s'".formatted(KeycloakDevServicesProperties.CONFIG_PREFIX, name))
                        .collect(Collectors.joining(" and ")),
                ignored.size() == 1 ? "is" : "are");
    }

    private void copyGeneratedRealm() {
        this.generatedRealmJson = KeycloakRealmFactory.generateRealmJson(properties);
        this.withCopyToContainer(Transferable.of(generatedRealmJson, REALM_FILE_MODE),
                IMPORT_DIRECTORY + importFileName(realm));
    }

    @Nullable
    String getGeneratedRealmJson() {
        return generatedRealmJson;
    }

    private void copyRealmImportFile(String sourcePath, String realmName) {
        MountableFile realmFile = ContainerConfigurer.resolveMountableFile(sourcePath, REALM_FILE_MODE);
        this.withCopyFileToContainer(realmFile, IMPORT_DIRECTORY + importFileName(realmName));
    }

    static String importFileName(String realmName) {
        return realmName + "-realm.json";
    }

    @Override
    protected void configure() {
        super.configure();
        if (ContainerUtils.isFixedPort(properties.getPort())) {
            addFixedExposedPort(properties.getPort(), HTTP_PORT);
        }
        // Publishes host ports into the container's network namespace, so Keycloak can call
        // back into the application (for example for Back-Channel Logout). Must happen before
        // the container starts, which is why it lives here rather than in the registrar.
        for (Integer hostPort : properties.getHostAccessiblePorts()) {
            Testcontainers.exposeHostPorts(hostPort);
        }
    }

    String getRealm() {
        return realm;
    }

    String getIssuerUri() {
        return "%s/realms/%s".formatted(getAuthServerUrl(), realm);
    }

    @Override
    public List<DevServiceLinkDefinition> devServiceLinkDefinitions() {
        return List.of(DevServiceLinkDefinition.builder()
                .id("keycloak")
                .label("Keycloak Admin Console")
                .port(HTTP_PORT)
                .path("/admin")
                .build());
    }

}
