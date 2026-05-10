package io.arconia.dev.services.floci;

import io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.s3.S3AutoConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import software.amazon.awssdk.services.s3.S3Client;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Floci Dev Service with Spring Cloud AWS S3.
 */
@EnabledIfDockerAvailable
class FlociS3DevServiceIT {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(RestartScope.class))
            .withConfiguration(AutoConfigurations.of(
                    FlociDevServicesAutoConfiguration.class,
                    ServiceConnectionAutoConfiguration.class,
                    CredentialsProviderAutoConfiguration.class,
                    RegionProviderAutoConfiguration.class,
                    AwsAutoConfiguration.class,
                    S3AutoConfiguration.class
            ));

    @Test
    void shouldCreateBucket() {
        contextRunner.run(context -> {
            var s3Client = context.getBean(S3Client.class);
            s3Client.createBucket(b -> b.bucket("pandora-bucket"));
            var buckets = s3Client.listBuckets().buckets();
            assertThat(buckets).anyMatch(b -> b.name().equals("pandora-bucket"));
        });
    }

}
