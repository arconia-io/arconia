package io.arconia.dev.services.floci;

import java.net.URI;

import io.awspring.cloud.autoconfigure.core.AwsConnectionDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link FlociAwsContainerConnectionDetailsFactory}.
 */
@SpringJUnitConfig
@Testcontainers(disabledWithoutDocker = true)
class FlociAwsContainerConnectionDetailsFactoryTests {

    @Container
    @ServiceConnection
    static ArconiaFlociContainer flociContainer = new ArconiaFlociContainer(new FlociDevServicesProperties());

    @Autowired
    AwsConnectionDetails connectionDetails;

    @Test
    void shouldProvideEndpoint() {
        assertThat(connectionDetails.getEndpoint()).isEqualTo(URI.create(flociContainer.getEndpoint()));
    }

    @Test
    void shouldProvideRegion() {
        assertThat(connectionDetails.getRegion()).isEqualTo(flociContainer.getRegion());
    }

    @Test
    void shouldProvideCredentials() {
        assertThat(connectionDetails.getAccessKey()).isEqualTo(flociContainer.getAccessKey());
        assertThat(connectionDetails.getSecretKey()).isEqualTo(flociContainer.getSecretKey());
    }

    @Configuration(proxyBeanMethods = false)
    static class TestConfiguration {}

}
