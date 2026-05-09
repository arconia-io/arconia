package io.arconia.dev.services.core.autoconfigure;

import java.util.List;

/**
 * Exception thrown when multiple dev services in the same category are active simultaneously.
 * Only one dev service per category may be enabled at a time.
 */
public class MultipleDevServicesException extends RuntimeException {

    private final String category;

    private final List<String> serviceNames;

    public MultipleDevServicesException(String category, List<String> serviceNames) {
        super("Multiple " + category + " dev services detected: " + serviceNames);
        this.category = category;
        this.serviceNames = serviceNames;
    }

    public String getCategory() {
        return category;
    }

    public List<String> getServiceNames() {
        return serviceNames;
    }

}
