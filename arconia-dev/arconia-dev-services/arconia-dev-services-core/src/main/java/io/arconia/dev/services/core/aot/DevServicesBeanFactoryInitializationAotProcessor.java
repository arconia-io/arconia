package io.arconia.dev.services.core.aot;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.core.registration.DevServiceContainerBeanDefinition;

/**
 * AOT processor that excludes dev service beans from AOT processing.
 * <p>
 * Dev services are only meant for development and testing, so they should not be
 * included in the AOT-compiled application. This processor removes both:
 * <ul>
 *   <li>Container beans (identified by {@link DevServiceContainerBeanDefinition})</li>
 *   <li>Registration beans (identified by bean class {@link DevServiceRegistration})</li>
 * </ul>
 */
class DevServicesBeanFactoryInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {

    @Override
    public @Nullable BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
        String[] beanNames = beanFactory.getBeanDefinitionNames();
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;

        for (String beanName : beanNames) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
            if (beanDefinition instanceof DevServiceContainerBeanDefinition) {
                // Remove container beans
                registry.removeBeanDefinition(beanName);
            } else if (DevServiceRegistration.class.getName().equals(beanDefinition.getBeanClassName())) {
                // Remove registration beans
                registry.removeBeanDefinition(beanName);
            }
        }

        return null;
    }

}
