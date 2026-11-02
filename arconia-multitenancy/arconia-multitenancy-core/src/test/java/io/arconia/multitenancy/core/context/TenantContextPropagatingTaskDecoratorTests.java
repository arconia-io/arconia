package io.arconia.multitenancy.core.context;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link TenantContextPropagatingTaskDecorator}.
 */
class TenantContextPropagatingTaskDecoratorTests {

    private static final String MDC_KEY = "tenantId";

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void whenEmptyMdcKeyNameThenThrow() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> TenantContextPropagatingTaskDecorator.builder().mdcKeyName(" ").build())
            .withMessageContaining("mdcKeyName cannot be empty");
    }

    @Test
    void whenNoTenantBoundThenTaskIsUnchanged() {
        Runnable task = () -> {};

        assertThat(TenantContextPropagatingTaskDecorator.builder().build().decorate(task)).isSameAs(task);
    }

    @Test
    void whenTenantBoundThenTaskRunsWithTenantOnAnotherThread() throws InterruptedException {
        var decorator = TenantContextPropagatingTaskDecorator.builder().build();
        var boundTenant = new AtomicReference<String>();
        var decorated = TenantContext.where("acme")
            .call(() -> decorator.decorate(() -> boundTenant.set(TenantContext.getTenantIdentifier())));

        Thread.ofVirtual().start(decorated).join();

        assertThat(boundTenant).hasValue("acme");
    }

    @Test
    void whenMdcKeyNameThenTenantIsInMdcWhileTaskRunsAndRemovedAfterwards() throws InterruptedException {
        var decorator = TenantContextPropagatingTaskDecorator.builder().mdcKeyName(MDC_KEY).build();
        var mdcDuringTask = new AtomicReference<String>();
        var mdcAfterTask = new AtomicReference<String>("unset");
        var decorated = TenantContext.where("acme").call(() -> decorator.decorate(() -> mdcDuringTask.set(MDC.get(MDC_KEY))));

        Thread.ofVirtual().start(() -> {
            decorated.run();
            mdcAfterTask.set(MDC.get(MDC_KEY));
        }).join();

        assertThat(mdcDuringTask).hasValue("acme");
        assertThat(mdcAfterTask).hasNullValue();
    }

    @Test
    void whenNoMdcKeyNameThenMdcIsUntouched() {
        var decorator = TenantContextPropagatingTaskDecorator.builder().build();
        var mdcDuringTask = new AtomicReference<String>("unset");
        var decorated = TenantContext.where("acme").call(() -> decorator.decorate(() -> mdcDuringTask.set(MDC.get(MDC_KEY))));

        decorated.run();

        assertThat(mdcDuringTask).hasNullValue();
    }

    @Test
    void whenTaskRunsOnSubmittingThreadThenPreviousMdcValueIsRestored() {
        var decorator = TenantContextPropagatingTaskDecorator.builder().mdcKeyName(MDC_KEY).build();
        var boundTenant = new AtomicReference<String>();
        MDC.put(MDC_KEY, "acme");

        TenantContext.where("acme").run(() ->
            decorator.decorate(() -> boundTenant.set(TenantContext.getTenantIdentifier())).run());

        assertThat(boundTenant).hasValue("acme");
        assertThat(MDC.get(MDC_KEY)).isEqualTo("acme");
    }

    @Test
    void whenTaskFailsThenMdcIsStillRestored() {
        var decorator = TenantContextPropagatingTaskDecorator.builder().mdcKeyName(MDC_KEY).build();
        var decorated = TenantContext.where("acme").call(() -> decorator.decorate(() -> {
            throw new IllegalStateException("task failed");
        }));

        assertThatIllegalStateException().isThrownBy(decorated::run).withMessage("task failed");
        assertThat(MDC.get(MDC_KEY)).isNull();
    }

}
