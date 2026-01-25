package io.arconia.dev.services.oracle;

import org.testcontainers.containers.OracleContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.util.ContainerUtils;

/**
 * An {@link OracleContainer} configured for use with Arconia Dev Services.
 */
final class ArconiaOracleXeContainer extends OracleContainer {

    private static final String COMPATIBLE_IMAGE_NAME = "gvenzl/oracle-xe";

    private final OracleXeDevServicesProperties properties;

    static final int ORACLE_PORT = 1521;

    public ArconiaOracleXeContainer(OracleXeDevServicesProperties properties) {
        super(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        this.properties = properties;
    }

    @Override
    protected void configure() {
        super.configure();
        if (ContainerUtils.isValidPort(properties.getPort())) {
            addFixedExposedPort(properties.getPort(), ORACLE_PORT);
        }
    }

}
