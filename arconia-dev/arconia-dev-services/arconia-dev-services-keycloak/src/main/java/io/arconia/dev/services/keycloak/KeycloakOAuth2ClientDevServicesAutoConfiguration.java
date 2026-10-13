package io.arconia.dev.services.keycloak;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;

/**
 * Auto-configuration wiring an OAuth2 Client to the Keycloak Dev Service.
 * <p>
 * The client registration is provided as a bean that replaces any other
 * {@link ClientRegistrationRepository}, so neither the application's own repository nor its
 * {@code spring.security.oauth2.client} properties take part in it while the dev service is
 * active. Setting {@code arconia.dev.services.keycloak.client.enabled=false} keeps them.
 */
@AutoConfiguration(after = KeycloakDevServicesAutoConfiguration.class,
        beforeName = "org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration")
@ConditionalOnDevServicesEnabled("keycloak")
@ConditionalOnClass(ClientRegistration.class)
@ConditionalOnProperty(prefix = KeycloakDevServicesProperties.CONFIG_PREFIX + ".client", name = "enabled",
        havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(KeycloakDevServicesProperties.class)
public final class KeycloakOAuth2ClientDevServicesAutoConfiguration {

    static final String CLIENT_REGISTRATION_REPOSITORY_BEAN_NAME = "keycloakDevServicesClientRegistrationRepository";

    private static final Logger logger = LoggerFactory.getLogger(KeycloakOAuth2ClientDevServicesAutoConfiguration.class);

    @Bean(CLIENT_REGISTRATION_REPOSITORY_BEAN_NAME)
    InMemoryClientRegistrationRepository clientRegistrationRepository(KeycloakDevServicesProperties properties, KeycloakConnectionDetails connectionDetails) {
        var client = properties.getClient();

        var registration = ClientRegistrations
                .fromIssuerLocation(connectionDetails.getIssuerUri())
                .registrationId(client.getRegistrationId())
                .clientId(client.getId())
                .clientSecret(client.getSecret())
                .scope("openid", "profile", "email")
                .build();

        logger.info("Keycloak Dev Service: registered the OAuth2 client '{}' with Spring Security. Log in at /oauth2/authorization/{}.",
                client.getId(), client.getRegistrationId());

        return new InMemoryClientRegistrationRepository(registration);
    }

    @Bean
    static ClientRegistrationRepositoryReplacer keycloakDevServicesClientRegistrationRepositoryReplacer() {
        return new ClientRegistrationRepositoryReplacer();
    }

    /**
     * Removes every {@link ClientRegistrationRepository} other than the dev service's own, so that
     * a single one exists. Registering it beside the application's, even as primary, would not
     * do: Spring Security only provides its default {@code OAuth2AuthorizedClientManager} when
     * exactly one repository is defined, and the application's repository might still contact
     * production identity providers at startup. Ordered so that it runs before Spring Security
     * counts the repositories.
     */
    static final class ClientRegistrationRepositoryReplacer implements BeanDefinitionRegistryPostProcessor, Ordered {

        @Override
        public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
            if (!(registry instanceof ListableBeanFactory beanFactory)) {
                return;
            }
            List<String> replaced = Arrays.stream(beanFactory.getBeanNamesForType(ClientRegistrationRepository.class, true, false))
                    .filter(name -> !CLIENT_REGISTRATION_REPOSITORY_BEAN_NAME.equals(name))
                    .toList();
            if (replaced.isEmpty()) {
                return;
            }
            replaced.forEach(registry::removeBeanDefinition);
            logger.info("Keycloak Dev Service: replaced the ClientRegistrationRepository bean(s) {} with its own. Set '{}.client.enabled=false' to keep them.",
                    replaced, KeycloakDevServicesProperties.CONFIG_PREFIX);
        }

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }

    }

}
