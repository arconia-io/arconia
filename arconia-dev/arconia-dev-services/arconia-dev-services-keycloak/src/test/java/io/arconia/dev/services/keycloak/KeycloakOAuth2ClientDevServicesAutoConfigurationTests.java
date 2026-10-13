package io.arconia.dev.services.keycloak;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

import io.arconia.boot.bootstrap.BootstrapMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link KeycloakOAuth2ClientDevServicesAutoConfiguration}.
 */
class KeycloakOAuth2ClientDevServicesAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
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
        contextRunner
                .withClassLoader(new FilteredClassLoader(ClientRegistration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(ClientRegistrationRepository.class);
                });
    }

    @Test
    void replacerRemovesEveryOtherClientRegistrationRepository() {
        var beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerBeanDefinition(KeycloakOAuth2ClientDevServicesAutoConfiguration.CLIENT_REGISTRATION_REPOSITORY_BEAN_NAME,
                new RootBeanDefinition(InMemoryClientRegistrationRepository.class));
        beanFactory.registerBeanDefinition("clientRegistrationRepository", new RootBeanDefinition(InMemoryClientRegistrationRepository.class));
        beanFactory.registerBeanDefinition("unrelated", new RootBeanDefinition(String.class));

        new KeycloakOAuth2ClientDevServicesAutoConfiguration.ClientRegistrationRepositoryReplacer()
                .postProcessBeanDefinitionRegistry(beanFactory);

        assertThat(beanFactory.getBeanDefinitionNames()).containsExactlyInAnyOrder(
                KeycloakOAuth2ClientDevServicesAutoConfiguration.CLIENT_REGISTRATION_REPOSITORY_BEAN_NAME, "unrelated");
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

}
