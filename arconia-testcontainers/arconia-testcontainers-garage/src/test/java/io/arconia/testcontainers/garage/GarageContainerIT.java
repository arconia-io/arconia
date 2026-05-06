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

    private static final DockerImageName IMAGE = DockerImageName.parse(GarageContainer.DEFAULT_IMAGE);

    @Test
    void containerStartsAndStopsSuccessfully() {
        try (var container = new GarageContainer(IMAGE)) {
            container.start();
            assertThat(container.getS3Endpoint()).startsWith("http://");
            assertThat(container.getS3Port()).isPositive();
        }
    }

    @Test
    void customCredentialsAcceptedByS3Api() {
        var customKey = "GK11111111111111111111111a";
        var customSecret = "1111111111111111111111111111111111111111111111111111111111111111";

        try (var container = new GarageContainer(IMAGE).withCredentials(customKey, customSecret)) {
            container.start();
            assertThat(container.getAccessKey()).isEqualTo(customKey);
            assertThat(container.getSecretKey()).isEqualTo(customSecret);

            try (var s3 = buildClient(container)) {
                var key = "custom-creds.txt";
                var body = "custom credentials work";
                s3.putObject(PutObjectRequest.builder().bucket(container.getDefaultBucket()).key(key).build(),
                        RequestBody.fromString(body));
                var fetched = s3.getObjectAsBytes(GetObjectRequest.builder()
                        .bucket(container.getDefaultBucket()).key(key).build());
                assertThat(fetched.asString(StandardCharsets.UTF_8)).isEqualTo(body);
            }
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
