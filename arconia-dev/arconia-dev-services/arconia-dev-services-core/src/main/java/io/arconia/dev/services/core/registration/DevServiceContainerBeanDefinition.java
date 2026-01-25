package io.arconia.dev.services.core.registration;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.testcontainers.beans.TestcontainerBeanDefinition;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.annotation.MergedAnnotations;

import io.arconia.core.support.Internal;

/**
 * A {@link GenericBeanDefinition} specialized for registering Testcontainers-based beans.
 * <p>
 * It ensures that the Testcontainers's container beans are properly configured
 * and managed by Spring Boot via the {@link ServiceConnection} mechanism.
 */
@Internal
public class DevServiceContainerBeanDefinition extends GenericBeanDefinition implements TestcontainerBeanDefinition {

    private MergedAnnotations mergedAnnotations = MergedAnnotations.from();

    @Override
    public @Nullable String getContainerImageName() {
        return null;
    }

    @Override
    public MergedAnnotations getAnnotations() {
        return mergedAnnotations;
    }

    public void setAnnotations(MergedAnnotations annotations) {
        this.mergedAnnotations = annotations;
    }

}
