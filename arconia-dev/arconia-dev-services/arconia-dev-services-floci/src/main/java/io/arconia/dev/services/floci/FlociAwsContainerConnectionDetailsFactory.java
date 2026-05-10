package io.arconia.dev.services.floci;

import java.net.URI;

import io.awspring.cloud.autoconfigure.core.AwsConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;

/**
 * {@link ContainerConnectionDetailsFactory} that produces {@link AwsConnectionDetails}
 * from an {@link ArconiaFlociContainer}.
 */
class FlociAwsContainerConnectionDetailsFactory extends ContainerConnectionDetailsFactory<ArconiaFlociContainer, AwsConnectionDetails> {

    FlociAwsContainerConnectionDetailsFactory() {}

    @Override
    protected AwsConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<ArconiaFlociContainer> source) {
        return new FlociAwsContainerConnectionDetails(source);
    }

    private static final class FlociAwsContainerConnectionDetails extends ContainerConnectionDetails<ArconiaFlociContainer> implements AwsConnectionDetails {

        private FlociAwsContainerConnectionDetails(ContainerConnectionSource<ArconiaFlociContainer> source) {
            super(source);
        }

        @Override
        public URI getEndpoint() {
            return URI.create(getContainer().getEndpoint());
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
