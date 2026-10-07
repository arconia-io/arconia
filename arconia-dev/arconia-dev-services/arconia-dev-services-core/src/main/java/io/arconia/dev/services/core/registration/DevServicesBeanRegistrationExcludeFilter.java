package io.arconia.dev.services.core.registration;

import org.springframework.beans.factory.aot.BeanRegistrationExcludeFilter;
import org.springframework.beans.factory.support.RegisteredBean;

/**
 * Excludes dev service beans from AOT processing. They are created by instance suppliers and
 * decided at startup from the state of the container runtime, so no code can be generated for
 * them ahead of time, the same way Spring Boot excludes its own service connection beans.
 * Dev services are therefore not available in AOT-processed applications and tests.
 */
class DevServicesBeanRegistrationExcludeFilter implements BeanRegistrationExcludeFilter {

    @Override
    public boolean isExcludedFromAotProcessing(RegisteredBean registeredBean) {
        return registeredBean.getMergedBeanDefinition().getAttribute(DevServicesRegistry.AOT_EXCLUDED_ATTRIBUTE) != null;
    }

}
