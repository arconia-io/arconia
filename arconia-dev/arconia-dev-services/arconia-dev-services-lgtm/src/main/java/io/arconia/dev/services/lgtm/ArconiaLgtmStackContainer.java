package io.arconia.dev.services.lgtm;

import org.testcontainers.grafana.LgtmStackContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * A {@link LgtmStackContainer} specialized for Arconia Dev Services.
 */
public final class ArconiaLgtmStackContainer extends LgtmStackContainer {

    public ArconiaLgtmStackContainer(DockerImageName image) {
        super(image);
    }

}
