package io.arconia.dev.services.redis;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.PemSslBundleProperties;
import org.springframework.boot.autoconfigure.ssl.PropertiesSslBundle;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.DataRedisConnectionDetails;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testcontainers.lifecycle.TestcontainersLifecycleApplicationContextInitializer;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.data.redis.core.StringRedisTemplate;

import io.arconia.boot.bootstrap.BootstrapMode;
import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.tests.DevServiceContainers;
import io.arconia.dev.services.tests.DisabledIfDockerUnavailable;
import io.arconia.dev.services.tests.SslTestCertificates;
import io.arconia.testcontainers.redis.RedisContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Redis Dev Service with SSL enabled: the application connects to it
 * over SSL through Spring Boot's service connection, with the bundle built from the properties.
 */
@DisabledIfDockerUnavailable
class RedisDevServicesSslIT {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RedisDevServicesAutoConfiguration.class,
                    ServiceConnectionAutoConfiguration.class, DataRedisAutoConfiguration.class, SslAutoConfiguration.class))
            .withInitializer(new TestcontainersLifecycleApplicationContextInitializer());

    @BeforeEach
    @AfterEach
    void resetBootstrapMode() {
        BootstrapMode.clear();
    }

    @Test
    void applicationConnectsOverSslWithPemBundle() {
        contextRunner
                .withPropertyValues(SslTestCertificates.pemProperties("redis"))
                .run(context -> {
                    assertThat(context.getBean(DataRedisConnectionDetails.class).getSslBundle()).isNotNull();
                    StringRedisTemplate redisTemplate = context.getBean(StringRedisTemplate.class);
                    redisTemplate.opsForValue().set("greeting", "hello");
                    assertThat(redisTemplate.opsForValue().get("greeting")).isEqualTo("hello");

                    assertThat(context.getBean(DevServiceRegistration.class).getSslBundleName()).isEqualTo("dev-services-redis");
                    assertThat(context.getBean(SslBundles.class).getBundleNames()).contains("dev-services-redis");
                });
    }

    @Test
    void applicationConnectsToDiscoveredContainerOverSsl() {
        // A container started by another application with the same SSL configuration.
        var properties = new RedisDevServicesProperties();
        properties.getSsl().setEnabled(true);
        properties.getSsl().getPem().getKeystore().setCertificate(SslTestCertificates.CERTIFICATE);
        properties.getSsl().getPem().getKeystore().setPrivateKey(SslTestCertificates.PRIVATE_KEY);
        RedisContainer peer = DevServiceContainers.discoverable(RedisDevServicesAutoConfiguration.createContainer(properties), properties, "redis", "another-application");
        RedisDevServicesAutoConfiguration.configureSsl(peer, sslBundle());
        // Constructing a dev service container detects and caches the bootstrap mode. Clear it, or
        // the application under test never sees the dev mode it asks for.
        BootstrapMode.clear();
        try (peer) {
            peer.start();

            contextRunner
                    .withSystemProperties("arconia.bootstrap.mode=dev")
                    .withPropertyValues(SslTestCertificates.pemProperties("redis"))
                    .withPropertyValues("arconia.dev.services.redis.reuse-strategy=framework")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(RedisContainer.class);
                        assertThat(context.getBean(DevServiceRegistration.class).getOrigin()).isEqualTo(DevServiceRegistration.Origin.DISCOVERED);
                        assertThat(context.getBean(DataRedisConnectionDetails.class).getSslBundle()).isNotNull();
                        StringRedisTemplate redisTemplate = context.getBean(StringRedisTemplate.class);
                        redisTemplate.opsForValue().set("greeting", "hello");
                        assertThat(redisTemplate.opsForValue().get("greeting")).isEqualTo("hello");
                    });
        }
    }

    @Test
    void applicationConnectsOverSslWithJksBundle() {
        contextRunner
                .withPropertyValues(SslTestCertificates.jksProperties("redis"))
                .run(context -> {
                    StringRedisTemplate redisTemplate = context.getBean(StringRedisTemplate.class);
                    redisTemplate.opsForValue().set("greeting", "hello");
                    assertThat(redisTemplate.opsForValue().get("greeting")).isEqualTo("hello");
                });
    }

    private static SslBundle sslBundle() {
        PemSslBundleProperties properties = new PemSslBundleProperties();
        properties.getKeystore().setCertificate(SslTestCertificates.CERTIFICATE);
        properties.getKeystore().setPrivateKey(SslTestCertificates.PRIVATE_KEY);
        return PropertiesSslBundle.get(properties);
    }

}
