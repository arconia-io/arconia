package io.arconia.dev.services.floci;

import io.awspring.cloud.autoconfigure.core.AwsConnectionDetails;
import io.awspring.cloud.autoconfigure.s3.S3ClientCustomizer;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import software.amazon.awssdk.awscore.AwsClient;
import software.amazon.awssdk.services.s3.S3Client;

import io.arconia.dev.services.api.provider.DevServiceCategories;
import io.arconia.dev.services.api.provider.DevServiceProvider;
import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.autoconfigure.DevServicesAutoConfiguration;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.floci.FlociDevServicesAutoConfiguration.FlociDevServicesRegistrar;
import io.arconia.dev.services.floci.FlociDevServicesAutoConfiguration.S3Configuration;

/**
 * Auto-configuration for Floci Dev Services.
 */
@AutoConfiguration(after = DevServicesAutoConfiguration.class, before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("floci")
@ConditionalOnClass({AwsConnectionDetails.class, AwsClient.class})
@EnableConfigurationProperties(FlociDevServicesProperties.class)
@Import({FlociDevServicesRegistrar.class, S3Configuration.class})
public final class FlociDevServicesAutoConfiguration {

    @Bean
    DevServiceProvider flociDevServiceProvider() {
        return DevServiceProvider.of("floci", DevServiceCategories.AWS);
    }

    static class FlociDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(FlociDevServicesProperties.CONFIG_PREFIX, FlociDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("floci")
                    .description("Floci Dev Service")
                    .container(container -> container
                            .type(ArconiaFlociContainer.class)
                            .supplier(() -> new ArconiaFlociContainer(properties))
                    ));
        }

    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({S3ClientCustomizer.class, S3Client.class})
    static class S3Configuration {

        @Bean
        S3ClientCustomizer flociS3PathStyleCustomizer() {
            return builder -> builder.forcePathStyle(true);
        }

    }

}
