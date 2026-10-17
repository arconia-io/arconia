package io.arconia.multitenancy.web.resolution;

import org.junit.jupiter.api.Test;

import io.arconia.multitenancy.web.resolution.TenantResolutionFailure.Outcome;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link TenantResolutionFailure}.
 */
class TenantResolutionFailureTests {

    @Test
    void whenNullOutcomeThenThrow() {
        assertThatIllegalArgumentException().isThrownBy(() -> new TenantResolutionFailure(null, "message", null))
            .withMessageContaining("outcome cannot be null");
    }

    @Test
    void whenEmptyMessageThenThrow() {
        assertThatIllegalArgumentException().isThrownBy(() -> new TenantResolutionFailure(Outcome.MISSING, "", null))
            .withMessageContaining("message cannot be null or empty");
    }

    @Test
    void whenCauseIsAbsentThenNull() {
        var failure = new TenantResolutionFailure(Outcome.MISSING, "A tenant identifier must be specified", null);

        assertThat(failure.cause()).isNull();
        assertThat(failure.outcome()).isEqualTo(Outcome.MISSING);
    }

}
