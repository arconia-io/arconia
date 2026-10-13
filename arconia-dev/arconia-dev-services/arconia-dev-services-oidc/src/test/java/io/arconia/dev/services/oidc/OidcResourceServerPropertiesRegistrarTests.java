package io.arconia.dev.services.oidc;

import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.PropertySource;

import io.arconia.dev.services.core.registration.DevServicesRegistrar;

import static io.arconia.dev.services.oidc.OidcResourceServerPropertiesRegistrar.JWT_PROPERTY_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OidcResourceServerPropertiesRegistrar}.
 */
class OidcResourceServerPropertiesRegistrarTests {

    private static final String ISSUER_URI_PROPERTY = JWT_PROPERTY_PREFIX + "issuer-uri";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(RegistrarConfiguration.class)
            .withBean(OidcConnectionDetails.class, () -> new FixedConnectionDetails(
                    "http://localhost:8180/realms/arconia", "http://localhost:8180/realms/arconia/certs"));

    @Test
    void tokenPropertiesConfiguredByTheApplicationAreSupersededByTheDevService() {
        // Production configuration commonly names a key set, an audience or a signature algorithm
        // of its own. Left in place, any of them would reject every token the dev service issues,
        // and code reading the properties directly would see an issuer that is not running.
        contextRunner
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
                            .isEqualTo("http://localhost:8180/realms/arconia/certs");

                    var jwt = Binder.get(environment)
                            .bindOrCreate("spring.security.oauth2.resourceserver", OAuth2ResourceServerProperties.class)
                            .getJwt();
                    assertThat(jwt.getJwsAlgorithms()).containsExactly("RS256");
                    assertThat(jwt.getAudiences()).isEmpty();
                });
    }

    @Test
    void dynamicPropertySourceTakesPrecedenceOverTheApplicationConfiguration() {
        contextRunner
                .withPropertyValues(ISSUER_URI_PROPERTY + "=https://issuer.example.com/realms/mine")
                .run(context -> {
                    assertThat(context).hasNotFailed();

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

    @Configuration(proxyBeanMethods = false)
    @Import(OidcResourceServerPropertiesRegistrar.class)
    static class RegistrarConfiguration {
    }

    private record FixedConnectionDetails(String issuerUri, String jwkSetUri) implements OidcConnectionDetails {

        @Override
        public String getIssuerUri() {
            return issuerUri;
        }

        @Override
        public String getJwkSetUri() {
            return jwkSetUri;
        }

    }

}
