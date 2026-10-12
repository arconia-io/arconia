package io.arconia.dev.services.redis;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.redis.autoconfigure.DataRedisConnectionDetails;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.autoconfigure.DevServicesAutoConfiguration;
import io.arconia.dev.services.core.registration.DevServiceCategories;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.core.ssl.SslBundleFiles;
import io.arconia.dev.services.redis.RedisDevServicesAutoConfiguration.RedisDevServicesRegistrar;
import io.arconia.testcontainers.redis.RedisContainer;

/**
 * Auto-configuration for Redis Dev Services.
 */
@AutoConfiguration(after = DevServicesAutoConfiguration.class, before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("redis")
@EnableConfigurationProperties(RedisDevServicesProperties.class)
@Import(RedisDevServicesRegistrar.class)
public final class RedisDevServicesAutoConfiguration {

    static final String COMPATIBLE_IMAGE_NAME = "redis";

    /**
     * Where the SSL files are copied in the container.
     */
    static final String SSL_DIRECTORY = "/arconia/ssl/";

    static RedisContainer createContainer(RedisDevServicesProperties properties) {
        return new RedisContainer(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
    }

    /**
     * Configure Redis to serve its port over SSL only, with the certificate and key of the
     * bundle. Redis requires a certificate authority file whenever SSL is enabled, even when
     * clients are not asked for a certificate.
     */
    static void configureSsl(RedisContainer container, SslBundle bundle) {
        container.withCopyToContainer(SslBundleFiles.certificateChain(bundle), SSL_DIRECTORY + "server.crt");
        container.withCopyToContainer(SslBundleFiles.privateKey(bundle), SSL_DIRECTORY + "server.key");
        container.withCopyToContainer(SslBundleFiles.certificateAuthorities(bundle), SSL_DIRECTORY + "ca.crt");
        String[] command = container.getCommandParts();
        List<String> parts = new ArrayList<>((command != null && command.length > 0) ? List.of(command) : List.of("redis-server"));
        parts.addAll(List.of(
                "--tls-port", String.valueOf(RedisContainer.REDIS_PORT),
                "--port", "0",
                "--tls-cert-file", SSL_DIRECTORY + "server.crt",
                "--tls-key-file", SSL_DIRECTORY + "server.key",
                "--tls-ca-cert-file", SSL_DIRECTORY + "ca.crt",
                "--tls-auth-clients", "no"));
        container.withCommand(parts.toArray(new String[0]));
    }

    static class RedisDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(RedisDevServicesProperties.CONFIG_PREFIX, RedisDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("redis")
                    .description("Redis Dev Service")
                    .category(DevServiceCategories.REDIS)
                    .properties(properties)
                    .container(RedisContainer.class, () -> createContainer(properties))
                    .port(RedisContainer.REDIS_PORT)
                    .serviceConnectionName(COMPATIBLE_IMAGE_NAME)
                    .ssl(RedisDevServicesAutoConfiguration::configureSsl)
                    .discovery(DataRedisConnectionDetails.class, RedisDiscoveredConnectionDetails::new));
        }

    }

}
