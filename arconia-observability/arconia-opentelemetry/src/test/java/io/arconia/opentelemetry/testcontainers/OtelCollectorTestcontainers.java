package io.arconia.opentelemetry.testcontainers;

import java.time.Duration;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true, parallel = true)
public class OtelCollectorTestcontainers {

    @Container
    @ServiceConnection("otel/opentelemetry-collector")
    public static GenericContainer<?> otelCollectorContainer = new GenericContainer<>(Images.OTEL_COLLECTOR)
            .withExposedPorts(4317, 4318)
            .withStartupTimeout(Duration.ofMinutes(2));

}
