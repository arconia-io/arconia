package io.arconia.dev.services.mongodb;

import java.util.ArrayList;

import com.mongodb.client.MongoClient;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration;
import org.springframework.boot.mongodb.autoconfigure.MongoConnectionDetails;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testcontainers.lifecycle.TestcontainersLifecycleApplicationContextInitializer;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;

import io.arconia.boot.bootstrap.BootstrapMode;
import io.arconia.dev.services.tests.DisabledIfDockerUnavailable;
import io.arconia.dev.services.tests.SslTestCertificates;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the MongoDB Dev Service with SSL enabled: the application connects to it
 * over SSL through Spring Boot's service connection, with the bundle built from the properties.
 */
@DisabledIfDockerUnavailable
class MongoDbDevServicesSslIT {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MongoDbDevServicesAutoConfiguration.class,
                    ServiceConnectionAutoConfiguration.class, MongoAutoConfiguration.class))
            .withInitializer(new TestcontainersLifecycleApplicationContextInitializer());

    @BeforeEach
    @AfterEach
    void resetBootstrapMode() {
        BootstrapMode.clear();
    }

    @Test
    void applicationConnectsOverSsl() {
        contextRunner
                .withPropertyValues(SslTestCertificates.pemProperties("mongodb"))
                .run(context -> {
                    assertThat(context.getBean(MongoConnectionDetails.class).getSslBundle()).isNotNull();
                    MongoClient mongoClient = context.getBean(MongoClient.class);
                    assertThat(mongoClient.listDatabaseNames().into(new ArrayList<>())).contains("admin");
                });
    }

}
