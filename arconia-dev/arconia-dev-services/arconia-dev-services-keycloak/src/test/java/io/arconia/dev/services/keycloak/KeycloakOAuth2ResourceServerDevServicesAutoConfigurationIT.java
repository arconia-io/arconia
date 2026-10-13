package io.arconia.dev.services.keycloak;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import io.arconia.boot.bootstrap.BootstrapMode;
import io.arconia.dev.services.tests.DisabledIfDockerUnavailable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link KeycloakOAuth2ResourceServerDevServicesAutoConfiguration}.
 */
@DisabledIfDockerUnavailable
class KeycloakOAuth2ResourceServerDevServicesAutoConfigurationIT {

    private static final String ISSUER_URI_PROPERTY = "spring.security.oauth2.resourceserver.jwt.issuer-uri";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    KeycloakDevServicesAutoConfiguration.class,
                    KeycloakOAuth2ResourceServerDevServicesAutoConfiguration.class,
                    ServiceConnectionAutoConfiguration.class,
                    OAuth2ResourceServerAutoConfiguration.class));

    @BeforeEach
    @AfterEach
    void resetBootstrapMode() {
        BootstrapMode.clear();
    }

    @Test
    void jwtDecoderResolvedAgainstTheDevService() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(JwtDecoder.class);

            String issuerUri = context.getEnvironment().getProperty(ISSUER_URI_PROPERTY);
            assertThat(issuerUri).endsWith("/realms/" + KeycloakDevServicesProperties.DEFAULT_REALM);

            // A token the realm actually minted, decoded by the bean the application would use.
            Jwt jwt = context.getBean(JwtDecoder.class).decode(obtainAccessToken(issuerUri));

            assertThat(jwt.getIssuer()).hasToString(issuerUri);
            assertThat(jwt.getClaimAsString("preferred_username")).isEqualTo("isabella");
        });
    }

    @Test
    void tokenPropertiesTakePrecedenceOverUserProvidedOnes() {
        contextRunner
                .withPropertyValues(
                        ISSUER_URI_PROPERTY + "=https://issuer.invalid/realms/nowhere",
                        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://issuer.invalid/realms/nowhere/certs",
                        "spring.security.oauth2.resourceserver.jwt.jws-algorithms[0]=ES256",
                        "spring.security.oauth2.resourceserver.jwt.audiences[0]=orders-api")
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    String issuerUri = context.getEnvironment().getProperty(ISSUER_URI_PROPERTY);
                    assertThat(issuerUri).endsWith("/realms/" + KeycloakDevServicesProperties.DEFAULT_REALM);

                    Jwt jwt = context.getBean(JwtDecoder.class).decode(obtainAccessToken(issuerUri));
                    assertThat(jwt.getIssuer()).hasToString(issuerUri);
                });
    }

    /**
     * An access token for a default user, obtained by direct access grant, which the generated
     * realm enables precisely so that a test needs no browser.
     */
    private static String obtainAccessToken(String issuerUri) throws Exception {
        Map<String, Object> discoveryDocument = parseJson(
                send(HttpRequest.newBuilder(URI.create(issuerUri + "/.well-known/openid-configuration")).GET()));

        String form = "grant_type=password&username=isabella&password=isabella&client_id=%s&client_secret=%s&scope=openid"
                .formatted(KeycloakDevServicesProperties.Client.DEFAULT_CLIENT_ID,
                        KeycloakDevServicesProperties.Client.DEFAULT_CLIENT_SECRET);
        String tokenResponse = send(HttpRequest.newBuilder(URI.create((String) discoveryDocument.get("token_endpoint")))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form)));

        return (String) parseJson(tokenResponse).get("access_token");
    }

    private static String send(HttpRequest.Builder request) throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode())
                .withFailMessage("request to %s failed: %s", response.uri(), response.body())
                .isEqualTo(200);
        return response.body();
    }

    private static Map<String, Object> parseJson(String json) {
        return JsonParserFactory.getJsonParser().parseMap(json);
    }

}
