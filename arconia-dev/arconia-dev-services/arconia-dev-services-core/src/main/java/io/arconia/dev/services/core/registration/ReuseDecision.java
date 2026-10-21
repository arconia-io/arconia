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
     * discovery. The {@code testcontainers} strategy requires reuse to be enabled in the
     * Testcontainers configuration, which the given check reports, and doesn't combine with
     * the shared network: the network is created anew with every run and the Testcontainers
     * reuse hash includes it, so a reusable container on the shared network would never be
     * found again and would be left running after every run.
     */
    static ReuseDecision decide(DevServiceDefinition service, BooleanSupplier testcontainersReuseSupport, boolean networkEnabled) {
        ReuseStrategy configured = service.properties().getReuseStrategy();
        if (configured == ReuseStrategy.NONE) {
            return new ReuseDecision(ReuseStrategy.NONE, null);
        }
        if (!BootstrapMode.isDev()) {
            return new ReuseDecision(ReuseStrategy.NONE, "reuse only applies in dev mode");
        }
        if (configured == ReuseStrategy.FRAMEWORK && service.discovery() == null) {
            return new ReuseDecision(ReuseStrategy.NONE, "'framework' is not supported by this dev service");
        }
        if (configured == ReuseStrategy.TESTCONTAINERS && networkEnabled) {
            return new ReuseDecision(ReuseStrategy.NONE,
                    "'testcontainers' doesn't combine with the shared network (" + DevServiceNetworkSupport.NETWORK_ENABLED_PROPERTY + "=true)");
        }
        if (configured == ReuseStrategy.TESTCONTAINERS && !testcontainersReuseSupport.getAsBoolean()) {
            return new ReuseDecision(ReuseStrategy.NONE,
                    "'testcontainers' requires reuse to be enabled in the Testcontainers configuration (testcontainers.reuse.enable=true in ~/.testcontainers.properties or TESTCONTAINERS_REUSE_ENABLE=true)");
        }
        return new ReuseDecision(configured, null);
    }

}
