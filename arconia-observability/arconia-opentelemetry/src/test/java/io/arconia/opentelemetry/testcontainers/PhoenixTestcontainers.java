package io.arconia.opentelemetry.testcontainers;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true, parallel = true)
public class PhoenixTestcontainers {

    static final int GRPC_PORT = 4317;
    static final int HTTP_PORT = 6006;

    @Container
    @ServiceConnection("phoenix")
    public static final GenericContainer<?> phoenixContainer = new GenericContainer<>(Images.PHOENIX)
            .withExposedPorts(GRPC_PORT, HTTP_PORT);

}
