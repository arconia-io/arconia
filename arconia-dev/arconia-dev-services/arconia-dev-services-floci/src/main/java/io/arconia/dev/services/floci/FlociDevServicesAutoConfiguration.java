package io.arconia.dev.services.floci;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.HostConfig;

import io.awspring.cloud.autoconfigure.core.AwsConnectionDetails;
import io.awspring.cloud.autoconfigure.s3.S3ClientCustomizer;
import io.floci.testcontainers.FlociContainer;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.testcontainers.utility.DockerImageName;

import software.amazon.awssdk.services.s3.S3Client;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.registration.DevServiceCategories;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.floci.FlociDevServicesAutoConfiguration.FlociDevServicesRegistrar;
import io.arconia.dev.services.floci.FlociDevServicesAutoConfiguration.S3Configuration;

/**
 * Auto-configuration for Floci Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("floci")
@EnableConfigurationProperties(FlociDevServicesProperties.class)
@Import({FlociDevServicesRegistrar.class, S3Configuration.class})
public final class FlociDevServicesAutoConfiguration {

    static final String COMPATIBLE_IMAGE_NAME = "floci/floci";

    static final String UI_PATH = "/_floci/ui";

    private static final String DOCKER_SOCKET_PATH = "/var/run/docker.sock";

    private static final String SELINUX_LABEL_DISABLED = "label=disable";

    static FlociContainer createContainer(FlociDevServicesProperties properties) {
        return new FlociContainer(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME))
                .withCreateContainerCmdModifier(FlociDevServicesAutoConfiguration::disableSelinuxLabelsWithDockerSocket);
    }

    /**
     * Disable the SELinux label separation of the container when the socket of the container
     * runtime is mounted into it. On a host enforcing SELinux, such as a Podman machine, a
     * confined container is denied access to the socket, and Floci then fails to start the
     * containers it runs its console and services like Lambda in. The option has no effect on
     * a host without SELinux.
     * <p>
     * To be removed when the Floci module does it:
     * https://github.com/floci-io/testcontainers-floci/issues/543
     */
    static void disableSelinuxLabelsWithDockerSocket(CreateContainerCmd command) {
        HostConfig hostConfig = command.getHostConfig();
        if (hostConfig == null || hostConfig.getBinds() == null) {
            return;
        }
        boolean dockerSocketMounted = Arrays.stream(hostConfig.getBinds())
                .anyMatch(bind -> DOCKER_SOCKET_PATH.equals(bind.getVolume().getPath()));
        if (!dockerSocketMounted) {
            return;
        }
        List<String> securityOptions = new ArrayList<>();
        if (hostConfig.getSecurityOpts() != null) {
            securityOptions.addAll(hostConfig.getSecurityOpts());
        }
        if (!securityOptions.contains(SELINUX_LABEL_DISABLED)) {
            securityOptions.add(SELINUX_LABEL_DISABLED);
        }
        hostConfig.withSecurityOpts(securityOptions);
    }

    static class FlociDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(FlociDevServicesProperties.CONFIG_PREFIX, FlociDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("floci")
                    .title("Floci")
                    .description("Free and open-source local AWS emulator for developing and testing cloud applications.")
                    .icon("cloud-cog")
                    .category(DevServiceCategories.AWS)
                    .properties(properties)
                    .container(FlociContainer.class, () -> createContainer(properties))
                    .port(FlociContainer.PORT)
                    .discovery(AwsConnectionDetails.class,
                            container -> new FlociDiscoveredConnectionDetails(container, properties))
                    // Floci starts its web console in a container of its own on the first request to this path.
                    .link("Floci UI", FlociContainer.PORT, UI_PATH));
        }

    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(S3Client.class)
    static class S3Configuration {

        @Bean
        S3ClientCustomizer flociS3PathStyleCustomizer() {
            return builder -> builder.forcePathStyle(true);
        }

    }

}
