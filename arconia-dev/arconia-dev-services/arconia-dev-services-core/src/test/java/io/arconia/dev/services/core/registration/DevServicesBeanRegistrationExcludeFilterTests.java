package io.arconia.dev.services.core.registration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;

import io.arconia.dev.services.core.registration.DevServicesRegistry.BeanName;
import io.arconia.dev.services.core.registration.DevServicesTestSupport.TestPostgresContainer;

import static io.arconia.dev.services.core.registration.DevServicesTestSupport.discoveredContainer;
import static io.arconia.dev.services.core.registration.DevServicesTestSupport.enableDevMode;
import static io.arconia.dev.services.core.registration.DevServicesTestSupport.registerPostgresDevService;
import static io.arconia.dev.services.core.registration.DevServicesTestSupport.registerSharedDevService;
import static io.arconia.dev.services.core.registration.DevServicesTestSupport.registryDiscovering;
import static io.arconia.dev.services.core.registration.DevServicesTestSupport.resetBootstrapMode;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevServicesBeanRegistrationExcludeFilter}.
 */
class DevServicesBeanRegistrationExcludeFilterTests {

    private final DevServicesBeanRegistrationExcludeFilter filter = new DevServicesBeanRegistrationExcludeFilter();

    private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

    @BeforeEach
    @AfterEach
    void reset() {
        resetBootstrapMode();
    }

    @Test
    void managedDevServiceBeansAreExcluded() {
        registerPostgresDevService(new DevServicesRegistry(beanFactory, new StandardEnvironment(), new DefaultResourceLoader()));

        assertThat(isExcluded(BeanName.CONTAINER.of("postgres"))).isTrue();
        assertThat(isExcluded(BeanName.REGISTRATION.of("postgres"))).isTrue();
    }

    @Test
    void discoveredDevServiceBeansAreExcluded() {
        enableDevMode();
        registerSharedDevService(registryDiscovering(beanFactory, discoveredContainer()));

        assertThat(isExcluded(BeanName.CONNECTION_DETAILS.of("postgres"))).isTrue();
        assertThat(isExcluded(BeanName.REGISTRATION.of("postgres"))).isTrue();
    }

    @Test
    void otherBeansAreNotExcluded() {
        beanFactory.registerBeanDefinition("other", new RootBeanDefinition(TestPostgresContainer.class));

        assertThat(isExcluded("other")).isFalse();
    }

    /**
     * AOT works with the merged bean definition, a copy of the registered one, which is what
     * the filter has to recognize dev service beans from.
     */
    private boolean isExcluded(String beanName) {
        return filter.isExcludedFromAotProcessing(RegisteredBean.of(beanFactory, beanName));
    }

}
