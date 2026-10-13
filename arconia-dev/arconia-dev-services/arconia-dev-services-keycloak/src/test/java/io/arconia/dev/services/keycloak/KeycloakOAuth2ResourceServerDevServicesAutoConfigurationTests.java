package io.arconia.dev.services.keycloak;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
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

}
