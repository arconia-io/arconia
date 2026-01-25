package io.arconia.dev.services.core.registration;

import java.util.Map;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.DefaultPropertiesPropertySource;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;

import io.arconia.boot.bootstrap.BootstrapMode;
import io.arconia.core.support.Incubating;

/**
 * Base class for {@link ImportBeanDefinitionRegistrar} implementations that register dev services,
 * including Testcontainers beans and other components needed for development and testing.
 * <p>
 * If you create a Dev Services module, you should extend this class,
 * implement {@link #registerDevServices(DevServicesRegistry, Environment)},
 * and import it in your module's {@code @AutoConfiguration} class.
 */
@Incubating
public abstract class DevServicesRegistrar implements ImportBeanDefinitionRegistrar, BeanFactoryAware, EnvironmentAware {

    /**
     * The bean name of the {@link DevServicesRegistry}.
     */
    public static final String DEV_SERVICES_REGISTRY_BEAN_NAME = "devServicesRegistry";

    @Nullable
    private BeanFactory beanFactory;

    @Nullable
    private Environment environment;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        Assert.notNull(beanFactory, "beanFactory has not been initialized");
        Assert.notNull(environment, "environment has not been initialized");

        DevServicesRegistry devServicesRegistry = getOrCreateDevServicesRegistry(registry);

        registerDevServices(devServicesRegistry, environment);
    }

    /**
     * Get the existing {@link DevServicesRegistry} bean or create a new one if it doesn't exist.
     */
    private DevServicesRegistry getOrCreateDevServicesRegistry(BeanDefinitionRegistry beanDefinitionRegistry) {
        // Check if DevServicesRegistry bean already exists.
        if (beanFactory != null && beanFactory.containsBean(DEV_SERVICES_REGISTRY_BEAN_NAME)) {
            return beanFactory.getBean(DEV_SERVICES_REGISTRY_BEAN_NAME, DevServicesRegistry.class);
        }

        // Create a new DevServicesRegistry instance.
        DevServicesRegistry devServicesRegistry = new DevServicesRegistry(beanDefinitionRegistry);

        // Register the new DevServicesRegistry instance as a singleton bean.
        if (beanDefinitionRegistry instanceof DefaultListableBeanFactory beanFactoryRegistry) {
            beanFactoryRegistry.registerSingleton(DEV_SERVICES_REGISTRY_BEAN_NAME, devServicesRegistry);
        }

        return devServicesRegistry;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    /**
     * Bind configuration properties from the environment for the given prefix and target type.
     */
    protected <T> T bindProperties(String prefix, Class<T> type) {
        Assert.notNull(environment, "environment has not been initialized");
        return Binder.get(environment).bindOrCreate(prefix, type);
    }

    /**
     * Set default properties that will only be used if not already defined.
     * These defaults have the lowest priority and can be overridden by any other property source.
     */
    protected void setDefaultProperties(Map<String, Object> defaults) {
        Assert.notNull(environment, "environment has not been initialized");

        if (environment instanceof ConfigurableEnvironment configurableEnvironment) {
            // Filter out properties that are already set.
            Map<String, Object> propertiesToAdd = defaults.entrySet().stream()
                    .filter(entry -> !configurableEnvironment.containsProperty(entry.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            if (!propertiesToAdd.isEmpty()) {
                // Add the filtered properties to the environment.
                DefaultPropertiesPropertySource.addOrMerge(propertiesToAdd, configurableEnvironment.getPropertySources());
            }
        }
    }

    /**
     * Set a default property that will only be used if not already defined.
     */
    protected void setDefaultProperty(String key, Object value) {
        setDefaultProperties(Map.of(key, value));
    }

    /**
     * Check if the application is running in development mode.
     */
    protected boolean isDevMode() {
        return BootstrapMode.DEV.equals(BootstrapMode.detect());
    }

    /**
     * Register dev services with the given registry.
     */
    protected abstract void registerDevServices(DevServicesRegistry registry, Environment environment);

}
