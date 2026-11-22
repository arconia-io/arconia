package io.arconia.dev.ui.server.web;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link DevUiHosts}.
 */
class DevUiHostsTests {

    private final DevUiHosts hosts = new DevUiHosts(List.of("Dev.Example.com"), false, true);

    @ParameterizedTest
    @ValueSource(strings = { "localhost", "LOCALHOST", "127.0.0.1", "::1", "[::1]", "dev.example.com" })
    void requestIsAcceptedWhenHostIsAllowed(String host) {
        assertThat(this.hosts.accepts(request(host))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = { "evil.example.com", "localhost.evil.example.com", "192.168.1.10", "sub.dev.example.com" })
    void requestIsRejectedWhenHostIsNotAllowed(String host) {
        assertThat(this.hosts.rejection(request(host))).isEqualTo(new DevUiHosts.Rejection("host", host));
    }

    @ParameterizedTest
    @ValueSource(strings = { "http://localhost:8080", "http://localhost:3000", "https://127.0.0.1", "http://[::1]:8080",
            "https://dev.example.com" })
    void requestIsAcceptedWhenOriginIsAllowed(String origin) {
        MockHttpServletRequest request = request("localhost");
        request.addHeader(HttpHeaders.ORIGIN, origin);

        assertThat(this.hosts.accepts(request)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = { "https://evil.example.com", "http://localhost.evil.example.com", "null",
            "chrome-extension://abcdef", "not an origin" })
    void requestIsRejectedWhenOriginIsNotAllowed(String origin) {
        MockHttpServletRequest request = request("localhost");
        request.addHeader(HttpHeaders.ORIGIN, origin);

        assertThat(this.hosts.rejection(request)).isEqualTo(new DevUiHosts.Rejection("origin", origin));
    }

    @ParameterizedTest
    @ValueSource(strings = { "localhost", "localhost:8080", "127.0.0.1:8080", "[::1]", "[::1]:8080", "Dev.Example.com:443" })
    void requestIsAcceptedWhenHostHeaderIsAllowed(String hostHeader) {
        MockHttpServletRequest request = request("localhost");
        request.addHeader(HttpHeaders.HOST, hostHeader);

        assertThat(this.hosts.accepts(request)).isTrue();
    }

    /**
     * An application that trusts forwarded headers reports the host they name, which a web
     * page can set to the local machine on its own requests.
     */
    @ParameterizedTest
    @ValueSource(strings = { "evil.example.com", "evil.example.com:8080", "localhost.evil.example.com:8080", "[2001:db8::1]:8080", "" })
    void requestIsRejectedWhenHostHeaderIsNotAllowedWhateverTheServerReports(String hostHeader) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.HOST, hostHeader);
        HttpServletRequest forwardedRequest = new HttpServletRequestWrapper(request) {
            @Override
            public String getServerName() {
                return "localhost";
            }
        };

        assertThat(this.hosts.rejection(forwardedRequest)).isEqualTo(new DevUiHosts.Rejection("host", hostHeader));
    }

