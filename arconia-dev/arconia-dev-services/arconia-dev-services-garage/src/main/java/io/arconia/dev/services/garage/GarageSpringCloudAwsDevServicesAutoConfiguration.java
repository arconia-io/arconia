package io.arconia.dev.services.garage;

import java.util.Map;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.garage.GarageSpringCloudAwsDevServicesAutoConfiguration.GarageSpringCloudAwsDefaultsRegistrar;

/**
 * Auto-configuration that contributes Spring Cloud AWS S3 defaults required by Garage —
 * specifically path-style addressing, which is mandatory for S3-compatible stores like Garage
 * but is not part of {@code AwsConnectionDetails}.
 *
 * <p>The endpoint, region, and credentials are provided through the
 * {@link GarageContainerConnectionDetailsFactory} via {@code AwsConnectionDetails}.
 */
@AutoConfiguration(after = GarageDevServicesAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("garage")
@ConditionalOnClass(name = "io.awspring.cloud.autoconfigure.s3.properties.S3Properties")
@Import(GarageSpringCloudAwsDefaultsRegistrar.class)
public final class GarageSpringCloudAwsDevServicesAutoConfiguration {

    private GarageSpringCloudAwsDevServicesAutoConfiguration() {}

    static class GarageSpringCloudAwsDefaultsRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            setDefaultProperties(Map.of(
                    "spring.cloud.aws.s3.path-style-access-enabled", "true"
            ));
        }

    }

}
