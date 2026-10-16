package io.arconia.dev.services.keycloak;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.testcontainers.containers.GenericContainer;

import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationTests;
import io.arconia.dev.services.tests.DisabledIfDockerUnavailable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link KeycloakDevServicesAutoConfiguration}.
 */
class KeycloakDevServicesAutoConfigurationTests extends BaseDevServicesAutoConfigurationTests {

    KeycloakDevServicesAutoConfigurationTests() {
        super(KeycloakDevServicesAutoConfiguration.class, "keycloak", ArconiaKeycloakContainer.class, KeycloakConnectionDetails.class);
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
        assertThat(connectionDetails.getIssuerUri()).isEqualTo(issuerUri(container));
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
    @DisabledIfDockerUnavailable
    void devServiceLinksExposeAdminConsoleAndIssuerUrls() {
        contextRunnerWithContainerLifecycle().run(context -> {
            var links = context.getBean(DevServiceRegistration.class).links();
            String adminConsoleUrl = links.getFirst().url();
            assertThat(adminConsoleUrl).endsWith("/admin");
            assertThat(get(adminConsoleUrl).statusCode()).isLessThan(400);
            String issuerUrl = links.get(1).url();
            assertThat(issuerUrl).endsWith("/realms/" + KeycloakDevServicesProperties.DEFAULT_REALM);
            assertThat(get(issuerUrl).statusCode()).isEqualTo(200);
        });
    }

    @Test
    @DisabledIfDockerUnavailable
    void generatedRealmImported() {
        getContextRunner().run(context -> {
            var container = (ArconiaKeycloakContainer) context.getBean(getContainerClass());
            container.start();
            try {
                Map<String, Object> token = assertRealmIsUsable(issuerUri(container), "isabella", "isabella",
                        KeycloakDevServicesProperties.Client.DEFAULT_CLIENT_ID,
                        KeycloakDevServicesProperties.Client.DEFAULT_CLIENT_SECRET,
                        List.of("admin", "user"));
                assertThat(realmRoles(parseJson(decodeJwtPayload((String) token.get("id_token")))))
                        .containsExactlyInAnyOrder("admin", "user");
                HttpResponse<String> userInfo = HttpClient.newHttpClient().send(
                        HttpRequest.newBuilder(URI.create(issuerUri(container) + "/protocol/openid-connect/userinfo"))
                                .header("Authorization", "Bearer " + token.get("access_token"))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
                assertThat(realmRoles(parseJson(userInfo.body()))).containsExactlyInAnyOrder("admin", "user");
                // The second default user carries the narrower role set.
                assertRealmIsUsable(issuerUri(container), "bjorn", "bjorn",
                        KeycloakDevServicesProperties.Client.DEFAULT_CLIENT_ID,
                        KeycloakDevServicesProperties.Client.DEFAULT_CLIENT_SECRET,
                        List.of("user"));
            } finally {
                container.stop();
            }
        });
    }

    @Test
    @DisabledIfDockerUnavailable
    void generatedRealmWithConfiguredUsersRolesAndClient() {
        getContextRunner()
                .withPropertyValues(
                        "arconia.dev.services.%s.realm.users.alice.password=alice-password".formatted(getServiceName()),
                        "arconia.dev.services.%s.realm.users.alice.roles=editor,reader".formatted(getServiceName()),
                        "arconia.dev.services.%s.realm.users.bob.password=bob-password".formatted(getServiceName()),
                        "arconia.dev.services.%s.realm.users.bob.roles=reader".formatted(getServiceName()),
                        "arconia.dev.services.%s.client.id=my-client".formatted(getServiceName()),
                        "arconia.dev.services.%s.client.secret=my-secret".formatted(getServiceName()))
                .run(context -> {
                    var container = (ArconiaKeycloakContainer) context.getBean(getContainerClass());
                    container.start();
                    try {
                        assertRealmIsUsable(issuerUri(container), "alice", "alice-password",
                                "my-client", "my-secret", List.of("editor", "reader"));
                        assertRealmIsUsable(issuerUri(container), "bob", "bob-password",
                                "my-client", "my-secret", List.of("reader"));
                    } finally {
                        container.stop();
                    }
                });
    }

    @Test
    @DisabledIfDockerUnavailable
    void realmImported() {
        getContextRunner()
                .withPropertyValues("arconia.dev.services.%s.realm.import-paths=classpath:keycloak/test.json"
                        .formatted(getServiceName()))
                .run(context -> {
                    var container = (ArconiaKeycloakContainer) context.getBean(getContainerClass());
                    assertThat(container.getRealm()).isEqualTo("test-realm");
                    container.start();
                    try {
                        assertRealmIsUsable(issuerUri(container), "tessa", "tessa",
                                "test-client", "test-secret", List.of("tester"));
                    } finally {
                        container.stop();
                    }
                });
    }

    @Test
    @DisabledIfDockerUnavailable
    void severalRealmsImportedFromSeveralFiles() {
        contextRunnerWithContainerLifecycle()
                .withPropertyValues(
                        "arconia.dev.services.%s.realm.import-paths=classpath:keycloak/tenants.json,classpath:keycloak/gamma-realm.json"
                                .formatted(getServiceName()),
                        "arconia.dev.services.%s.realm.name=beta".formatted(getServiceName()))
                .run(context -> {
                    var container = (ArconiaKeycloakContainer) context.getBean(getContainerClass());
                    assertThat(container.getRealm()).isEqualTo("beta");
                    assertThat(context.getBean(DevServiceRegistration.class).links())
                            .extracting(link -> link.url())
                            .containsExactly(
                                    container.getAuthServerUrl() + "/admin",
                                    container.getAuthServerUrl() + "/realms/alpha",
                                    container.getAuthServerUrl() + "/realms/beta",
                                    container.getAuthServerUrl() + "/realms/gamma");
                    String realms = container.getAuthServerUrl() + "/realms/";
                    assertRealmIsUsable(realms + "alpha", "alice", "alice", "alpha-client", "alpha-client-secret", List.of("alpha-role"));
                    assertRealmIsUsable(realms + "beta", "bob", "bob", "beta-client", "beta-client-secret", List.of("beta-role"));
                    assertRealmIsUsable(realms + "gamma", "gina", "gina", "gamma-client", "gamma-client-secret", List.of("gamma-role"));
                });
    }

    private static String issuerUri(ArconiaKeycloakContainer container) {
        return container.getAuthServerUrl() + "/realms/" + container.getRealm();
    }

    /**
     * Assert that the realm behind the given issuer is actually usable: the discovery document
     * advertises the expected issuer, the confidential client authenticates, the user exists,
     * and the expected realm roles are present in the issued token.
     */
    private static Map<String, Object> assertRealmIsUsable(String issuerUri, String username, String password,
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
        return token;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> realmRoles(Map<String, Object> claims) {
        Map<String, Object> realmAccess = (Map<String, Object>) claims.get("realm_access");
        assertThat(realmAccess).withFailMessage("no realm_access claim in %s", claims).isNotNull();
        return (List<Object>) realmAccess.get("roles");
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

}
