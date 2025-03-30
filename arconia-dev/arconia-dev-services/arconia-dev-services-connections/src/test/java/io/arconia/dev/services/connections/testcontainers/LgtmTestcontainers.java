package io.arconia.dev.services.connections.testcontainers;

import java.time.Duration;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.grafana.LgtmStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true, parallel = true)
public class LgtmTestcontainers {

    @Container
    @ServiceConnection
    public static LgtmStackContainer lgtmContainer = new LgtmStackContainer(Images.LGTM)
            .withStartupTimeout(Duration.ofMinutes(2));

}
