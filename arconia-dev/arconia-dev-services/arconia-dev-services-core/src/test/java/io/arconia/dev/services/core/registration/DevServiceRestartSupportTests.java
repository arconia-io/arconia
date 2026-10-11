package io.arconia.dev.services.core.registration;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.support.SimpleThreadScope;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import io.arconia.dev.services.core.registration.DevServicesTestSupport.RestartingContainer;
import io.arconia.dev.services.core.registration.DevServicesTestSupport.TestPostgresContainer;

import static io.arconia.dev.services.core.registration.DevServicesTestSupport.enableDevMode;
import static io.arconia.dev.services.core.registration.DevServicesTestSupport.resetBootstrapMode;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevServiceRestartSupport}.
 */
@ExtendWith(OutputCaptureExtension.class)
class DevServiceRestartSupportTests {

    private static final String CONTAINER_BEAN_NAME = "devService.container.postgres";

    private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

    private final DevServiceRestartSupport support = new DevServiceRestartSupport(beanFactory, new StandardEnvironment());

    /**
     * Stands in for the scope DevTools registers, which outlives the application contexts.
     */
    private final SimpleThreadScope restartScope = new SimpleThreadScope();

    @BeforeEach
    @AfterEach
    void reset() {
        resetBootstrapMode();
    }

    @Test
    void whenDevModeAndRestartScopeRegisteredThenScopeIsActive() {
        enableDevMode();
        beanFactory.registerScope(DevServiceRestartSupport.SCOPE_NAME, restartScope);

        assertThat(support.isRestartScopeActive()).isTrue();
    }