    @ParameterizedTest
    @ValueSource(strings = { "dev.example.com", "workspace-1", "192.168.1.10", "2001:db8::1", "[2001:db8::1]" })
    void allowedHostIsHostNameOrAddress(String allowedHost) {
        assertThat(new DevUiHosts(List.of(allowedHost), false, true).accepts(request(allowedHost))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = { "dev.example.com:8080", "https://dev.example.com", "dev.example.com/path", "*.example.com", "" })
    void allowedHostCannotBeAnythingElse(String allowedHost) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DevUiHosts(List.of(allowedHost), false, true))
                .withMessage("'%s' is not a valid allowed host for the Dev UI: it must be a host name or an IP address, without scheme, port or path",
                        allowedHost);
    }

    /**
     * An allowed host is accepted whichever machine a request comes from, so the local machine
     * as an allowed host would let any machine in that names it.
     */
    @ParameterizedTest
    @ValueSource(strings = { "localhost", "LocalHost", "127.0.0.1", "127.0.0.5", "::1", "[::1]", "0:0:0:0:0:0:0:1" })
    void allowedHostCannotBeTheLocalMachine(String allowedHost) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DevUiHosts(List.of(allowedHost), false, true))
                .withMessage("'%s' is not a valid allowed host for the Dev UI: the local machine is allowed already. "
                        + "To accept the requests that other machines address to it, as for an application in a container, "
                        + "set 'arconia.dev.ui.remote-access.enabled' to 'true'", allowedHost);
    }

    @ParameterizedTest
    @ValueSource(strings = { "127.0.0.1", "127.0.0.5", "::1", "0:0:0:0:0:0:0:1" })
    void requestAddressedToTheLocalMachineIsAcceptedWhenItComesFromIt(String remoteAddress) {
        MockHttpServletRequest request = request("localhost");
        request.setRemoteAddr(remoteAddress);

        assertThat(this.hosts.accepts(request)).isTrue();
    }

    /**
     * A web server listens on every network interface by default, so another machine on the
     * same network can send a request that names the local machine.
     */
    @ParameterizedTest
    @ValueSource(strings = { "192.168.1.20", "10.0.0.7", "172.17.0.1", "2001:db8::1", "not an address" })
    void requestAddressedToTheLocalMachineIsRejectedWhenItComesFromAnotherMachine(String remoteAddress) {
        MockHttpServletRequest request = request("localhost");
        request.setRemoteAddr(remoteAddress);

        DevUiHosts.Rejection rejection = this.hosts.rejection(request);

        assertThat(rejection).isEqualTo(new DevUiHosts.Rejection("client", remoteAddress));
        assertThat(rejection.message()).isEqualTo("the client '%s' is another machine. The Dev UI only accepts requests that come from the local machine. "
                .formatted(remoteAddress) + "To allow other machines, as for an application in a container, "
                + "set 'arconia.dev.ui.remote-access.enabled' to 'true'.");
    }

    @Test
    void addressThatFilterClaimsForTheRequestDoesNotCount() {
        MockHttpServletRequest connection = request("localhost");
        connection.setRemoteAddr("192.168.1.20");
        HttpServletRequestWrapper forwarded = new HttpServletRequestWrapper(connection) {

            @Override
            public String getRemoteAddr() {
                return "127.0.0.1";
            }

        };

        assertThat(this.hosts.accepts(forwarded)).isFalse();
        assertThat(this.hosts.rejection(forwarded).value()).isEqualTo("192.168.1.20");
    }

    @Test
    void whenTheWebServerDoesNotReportWhereRequestsComeFromThenNoneIsOneOfTheLocalMachine() {
        MockHttpServletRequest request = request("localhost");
        request.setRemoteAddr("127.0.0.1");

        assertThat(new DevUiHosts(List.of("dev.example.com"), false, false).accepts(request)).isFalse();
        assertThat(new DevUiHosts(List.of("dev.example.com"), false, false).accepts(request("dev.example.com"))).isTrue();
        assertThat(new DevUiHosts(List.of(), true, false).accepts(request)).isTrue();
    }

    /**
     * The address the web server reports is then the one a forwarded header claims, which says
     * nothing about the client.
     */
    @Test
    void whenTheWebServerDoesNotReportWhereRequestsComeFromThenTheRejectionLeavesTheAddressOut() {
        MockHttpServletRequest request = request("localhost");
        request.setRemoteAddr("127.0.0.1");

        DevUiHosts.Rejection rejection = new DevUiHosts(List.of(), false, false).rejection(request);

        assertThat(rejection).isEqualTo(new DevUiHosts.Rejection("unknown client", ""));
        assertThat(rejection.message()).doesNotContain("127.0.0.1")
                .contains("'server.forward-headers-strategy=native'")
                .contains("'arconia.dev.ui.remote-access.enabled'");
    }

    @Test
    void rejectionCannotAddLinesToTheLog() {
        DevUiHosts.Rejection rejection = new DevUiHosts.Rejection("host", "evil.example.com\r\nINFO Forged line\u001b[31m");

        assertThat(rejection.value()).isEqualTo("evil.example.com��INFO Forged line�[31m");
        assertThat(rejection.message()).doesNotContain("\r", "\n", "\u001b");
    }

    @Test
    void rejectionCannotFloodTheLog() {
        DevUiHosts.Rejection rejection = new DevUiHosts.Rejection("host", "a".repeat(10_000));

        assertThat(rejection.value()).isEqualTo("a".repeat(100) + "…");
    }

    @Test
    void requestAddressedToAnAllowedHostIsAcceptedWhereverItComesFrom() {
        MockHttpServletRequest request = request("dev.example.com");
        request.setRemoteAddr("192.168.1.20");

        assertThat(this.hosts.accepts(request)).isTrue();
    }

    @Test
    void originOfTheLocalMachineDoesNotCountForRequestFromAnotherMachine() {
        MockHttpServletRequest request = request("dev.example.com");
        request.setRemoteAddr("192.168.1.20");
        request.addHeader(HttpHeaders.ORIGIN, "http://localhost:3000");

        assertThat(this.hosts.rejection(request)).isEqualTo(new DevUiHosts.Rejection("origin", "http://localhost:3000"));
    }

    @Test
    void requestAddressedToTheLocalMachineIsAcceptedFromAnotherMachineWithRemoteAccess() {
        MockHttpServletRequest request = request("localhost");
        request.setRemoteAddr("172.17.0.1");

        assertThat(new DevUiHosts(List.of(), true, true).accepts(request)).isTrue();
    }

    @Test
    void requestAddressedToAnotherHostIsStillRejectedWithRemoteAccess() {
        MockHttpServletRequest request = request("evil.example.com");
        request.setRemoteAddr("172.17.0.1");

        assertThat(new DevUiHosts(List.of(), true, true).rejection(request)).isEqualTo(new DevUiHosts.Rejection("host", "evil.example.com"));
    }

    @Test
    void requestThatChangesSomethingIsAcceptedWithTheHeaderOfTheDevUi() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/a/dev-ui/api/actions/restart");
        request.addHeader("Arconia-Dev-UI", "1");

        assertThat(this.hosts.writeRejection(request)).isNull();
    }

    @Test
    void requestThatChangesSomethingIsRejectedWithoutTheHeaderOfTheDevUi() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/a/dev-ui/api/actions/restart");
        request.addHeader(HttpHeaders.ORIGIN, "http://localhost:8080");

        assertThat(this.hosts.writeRejection(request))
                .isEqualTo(new DevUiHosts.Rejection("write", "POST"));
    }

    @Test
    void allowedHostsCannotBeNull() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DevUiHosts(null, false, true))
                .withMessage("allowedHosts cannot be null");
    }

    private static MockHttpServletRequest request(String host) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/a/dev-ui");
        request.setServerName(host);
        return request;
    }

}
