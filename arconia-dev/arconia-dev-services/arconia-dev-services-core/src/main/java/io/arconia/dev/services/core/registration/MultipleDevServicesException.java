package io.arconia.dev.services.core.registration;

import java.util.List;

import io.arconia.core.support.Incubating;

/**
 * Exception thrown when multiple dev services in the same category are active simultaneously.
 * Only one dev service per category may be enabled at a time.
 */
@Incubating
public final class MultipleDevServicesException extends RuntimeException {

    private final String category;

    private final List<String> serviceNames;

    public MultipleDevServicesException(String category, List<String> serviceNames) {
        super("Multiple " + category + " dev services detected: " + serviceNames);
        this.category = category;
        this.serviceNames = List.copyOf(serviceNames);
    }

    public String getCategory() {
        return category;
    }

    public List<String> getServiceNames() {
        return serviceNames;
    }

}
