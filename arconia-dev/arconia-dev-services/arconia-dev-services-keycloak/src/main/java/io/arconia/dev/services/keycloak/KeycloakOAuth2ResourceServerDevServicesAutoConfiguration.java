package io.arconia.dev.services.keycloak;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.keycloak.KeycloakOAuth2ResourceServerDevServicesAutoConfiguration.KeycloakResourceServerPropertyRegistrar;

/**
 * Auto-configuration wiring an OAuth2 Resource Server to the Keycloak Dev Service.
 */
@AutoConfiguration(after = KeycloakDevServicesAutoConfiguration.class,
        beforeName = {
                "org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration",
                "org.springframework.boot.security.oauth2.server.resource.autoconfigure.reactive.ReactiveOAuth2ResourceServerAutoConfiguration"
        })
@ConditionalOnDevServicesEnabled("keycloak")
@ConditionalOnClass(BearerTokenAuthenticationToken.class)
@ConditionalOnProperty(prefix = KeycloakResourceServerDevServicesProperties.CONFIG_PREFIX, name = "enabled",
        havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(KeycloakResourceServerDevServicesProperties.class)
@Import(KeycloakResourceServerPropertyRegistrar.class)
public final class KeycloakOAuth2ResourceServerDevServicesAutoConfiguration {

    private static final String ISSUER_URI_PROPERTY = "spring.security.oauth2.resourceserver.jwt.issuer-uri";

    static class KeycloakResourceServerPropertyRegistrar extends DevServicesRegistrar {

        private static final Logger logger = LoggerFactory.getLogger(KeycloakResourceServerPropertyRegistrar.class);

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(KeycloakDevServicesProperties.CONFIG_PREFIX, KeycloakDevServicesProperties.class);

            // With no realm to validate tokens against, the issuer could only point at one that
            // does not exist, so the application's own issuer configuration is left alone.
            if (!KeycloakRealmMetadata.hasRealm(properties.getRealm())) {
                logger.info("Keycloak Dev Service: no realm is generated or imported, so no OAuth2 resource server issuer is contributed. The application's own OAuth2 configuration applies.");
                return;
            }

            addDynamicProperty(ISSUER_URI_PROPERTY,
                    () -> getBeanFactory().getBean(KeycloakConnectionDetails.class).getIssuerUri());
        }

    }

}
