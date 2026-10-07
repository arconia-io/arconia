package io.arconia.dev.services.core.registration;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.testcontainers.containers.GenericContainer;

import io.arconia.boot.bootstrap.BootstrapMode;

/**
 * Keeps dev service containers across Spring Boot DevTools restarts by placing them in the
 * DevTools {@code restart} scope, where they stay until the application process exits.
 * <p>
 * A kept container is never reconfigured: configuration changes take effect once the
 * application is stopped and started again.
 */
final class DevServiceRestartSupport {

    /**
     * The name of the scope Spring Boot DevTools registers for beans surviving restarts.
     */
    static final String SCOPE_NAME = "restart";

    private static final Logger logger = LoggerFactory.getLogger(DevServiceRestartSupport.class);

    private static final String DEVTOOLS_RESTART_ENABLED_PROPERTY = "spring.devtools.restart.enabled";

    private final BeanDefinitionRegistry beanDefinitionRegistry;

    private final Environment environment;

    DevServiceRestartSupport(BeanDefinitionRegistry beanDefinitionRegistry, Environment environment) {
        Assert.notNull(beanDefinitionRegistry, "beanDefinitionRegistry cannot be null");
        Assert.notNull(environment, "environment cannot be null");
        this.beanDefinitionRegistry = beanDefinitionRegistry;
        this.environment = environment;
    }

    /**
     * Whether dev service containers are placed in the {@code restart} scope. That requires
     * dev mode, the scope registered by DevTools, and DevTools restarts not being disabled
     * via {@code spring.devtools.restart.enabled}. Otherwise, containers are singletons.
     */
    boolean isRestartScopeActive() {
        return BootstrapMode.isDev()
                && restartScope() != null
                && environment.getProperty(DEVTOOLS_RESTART_ENABLED_PROPERTY, Boolean.class, true);
    }

    /**
     * Retain the container the {@code restart} scope holds for the dev service, if it is still
     * running, and return whether one was retained. A container that is no longer running is
     * removed from the scope, so that a new one takes its place. Nothing is retained when the
     * scope is not active.
     */
    boolean retainKeptContainer(String serviceName, String containerBeanName) {
        Assert.hasText(serviceName, "serviceName cannot be null or empty");
        Assert.hasText(containerBeanName, "containerBeanName cannot be null or empty");

        Scope restartScope = restartScope();
        if (restartScope == null || !isRestartScopeActive()) {
            return false;
        }
        // The scope offers no way to look at an object without creating it,
        // so the container is taken out and put back when it is kept.
        Object container = restartScope.remove(containerBeanName);
        if (!(container instanceof GenericContainer<?> keptContainer)) {
            return false;
        }
        if (!keptContainer.isRunning()) {
            logger.info("Dev Service '{}': container {} is no longer running, a new one takes its place",
                    serviceName, ContainerRuntimeInfo.shortId(keptContainer.getContainerId()));
            return false;
        }
        restartScope.get(containerBeanName, () -> container);
        return true;
    }

    /**
     * The {@code restart} scope, or {@code null} when DevTools has not registered it.
     */
    @Nullable
    private Scope restartScope() {
        return (beanDefinitionRegistry instanceof ConfigurableBeanFactory beanFactory)
                ? beanFactory.getRegisteredScope(SCOPE_NAME) : null;
    }

}
