package io.arconia.dev.services.core.container;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.github.dockerjava.api.DockerClient;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Network;

import io.arconia.boot.bootstrap.BootstrapMode;
import io.arconia.core.support.Incubating;

/**
 * Factory resolving the {@link Network} that dev service containers join.
 * <p>
 * The backing network is mode-aware:
 * <ul>
 *     <li>in test mode, or in dev mode with no network name configured, the isolated
 *     per-application {@link Network#SHARED} is used (reaped by Testcontainers/Ryuk on JVM exit);</li>
 *     <li>in dev mode with a network name configured, a stable, named OCI network is
 *     found or created, so it can be shared across applications running simultaneously.</li>
 * </ul>
 * When this process creates a named network, a best-effort JVM shutdown hook removes it;
 * container runtimes refuse to remove a network that still has attached containers, which is the
 * natural safety net when other applications (or reused containers) are still using it.
 */
@Incubating
public final class DevServicesNetworkFactory {

    private static final Logger logger = LoggerFactory.getLogger(DevServicesNetworkFactory.class);

    /**
     * Reserved Docker network names that must never be created; using one of them
     * as a network name falls back to the isolated per-application network.
     */
    private static final Set<String> RESERVED_NETWORK_NAMES = Set.of("bridge", "container", "default", "host", "nat", "none");

    /**
     * Network ids for which a removal shutdown hook has already been registered in this JVM,
     * so a hook is never registered more than once per network (e.g. across DevTools restarts).
     */
    private static final Set<String> REMOVAL_HOOKS_REGISTERED = ConcurrentHashMap.newKeySet();

    private DevServicesNetworkFactory() {}

    /**
     * Resolve the network dev service containers join for the given configured name.
     */
    public static Network resolve(@Nullable String name) {
        if (isDevMode() && StringUtils.hasText(name)) {
            return namedNetwork(name.trim());
        }
        return Network.SHARED;
    }

    private static Network namedNetwork(String name) {
        if (RESERVED_NETWORK_NAMES.contains(name.toLowerCase(Locale.ROOT)) || name.toLowerCase(Locale.ROOT).startsWith("container:")) {
            logger.warn("'{}' is a reserved OCI network name and cannot be used as a dev services network; using an isolated network instead", name);
            return Network.SHARED;
        }

        try {
            DockerClient client = DockerClientFactory.lazyClient();
            String existingId = findNetworkIdByName(client, name);
            if (existingId != null) {
                // Found an existing network (possibly created by another application): never remove it.
                return new ExistingDockerNetwork(existingId);
            }

            String createdId;
            try {
                createdId = client.createNetworkCmd().withName(name).withDriver("bridge").exec().getId();
            } catch (Exception ex) {
                // Another application may have created the network concurrently; try to find it again.
                String racedId = findNetworkIdByName(client, name);
                if (racedId != null) {
                    return new ExistingDockerNetwork(racedId);
                }
                throw ex;
            }

            // This process created the network: remove it on shutdown on a best-effort basis.
            registerRemovalShutdownHook(createdId, name);
            return new ExistingDockerNetwork(createdId);
        } catch (Exception ex) {
            logger.warn("Failed to resolve the named dev services network '{}'; falling back to an isolated network. "
                    + "Containers will NOT be reachable from other applications on network '{}'", name, name, ex);
            return Network.SHARED;
        }
    }

    @Nullable
    private static String findNetworkIdByName(DockerClient client, String name) {
        List<com.github.dockerjava.api.model.Network> matches = client.listNetworksCmd()
                .withNameFilter(name)
                .exec()
                .stream()
                .filter(network -> name.equals(network.getName()))
                .toList();
        return matches.isEmpty() ? null : matches.getFirst().getId();
    }

    private static void registerRemovalShutdownHook(String networkId, String name) {
        if (!REMOVAL_HOOKS_REGISTERED.add(networkId)) {
            // A removal hook was already registered for this network in this JVM.
            return;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                DockerClientFactory.lazyClient().removeNetworkCmd(networkId).exec();
            } catch (Exception ex) {
                // Removal fails while containers are still attached (e.g. another application or a
                // reused container is using it); that is expected and safe to ignore.
                logger.debug("Could not remove the dev services network '{}' ({}) on shutdown", name, networkId, ex);
            }
        }, "arconia-dev-services-network-cleanup"));
    }

    private static boolean isDevMode() {
        return BootstrapMode.DEV.equals(BootstrapMode.detect());
    }

    /**
     * A {@link Network} backed by an existing OCI network id, so containers join it
     * without Testcontainers creating or managing (removing) the network itself.
     */
    private record ExistingDockerNetwork(String id) implements Network {

        @Override
        public String getId() {
            return id;
        }

        @Override
        public void close() {
            // The network lifecycle is managed by the factory's shutdown hook, not by Testcontainers.
        }

    }

}
