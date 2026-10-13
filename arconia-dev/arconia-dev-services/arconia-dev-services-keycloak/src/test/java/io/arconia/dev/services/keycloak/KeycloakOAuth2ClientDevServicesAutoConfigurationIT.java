package io.arconia.dev.services.keycloak;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import io.arconia.boot.bootstrap.BootstrapMode;
import io.arconia.dev.services.tests.DisabledIfDockerUnavailable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link KeycloakOAuth2ClientDevServicesAutoConfiguration}.
 */
@DisabledIfDockerUnavailable
class KeycloakOAuth2ClientDevServicesAutoConfigurationIT {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
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
            assertThat(registration.getScopes()).containsExactlyInAnyOrder("openid", "profile", "email");
            assertThat(registration.getAuthorizationGrantType().getValue()).isEqualTo("authorization_code");
            assertThat(registration.getRedirectUri()).isNotBlank();
            assertThat(registration.getProviderDetails().getIssuerUri())
                    .endsWith("/realms/" + KeycloakDevServicesProperties.DEFAULT_REALM);
            assertThat(registration.getProviderDetails().getAuthorizationUri()).isNotBlank();
            assertThat(registration.getProviderDetails().getTokenUri()).isNotBlank();
        });
    }

    @Test
    void applicationSpringSecurityPropertiesAreInert() {
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
        contextRunner
                .withPropertyValues(
                        KeycloakDevServicesProperties.CONFIG_PREFIX + ".client.id=my-app",
                        KeycloakDevServicesProperties.CONFIG_PREFIX + ".client.secret=my-secret",
                        KeycloakDevServicesProperties.CONFIG_PREFIX + ".client.registration-id=my-registration")
                .run(context -> {
                    ClientRegistration registration = context.getBean(ClientRegistrationRepository.class)
                            .findByRegistrationId("my-registration");
                    assertThat(registration).isNotNull();
                    assertThat(registration.getClientId()).isEqualTo("my-app");
                    assertThat(registration.getClientSecret()).isEqualTo("my-secret");
                });
    }

    @Test
    void devServiceReplacesAClientRegistrationRepositoryTheApplicationDefines() {
        contextRunner
                .withUserConfiguration(ApplicationConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ClientRegistrationRepository.class);
                    assertThat(context.getBean(ClientRegistrationRepository.class).findByRegistrationId("keycloak"))
                            .isNotNull();
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class ApplicationConfiguration {

        @Bean
        ClientRegistrationRepository clientRegistrationRepository() {
            throw new IllegalStateException("The application's repository must not be created in dev.");
        }

    }

}
