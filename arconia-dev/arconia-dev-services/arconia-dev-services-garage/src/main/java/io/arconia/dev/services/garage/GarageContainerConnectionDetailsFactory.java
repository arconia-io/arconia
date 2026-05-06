package io.arconia.dev.services.garage;

import java.net.URI;

import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

import io.awspring.cloud.autoconfigure.core.AwsConnectionDetails;

import io.arconia.testcontainers.garage.GarageContainer;

/**
 * {@link ContainerConnectionDetailsFactory} producing {@link AwsConnectionDetails} from a
 * {@link ServiceConnection @ServiceConnection}-annotated {@link GarageContainer}.
 *
 * <p>Mirrors {@code AwsContainerConnectionDetailsFactory} from Spring Cloud AWS, which does
 * the same for LocalStack. Loaded only when Spring Cloud AWS is on the classpath.
 */
class GarageContainerConnectionDetailsFactory
        extends ContainerConnectionDetailsFactory<GarageContainer, AwsConnectionDetails> {

    @Override
    protected AwsConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<GarageContainer> source) {
        return new GarageAwsConnectionDetails(source);
    }

    private static final class GarageAwsConnectionDetails
            extends ContainerConnectionDetails<GarageContainer> implements AwsConnectionDetails {

        GarageAwsConnectionDetails(ContainerConnectionSource<GarageContainer> source) {
            super(source);
        }

        @Override
        public URI getEndpoint() {
            return getContainer().getS3EndpointUri();
        }

        @Override
        public String getRegion() {
            return getContainer().getRegion();
        }

        @Override
        public String getAccessKey() {
            return getContainer().getAccessKey();
        }

        @Override
        public String getSecretKey() {
            return getContainer().getSecretKey();
        }
    }

}
