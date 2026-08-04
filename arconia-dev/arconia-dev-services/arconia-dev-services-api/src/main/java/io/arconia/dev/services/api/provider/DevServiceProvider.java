package io.arconia.dev.services.api.provider;

import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;

/**
 * Service Provider Interface (SPI) for dev service modules that belong to a mutually
 * exclusive category. Each module registers a bean implementing this interface to enable
 * conflict detection at startup.
 * <p>
 * Implementations should use the constants in {@link DevServiceCategories} for their
 * {@link #category()} rather than inventing new category strings, so that mutually
 * exclusive services are detected consistently across modules.
 *
 * @see DevServiceCategories
 */
@Incubating
public interface DevServiceProvider {

    /**
     * The name of this dev service (e.g., {@code "lgtm"}, {@code "phoenix"}).
     */
    String name();

    /**
     * The category this dev service belongs to (e.g., {@link DevServiceCategories#OPENTELEMETRY}).
     * Only one dev service per category may be active at a time.
     */
    String category();

    /**
     * Create a {@link DevServiceProvider} with the given name and category.
     */
    static DevServiceProvider of(String name, String category) {
        Assert.hasText(name, "name cannot be null or empty");
        Assert.hasText(category, "category cannot be null or empty");

        return new DevServiceProvider() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String category() {
                return category;
            }
        };
    }

}
