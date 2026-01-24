package io.arconia.dev.services.keycloak;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import dasniko.testcontainers.keycloak.KeycloakContainer;

@EnabledIfDockerAvailable
@SpringBootTest(properties = { "spring.config.import=arconia-keycloak:",
        "arconia.bootstrap.mode=dev" }, classes = KeycloakDevServicesSpringBootTestIT.TestConfig.class)
class KeycloakDevServicesSpringBootTestIT {

    @Autowired
    ApplicationContext context;

    @Autowired KeycloakDevServicesProperties properties;

    @Test
    void loaderStartsContainerAndRegistersBean() {

        // var env = context.getEnvironment();
        // var binder = Binder.get(env);

        // KeycloakDevServicesProperties properties = binder
        //         .bind("arconia.dev.services.keycloak", KeycloakDevServicesProperties.class)
        //         .orElseGet(KeycloakDevServicesProperties::new);

        // assertThat(properties).isNotNull();


        KeycloakContainer container = context.getBean(KeycloakContainer.class);
        assertThat(container).isNotNull();
        assertThat(container.getDockerImageName()).contains("keycloak/keycloak");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestConfig {
    }

}
