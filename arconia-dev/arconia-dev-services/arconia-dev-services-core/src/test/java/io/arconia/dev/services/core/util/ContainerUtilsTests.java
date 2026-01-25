package io.arconia.dev.services.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ContainerUtils}.
 */
class ContainerUtilsTests {

    @Test
    void isValidPortWhenPortIsInvalidThenReturnFalse() {
        assertFalse(ContainerUtils.isValidPort(-1));
        assertFalse(ContainerUtils.isValidPort(0));
        assertFalse(ContainerUtils.isValidPort(65536));
    }

    @Test
    void isValidPortWhenPortIsValidThenReturnTrue() {
        assertTrue(ContainerUtils.isValidPort(1234));
        assertTrue(ContainerUtils.isValidPort(65535));
    }

}
