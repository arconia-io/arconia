package io.arconia.dev.services.keycloak;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;

/**
 * Auto-configuration wiring an OAuth2 Client to the Keycloak Dev Service.
 */
@AutoConfiguration(after = KeycloakDevServicesAutoConfiguration.class,
        beforeName = "org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration")
@ConditionalOnDevServicesEnabled("keycloak")
@ConditionalOnClass(ClientRegistration.class)
@ConditionalOnProperty(prefix = KeycloakDevServicesProperties.CONFIG_PREFIX + ".client", name = "enabled",
        havingValue = "true", matchIfMissing = true)
@Conditional(KeycloakOAuth2ClientDevServicesAutoConfiguration.RealmAvailableCondition.class)
@EnableConfigurationProperties(KeycloakDevServicesProperties.class)
public final class KeycloakOAuth2ClientDevServicesAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ClientRegistrationRepository.class)
    InMemoryClientRegistrationRepository clientRegistrationRepository(KeycloakDevServicesProperties properties,
            Environment environment, ObjectProvider<KeycloakConnectionDetails> connectionDetails) {
        validateClientIsConfigured(properties, environment);

        var client = properties.getClient();
        Assert.hasText(client.getRegistrationId(), "the client registration identifier cannot be null or empty; set '%s.client.registration-id'"
                .formatted(KeycloakDevServicesProperties.CONFIG_PREFIX));

        // Resolving the connection details is what starts the container, or adopts a shared one
        // started by another application. Fetching the discovery document from it then resolves
        // the endpoints the same way Spring Boot would from an issuer-uri property.
        var registration = ClientRegistrations
                .fromIssuerLocation(connectionDetails.getObject().getIssuerUri())
                .registrationId(client.getRegistrationId())
                .clientId(client.getId())
                .scope(client.getScopes());

        // An empty secret means a public client: registered with no client authentication,
        // since the discovery document advertises secret-based methods and Spring Security
        // would otherwise attempt to authenticate, failing the token exchange.
        if (client.getSecret().isEmpty()) {
            registration.clientAuthenticationMethod(ClientAuthenticationMethod.NONE);
        }
        else {
            registration.clientSecret(client.getSecret());
        }

        return new InMemoryClientRegistrationRepository(registration.build());
    }

    /**
     * A realm file declares its own clients, and this dev service deliberately does not inspect
     * them, so the client to authenticate as has to be stated. Failing here, rather than letting
     * the default client identifier reach a realm that does not declare it, turns a puzzling
     * authentication error into a clear startup message.
     */
    static void validateClientIsConfigured(KeycloakDevServicesProperties properties, Environment environment) {
        if (properties.getRealm().getImportPaths().isEmpty()) {
            return;
        }
        // Asked of the environment rather than of the bound properties, which always carry a
        // value because they fall back to a default: only an identifier the application actually
        // stated counts, and it is checked for text rather than presence, since setting the
        // property to nothing states nothing.
        String clientId = Binder.get(environment)
                .bind(KeycloakDevServicesProperties.CONFIG_PREFIX + ".client.id", String.class)
                .orElse(null);
        if (StringUtils.hasText(clientId)) {
            return;
        }
        throw new IllegalStateException(
                """
                The Keycloak dev service is configured to import the realm file(s) %s, but no OAuth2 client is configured. \
                The clients declared in a realm file are not inspected, because an exported realm masks client secrets \
                and commonly declares several clients. Set '%s.client.id' and '%s.client.secret' to the client the \
                application authenticates as, leaving the secret empty for a public client."""
                        .formatted(properties.getRealm().getImportPaths(),
                                KeycloakDevServicesProperties.CONFIG_PREFIX,
                                KeycloakDevServicesProperties.CONFIG_PREFIX));
    }

    /**
     * With no realm to authenticate against, a client registration could only point at one that
     * does not exist. Backing off as a condition rather than inside the bean method leaves
     * Spring Boot's own property-based repository in charge, so an application that keeps its
     * own OAuth2 configuration works unchanged with the dev service merely running Keycloak
     * beside it.
     */
    static final class RealmAvailableCondition extends SpringBootCondition {

        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
            var realm = Binder.get(context.getEnvironment())
                    .bindOrCreate(KeycloakDevServicesProperties.CONFIG_PREFIX + ".realm",
                            KeycloakDevServicesProperties.Realm.class);
            var message = ConditionMessage.forCondition("Keycloak Dev Service Realm");
            if (KeycloakRealmMetadata.hasRealm(realm)) {
                return ConditionOutcome.match(message.because("a realm is generated or imported"));
            }
            return ConditionOutcome.noMatch(message.because("no realm is generated or imported"));
        }

    }

}
