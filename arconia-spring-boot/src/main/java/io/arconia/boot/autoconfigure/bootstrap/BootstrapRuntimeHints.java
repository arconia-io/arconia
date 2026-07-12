package io.arconia.boot.autoconfigure.bootstrap;

import org.jspecify.annotations.Nullable;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

import io.arconia.boot.bootstrap.BootstrapConfigurationFile;

/**
 * Makes the bootstrap configuration file available in a GraalVM native image.
 */
class BootstrapRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
        hints.resources().registerPattern(BootstrapConfigurationFile.LOCATION);
    }

}
