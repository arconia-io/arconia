package io.arconia.dev.services.phoenix;

import org.testcontainers.utility.DockerImageName;

import io.arconia.testcontainers.phoenix.PhoenixContainer;

/**
 * A {@link PhoenixContainer} specialized for Arconia Dev Services.
 */
public final class ArconiaPhoenixContainer extends PhoenixContainer {

    public ArconiaPhoenixContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
    }

}
