package io.arconia.dev.services.oracle;

import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * An {@link OracleContainer} specialized for Arconia Dev Services.
 */
public final class ArconiaOracleContainer extends OracleContainer {

    public ArconiaOracleContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
    }

}
