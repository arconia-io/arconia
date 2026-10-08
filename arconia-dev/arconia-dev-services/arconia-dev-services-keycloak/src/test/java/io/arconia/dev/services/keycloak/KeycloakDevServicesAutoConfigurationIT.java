package io.arconia.dev.services.keycloak;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationIT;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link KeycloakDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class KeycloakDevServicesAutoConfigurationIT extends BaseDevServicesAutoConfigurationIT {

    private final ApplicationContextRunner contextRunner = defaultContextRunner(KeycloakDevServicesAutoConfiguration.class);

    @Override
    protected ApplicationContextRunner getContextRunner() {
        return contextRunner;
    }

    @Override
    protected Class<?> getAutoConfigurationClass() {
        return KeycloakDevServicesAutoConfiguration.class;
    }

    @Override
    protected Class<? extends GenericContainer<?>> getContainerClass() {
        return ArconiaKeycloakContainer.class;
    }

    @Override
    protected String getServiceName() {
        return "keycloak";
    }

    @Override
    protected Class<?> getConnectionDetailsClass() {
        return KeycloakConnectionDetails.class;
    }

    private static ArconiaKeycloakContainer defaultContainer() {
        return new ArconiaKeycloakContainer(new KeycloakDevServicesProperties());
    }

    @Override
    protected GenericContainer<?> createDiscoverableContainer(String ownerId) {
        var properties = new KeycloakDevServicesProperties();
        return asDiscoverableContainer(new ArconiaKeycloakContainer(properties), properties, ownerId);
    }

    @Override
    protected void assertDiscoveredConnectionDetails(AssertableApplicationContext context, GenericContainer<?> discoveredContainer) {
        ArconiaKeycloakContainer container = (ArconiaKeycloakContainer) discoveredContainer;
        KeycloakConnectionDetails connectionDetails = context.getBean(KeycloakConnectionDetails.class);
        assertThat(connectionDetails.getAuthServerUrl()).isEqualTo(container.getAuthServerUrl());
        assertThat(connectionDetails.getRealm()).isEqualTo(container.getRealm());
        assertThat(connectionDetails.getIssuerUri()).isEqualTo(container.getIssuerUri());
    }

    @Test
    void adminCredentialsApplied() {
        getContextRunner()
                .withPropertyValues(
                        "arconia.dev.services.%s.admin-username=keycloak".formatted(getServiceName()),
                        "arconia.dev.services.%s.admin-password=s3cret".formatted(getServiceName()))
                .run(context -> {
                    var container = (ArconiaKeycloakContainer) context.getBean(getContainerClass());
                    assertThat(container.getAdminUsername()).isEqualTo("keycloak");
                    assertThat(container.getAdminPassword()).isEqualTo("s3cret");
                });
    }

    @Test
    void devServiceLinksExposeAdminConsoleUrl() {
        contextRunnerWithContainerLifecycle().run(context -> {
            String adminConsoleUrl = context.getBean(DevServiceRegistration.class).links().getFirst().url();
            assertThat(adminConsoleUrl).endsWith("/admin");
            assertThat(get(adminConsoleUrl).statusCode()).isLessThan(400);
        });
    }

    @Test
    void generatedRealmImported() {
        getContextRunner().run(context -> {
            var container = (ArconiaKeycloakContainer) context.getBean(getContainerClass());
            container.start();
            try {
                assertRealmIsUsable(container.getIssuerUri(), "isabella", "isabella",
                        KeycloakDevServicesProperties.Client.DEFAULT_CLIENT_ID,
                        KeycloakDevServicesProperties.Client.DEFAULT_CLIENT_SECRET,
                        List.of("admin", "user"));
                // The second default user carries the narrower role set.
                assertRealmIsUsable(container.getIssuerUri(), "bjorn", "bjorn",
                        KeycloakDevServicesProperties.Client.DEFAULT_CLIENT_ID,
                        KeycloakDevServicesProperties.Client.DEFAULT_CLIENT_SECRET,
                        List.of("user"));
            }
            finally {
                container.stop();
            }
        });
    }

    @Test
    void generatedRealmWithConfiguredUsersAndRoles() {
        // The test that keeps configured users honest, in both configuration forms. A user given
        // as a password alone has its email address and names derived, and Keycloak's default
        // user profile — which requires all three — would reject the direct access grant with
        // "Account is not fully set up" if that derivation stopped happening. Nothing short of
        // obtaining a real token catches it, and nothing but a real Keycloak proves that a
        // stated identity is accepted where a derived one was.
        getContextRunner()
                .withPropertyValues(
                        "arconia.dev.services.%s.realm.users.alice=alice-password".formatted(getServiceName()),
                        "arconia.dev.services.%s.realm.users.bob.password=bob-password".formatted(getServiceName()),
                        "arconia.dev.services.%s.realm.users.bob.first-name=Bjorn".formatted(getServiceName()),
                        "arconia.dev.services.%s.realm.users.bob.last-name=Vinterberg".formatted(getServiceName()),
                        "arconia.dev.services.%s.realm.users.bob.email=bjorn@example.com".formatted(getServiceName()),
                        "arconia.dev.services.%s.realm.roles.editor[0]=alice".formatted(getServiceName()),
                        "arconia.dev.services.%s.realm.roles.reader[0]=alice".formatted(getServiceName()),
                        "arconia.dev.services.%s.realm.roles.reader[1]=bob".formatted(getServiceName()))
                .run(context -> {
                    var container = (ArconiaKeycloakContainer) context.getBean(getContainerClass());
                    container.start();
                    try {
                        assertRealmIsUsable(container.getIssuerUri(), "alice", "alice-password",
                                KeycloakDevServicesProperties.Client.DEFAULT_CLIENT_ID,
                                KeycloakDevServicesProperties.Client.DEFAULT_CLIENT_SECRET,
                                List.of("editor", "reader"));
                        assertRealmIsUsable(container.getIssuerUri(), "bob", "bob-password",
                                KeycloakDevServicesProperties.Client.DEFAULT_CLIENT_ID,
                                KeycloakDevServicesProperties.Client.DEFAULT_CLIENT_SECRET,
                                List.of("reader"));
                    }
                    finally {
                        container.stop();
                    }
                });
    }

    @Test
    void generatedRealmDeclaresTheClientTheApplicationConfigured() {
        // Both places a client can be named, proven against a real Keycloak by actually obtaining
        // a token as that client. The dev service property is the resource-server fallback, and
        // the registration is what an OAuth2 client would use; each must reach the realm.
        getContextRunner()
                .withPropertyValues(
                        "arconia.dev.services.%s.client.id=fallback-client".formatted(getServiceName()),
                        "arconia.dev.services.%s.client.secret=fallback-secret".formatted(getServiceName()))
                .run(context -> {
                    var container = (ArconiaKeycloakContainer) context.getBean(getContainerClass());
                    container.start();
                    try {
                        assertRealmIsUsable(container.getIssuerUri(), "isabella", "isabella",
                                "fallback-client", "fallback-secret", List.of("admin", "user"));
                    }
                    finally {
                        container.stop();
                    }
                });
    }

    @Test
    void realmImportedFromClasspath() {
        getContextRunner()
                .withPropertyValues("arconia.dev.services.%s.realm.import-paths=classpath:keycloak/test-realm.json"
                        .formatted(getServiceName()))
                .run(context -> {
                    var container = (ArconiaKeycloakContainer) context.getBean(getContainerClass());
                    assertThat(container.getRealm()).isEqualTo("test-realm");
                    container.start();
                    try {
                        assertRealmIsUsable(container.getIssuerUri(), "tessa", "tessa",
                                "test-client", "test-secret", List.of("tester"));
                    }
                    finally {
                        container.stop();
                    }
                });
    }

    @Test
    void realmImportedFromFileSystem(@TempDir Path tempDir) throws IOException {
        Path realmFile = tempDir.resolve("disk-realm.json");
        Files.writeString(realmFile, readBundledTestRealm().replace("test-realm", "disk-realm"));

        getContextRunner()
                .withPropertyValues("arconia.dev.services.%s.realm.import-paths=file:%s"
                        .formatted(getServiceName(), realmFile))
                .run(context -> {
                    var container = (ArconiaKeycloakContainer) context.getBean(getContainerClass());
                    assertThat(container.getRealm()).isEqualTo("disk-realm");
                    container.start();
                    try {
                        assertRealmIsUsable(container.getIssuerUri(), "tessa", "tessa",
                                "test-client", "test-secret", List.of("tester"));
                    }
                    finally {
                        container.stop();
                    }
                });
    }

    /**
     * Assert that the realm behind the given issuer is actually usable: the discovery document
     * advertises the expected issuer, the confidential client authenticates, the user exists,
     * and the expected realm roles are present in the issued token.
     */
    private static void assertRealmIsUsable(String issuerUri, String username, String password,
            String clientId, String clientSecret, List<String> expectedRoles) throws Exception {
        Map<String, Object> discoveryDocument = parseJson(
                get(issuerUri + "/.well-known/openid-configuration").body());
        assertThat(discoveryDocument.get("issuer")).isEqualTo(issuerUri);

        String form = "grant_type=password&username=%s&password=%s&client_id=%s&client_secret=%s&scope=openid"
                .formatted(username, password, clientId, clientSecret);
        HttpResponse<String> tokenResponse = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create((String) discoveryDocument.get("token_endpoint")))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(form))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(tokenResponse.statusCode())
                .withFailMessage("token request failed: %s", tokenResponse.body())
                .isEqualTo(200);

        Map<String, Object> token = parseJson(tokenResponse.body());
        // An id_token is only issued when the openid scope resolves, so its presence proves
        // the realm supports OpenID Connect login and not merely plain OAuth2.
        assertThat(token).containsKey("id_token");

        Map<String, Object> claims = parseJson(decodeJwtPayload((String) token.get("access_token")));
        @SuppressWarnings("unchecked")
        Map<String, Object> realmAccess = (Map<String, Object>) claims.get("realm_access");
        assertThat(realmAccess).isNotNull();
        @SuppressWarnings("unchecked")
        List<Object> roles = (List<Object>) realmAccess.get("roles");
        assertThat(roles).containsAll(expectedRoles);
    }

    private static HttpResponse<String> get(String url) throws Exception {
        return HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static Map<String, Object> parseJson(String json) {
        return JsonParserFactory.getJsonParser().parseMap(json);
    }

    private static String decodeJwtPayload(String jwt) {
        String payload = jwt.split("\\.")[1];
        return new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
    }

    private static String readBundledTestRealm() throws IOException {
        try (var input = KeycloakDevServicesAutoConfigurationIT.class.getClassLoader()
                .getResourceAsStream("keycloak/test-realm.json")) {
            assertThat(input).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

}
