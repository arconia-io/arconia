package io.arconia.core.bootstrap.autoconfigure;

import org.jspecify.annotations.Nullable;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

import io.arconia.core.bootstrap.BootstrapConfigurationFile;

/**
 * Makes the bootstrap configuration file available in a GraalVM native image.
 */
class BootstrapRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
        hints.resources().registerPattern(BootstrapConfigurationFile.LOCATION);
    }

}
