package io.arconia.dev.services.core.registration;

import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

import io.arconia.core.support.Internal;

/**
 * A {@link RootBeanDefinition} specialized for registering {@link ConnectionDetails} beans
 * for discovered shared dev services, so they can be identified and excluded
 * from AOT processing.
 */
@Internal
public class DevServiceConnectionDetailsBeanDefinition extends RootBeanDefinition {

}
