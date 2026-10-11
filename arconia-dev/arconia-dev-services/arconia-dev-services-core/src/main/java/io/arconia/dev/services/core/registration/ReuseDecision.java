package io.arconia.dev.services.core.registration;

import java.util.function.BooleanSupplier;

import org.jspecify.annotations.Nullable;

import io.arconia.boot.bootstrap.BootstrapMode;
import io.arconia.dev.services.api.config.ReuseStrategy;

/**
 * The reuse strategy in effect for a dev service and, when it differs from the configured
 * one, the reason why the configured strategy could not be applied.
 */
record ReuseDecision(ReuseStrategy effective, @Nullable String reason) {

    /**
     * Decide the reuse strategy in effect for the given dev service. Reuse only applies in
     * dev mode. The {@code framework} strategy requires the dev service to declare its
     * discovery, and the {@code testcontainers} strategy requires reuse to be enabled in the
     * Testcontainers configuration, which the given check reports.
     */
    static ReuseDecision decide(DevServiceSpec service, BooleanSupplier testcontainersReuseSupport) {
        ReuseStrategy configured = service.getProperties().getReuseStrategy();
        if (configured == ReuseStrategy.NONE || !BootstrapMode.isDev()) {
            return new ReuseDecision(ReuseStrategy.NONE, null);
        }
        if (configured == ReuseStrategy.FRAMEWORK && !service.supportsDiscovery()) {
            return new ReuseDecision(ReuseStrategy.NONE, "'framework' is not supported by this dev service");
        }
        if (configured == ReuseStrategy.TESTCONTAINERS && !testcontainersReuseSupport.getAsBoolean()) {
            return new ReuseDecision(ReuseStrategy.NONE,
                    "'testcontainers' requires reuse to be enabled in the Testcontainers configuration (testcontainers.reuse.enable=true in ~/.testcontainers.properties or TESTCONTAINERS_REUSE_ENABLE=true)");
        }
        return new ReuseDecision(configured, null);
    }

}
