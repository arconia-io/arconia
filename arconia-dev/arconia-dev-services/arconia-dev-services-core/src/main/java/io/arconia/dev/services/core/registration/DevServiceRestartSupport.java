package io.arconia.dev.services.core.registration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.testcontainers.containers.GenericContainer;

import io.arconia.boot.bootstrap.BootstrapMode;

/**
 * Keeps dev service containers across Spring Boot DevTools restarts by placing them in the
 * DevTools {@code restart} scope, where they stay until the application process exits, and
 * stops them when it does.
 * <p>
 * The {@code restart} scope never destroys what it holds, and Testcontainers leaves a container
 * running until Ryuk notices the process is gone, which takes a few seconds. In the meantime,
 * another application could discover the container and connect to it right before it disappears.
 * So the containers kept by this process are stopped when the JVM exits, as the containers of
 * an application without DevTools are stopped with its application context.
 */
final class DevServiceRestartSupport {

    /**
     * The name of the scope Spring Boot DevTools registers for beans surviving restarts.
     */
    static final String SCOPE_NAME = "restart";

    private static final Logger logger = LoggerFactory.getLogger(DevServiceRestartSupport.class);

    private static final String DEVTOOLS_RESTART_ENABLED_PROPERTY = "spring.devtools.restart.enabled";

    /**
     * The containers this process keeps across restarts. One per JVM, as the {@code restart} scope is.
     */
    private static final Map<GenericContainer<?>, KeptContainer> KEPT_CONTAINERS = new LinkedHashMap<>();

    private static final AtomicBoolean SHUTDOWN_HANDLER_REGISTERED = new AtomicBoolean();

    private final BeanDefinitionRegistry beanDefinitionRegistry;

    private final Environment environment;

    private final BooleanSupplier testcontainersReuseSupport;

    DevServiceRestartSupport(BeanDefinitionRegistry beanDefinitionRegistry, Environment environment, BooleanSupplier testcontainersReuseSupport) {
        Assert.notNull(beanDefinitionRegistry, "beanDefinitionRegistry cannot be null");
        Assert.notNull(environment, "environment cannot be null");
        Assert.notNull(testcontainersReuseSupport, "testcontainersReuseSupport cannot be null");
        this.beanDefinitionRegistry = beanDefinitionRegistry;
        this.environment = environment;
        this.testcontainersReuseSupport = testcontainersReuseSupport;
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
            release(keptContainer, isReused(keptContainer));
            return false;
        }
        if (!containerType.isInstance(keptContainer)) {
            logger.info("Dev Service '{}': the container class was reloaded, a new container takes the place of {}",
                    serviceName, ContainerRuntimeInfo.shortId(keptContainer.getContainerId()));
            release(keptContainer, isReused(keptContainer));
            return false;
        }
        // The scope offers no way to look at an object without creating it,
        // so the container taken out is put back when it is kept.
        requireRestartScope().get(containerBeanName, () -> keptContainer);
        return true;
    }

    /**
     * Keep the given container of the dev service across restarts: the {@code restart} scope
     * holds it, and it is stopped when the JVM exits. Nothing happens when the scope is not active.
     */
    void keepAcrossRestarts(String serviceName, GenericContainer<?> container) {
        Assert.hasText(serviceName, "serviceName cannot be null or empty");
        Assert.notNull(container, "container cannot be null");

        if (!isRestartScopeActive()) {
            return;
        }
        synchronized (KEPT_CONTAINERS) {
            KEPT_CONTAINERS.put(container, new KeptContainer(serviceName, isReused(container)));
        }
        if (SHUTDOWN_HANDLER_REGISTERED.compareAndSet(false, true)) {
            SpringApplication.getShutdownHandlers().add(DevServiceRestartSupport::stopKeptContainers);
        }
    }

    /**
     * Stop the containers kept across restarts, at JVM exit. A container reused via
     * Testcontainers is left running, as everywhere else.
     */
    static void stopKeptContainers() {
        List<Map.Entry<GenericContainer<?>, KeptContainer>> kept;
        synchronized (KEPT_CONTAINERS) {
            kept = new ArrayList<>(KEPT_CONTAINERS.entrySet());
            KEPT_CONTAINERS.clear();
        }
        for (Map.Entry<GenericContainer<?>, KeptContainer> entry : kept) {
            GenericContainer<?> container = entry.getKey();
            KeptContainer keptContainer = entry.getValue();
            try {
                if (!keptContainer.reused()) {
                    logger.info("Dev Service '{}': stopping container {} as the application exits",
                            keptContainer.serviceName(), ContainerRuntimeInfo.shortId(container.getContainerId()));
                }
                release(container, keptContainer.reused());
            } catch (Exception ex) {
                logger.warn("Dev Service '{}': failed to stop container {} ({})",
                        keptContainer.serviceName(), ContainerRuntimeInfo.shortId(container.getContainerId()), ex.getMessage());
            }
        }
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
        release(keptContainer, isReused(keptContainer));
    }

    /**
     * Take the container the {@code restart} scope holds under the given bean name out of the
     * scope, or {@code null} when the scope is not active or holds none.
     */
    @Nullable
    private GenericContainer<?> takeKeptContainer(String containerBeanName) {
        if (!isRestartScopeActive()) {
            return null;
        }
        return (requireRestartScope().remove(containerBeanName) instanceof GenericContainer<?> keptContainer) ? keptContainer : null;
    }

    /**
     * Stop a container the application no longer uses. A container reused via Testcontainers
     * is left alone: it outlives the application by design, and Testcontainers reattaches to
     * it as long as it runs.
     */
    private static void release(GenericContainer<?> container, boolean reused) {
        synchronized (KEPT_CONTAINERS) {
            KEPT_CONTAINERS.remove(container);
        }
        if (reused) {
            logger.info("Container {} is left running, as the 'testcontainers' reuse strategy keeps it",
                    ContainerRuntimeInfo.shortId(container.getContainerId()));
            return;
        }
        container.stop();
    }

    /**
     * Whether the container is reused via Testcontainers, which requires the reuse feature to be
     * enabled on the machine as well, the same condition Spring Boot applies when it closes containers.
     */
    private boolean isReused(GenericContainer<?> container) {
        return container.isShouldBeReused() && testcontainersReuseSupport.getAsBoolean();
    }

    /**
     * A container kept across restarts: the dev service it belongs to, and whether it is reused
     * via Testcontainers, decided when it is kept.
     */
    private record KeptContainer(String serviceName, boolean reused) {}

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
