package io.arconia.dev.services.pulsar;

import org.testcontainers.pulsar.PulsarContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * A {@link PulsarContainer} specialized for Arconia Dev Services.
 */
public final class ArconiaPulsarContainer extends PulsarContainer {

    public ArconiaPulsarContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
    }

}
