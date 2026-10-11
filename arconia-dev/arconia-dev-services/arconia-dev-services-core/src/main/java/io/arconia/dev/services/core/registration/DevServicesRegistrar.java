package io.arconia.dev.services.core.registration;

import java.util.Map;
import java.util.function.Supplier;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.DefaultPropertiesPropertySource;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;

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
     * The name of the property source holding the properties added via
     * {@link #addDynamicProperty(String, Supplier)}.
     */
    public static final String DYNAMIC_PROPERTY_SOURCE_NAME = "Arconia Dev Services Dynamic Properties";

    @SuppressWarnings("NullAway.Init")
    private BeanFactory beanFactory;

    @SuppressWarnings("NullAway.Init")
    private Environment environment;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        registerDevServices(new DevServicesRegistry(registry, environment), environment);
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
        return Binder.get(environment).bindOrCreate(prefix, type);
    }

    /**
     * Set a default value for a property. It goes to the lowest-precedence property source,
     * so a value configured anywhere else (application.yml, environment variables, the defaults
     * of the {@code SpringApplication}, etc.) wins, and the default isn't added at all when the
     * property already has a value.
     */
    protected void setDefaultProperty(String key, Object value) {
        Assert.state(environment instanceof ConfigurableEnvironment,
                "environment must be a ConfigurableEnvironment");
        if (environment.containsProperty(key)) {
            return;
        }
        DefaultPropertiesPropertySource.addOrMerge(Map.of(key, value), ((ConfigurableEnvironment) environment).getPropertySources());
    }

    /**
     * Add a dynamic property whose value is resolved lazily from the given supplier.
     * <p>
     * This is useful when there is no Spring Boot {@code ConnectionDetails} interface available
     * for a given integration, providing an alternative mechanism for dev services to contribute
     * dynamic properties. The property takes precedence over any other property source, the
     * same way the connection details of a service connection replace the configured ones.
     * <p>
     * The property exists only once this registrar has run. When the auto-configuration consuming
     * the property reads it from a condition, order the auto-configuration importing this registrar
     * before the consuming one. Otherwise, the condition finds no value and silently backs off.
     * The supplier may be called at that point, before the context is refreshed, so any bean it relies on
     * must already be defined; container connection details start their container on demand
     * and are safe to look up from the supplier.
     */
    protected void addDynamicProperty(String name, Supplier<Object> valueSupplier) {
        Assert.state(environment instanceof ConfigurableEnvironment, "environment must be a ConfigurableEnvironment");
        DevServiceDynamicPropertySource.getOrCreate((ConfigurableEnvironment) environment)
                .add(name, valueSupplier);
    }

    /**
     * Get the {@link BeanFactory} associated with this registrar.
     */
    protected BeanFactory getBeanFactory() {
        return beanFactory;
    }

    /**
     * Register dev services with the given registry.
     */
    protected abstract void registerDevServices(DevServicesRegistry registry, Environment environment);

}
