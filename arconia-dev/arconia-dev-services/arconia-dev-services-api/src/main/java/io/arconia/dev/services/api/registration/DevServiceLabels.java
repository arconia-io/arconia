package io.arconia.dev.services.api.registration;

import java.util.UUID;

import io.arconia.core.support.Incubating;

/**
 * Labels applied to every container started by Arconia Dev Services,
 * making dev service containers identifiable across applications
 * (e.g. {@code docker ps --filter label=io.arconia.dev-service.name}).
 * <p>
 * Dev services using the {@code framework} reuse strategy rely on these labels to
 * discover running containers started by other applications.
 */
@Incubating
public final class DevServiceLabels {

    /**
     * The logical name of the dev service the container belongs to (e.g. {@code postgresql}).
     */
    public static final String NAME = "io.arconia.dev-service.name";

    /**
     * Whether the container can be discovered by other applications ({@code true}). Applied
     * only to the containers of dev services using the {@code framework} reuse strategy.
     */
    public static final String DISCOVERABLE = "io.arconia.dev-service.discoverable";

    /**
     * Identifier of the application instance that started the container, used to prevent an
     * application from discovering its own containers. Applied only to discoverable containers.
     */
    public static final String OWNER = "io.arconia.dev-service.owner";

    private static final String OWNER_ID = UUID.randomUUID().toString();

    private DevServiceLabels() {}

    /**
     * The identifier of this application instance, used as the value of the {@link #OWNER} label.
     * The identifier is unique per JVM and stable across Spring Boot DevTools restarts.
     */
    public static String ownerId() {
        return OWNER_ID;
    }

}
