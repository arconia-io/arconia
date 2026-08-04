package io.arconia.dev.services.core.aot;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.Ordered;

import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.core.autoconfigure.DevServicesConflictValidator;
import io.arconia.dev.services.core.registration.DevServiceConnectionDetailsBeanDefinition;
import io.arconia.dev.services.core.registration.DevServiceContainerBeanDefinition;

/**
 * AOT processor that excludes dev service beans from AOT processing.
 * <p>
 * Dev services are only meant for development and testing, so they should not be
 * included in the AOT-compiled application. This processor removes:
 * <ul>
 *   <li>Container beans (identified by {@link DevServiceContainerBeanDefinition})</li>
 *   <li>Connection details beans for discovered shared dev services (identified by {@link DevServiceConnectionDetailsBeanDefinition})</li>
 *   <li>Registration beans (identified by bean class {@link DevServiceRegistration})</li>
 * </ul>
 */
class DevServicesBeanFactoryInitializationAotProcessor implements BeanFactoryInitializationAotProcessor, Ordered {

    @Override
    public @Nullable BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
        if (!(beanFactory instanceof BeanDefinitionRegistry registry)) {
            return null;
        }

        String[] beanNames = beanFactory.getBeanDefinitionNames();

        for (String beanName : beanNames) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
            if (beanDefinition instanceof DevServiceContainerBeanDefinition) {
                // Remove container beans
                registry.removeBeanDefinition(beanName);
            } else if (beanDefinition instanceof DevServiceConnectionDetailsBeanDefinition) {
                // Remove connection details beans for discovered shared dev services
                registry.removeBeanDefinition(beanName);
            } else if (DevServiceRegistration.class.getName().equals(beanDefinition.getBeanClassName())) {
                // Remove registration beans
                registry.removeBeanDefinition(beanName);
            } else if (DevServicesConflictValidator.class.getName().equals(beanDefinition.getBeanClassName())) {
                // Remove the conflict validator bean (backed by an instance supplier)
                registry.removeBeanDefinition(beanName);
            }
        }

        return null;
    }

    @Override
    public int getOrder() {
        // Run before Spring's BeanRegistrationsAotProcessor so that dev service beans
        // (backed by instance suppliers, which cannot be code-generated) are removed
        // before AOT code generation takes place.
        return Ordered.HIGHEST_PRECEDENCE;
    }

}
