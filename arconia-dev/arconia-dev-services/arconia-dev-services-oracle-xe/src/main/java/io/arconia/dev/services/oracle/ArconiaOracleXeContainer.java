package io.arconia.dev.services.oracle;

import org.testcontainers.containers.OracleContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * An {@link OracleContainer} specialized for Arconia Dev Services.
 */
public final class ArconiaOracleXeContainer extends OracleContainer {

    private final OracleXeDevServicesProperties properties;

    /**
     * Oracle Net Listener port (JDBC/SQL*Net).
     */
    protected static final int ORACLE_XE_PORT = 1521;

    public ArconiaOracleXeContainer(DockerImageName dockerImageName, OracleXeDevServicesProperties properties) {
        super(dockerImageName);
        this.properties = properties;
    }

    @Override
    protected void configure() {
        super.configure();
        if (properties.getPort() > 0) {
            addFixedExposedPort(properties.getPort(), ORACLE_XE_PORT);
        }
    }
}
