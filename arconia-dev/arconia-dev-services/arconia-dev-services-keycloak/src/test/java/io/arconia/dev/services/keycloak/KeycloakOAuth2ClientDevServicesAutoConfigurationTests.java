package io.arconia.dev.services.keycloak;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import io.arconia.boot.bootstrap.BootstrapMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Unit tests for {@link KeycloakOAuth2ClientDevServicesAutoConfiguration}.
 */
class KeycloakOAuth2ClientDevServicesAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            // Dev Service container beans live in the "restart" scope when DevTools is present,
            // which a plain runner doesn't register.
            .withClassLoader(new FilteredClassLoader(RestartScope.class))
            .withConfiguration(AutoConfigurations.of(
                    KeycloakDevServicesAutoConfiguration.class,
                    KeycloakOAuth2ClientDevServicesAutoConfiguration.class));

    @BeforeEach
    @AfterEach
    void resetBootstrapMode() {
        BootstrapMode.clear();
    }

    @Test
    void backsOffWhenSpringSecurityOAuth2ClientIsAbsent() {
        // The module is on the classpath of applications that are only resource servers, which
        // must not gain a client registration they have no way to use.
        contextRunner
                .withClassLoader(new FilteredClassLoader(RestartScope.class, ClientRegistration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(ClientRegistrationRepository.class);
                });
    }

    @Test
    void backsOffToAClientRegistrationRepositoryTheApplicationDefines() {
        // An application that defines its own repository has taken charge of its OAuth2 clients,
        // and the dev service must not compete with it. Backing off is also what proves no
        // container is started for it: creating the dev service's repository would.
        ClientRegistrationRepository applicationRepository = registrationId -> null;
        contextRunner
                .withBean(ClientRegistrationRepository.class, () -> applicationRepository)
                .run(context -> {
                    assertThat(context).hasSingleBean(ClientRegistrationRepository.class);
                    assertThat(context.getBean(ClientRegistrationRepository.class)).isSameAs(applicationRepository);
                });
    }

    @Test
    void clientWiringCanBeDisabled() {
        contextRunner
                .withPropertyValues(KeycloakDevServicesProperties.CONFIG_PREFIX + ".client.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(ClientRegistrationRepository.class);
                });
    }

    @Test
    void noClientRegistrationRepositoryWhenNoRealmIsGeneratedOrImported() {
        // With no realm, a registration could only point at one that does not exist, so the
        // application's own OAuth2 configuration is left to apply untouched.
        contextRunner
                .withPropertyValues(KeycloakDevServicesProperties.CONFIG_PREFIX + ".realm.create=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(ClientRegistrationRepository.class);
                });
    }

    @Test
    void failsWhenRealmIsImportedWithoutConfiguringTheClient() {
        // The message is asserted, not merely the failure: this context has several ways to fail,
        // and the value of failing here is entirely in telling the user what to set.
        contextRunner
                .withPropertyValues(KeycloakDevServicesProperties.CONFIG_PREFIX
                        + ".realm.import-paths=classpath:keycloak/test-realm.json")
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .rootCause()
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("no OAuth2 client is configured")
                        .hasMessageContaining(KeycloakDevServicesProperties.CONFIG_PREFIX + ".client.id")
                        .hasMessageContaining(KeycloakDevServicesProperties.CONFIG_PREFIX + ".client.secret"));
    }

    @Test
    void failsWhenRealmIsImportedAndTheConfiguredClientIsEmpty() {
        // Setting the property to nothing states nothing. Checking only for its presence would
        // skip the message that explains what to set, in favour of a bare assertion further down.
        contextRunner
                .withPropertyValues(
                        KeycloakDevServicesProperties.CONFIG_PREFIX + ".realm.import-paths=classpath:keycloak/test-realm.json",
                        KeycloakDevServicesProperties.CONFIG_PREFIX + ".client.id=")
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .rootCause()
                        .hasMessageContaining("no OAuth2 client is configured"));
    }

    @Test
    void failsWhenTheRegistrationIdIsEmpty() {
        // Checked before the container is resolved, so the mistake surfaces as this message
        // rather than as a Keycloak boot followed by a failure to build the registration.
        contextRunner
                .withPropertyValues(KeycloakDevServicesProperties.CONFIG_PREFIX + ".client.registration-id=")
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .rootCause()
                        .hasMessageContaining(KeycloakDevServicesProperties.CONFIG_PREFIX + ".client.registration-id"));
    }

    @Test
    void validationAcceptsAnImportedRealmWithAnExplicitClient() {
        // The positive counterpart of the fail-fast tests, exercised directly: in a full context
        // it is followed by a container start, which the integration tests cover.
        KeycloakDevServicesProperties properties = new KeycloakDevServicesProperties();
        properties.getRealm().setImportPaths(List.of("classpath:keycloak/test-realm.json"));
        MockEnvironment environment = new MockEnvironment()
                .withProperty(KeycloakDevServicesProperties.CONFIG_PREFIX + ".client.id", "test-client");

        assertThatCode(() -> KeycloakOAuth2ClientDevServicesAutoConfiguration
                .validateClientIsConfigured(properties, environment))
                .doesNotThrowAnyException();
    }

}
