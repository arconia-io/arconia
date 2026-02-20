package io.arconia.dev.services.keycloak;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultSingletonBeanRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;

import dasniko.testcontainers.keycloak.KeycloakContainer;

/**
 * Starts a Keycloak Testcontainers instance early, publishes derived properties
 * and registers the started instance as a singleton with a disposable.
 */
public final class KeycloakContainerRegistrar implements BeanDefinitionRegistryPostProcessor, EnvironmentAware, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakContainerRegistrar.class);

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        if (!registry.containsBeanDefinition("keycloakContainer")) {
            RootBeanDefinition bd = new RootBeanDefinition(KeycloakContainer.class);
            bd.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            bd.setSynthetic(true);
            registry.registerBeanDefinition("keycloakContainer", bd);
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        var binder = Binder.get(environment);

        KeycloakDevServicesProperties properties;

        try {
            properties = binder.bind("arconia.dev.services.keycloak", KeycloakDevServicesProperties.class)
                    .orElse(new KeycloakDevServicesProperties());
        } catch (BindException ex) {
            logger.warn("Failed to bind KeycloakDevServicesProperties; using defaults", ex);
            properties = new KeycloakDevServicesProperties();
        }

        if (Boolean.FALSE.equals(properties.isEnabled())) {
            logger.debug("Keycloak dev services disabled via properties");
            return;
        }

        synchronized (KeycloakContainerRegistrar.class) {
            if (beanFactory.containsSingleton("keycloakContainer")) {
                logger.debug("Keycloak container already registered as singleton");
                return;
            }

            var container = new ArconiaKeycloakContainer(properties);

            logger.info("Starting Keycloak dev-services container (image={})", properties.getImageName());
            container.start();

            // inject minimal derived properties so other auto-config can bind
            if (environment instanceof ConfigurableEnvironment) {
                Map<String, Object> map = new HashMap<>();
                try {
                    String issuer = container.getAuthServerUrl();
                    if (issuer != null) {
                        // Resource server expects this property
                        map.put("spring.security.oauth2.resourceserver.jwt.issuer-uri", issuer);
                    }
                } catch (Exception ex) {
                    logger.warn("Failed to derive Keycloak properties from container", ex);
                }

                if (!map.isEmpty()) {
                    ((ConfigurableEnvironment) environment).getPropertySources()
                            .addFirst(new MapPropertySource("arconia-keycloak", map));
                }
            }

            // register the started instance and arrange for shutdown
            beanFactory.registerSingleton("keycloakContainer", container);

            if (beanFactory instanceof DefaultSingletonBeanRegistry) {
                ((DefaultSingletonBeanRegistry) beanFactory).registerDisposableBean("keycloakContainer", new DisposableBean() {
                    @Override
                    public void destroy() {
                        try {
                            container.stop();
                        } catch (Exception ex) {
                            logger.warn("Error stopping Keycloak container", ex);
                        }
                    }
                });
            } else {
                // Fallback: ensure container is stopped on JVM shutdown
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        container.stop();
                    } catch (Exception ex) {
                        logger.warn("Error stopping Keycloak container during shutdown hook", ex);
                    }
                }));
            }

            logger.info("Keycloak dev-services container started and registered");
        }
    }

}
