package io.arconia.multitenancy.core.tenantdetails;

import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.support.DefaultConversionService;

import io.arconia.core.annotations.Incubating;

/**
 * Provides core tenant information.
 */
@Incubating
public interface TenantDetails {

    /**
     * Identifier for the tenant.
     */
    String identifier();

    /**
     * Whether the tenant is enabled.
     */
    boolean enabled();

    /**
     * Additional information about the tenant.
     */
    default Map<String, Object> attributes() {
        return Map.of();
    }

    /**
     * Returns an attribute value by key, converted to the given type when needed, or
     * {@code null} when the attribute is absent. Values are converted with Spring's
     * {@link DefaultConversionService}, so the same call works whether the tenant store
     * keeps typed values, as configuration properties do, or only text, as a database does.
     * @throws IllegalStateException if the value cannot be converted to the given type
     */
    @Nullable
    default <T> T attribute(String key, Class<T> type) {
        var value = attributes().get(key);
        if (value == null) {
            return null;
        }
        try {
            return DefaultConversionService.getSharedInstance().convert(value, type);
        } catch (ConversionException ex) {
            throw new IllegalStateException("Attribute '%s' cannot be converted to type '%s'".formatted(key, type.getName()), ex);
        }
    }

}
