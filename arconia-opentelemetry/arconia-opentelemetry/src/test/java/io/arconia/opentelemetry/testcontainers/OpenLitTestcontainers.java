package io.arconia.opentelemetry.testcontainers;

import java.time.Duration;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import io.arconia.testcontainers.openlit.OpenLitContainer;

@Testcontainers(disabledWithoutDocker = true, parallel = true)
public class OpenLitTestcontainers {

    @Container
    @ServiceConnection
    public static final OpenLitContainer openLitContainer =
            new OpenLitContainer(DockerImageName.parse(Images.OPENLIT))
                    .withClickHouseImage(DockerImageName.parse(Images.CLICKHOUSE))
                    .withStartupTimeout(Duration.ofMinutes(2));

}
