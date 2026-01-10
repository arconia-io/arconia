package io.arconia.dev.services.oracle;

import org.testcontainers.containers.OracleContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * An {@link OracleContainer} specialized for Arconia Dev Services.
 */
public final class ArconiaOracleXeContainer extends OracleContainer {

    public ArconiaOracleXeContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
    }

}
