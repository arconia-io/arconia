package io.arconia.observation.autoconfigure;

import java.util.List;

/**
 * Exception thrown when multiple observation convention modules are detected
 * on the classpath without an explicit selection via the
 * {@code arconia.observations.conventions.type} property.
 */
public class MultipleObservationConventionsException extends RuntimeException {

    private final List<String> conventionNames;

    public MultipleObservationConventionsException(List<String> conventionNames) {
        super("Multiple observation convention modules detected: " + conventionNames);
        this.conventionNames = conventionNames;
    }

    public List<String> getConventionNames() {
        return conventionNames;
    }

}
