package io.arconia.dev.services.artemis;

import org.testcontainers.activemq.ArtemisContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * A {@link ArtemisContainer} specialized for Arconia Dev Services.
 */
public final class ArconiaArtemisContainer extends ArtemisContainer {

    public ArconiaArtemisContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
    }

}
