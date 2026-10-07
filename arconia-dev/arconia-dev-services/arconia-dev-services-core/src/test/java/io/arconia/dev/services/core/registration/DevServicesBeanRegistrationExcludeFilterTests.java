package io.arconia.dev.services.core.registration;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.core.env.StandardEnvironment;
import org.testcontainers.containers.GenericContainer;

import io.arconia.boot.bootstrap.BootstrapMode;
import io.arconia.dev.services.api.registration.ContainerInfo;
import io.arconia.dev.services.api.registration.DevServiceLabels;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevServicesBeanRegistrationExcludeFilter}.
 */
class DevServicesBeanRegistrationExcludeFilterTests {

    private final DevServicesBeanRegistrationExcludeFilter filter = new DevServicesBeanRegistrationExcludeFilter();

    private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

    @BeforeEach
    @AfterEach
    void resetBootstrapMode() {
        System.clearProperty(BootstrapMode.PROPERTY_KEY);
        BootstrapMode.clear();
    }

    @Test
    void ownedDevServiceBeansAreExcluded() {
        new DevServicesRegistry(beanFactory, new StandardEnvironment()).registerDevService(service -> service
                .name("postgres")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(TestContainer.class, TestContainer::new));

        assertThat(isExcluded("devService.container.postgres")).isTrue();
        assertThat(isExcluded("devService.registration.postgres")).isTrue();
    }

    @Test
    void discoveredDevServiceBeansAreExcluded() {
        System.setProperty(BootstrapMode.PROPERTY_KEY, "dev");
        BootstrapMode.clear();
        new DevServicesRegistry(beanFactory, new StandardEnvironment(), serviceName -> List.of(discoveredContainer()))
                .registerDevService(service -> service
                        .name("postgres")
                        .properties(TestDevServicesProperties.FRAMEWORK)
                        .container(TestContainer.class, TestContainer::new)
                        .discovery(TestConnectionDetails.class, container -> new TestConnectionDetails(container.host())));

        assertThat(isExcluded("devService.connectionDetails.postgres")).isTrue();
        assertThat(isExcluded("devService.registration.postgres")).isTrue();
    }

    @Test
    void otherBeansAreNotExcluded() {
        beanFactory.registerBeanDefinition("other", new RootBeanDefinition(TestContainer.class));

        assertThat(isExcluded("other")).isFalse();
    }

    /**
     * AOT works with the merged bean definition, a copy of the registered one, which is what
     * the filter has to recognize dev service beans from.
     */
    private boolean isExcluded(String beanName) {
        return filter.isExcludedFromAotProcessing(RegisteredBean.of(beanFactory, beanName));
    }

    private static DiscoveredContainer discoveredContainer() {
        return new DiscoveredContainer(ContainerInfo.builder()
                .id("abc123")
                .imageName("postgres:latest")
                .names(List.of("shared-postgres"))
                .exposedPorts(List.of(new ContainerInfo.ContainerPort("0.0.0.0", 5432, 54321, "tcp")))
                .labels(Map.of(DevServiceLabels.NAME, "postgres", DevServiceLabels.DISCOVERABLE, "true"))
                .status("running")
                .build(), "localhost");
    }

    private record TestConnectionDetails(String host) implements ConnectionDetails {}

    private static class TestContainer extends GenericContainer<TestContainer> {
        TestContainer() {
            super("postgres:latest");
        }
    }

}
