package io.arconia.dev.ui.server.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevUiResponses}.
 */
class DevUiResponsesTests {

    @Test
    void statusIsWrittenAsTheCompleteResponse() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        DevUiResponses.writeStatus(response, 404, "Not Found");

        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getContentType()).isEqualTo("text/plain;charset=UTF-8");
        assertThat(response.getContentAsString()).isEqualTo("Not Found");
        assertThat(response.getErrorMessage()).isNull();
    }

}
