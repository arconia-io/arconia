package io.arconia.dev.services.core.autoconfigure;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

import io.arconia.core.bootstrap.autoconfigure.ConditionalOnDevMode;
import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.core.devui.DevServicesDevUiPage;
import io.arconia.dev.ui.api.page.CardsPage;
import io.arconia.dev.ui.api.page.DevUiPage;

/**
 * Auto-configuration for the Dev Services page of the Dev UI.
 */
@AutoConfiguration
@ConditionalOnDevMode
@ConditionalOnDevServicesEnabled
@ConditionalOnClass(DevUiPage.class)
public final class DevServicesDevUiAutoConfiguration {

    @Bean
    CardsPage devServicesDevUiPage(ObjectProvider<DevServiceRegistration> registrations) {
        return DevServicesDevUiPage.create(registrations.orderedStream().toList());
    }

}
