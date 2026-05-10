package io.arconia.observation.autoconfigure;

import java.util.List;

/**
 * Exception thrown when multiple AI observation convention modules are detected on the classpath.
 */
public class MultipleAiObservationConventionsException extends RuntimeException {

    private final List<String> conventionNames;

    public MultipleAiObservationConventionsException(List<String> conventionNames) {
        super("Multiple AI observation convention modules detected: " + conventionNames);
        this.conventionNames = conventionNames;
    }

    public List<String> getConventionNames() {
        return conventionNames;
    }

}
