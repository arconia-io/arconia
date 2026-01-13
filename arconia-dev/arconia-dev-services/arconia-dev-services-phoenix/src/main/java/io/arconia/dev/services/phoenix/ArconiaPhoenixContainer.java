package io.arconia.dev.services.phoenix;

import org.testcontainers.utility.DockerImageName;

import io.arconia.testcontainers.phoenix.PhoenixContainer;

/**
 * A {@link PhoenixContainer} specialized for Arconia Dev Services.
 */
public final class ArconiaPhoenixContainer extends PhoenixContainer {

    private final PhoenixDevServicesProperties properties;

    /**
     * HBase Master web UI port.
     */
    private static final int HBASE_MASTER_WEB_PORT = 16010;

    /**
     * ZooKeeper client connections port.
     */
    private static final int ZOOKEEPER_PORT = 2181;

    public ArconiaPhoenixContainer(DockerImageName dockerImageName, PhoenixDevServicesProperties properties) {
        super(dockerImageName);
        this.properties = properties;
    }

    @Override
    protected void configure() {
        super.configure();
        if (properties.getPort() > 0) {
            addFixedExposedPort(properties.getPort(), ZOOKEEPER_PORT);
        }
    }
}
