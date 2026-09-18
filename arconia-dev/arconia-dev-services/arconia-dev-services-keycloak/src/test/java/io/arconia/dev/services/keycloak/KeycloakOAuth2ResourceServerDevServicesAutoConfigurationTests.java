package io.arconia.dev.services.keycloak;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.PropertySource;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;

import io.arconia.boot.bootstrap.BootstrapMode;
import io.arconia.dev.services.core.registration.DevServiceDynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link KeycloakOAuth2ResourceServerDevServicesAutoConfiguration}.
 */
class KeycloakOAuth2ResourceServerDevServicesAutoConfigurationTests {

    /**
     * Spelled out rather than referenced from the auto-configuration, so that a change to the
     * constant has to be made deliberately here too instead of silently agreeing with itself.
     */
    private static final String ISSUER_URI_PROPERTY = "spring.security.oauth2.resourceserver.jwt.issuer-uri";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            // Dev Service container beans live in the "restart" scope when DevTools is present,
            // which a plain runner doesn't register.
            .withClassLoader(new FilteredClassLoader(RestartScope.class))
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
                .get(DevServiceDynamicPropertySource.PROPERTY_SOURCE_NAME);
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
        // The module is on the classpath of applications that are only OAuth2 clients, which have
        // no resource server to configure an issuer for.
        contextRunner
                .withClassLoader(new FilteredClassLoader(RestartScope.class, BearerTokenAuthenticationToken.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(contributesIssuerUri(context)).isFalse();
                });
    }

    @Test
    void resourceServerWiringCanBeDisabled() {
        contextRunner
                .withPropertyValues(KeycloakResourceServerDevServicesProperties.CONFIG_PREFIX + ".enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(contributesIssuerUri(context)).isFalse();
                });
    }

    @Test
    void noIssuerUriWhenNoRealmIsGeneratedOrImported() {
        // With no realm, the issuer could only point at one that does not exist, so the
        // application's own OAuth2 configuration is left to apply untouched.
        contextRunner
                .withPropertyValues(KeycloakDevServicesProperties.CONFIG_PREFIX + ".realm.create=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(contributesIssuerUri(context)).isFalse();
                });
    }

    @Test
    void issuerUriContributedWhenRealmCreationIsDisabledButOneIsImported() {
        // Turning off generation is not the same as wanting no realm.
        contextRunner
                .withPropertyValues(
                        KeycloakDevServicesProperties.CONFIG_PREFIX + ".realm.create=false",
                        KeycloakDevServicesProperties.CONFIG_PREFIX + ".realm.import-paths=classpath:keycloak/test-realm.json")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(contributesIssuerUri(context)).isTrue();
                });
    }

    @Test
    void issuerUriTakesPrecedenceOverAUserProvidedOne() {
        // The dev service property source is registered first, so it supersedes configuration
        // the application provides itself. That is promised in the class javadoc and in the
        // documentation, and the ordering is what makes it true.
        contextRunner
                .withPropertyValues(ISSUER_URI_PROPERTY + "=https://issuer.example.com/realms/mine")
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    assertThat(contributesIssuerUri(context)).isTrue();

                    // Expressed as a comparison rather than as "index 0", so it states the
                    // property that matters — the dev service is consulted first — instead of a
                    // position that other property sources could shift without breaking anything.
                    var propertySources = context.getEnvironment().getPropertySources();
                    PropertySource<?> userProvided = null;
                    for (PropertySource<?> propertySource : propertySources) {
                        if (!DevServiceDynamicPropertySource.PROPERTY_SOURCE_NAME.equals(propertySource.getName())
                                && propertySource.containsProperty(ISSUER_URI_PROPERTY)) {
                            userProvided = propertySource;
                            break;
                        }
                    }
                    assertThat(userProvided)
                            .withFailMessage("the user-provided issuer URI never reached the environment")
                            .isNotNull();
                    assertThat(propertySources.precedenceOf(
                            propertySources.get(DevServiceDynamicPropertySource.PROPERTY_SOURCE_NAME)))
                            .isLessThan(propertySources.precedenceOf(userProvided));
                });
    }

}
