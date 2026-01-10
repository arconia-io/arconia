package io.arconia.dev.services.core.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.util.ClassUtils;

import io.arconia.core.support.Internal;

/**
 * Provides utility methods for registering dev services beans.
 * Only for internal use.
 */
@Internal
public final class DevServicesBeanRegistrations {

    private static final String RESTART_SCOPE_CLASS = "org.springframework.boot.devtools.restart.RestartScope";

    private DevServicesBeanRegistrations() {}

    public static BeanFactoryPostProcessor beanFactoryPostProcessor(Class<?> beanType) {
        return beanFactory -> {
            if (ClassUtils.isPresent(RESTART_SCOPE_CLASS, null)) {
                String[] beanNames = beanFactory.getBeanNamesForType(beanType);
                for (String beanName : beanNames) {
                    BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
                    beanDefinition.setScope("restart");
                }
            }
        };
    }

}
