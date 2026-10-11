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
     * Whether dev service containers are placed in the {@code restart} scope.
     * Otherwise, containers are singletons.
     */
    boolean isRestartScopeActive() {
        return BootstrapMode.isDev()
                && restartScope() != null
                && environment.getProperty(DEVTOOLS_RESTART_ENABLED_PROPERTY, Boolean.class, true);
    }

    /**
     * Retain the container the {@code restart} scope holds for the dev service, if it is still
     * running and of the given type, and return whether one was retained. A container that is
     * no longer running, or whose class DevTools has reloaded (the kept instance belongs to the
     * previous class loader and cannot be injected as the new type), is removed from the scope
     * and released, so that a new one takes its place. Nothing is retained when the scope is not active.
     */
    boolean retainKeptContainer(String serviceName, String containerBeanName, Class<? extends GenericContainer<?>> containerType) {
        Assert.hasText(serviceName, "serviceName cannot be null or empty");
        Assert.hasText(containerBeanName, "containerBeanName cannot be null or empty");
        Assert.notNull(containerType, "containerType cannot be null");

        GenericContainer<?> keptContainer = takeKeptContainer(containerBeanName);
        if (keptContainer == null) {
            return false;
        }
        if (!keptContainer.isRunning()) {
            logger.info("Dev Service '{}': container {} is no longer running, a new one takes its place",
                    serviceName, ContainerRuntimeInfo.shortId(keptContainer.getContainerId()));
            release(keptContainer);
            return false;
        }
        if (!containerType.isInstance(keptContainer)) {
            logger.info("Dev Service '{}': the container class was reloaded, a new container takes the place of {}",
                    serviceName, ContainerRuntimeInfo.shortId(keptContainer.getContainerId()));
            release(keptContainer);
            return false;
        }
        // The scope offers no way to look at an object without creating it,
        // so the container taken out is put back when it is kept.
        requireRestartScope().get(containerBeanName, () -> keptContainer);
        return true;
    }

    /**
     * Discard the container the {@code restart} scope holds for the dev service, if any, when
     * the dev service is not registered after a restart (for example, because the application now
     * defines its own connection details): the container is removed from the scope and released,
     * so that whatever it holds, such as a fixed port, is available again.
     */
    void discardKeptContainer(String serviceName, String containerBeanName) {
        Assert.hasText(serviceName, "serviceName cannot be null or empty");
        Assert.hasText(containerBeanName, "containerBeanName cannot be null or empty");

        GenericContainer<?> keptContainer = takeKeptContainer(containerBeanName);
        if (keptContainer == null) {
            return;
        }
        logger.info("Dev Service '{}': container {} is no longer needed", serviceName, ContainerRuntimeInfo.shortId(keptContainer.getContainerId()));
        release(keptContainer);
    }

    /**
     * Take the container the {@code restart} scope holds under the given bean name out of the
     * scope, or {@code null} when the scope is not active or holds none.
     */
    @Nullable
    private GenericContainer<?> takeKeptContainer(String containerBeanName) {
        Scope restartScope = restartScope();
        if (restartScope == null || !isRestartScopeActive()) {
            return null;
        }
        return (restartScope.remove(containerBeanName) instanceof GenericContainer<?> keptContainer) ? keptContainer : null;
    }

    /**
     * Stop a container the application no longer uses. A container reused via Testcontainers
     * is left alone: it outlives the application by design, and Testcontainers reattaches to
     * it as long as it runs.
     */
    private static void release(GenericContainer<?> container) {
        if (!container.isShouldBeReused()) {
            container.stop();
        }
    }

    private Scope requireRestartScope() {
        Scope restartScope = restartScope();
        Assert.notNull(restartScope, "restartScope cannot be null");
        return restartScope;
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
