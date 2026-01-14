package io.arconia.dev.services.oracle;

import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * An {@link OracleContainer} specialized for Arconia Dev Services.
 */
public final class ArconiaOracleContainer extends OracleContainer {

    private final OracleDevServicesProperties properties;

    /**
     * Oracle Net Listener port (JDBC/SQL*Net).
     */
    protected static final int ORACLE_PORT = 1521;

    public ArconiaOracleContainer(DockerImageName dockerImageName, OracleDevServicesProperties properties) {
        super(dockerImageName);
        this.properties = properties;
    }

    @Override
    protected void configure() {
        super.configure();
        if (properties.getPort() > 0) {
            addFixedExposedPort(properties.getPort(), ORACLE_PORT);
        }
    }
}
