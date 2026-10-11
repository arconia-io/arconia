package io.arconia.dev.services.core.registration;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.support.SimpleThreadScope;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.testcontainers.containers.GenericContainer;

import io.arconia.dev.services.api.config.BaseDevServicesProperties;
import io.arconia.dev.services.core.TestDevServicesProperties;
import io.arconia.dev.services.core.registration.DevServicesTestSupport.TestConnectionDetails;

import static io.arconia.dev.services.core.registration.DevServicesTestSupport.discoveredContainer;
import static io.arconia.dev.services.core.registration.DevServicesTestSupport.enableDevMode;
import static io.arconia.dev.services.core.registration.DevServicesTestSupport.registerPostgresDevService;
import static io.arconia.dev.services.core.registration.DevServicesTestSupport.resetBootstrapMode;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevServicesRegistry}: the scope of dev service containers, and how
 * they are kept across Spring Boot DevTools restarts.
 */
@ExtendWith(OutputCaptureExtension.class)
class DevServicesRestartTests {

    private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

    private final DevServicesRegistry registry = new DevServicesRegistry(beanFactory, new StandardEnvironment());

    /**
     * Stands in for the scope DevTools registers, which outlives the application contexts.
     */
    private final SimpleThreadScope restartScope = new SimpleThreadScope();

    @BeforeEach
    @AfterEach
    void reset() {
        resetBootstrapMode();
    }

    @Test
    void whenDevToolsRestartScopeRegisteredInDevModeThenContainerIsRestartScoped() {
        enableDevMode();
        beanFactory.registerScope("restart", restartScope);

        registerPostgresDevService(registry);

        assertThat(beanFactory.getBeanDefinition("devService.container.postgres").getScope()).isEqualTo("restart");
    }

