package io.arconia.opentelemetry.testcontainers;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import io.arconia.testcontainers.phoenix.PhoenixContainer;

@Testcontainers(disabledWithoutDocker = true, parallel = true)
public class PhoenixTestcontainers {

    @Container
    @ServiceConnection
    public static final PhoenixContainer phoenixContainer
            = new PhoenixContainer(DockerImageName.parse(Images.PHOENIX));

}