    @Test
    void whenDevToolsRestartIsDisabledThenScopeIsNotActive() {
        enableDevMode();
        beanFactory.registerScope(DevServiceRestartSupport.SCOPE_NAME, restartScope);
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test",
                Map.of("spring.devtools.restart.enabled", "false")));

        assertThat(new DevServiceRestartSupport(beanFactory, environment).isRestartScopeActive()).isFalse();
    }

    @Test
    void whenNotInDevModeThenScopeIsNotActive() {
        beanFactory.registerScope(DevServiceRestartSupport.SCOPE_NAME, restartScope);

        assertThat(support.isRestartScopeActive()).isFalse();
    }

    @Test
    void whenNoRestartScopeRegisteredThenScopeIsNotActive() {
        enableDevMode();

        assertThat(support.isRestartScopeActive()).isFalse();
    }

    @Test
    void whenKeptContainerIsRunningThenItIsRetained() {
        enableDevMode();
        beanFactory.registerScope(DevServiceRestartSupport.SCOPE_NAME, restartScope);
        RestartingContainer kept = keep(new RestartingContainer());

        boolean retained = support.retainKeptContainer("postgres", CONTAINER_BEAN_NAME, RestartingContainer.class);

        assertThat(retained).isTrue();
        assertThat(kept.stopped).isFalse();
        // The container is back in the scope for the container bean to resolve to it.
        assertThat(restartScope.remove(CONTAINER_BEAN_NAME)).isSameAs(kept);
    }

    @Test
    void whenKeptContainerIsNoLongerRunningThenItIsStoppedAndRemoved(CapturedOutput output) {
        enableDevMode();
        beanFactory.registerScope(DevServiceRestartSupport.SCOPE_NAME, restartScope);
        RestartingContainer kept = keep(new RestartingContainer());
        // The container was removed outside the application.
        kept.running = false;

        boolean retained = support.retainKeptContainer("postgres", CONTAINER_BEAN_NAME, RestartingContainer.class);

        assertThat(retained).isFalse();
        // The kept instance is stopped, so that whatever it still holds (e.g. a fixed port) is released.
        assertThat(kept.stopped).isTrue();
        assertThat(restartScope.remove(CONTAINER_BEAN_NAME)).isNull();
        assertThat(output).contains("is no longer running");
    }

    @Test
    void whenKeptContainerClassWasReloadedThenItIsStoppedAndRemoved(CapturedOutput output) {
        enableDevMode();
        beanFactory.registerScope(DevServiceRestartSupport.SCOPE_NAME, restartScope);
        RestartingContainer kept = keep(new RestartingContainer());

        // DevTools reloaded the container class: the kept instance belongs to the previous class
        // loader, which a different container type stands in for.
        boolean retained = support.retainKeptContainer("postgres", CONTAINER_BEAN_NAME, TestPostgresContainer.class);

        assertThat(retained).isFalse();
        assertThat(kept.stopped).isTrue();
        assertThat(restartScope.remove(CONTAINER_BEAN_NAME)).isNull();
        assertThat(output).contains("container class was reloaded");
    }

    @Test
    void whenKeptReusableContainerIsNoLongerRunningThenItIsRemovedButNotStopped() {
        enableDevMode();
        beanFactory.registerScope(DevServiceRestartSupport.SCOPE_NAME, restartScope);
        RestartingContainer kept = keep(new RestartingContainer().withReuse(true));
        kept.running = false;

        boolean retained = support.retainKeptContainer("postgres", CONTAINER_BEAN_NAME, RestartingContainer.class);

        // A container reused via Testcontainers is never stopped by the framework.
        assertThat(retained).isFalse();
        assertThat(kept.stopped).isFalse();
        assertThat(restartScope.remove(CONTAINER_BEAN_NAME)).isNull();
    }

    @Test
    void whenKeptReusableContainerClassWasReloadedThenItIsRemovedButNotStopped() {
        enableDevMode();
        beanFactory.registerScope(DevServiceRestartSupport.SCOPE_NAME, restartScope);
        RestartingContainer kept = keep(new RestartingContainer().withReuse(true));

        boolean retained = support.retainKeptContainer("postgres", CONTAINER_BEAN_NAME, TestPostgresContainer.class);

        // Testcontainers reattaches the new container instance to the running one through the reuse hash.
        assertThat(retained).isFalse();
        assertThat(kept.stopped).isFalse();
        assertThat(restartScope.remove(CONTAINER_BEAN_NAME)).isNull();
    }

    @Test
    void whenKeptContainerIsDiscardedThenItIsStoppedAndRemoved(CapturedOutput output) {
        enableDevMode();
        beanFactory.registerScope(DevServiceRestartSupport.SCOPE_NAME, restartScope);
        RestartingContainer kept = keep(new RestartingContainer());

        support.discardKeptContainer("postgres", CONTAINER_BEAN_NAME);

        assertThat(kept.stopped).isTrue();
        assertThat(restartScope.remove(CONTAINER_BEAN_NAME)).isNull();
        assertThat(output).contains("Dev Service 'postgres': container").contains("is no longer needed");
    }

    @Test
    void whenKeptReusableContainerIsDiscardedThenItIsRemovedButNotStopped() {
        enableDevMode();
        beanFactory.registerScope(DevServiceRestartSupport.SCOPE_NAME, restartScope);
        RestartingContainer kept = keep(new RestartingContainer().withReuse(true));

        support.discardKeptContainer("postgres", CONTAINER_BEAN_NAME);

        assertThat(kept.stopped).isFalse();
        assertThat(restartScope.remove(CONTAINER_BEAN_NAME)).isNull();
    }

    @Test
    void whenNoContainerIsKeptThenDiscardingDoesNothing(CapturedOutput output) {
        enableDevMode();
        beanFactory.registerScope(DevServiceRestartSupport.SCOPE_NAME, restartScope);

        support.discardKeptContainer("postgres", CONTAINER_BEAN_NAME);

        assertThat(output).doesNotContain("no longer needed");
    }

    @Test
    void whenRestartScopeIsNotActiveThenNothingIsRetained() {
        // Outside dev mode the scope is not active, even if registered and holding a container.
        beanFactory.registerScope(DevServiceRestartSupport.SCOPE_NAME, restartScope);
        RestartingContainer kept = keep(new RestartingContainer());

        boolean retained = support.retainKeptContainer("postgres", CONTAINER_BEAN_NAME, RestartingContainer.class);

        assertThat(retained).isFalse();
        assertThat(kept.stopped).isFalse();
        assertThat(restartScope.remove(CONTAINER_BEAN_NAME)).isSameAs(kept);
    }

    @Test
    void whenNoContainerIsKeptThenNothingIsRetained() {
        enableDevMode();
        beanFactory.registerScope(DevServiceRestartSupport.SCOPE_NAME, restartScope);

        assertThat(support.retainKeptContainer("postgres", CONTAINER_BEAN_NAME, RestartingContainer.class)).isFalse();
    }

    /**
     * Put the given container in the restart scope under the container bean name, as a
     * previous run of the application would have.
     */
    private RestartingContainer keep(RestartingContainer container) {
        restartScope.get(CONTAINER_BEAN_NAME, () -> container);
        return container;
    }

}
