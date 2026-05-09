package io.arconia.dev.services.core.aot;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;

import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.core.registration.DevServiceContainerBeanDefinition;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevServicesBeanFactoryInitializationAotProcessor}.
 */
class DevServicesBeanFactoryInitializationAotProcessorTests {

    private final DevServicesBeanFactoryInitializationAotProcessor processor = new DevServicesBeanFactoryInitializationAotProcessor();

    private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

    @Test
    void returnsNullWhenBeanFactoryIsEmpty() {
        var contribution = processor.processAheadOfTime(beanFactory);
        assertThat(contribution).isNull();
    }

    @Test
    void returnsNullWhenBeanFactoryHasBeans() {
        beanFactory.registerBeanDefinition("regularBean", regularBeanDefinition());
        var contribution = processor.processAheadOfTime(beanFactory);
        assertThat(contribution).isNull();
    }

    @Test
    void removesContainerBeanDefinitions() {
        beanFactory.registerBeanDefinition("devService.container.postgres", new DevServiceContainerBeanDefinition());
        beanFactory.registerBeanDefinition("devService.container.redis", new DevServiceContainerBeanDefinition());

        processor.processAheadOfTime(beanFactory);

        assertThat(beanFactory.containsBeanDefinition("devService.container.postgres")).isFalse();
        assertThat(beanFactory.containsBeanDefinition("devService.container.redis")).isFalse();
    }

    @Test
    void removesRegistrationBeanDefinitions() {
        beanFactory.registerBeanDefinition("devServiceRegistration.postgres", registrationBeanDefinition());
        beanFactory.registerBeanDefinition("devServiceRegistration.redis", registrationBeanDefinition());

        processor.processAheadOfTime(beanFactory);

        assertThat(beanFactory.containsBeanDefinition("devServiceRegistration.postgres")).isFalse();
        assertThat(beanFactory.containsBeanDefinition("devServiceRegistration.redis")).isFalse();
    }

    @Test
    void doesNotRemoveRegularBeanDefinitions() {
        beanFactory.registerBeanDefinition("regularBean", regularBeanDefinition());

        processor.processAheadOfTime(beanFactory);

        assertThat(beanFactory.containsBeanDefinition("regularBean")).isTrue();
    }

    private GenericBeanDefinition registrationBeanDefinition() {
        GenericBeanDefinition definition = new GenericBeanDefinition();
        definition.setBeanClassName(DevServiceRegistration.class.getName());
        return definition;
    }

    private GenericBeanDefinition regularBeanDefinition() {
        GenericBeanDefinition definition = new GenericBeanDefinition();
        definition.setBeanClassName(String.class.getName());
        return definition;
    }

}
