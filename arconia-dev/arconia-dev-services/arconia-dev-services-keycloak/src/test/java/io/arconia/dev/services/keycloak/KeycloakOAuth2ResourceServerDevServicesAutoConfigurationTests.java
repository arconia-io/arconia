package io.arconia.dev.services.keycloak;

import java.util.Objects;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.PropertySource;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;

import io.arconia.boot.bootstrap.BootstrapMode;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link KeycloakOAuth2ResourceServerDevServicesAutoConfiguration}.
 */
class KeycloakOAuth2ResourceServerDevServicesAutoConfigurationTests {

    /**
     * Spelled out rather than referenced from the auto-configuration, so that a change to the
     * constant has to be made deliberately here too instead of silently agreeing with itself.
     */
    private static final String JWT_PROPERTY_PREFIX = "spring.security.oauth2.resourceserver.jwt.";

    private static final String ISSUER_URI_PROPERTY = JWT_PROPERTY_PREFIX + "issuer-uri";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    KeycloakDevServicesAutoConfiguration.class,
                    KeycloakOAuth2ResourceServerDevServicesAutoConfiguration.class));

    @BeforeEach
    @AfterEach
    void resetBootstrapMode() {
        BootstrapMode.clear();
    }

    private static boolean contributesIssuerUri(AssertableApplicationContext context) {
        PropertySource<?> propertySource = context.getEnvironment().getPropertySources()
                .get(DevServicesRegistrar.DYNAMIC_PROPERTY_SOURCE_NAME);
        return propertySource != null && propertySource.containsProperty(ISSUER_URI_PROPERTY);
    }

    @Test
    void issuerUriIsContributedByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(contributesIssuerUri(context)).isTrue();
        });
    }

    @Test
    void backsOffWhenSpringSecurityOAuth2ResourceServerIsAbsent() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(BearerTokenAuthenticationToken.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(contributesIssuerUri(context)).isFalse();
                });
    }

    @Test
    void resourceServerWiringCanBeDisabled() {
        contextRunner
                .withPropertyValues(KeycloakDevServicesProperties.CONFIG_PREFIX + ".resource-server.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(contributesIssuerUri(context)).isFalse();
                });
    }

    @Test
    void tokenPropertiesConfiguredByTheApplicationAreSupersededByTheDevService() {
        contextRunner
                .withBean(KeycloakConnectionDetails.class, () -> new FixedConnectionDetails("http://localhost:8180", "arconia"))
                .withPropertyValues(
                        JWT_PROPERTY_PREFIX + "issuer-uri=https://idp.example.com/realms/production",
                        JWT_PROPERTY_PREFIX + "jwk-set-uri=https://idp.example.com/realms/production/certs",
                        JWT_PROPERTY_PREFIX + "jws-algorithms[0]=ES256",
                        JWT_PROPERTY_PREFIX + "audiences[0]=orders-api",
                        JWT_PROPERTY_PREFIX + "audiences[1]=billing-api")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var environment = context.getEnvironment();
                    assertThat(environment.getProperty(ISSUER_URI_PROPERTY))
                            .isEqualTo("http://localhost:8180/realms/arconia");
                    assertThat(environment.getProperty(JWT_PROPERTY_PREFIX + "jwk-set-uri"))
                            .isEqualTo("http://localhost:8180/realms/arconia/protocol/openid-connect/certs");

                    var jwt = Binder.get(environment)
                            .bindOrCreate("spring.security.oauth2.resourceserver", OAuth2ResourceServerProperties.class)
                            .getJwt();
                    assertThat(jwt.getJwsAlgorithms()).containsExactly("RS256");
                    assertThat(jwt.getAudiences()).isEmpty();
                });
    }

    @Test
    void issuerUriTakesPrecedenceOverAUserProvidedOne() {
        contextRunner
                .withPropertyValues(ISSUER_URI_PROPERTY + "=https://issuer.example.com/realms/mine")
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    assertThat(contributesIssuerUri(context)).isTrue();

                    var propertySources = context.getEnvironment().getPropertySources();
                    PropertySource<?> userProvided = null;
                    for (PropertySource<?> propertySource : propertySources) {
                        if (!DevServicesRegistrar.DYNAMIC_PROPERTY_SOURCE_NAME.equals(propertySource.getName())
                                && propertySource.containsProperty(ISSUER_URI_PROPERTY)) {
                            userProvided = propertySource;
                            break;
                        }
                    }
                    assertThat(userProvided)
                            .withFailMessage("the user-provided issuer URI never reached the environment")
                            .isNotNull();
                    assertThat(propertySources.precedenceOf(
                            Objects.requireNonNull(propertySources.get(DevServicesRegistrar.DYNAMIC_PROPERTY_SOURCE_NAME))))
                            .isLessThan(propertySources.precedenceOf(userProvided));
                });
    }

    /**
     * Connection details the application provides itself, which make the dev service step aside
     * so that no container is started.
     */
    private static final class FixedConnectionDetails implements KeycloakConnectionDetails {

        private final String authServerUrl;

        private final String realm;

        FixedConnectionDetails(String authServerUrl, String realm) {
            this.authServerUrl = authServerUrl;
            this.realm = realm;
        }

        @Override
        public String getAuthServerUrl() {
            return authServerUrl;
        }

        @Override
        public String getRealm() {
            return realm;
        }

    }

}
