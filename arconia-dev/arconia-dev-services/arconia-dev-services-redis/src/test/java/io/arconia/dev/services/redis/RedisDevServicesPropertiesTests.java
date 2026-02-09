package io.arconia.dev.services.redis;

import io.arconia.dev.services.tests.BaseDevServicesPropertiesTests;

/**
 * Unit tests for {@link RedisDevServicesProperties}.
 */
class RedisDevServicesPropertiesTests extends BaseDevServicesPropertiesTests<RedisDevServicesProperties> {

    @Override
    protected RedisDevServicesProperties createProperties() {
        return new RedisDevServicesProperties();
    }

    @Override
    protected DefaultValues getExpectedDefaults() {
        return DefaultValues.builder()
                .imageName(ArconiaRedisContainer.COMPATIBLE_IMAGE_NAME)
                .build();
    }

}
