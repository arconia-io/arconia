package io.arconia.testcontainers.garage;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.utility.DockerImageName;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link GarageContainer}.
 */
@EnabledIfDockerAvailable
class GarageContainerIT {

    private static final DockerImageName IMAGE = DockerImageName.parse("dxflrs/garage:v2.3.0");

    @Test
    void containerStartsAndStopsSuccessfully() {
        try (var container = new GarageContainer(IMAGE)) {
            container.start();
            assertThat(container.getCurrentContainerInfo().getState().getStatus())
                    .isEqualTo("running");
            container.stop();
        }
    }

    @Test
    void s3ApiIsUsableAfterBootstrap() {
        try (var container = new GarageContainer(IMAGE)) {
            container.start();

            try (var s3 = buildClient(container)) {
                var key = "hello.txt";
                var body = "hello garage";

                s3.putObject(PutObjectRequest.builder()
                                .bucket(container.getDefaultBucket())
                                .key(key)
                                .build(),
                        RequestBody.fromString(body));

                var fetched = s3.getObjectAsBytes(GetObjectRequest.builder()
                        .bucket(container.getDefaultBucket())
                        .key(key)
                        .build());

                assertThat(fetched.asString(StandardCharsets.UTF_8)).isEqualTo(body);
            }
        }
    }

    private static S3Client buildClient(GarageContainer container) {
        return S3Client.builder()
                .endpointOverride(URI.create(container.getS3Endpoint()))
                .region(Region.of(container.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(container.getAccessKey(), container.getSecretKey())))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .chunkedEncodingEnabled(false)
                        .build())
                .build();
    }

}
