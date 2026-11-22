package io.arconia.dev.services.core.autoconfigure;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.arconia.dev.services.api.registration.ContainerInfo;
import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.ui.api.page.Card;
import io.arconia.dev.ui.api.page.CardsPage;
import io.arconia.dev.ui.api.page.DevUiPage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevServicesDevUiAutoConfiguration}.
 */
class DevServicesDevUiAutoConfigurationTests {

    private static final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DevServicesDevUiAutoConfiguration.class));

    @Test
    void pageIsAvailableWhenDevModeIsEnabled() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .run(context -> assertThat(context).hasSingleBean(CardsPage.class));
    }

    @Test
    void pageIsNotAvailableWhenTestModeIsEnabled() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=test")
                .run(context -> assertThat(context).doesNotHaveBean(CardsPage.class));
    }

    @Test
    void pageIsNotAvailableWhenProdModeIsEnabled() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=prod")
                .run(context -> assertThat(context).doesNotHaveBean(CardsPage.class));
    }

    @Test
    void pageIsNotAvailableWhenDevServicesAreDisabled() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .withPropertyValues("arconia.dev.services.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(CardsPage.class));
    }

    @Test
    void pageIsNotAvailableWhenTheDevUiIsNotOnTheClassPath() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .withClassLoader(new FilteredClassLoader(DevUiPage.class))
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean("devServicesDevUiPage"));
    }

    @Test
    void pageShowsTheDevServicesOfTheApplication() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .withUserConfiguration(RegistrationsConfiguration.class)
                .run(context -> assertThat(context.getBean(CardsPage.class).getCards().get())
                        .extracting(Card::getTitle)
                        .containsExactly("PostgreSQL Database", "Docling Serve"));
    }

    @Configuration(proxyBeanMethods = false)
    static class RegistrationsConfiguration {

        @Bean
        DevServiceRegistration postgresqlRegistration() {
            return registration("postgresql", "PostgreSQL Database", "postgres");
        }

        @Bean
        DevServiceRegistration doclingRegistration() {
            return registration("docling", "Docling Serve", "docling");
        }

        private static DevServiceRegistration registration(String name, String description, String imageName) {
            return DevServiceRegistration.builder()
                    .name(name)
                    .title(description)
                    .origin(DevServiceRegistration.Origin.MANAGED)
                    .containerInfo(() -> ContainerInfo.builder()
                            .id("1234")
                            .imageName(imageName)
                            .names(List.of("/" + name))
                            .ports(List.of())
                            .labels(Map.of())
                            .state("running")
                            .status("Up 5 minutes")
                            .build())
                    .build();
        }

    }

}
