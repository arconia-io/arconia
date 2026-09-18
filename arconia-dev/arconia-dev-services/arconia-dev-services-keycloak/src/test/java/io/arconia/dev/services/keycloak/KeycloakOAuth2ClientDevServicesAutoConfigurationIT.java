package io.arconia.dev.services.keycloak;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import io.arconia.boot.bootstrap.BootstrapMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link KeycloakOAuth2ClientDevServicesAutoConfiguration}.
 * <p>
 * Creating the client registration repository starts the container and fetches the discovery
 * document from it, so this is the only place the full chain can be proven: the repository is
 * registered early enough for Spring Boot's own to back off, and the resulting registration
 * actually matches the realm the dev service imported.
 */
@EnabledIfDockerAvailable
class KeycloakOAuth2ClientDevServicesAutoConfigurationIT {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(RestartScope.class))
            .withConfiguration(AutoConfigurations.of(
                    KeycloakDevServicesAutoConfiguration.class,
                    KeycloakOAuth2ClientDevServicesAutoConfiguration.class,
                    ServiceConnectionAutoConfiguration.class,
                    OAuth2ClientAutoConfiguration.class));

    @BeforeEach
    @AfterEach
    void resetBootstrapMode() {
        BootstrapMode.clear();
    }

    @Test
    void devServiceRegistersTheClientRegistrationItself() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ClientRegistrationRepository.class);

            ClientRegistration registration = context.getBean(ClientRegistrationRepository.class)
                    .findByRegistrationId("keycloak");
            assertThat(registration).isNotNull();
            assertThat(registration.getClientId())
                    .isEqualTo(KeycloakDevServicesProperties.Client.DEFAULT_CLIENT_ID);
            assertThat(registration.getClientSecret())
                    .isEqualTo(KeycloakDevServicesProperties.Client.DEFAULT_CLIENT_SECRET);
            assertThat(registration.getScopes())
                    .containsExactlyInAnyOrderElementsOf(KeycloakDevServicesProperties.Client.DEFAULT_SCOPES);
            assertThat(registration.getAuthorizationGrantType().getValue()).isEqualTo("authorization_code");
            assertThat(registration.getRedirectUri()).isNotBlank();
            // Resolved from the discovery document of the realm the dev service imported.
            assertThat(registration.getProviderDetails().getIssuerUri())
                    .endsWith("/realms/" + KeycloakDevServicesProperties.DEFAULT_REALM);
            assertThat(registration.getProviderDetails().getAuthorizationUri()).isNotBlank();
            assertThat(registration.getProviderDetails().getTokenUri()).isNotBlank();
        });
    }

    @Test
    void applicationSpringSecurityPropertiesAreInert() {
        // The application's spring.security.oauth2.client properties describe the providers it
        // uses in production, and the dev service deliberately takes no part in them: the
        // registration is the dev service's own, and there being a single repository is what
        // proves Spring Boot's property-based one backed off.
        contextRunner
                .withPropertyValues(
                        "spring.security.oauth2.client.registration.keycloak.client-id=my-own-client",
                        "spring.security.oauth2.client.registration.keycloak.client-secret=my-own-secret")
                .run(context -> {
                    assertThat(context).hasSingleBean(ClientRegistrationRepository.class);

                    ClientRegistration registration = context.getBean(ClientRegistrationRepository.class)
                            .findByRegistrationId("keycloak");
                    assertThat(registration).isNotNull();
                    assertThat(registration.getClientId())
                            .isEqualTo(KeycloakDevServicesProperties.Client.DEFAULT_CLIENT_ID);
                    assertThat(registration.getClientSecret())
                            .isEqualTo(KeycloakDevServicesProperties.Client.DEFAULT_CLIENT_SECRET);
                });
    }

    @Test
    void clientIsCustomizedThroughDevServiceProperties() {
        // The same properties feed the generated realm, so the client Spring Security
        // authenticates as and the one the realm declares agree by construction.
        contextRunner
                .withPropertyValues(
                        KeycloakDevServicesProperties.CONFIG_PREFIX + ".client.id=my-app",
                        KeycloakDevServicesProperties.CONFIG_PREFIX + ".client.secret=my-secret",
                        KeycloakDevServicesProperties.CONFIG_PREFIX + ".client.registration-id=my-registration",
                        KeycloakDevServicesProperties.CONFIG_PREFIX + ".client.scopes=openid,custom:scope")
                .run(context -> {
                    ClientRegistration registration = context.getBean(ClientRegistrationRepository.class)
                            .findByRegistrationId("my-registration");
                    assertThat(registration).isNotNull();
                    assertThat(registration.getClientId()).isEqualTo("my-app");
                    assertThat(registration.getClientSecret()).isEqualTo("my-secret");
                    assertThat(registration.getScopes()).containsExactlyInAnyOrder("openid", "custom:scope");
                });
    }

    @Test
    void publicClientIsRegisteredWithoutClientAuthentication() {
        contextRunner
                .withPropertyValues(KeycloakDevServicesProperties.CONFIG_PREFIX + ".client.secret=")
                .run(context -> {
                    ClientRegistration registration = context.getBean(ClientRegistrationRepository.class)
                            .findByRegistrationId("keycloak");
                    assertThat(registration).isNotNull();
                    assertThat(registration.getClientAuthenticationMethod())
                            .isEqualTo(ClientAuthenticationMethod.NONE);
                    assertThat(registration.getClientSecret()).isNullOrEmpty();
                });
    }

}
