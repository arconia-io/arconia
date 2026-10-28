package io.arconia.opentelemetry.testcontainers;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import io.arconia.testcontainers.langfuse.LangfuseContainer;

@Testcontainers(disabledWithoutDocker = true, parallel = true)
public class LangfuseTestcontainers {

    @Container
    @ServiceConnection
    public static final LangfuseContainer langfuseContainer = new LangfuseContainer(DockerImageName.parse(Images.LANGFUSE))
            .withWorkerImage(Images.LANGFUSE_WORKER)
            .withPostgresImage(Images.POSTGRES)
            .withClickHouseImage(Images.CLICKHOUSE)
            .withRedisImage(Images.REDIS)
            .withFlociImage(Images.FLOCI);

}