    @Test
    void whenDevToolsRestartIsDisabledThenContainerIsSingleton() {
        enableDevMode();
        beanFactory.registerScope("restart", restartScope);
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test",
                Map.of("spring.devtools.restart.enabled", "false")));

        registerPostgresDevService(new DevServicesRegistry(beanFactory, environment));

        assertThat(beanFactory.getBeanDefinition("devService.container.postgres").getScope()).isEqualTo("singleton");
    }

    @Test
    void whenNotInDevModeThenContainerIsSingleton() {
        beanFactory.registerScope("restart", restartScope);

        registerPostgresDevService(registry);

        assertThat(beanFactory.getBeanDefinition("devService.container.postgres").getScope()).isEqualTo("singleton");
    }

    @Test
    void whenNoRestartScopeRegisteredThenContainerIsSingleton() {
        enableDevMode();

        registerPostgresDevService(registry);

        assertThat(beanFactory.getBeanDefinition("devService.container.postgres").getScope()).isEqualTo("singleton");
    }

    @Test
    void whenConfigurationChangedAcrossRestartsThenContainerIsKept() {
        enableDevMode();

        RestartingContainer first = restart(Map.of("test.dev.services.postgres.image-name", "postgres:17"),
                TestDevServicesProperties.DEFAULT, List.of());
        RestartingContainer second = restart(Map.of("test.dev.services.postgres.image-name", "postgres:18"),
                TestDevServicesProperties.DEFAULT, List.of());

        assertThat(second).isSameAs(first);
        assertThat(first.stopped).isFalse();
    }

    @Test
    void whenKeptContainerIsNoLongerRunningThenANewOneTakesItsPlace(CapturedOutput output) {
        enableDevMode();

        RestartingContainer first = restart(Map.of(), TestDevServicesProperties.DEFAULT, List.of());
        // The container was removed outside the application.
        first.running = false;
        RestartingContainer second = restart(Map.of(), TestDevServicesProperties.DEFAULT, List.of());

        assertThat(second).isNotSameAs(first);
        // The kept instance is stopped, so that whatever it still holds (e.g. a fixed port) is released.
        assertThat(first.stopped).isTrue();
        assertThat(output).contains("is no longer running");
    }

    @Test
    void whenKeptContainerClassWasReloadedThenANewOneTakesItsPlace(CapturedOutput output) {
        enableDevMode();

        RestartingContainer first = restart(Map.of(), TestDevServicesProperties.DEFAULT, List.of());
        // DevTools reloaded the container class: the kept instance belongs to the previous class
        // loader, which the new bean definition's type stands in for.
        ReloadedContainer second = restartedBeanFactory(Map.of(), TestDevServicesProperties.DEFAULT, List.of(), ReloadedContainer.class, ReloadedContainer::new)
                .getBean("devService.container.postgres", ReloadedContainer.class);

        assertThat(second).isNotSameAs(first);
        assertThat(first.stopped).isTrue();
        assertThat(output).contains("container class was reloaded");
    }

    @Test
    void whenKeptContainerIsNoLongerRunningThenDiscoveryHappensAgain() {
        enableDevMode();

        RestartingContainer first = restart(Map.of(), TestDevServicesProperties.FRAMEWORK, List.of());
        first.running = false;
        DefaultListableBeanFactory restarted = restartedBeanFactory(Map.of(),
                TestDevServicesProperties.FRAMEWORK, List.of(discoveredContainer()));

        assertThat(restarted.containsBeanDefinition("devService.container.postgres")).isFalse();
        assertThat(restarted.containsBeanDefinition("devService.connectionDetails.postgres")).isTrue();
    }

    @Test
    void whenManagedContainerKeptAcrossRestartsThenNoDiscoveryHappens() {
        enableDevMode();

        RestartingContainer first = restart(Map.of(), TestDevServicesProperties.FRAMEWORK, List.of());
        // Another application has started a discoverable container in the meantime.
        RestartingContainer second = restart(Map.of(), TestDevServicesProperties.FRAMEWORK, List.of(discoveredContainer()));

        assertThat(second).isSameAs(first);
    }

    /**
     * Simulate an application start, or a DevTools restart when called again: a new bean factory
     * sharing the same restart scope, in which the dev service is registered with the given
     * configuration, returning the container bean.
     */
    private RestartingContainer restart(Map<String, Object> configuration, BaseDevServicesProperties properties,
            List<DiscoveredContainer> discoveredContainers) {
        return restartedBeanFactory(configuration, properties, discoveredContainers)
                .getBean("devService.container.postgres", RestartingContainer.class);
    }

    private DefaultListableBeanFactory restartedBeanFactory(Map<String, Object> configuration, BaseDevServicesProperties properties,
            List<DiscoveredContainer> discoveredContainers) {
        return restartedBeanFactory(configuration, properties, discoveredContainers, RestartingContainer.class, RestartingContainer::new);
    }

    private <C extends GenericContainer<?>> DefaultListableBeanFactory restartedBeanFactory(Map<String, Object> configuration,
            BaseDevServicesProperties properties, List<DiscoveredContainer> discoveredContainers, Class<C> containerType, Supplier<C> containerSupplier) {
        var restartedBeanFactory = new DefaultListableBeanFactory();
        restartedBeanFactory.registerScope("restart", restartScope);
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", configuration));
        new DevServicesRegistry(restartedBeanFactory, environment, serviceName -> discoveredContainers)
                .registerDevService(service -> service
                        .name("postgres")
                        .description("Test Dev Service")
                        .properties(properties)
                        .container(containerType, containerSupplier)
                        .discovery(TestConnectionDetails.class,
                                container -> new TestConnectionDetails(container.host())));
        return restartedBeanFactory;
    }

    /**
     * A container recording whether it was stopped and reporting whether it is running,
     * without requiring a container runtime.
     */
    private static class RestartingContainer extends GenericContainer<RestartingContainer> {

        private boolean stopped;

        private boolean running = true;

        RestartingContainer() {
            super("postgres:latest");
        }

        @Override
        public void stop() {
            stopped = true;
        }

        @Override
        public boolean isRunning() {
            return running;
        }

    }

    /**
     * The container class as DevTools reloads it: a different class standing in for the one the
     * kept container was created from.
     */
    private static class ReloadedContainer extends GenericContainer<ReloadedContainer> {
        ReloadedContainer() {
            super("postgres:latest");
        }
    }

}
