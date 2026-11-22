package io.arconia.dev.ui.server.web;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServlet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link DevUiFilter}.
 */
@ExtendWith(OutputCaptureExtension.class)
class DevUiFilterTests {

    private final RecordingServlet servlet = new RecordingServlet();

    private final DevUiFilter filter = new DevUiFilter(this.servlet, new DevUiHosts(List.of("Dev.Example.com"), false, true));

    @Test
    void rejectedRequestGetsForbiddenStatusWithoutErrorDispatch() throws Exception {
        MockHttpServletRequest request = request("evil.example.com");
        MockHttpServletResponse response = new MockHttpServletResponse();

        this.filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).isEqualTo("The Dev UI only accepts requests for the local machine");
        assertThat(response.getErrorMessage()).isNull();
    }

    @Test
    void acceptedRequestGoesStraightToTheServletAndNotThroughTheOtherFilters() throws Exception {
        MockHttpServletRequest request = request("localhost");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        this.filter.doFilter(request, response, filterChain);

        assertThat(this.servlet.request).isSameAs(request);
        assertThat(this.servlet.response).isSameAs(response);
        assertThat(filterChain.getRequest()).isNull();
    }

    @Test
    void rejectedRequestGoesNeitherToTheServletNorThroughTheOtherFilters() throws Exception {
        MockFilterChain filterChain = new MockFilterChain();

        this.filter.doFilter(request("evil.example.com"), new MockHttpServletResponse(), filterChain);

        assertThat(this.servlet.request).isNull();
        assertThat(filterChain.getRequest()).isNull();
    }

    @ParameterizedTest
    @MethodSource("failures")
    void failureOfTheServletIsAnsweredHereAndNotLeftToTheApplication(Throwable failure, CapturedOutput output) throws Exception {
        this.servlet.failure = failure;
        MockHttpServletResponse response = new MockHttpServletResponse();

        this.filter.doFilter(request("localhost"), response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getContentAsString())
                .isEqualTo("The Dev UI failed to handle the request. See the application log for the reason.");
        assertThat(response.getErrorMessage()).isNull();
        assertThat(output).contains("The Dev UI failed to handle the request '/a/dev-ui'").contains(failure.getMessage());
    }

    static List<Throwable> failures() {
        return List.of(new IllegalStateException("Broken page"), new NoClassDefFoundError("com/example/Missing"),
                new ServletException("Broken servlet"));
    }

    /**
     * What happens when the browser goes away before it has the whole response, as when the
     * developer moves to another page.
     */
    @Test
    void responseThatCannotBeWrittenIsNotReportedAsFailure(CapturedOutput output) throws Exception {
        this.servlet.failure = new IOException("Broken pipe");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setCommitted(true);

        this.filter.doFilter(request("localhost"), response, new MockFilterChain());

        assertThat(output).doesNotContain("Broken pipe").doesNotContain("failed to handle");
    }

    @Test
    void failureAfterTheResponseHasStartedLeavesTheResponseAsItIs() throws Exception {
        this.servlet.failure = new IllegalStateException("Broken page");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        response.setCommitted(true);

        this.filter.doFilter(request("localhost"), response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void rejectedHostIsReportedOnce(CapturedOutput output) throws Exception {
        for (int i = 0; i < 3; i++) {
            this.filter.doFilter(request("evil.example.com"), new MockHttpServletResponse(), new MockFilterChain());
        }
        this.filter.doFilter(request("other.example.com"), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(output.getOut().split("the host 'evil.example.com' is not allowed", -1)).hasSize(2);
        assertThat(output).contains("the host 'other.example.com' is not allowed")
                .contains("To allow another host, add it to 'arconia.dev.ui.allowed-hosts'.");
    }

    @Test
    void requestThatChangesSomethingGoesToTheServletWithTheHeaderOfTheDevUi() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/a/dev-ui/api/actions/restart");
        request.addHeader("Arconia-Dev-UI", "1");

        this.filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(this.servlet.request).isSameAs(request);
    }

    @Test
    void requestThatChangesSomethingIsForbiddenWithoutTheHeaderOfTheDevUi(CapturedOutput output) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/a/dev-ui/api/actions/restart");
        request.addHeader("Origin", "http://localhost:8080");
        MockHttpServletResponse response = new MockHttpServletResponse();

        this.filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).isEqualTo("The Dev UI only accepts a request that changes something from the Dev UI itself");
        assertThat(this.servlet.request).isNull();
        assertThat(output).contains("a POST request changes something, and is only accepted from the Dev UI itself, with the 'Arconia-Dev-UI' header.")
                .doesNotContain("allowed-hosts");
    }

    @Test
    void requestThatChangesSomethingIsForbiddenFromAnotherWebsiteEvenWithTheHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/a/dev-ui/api/actions/restart");
        request.addHeader("Origin", "https://evil.example.com");
        request.addHeader("Arconia-Dev-UI", "1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        this.filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(this.servlet.request).isNull();
    }

    @Test
    void servletCannotBeNull() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DevUiFilter(null, new DevUiHosts(List.of(), false, true)))
                .withMessage("servlet cannot be null");
    }

    @Test
    void hostsCannotBeNull() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DevUiFilter(this.servlet, null))
                .withMessage("hosts cannot be null");
    }

    private static final class RecordingServlet extends HttpServlet {

        private ServletRequest request;

        private ServletResponse response;

        private Throwable failure;

        @Override
        public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
            switch (this.failure) {
                case null -> {}
                case IOException ex -> throw ex;
                case ServletException ex -> throw ex;
                case RuntimeException ex -> throw ex;
                case Error ex -> throw ex;
                default -> throw new IllegalStateException(this.failure);
            }
            this.request = request;
            this.response = response;
        }

    }

    private static MockHttpServletRequest request(String host) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/a/dev-ui");
        request.setServerName(host);
        return request;
    }

}
