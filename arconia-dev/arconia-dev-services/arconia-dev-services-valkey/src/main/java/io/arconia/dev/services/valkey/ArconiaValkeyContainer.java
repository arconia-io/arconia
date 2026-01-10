package io.arconia.dev.services.valkey;

import org.testcontainers.utility.DockerImageName;

import io.arconia.testcontainers.valkey.ValkeyContainer;

/**
 * A {@link ValkeyContainer} specialized for Arconia Dev Services.
 */
public final class ArconiaValkeyContainer extends ValkeyContainer {

    public ArconiaValkeyContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
    }

}
