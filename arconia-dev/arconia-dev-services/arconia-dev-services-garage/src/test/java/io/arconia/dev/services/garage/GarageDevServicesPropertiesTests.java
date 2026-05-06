package io.arconia.dev.services.garage;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.tests.BaseDevServicesPropertiesTests;
import io.arconia.testcontainers.garage.GarageContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link GarageDevServicesProperties}.
 */
class GarageDevServicesPropertiesTests extends BaseDevServicesPropertiesTests<GarageDevServicesProperties> {

    @Override
    protected GarageDevServicesProperties createProperties() {
        return new GarageDevServicesProperties();
    }

    @Override
    protected DefaultValues getExpectedDefaults() {
        return DefaultValues.builder()
                .imageName(ArconiaGarageContainer.COMPATIBLE_IMAGE_NAME)
                .startupTimeout(Duration.ofSeconds(60))
                .build();
    }

    @Test
    void bucketNameDefaultsToContainerDefault() {
        var properties = new GarageDevServicesProperties();
        assertThat(properties.getBucketName()).isEqualTo(GarageContainer.DEFAULT_BUCKET);
    }

    @Test
    void bucketNameCanBeChanged() {
        var properties = new GarageDevServicesProperties();
        properties.setBucketName("my-bucket");
        assertThat(properties.getBucketName()).isEqualTo("my-bucket");
    }

}
