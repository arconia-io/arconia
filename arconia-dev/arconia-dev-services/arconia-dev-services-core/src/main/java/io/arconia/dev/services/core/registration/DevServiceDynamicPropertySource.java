package io.arconia.dev.services.core.registration;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.util.Assert;

/**
 * A {@link MapPropertySource} backed by a map with dynamically supplied values, inspired by
 * Spring Framework's {@code DynamicValuesPropertySource}.
 * <p>
 * This property source stores mappings from property names to value {@link Supplier}s.
 * Values are resolved lazily when {@link #getProperty(String)} is called, making it suitable
 * for properties that depend on containers or services that may not yet be started at
 * registration time.
 * <p>
 * Use the {@link #getOrCreate(ConfigurableEnvironment)} method to get a shared instance
 * per environment. Multiple dev service registrars can contribute properties to the same
 * property source, which has the highest precedence in the environment.
 *
 * @see DevServicesRegistrar#addDynamicProperty(String, Supplier)
 */
final class DevServiceDynamicPropertySource extends MapPropertySource {

    private final Map<String, Supplier<Object>> valueSuppliers;

    DevServiceDynamicPropertySource(Map<String, Supplier<Object>> valueSuppliers) {
        super(DevServicesRegistrar.DYNAMIC_PROPERTY_SOURCE_NAME, Collections.unmodifiableMap(valueSuppliers));
        this.valueSuppliers = valueSuppliers;
    }

    /**
     * Add a {@link Supplier} providing the value for the given property name.
     */
    void add(String name, Supplier<Object> valueSupplier) {
        Assert.hasText(name, "name cannot be null or empty");
        Assert.notNull(valueSupplier, "valueSupplier cannot be null");
        this.valueSuppliers.put(name, valueSupplier);
    }

    @Override
    @Nullable
    public Object getProperty(String name) {
        Supplier<Object> supplier = valueSuppliers.get(name);
        return (supplier != null) ? supplier.get() : null;
    }

    /**
     * Get the {@code DevServiceDynamicPropertySource} registered in the environment
     * or create and register a new {@code DevServiceDynamicPropertySource} in the
     * environment.
     */
    static synchronized DevServiceDynamicPropertySource getOrCreate(ConfigurableEnvironment environment) {
        MutablePropertySources propertySources = environment.getPropertySources();
        PropertySource<?> existingPropertySource = propertySources.get(DevServicesRegistrar.DYNAMIC_PROPERTY_SOURCE_NAME);
        if (existingPropertySource instanceof DevServiceDynamicPropertySource devServicePropertySource) {
            return devServicePropertySource;
        } else if (existingPropertySource == null) {
            var propertySource = new DevServiceDynamicPropertySource(new ConcurrentHashMap<>());
            propertySources.addFirst(propertySource);
            return propertySource;
        } else {
            throw new IllegalStateException(
                    "PropertySource with name '%s' must be a DevServiceDynamicPropertySource".formatted(DevServicesRegistrar.DYNAMIC_PROPERTY_SOURCE_NAME));
        }
    }

}
