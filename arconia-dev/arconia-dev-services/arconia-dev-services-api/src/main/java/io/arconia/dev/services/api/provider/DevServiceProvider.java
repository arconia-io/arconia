package io.arconia.dev.services.api.provider;

/**
 * Marker interface for dev service modules that belong to a mutually exclusive category.
 * Each module registers a bean implementing this interface to enable conflict detection at startup.
 *
 * @see DevServiceCategories
 */
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
