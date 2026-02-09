package io.arconia.dev.services.oracle;

import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.container.ContainerConfigurer;
import io.arconia.dev.services.core.util.ContainerUtils;

/**
 * An {@link OracleContainer} configured for use with Arconia Dev Services.
 */
final class ArconiaOracleContainer extends OracleContainer {

    private final OracleDevServicesProperties properties;

    static final String COMPATIBLE_IMAGE_NAME = "gvenzl/oracle-free";

    static final int ORACLE_PORT = 1521;

    public ArconiaOracleContainer(OracleDevServicesProperties properties) {
        super(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        this.properties = properties;

        ContainerConfigurer.base(this, properties);
        ContainerConfigurer.jdbc(this, properties);
    }

    @Override
    protected void configure() {
        super.configure();
        if (ContainerUtils.isValidPort(properties.getPort())) {
            addFixedExposedPort(properties.getPort(), ORACLE_PORT);
        }
    }

}
