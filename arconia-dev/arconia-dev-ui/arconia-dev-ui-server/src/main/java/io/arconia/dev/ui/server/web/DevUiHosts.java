package io.arconia.dev.ui.server.web;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestWrapper;
import jakarta.servlet.http.HttpServletRequest;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;

import io.arconia.core.annotations.Internal;

/**
 * Decides which requests the Dev UI accepts.
 * <p>
 * A request must be addressed to the local machine and come from it, unless the host it is
 * addressed to is one of the allowed hosts.
 * <p>
 * Where it comes from is the address of the client. A web server listens on every network
 * interface by default, so without this check any machine on the same network could reach the
 * Dev UI by naming the local machine in its request. An application that runs in a container
 * sees the requests of its own host as coming from another machine, and has to be told to
 * accept them.
 * <p>
 * What it is addressed to stops a web page from reaching the Dev UI through a host name it
 * controls and resolves to the local machine (DNS rebinding). The host is checked both as the
 * web server reports it and as the {@code Host} header states it: an application that trusts
 * forwarded headers reports the host named by those headers, which a web page can set on its
 * own requests. When the request carries an {@code Origin} header, its host must pass the
 * same check, which stops a page of another website open in the browser of the developer
 * from calling the Dev UI (cross-site request forgery).
 * <p>
 * A request that changes something has to prove more: that it comes from the Dev UI itself.
 * It must carry a header of the Dev UI, which a page of another website, or of another
 * application on the local machine, cannot add without the browser asking the Dev UI first,
 * a question the Dev UI never answers.
 */
@Internal
public final class DevUiHosts {

    /**
     * The header a request that changes something must carry.
     */
    public static final String WRITE_HEADER = "Arconia-Dev-UI";

    private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "::1");

    private static final Pattern HOST_NAME = Pattern.compile("[a-z0-9]([a-z0-9.-]*[a-z0-9])?");

    private static final Pattern IP_ADDRESS = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3}|[0-9a-fA-F:.]*:[0-9a-fA-F:.]*");

    private static final Pattern IPV6_ADDRESS = Pattern.compile("[0-9a-f:.]*:[0-9a-f:.]*:[0-9a-f:.]*");

    private final Set<String> allowedHosts;

    private final boolean remoteAccess;

    private final boolean connectionAddressKnown;

    /**
     * Creates the check for the given allowed hosts. With remote access, a request addressed
     * to the local machine is accepted wherever it comes from. The check also needs to know
     * whether the web server reports the address a request really comes from. When it doesn't,
     * because it replaces the address with the one a forwarded header claims, no request is
     * taken to come from the local machine, and the ones addressed to it are only accepted with
     * remote access.
     */
    public DevUiHosts(Collection<String> allowedHosts, boolean remoteAccess, boolean connectionAddressKnown) {
        Assert.notNull(allowedHosts, "allowedHosts cannot be null");
        allowedHosts.forEach(DevUiHosts::validate);
        this.allowedHosts = allowedHosts.stream().map(DevUiHosts::normalize).collect(Collectors.toUnmodifiableSet());
        this.remoteAccess = remoteAccess;
        this.connectionAddressKnown = connectionAddressKnown;
    }

    /**
     * Whether the given request is accepted.
     */
    public boolean accepts(HttpServletRequest request) {
        return rejection(request) == null;
    }

    /**
     * Why the given request is not accepted, or {@code null} when it is.
     */
    @Nullable
    Rejection rejection(HttpServletRequest request) {
        // The names of the local machine only count for a request that comes from it.
        boolean local = this.remoteAccess || comesFromTheLocalMachine(request);
        String host = request.getServerName();
        if (isDisallowed(host, local)) {
            return isLocal(host) ? clientRejection(request) : new Rejection(Rejection.HOST, host);
        }
        String hostHeader = request.getHeader(HttpHeaders.HOST);
        if (hostHeader != null && isDisallowed(withoutPort(hostHeader), local)) {
            return new Rejection(Rejection.HOST, hostHeader);
        }
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin != null && isDisallowed(originHost(origin), local)) {
            return new Rejection(Rejection.ORIGIN, origin);
        }
        return null;
    }

    /**
     * A request addressed to the local machine that cannot be shown to come from it. When the
     * web server replaces the address of a request with a forwarded one, that address says
     * nothing about the client, and is left out.
     */
    private Rejection clientRejection(HttpServletRequest request) {
        if (!this.connectionAddressKnown) {
            return new Rejection(Rejection.UNKNOWN_CLIENT, "");
        }
        return new Rejection(Rejection.CLIENT, String.valueOf(connectionAddress(request)));
    }

    /**
     * Why the given request is not accepted as one that changes something, or {@code null}
     * when it is. The request must be accepted in the first place.
     */
    @Nullable
    Rejection writeRejection(HttpServletRequest request) {
        if (request.getHeader(WRITE_HEADER) == null) {
            return new Rejection(Rejection.WRITE, request.getMethod());
        }
        return null;
    }

    /**
     * Whether the request was sent by the local machine, going by the address of whoever
     * opened the connection it came on.
     * <p>
     * An application that trusts forwarded headers has the address of a request replaced with
     * the one a header claims, which anyone can set to the address of the local machine. When
     * a filter of Spring does that, the address of the connection is still there below what
     * the filter wraps the request with. When the web server does it, the address of the
     * connection is gone, and no request can be told to come from the local machine.
     */
    private boolean comesFromTheLocalMachine(HttpServletRequest request) {
        return this.connectionAddressKnown && isLoopback(connectionAddress(request));
    }

    private static @Nullable String connectionAddress(HttpServletRequest request) {
        ServletRequest connection = request;
        while (connection instanceof ServletRequestWrapper wrapper) {
            connection = wrapper.getRequest();
        }
        return connection.getRemoteAddr();
    }

    private boolean isDisallowed(@Nullable String host, boolean local) {
        if (host == null) {
            return true;
        }
        return (!local || !isLocal(host)) && !this.allowedHosts.contains(normalize(host));
    }

    private static boolean isLocal(String host) {
        return LOCAL_HOSTS.contains(normalize(host));
    }

    /**
     * Whether the address of a client is one of the local machine itself. Only an IP address
     * is read, so that nothing is ever looked up.
     */
    private static boolean isLoopback(@Nullable String address) {
        if (address == null || !IP_ADDRESS.matcher(address).matches()) {
            return false;
        }
        try {
            return InetAddress.getByName(address).isLoopbackAddress();
        } catch (UnknownHostException ex) {
            return false;
        }
    }

    /**
     * The host of an {@code Origin} header value, or {@code null} when the value is not an
     * HTTP origin, such as the {@code null} origin of a sandboxed page.
     */
    private static @Nullable String originHost(String origin) {
        try {
            URI uri = URI.create(origin);
            String scheme = uri.getScheme();
            if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                return uri.getHost();
            }
            return null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * The host of a {@code Host} header value, which may end with a port.
     */
    private static String withoutPort(String hostHeader) {
        String host = hostHeader.trim();
        if (host.startsWith("[")) {
            int end = host.indexOf(']');
            return (end > 0) ? host.substring(0, end + 1) : host;
        }
        int port = host.lastIndexOf(':');
        return (port > 0) ? host.substring(0, port) : host;
    }

    /**
     * An allowed host is a host name or an IP address. Anything else, such as a URL or a host
     * with a port, would never match a request and is reported instead of silently ignored.
     * <p>
     * The local machine cannot be an allowed host. Its names are accepted already, for the
     * requests that come from it, and an allowed host is accepted whichever machine a request
     * comes from: listing one of them would let any machine in by naming the local machine.
     */
    private static void validate(String allowedHost) {
        Assert.notNull(allowedHost, "allowedHosts cannot contain null elements");
        String host = normalize(allowedHost);
        Assert.isTrue(HOST_NAME.matcher(host).matches() || IPV6_ADDRESS.matcher(host).matches(),
                () -> "'%s' is not a valid allowed host for the Dev UI: it must be a host name or an IP address, without scheme, port or path"
                    .formatted(allowedHost));
        Assert.isTrue(!isLocal(host) && !isLoopback(host),
                () -> "'%s' is not a valid allowed host for the Dev UI: the local machine is allowed already. "
                    .formatted(allowedHost)
                        + "To accept the requests that other machines address to it, as for an application in a container, "
                        + "set 'arconia.dev.ui.remote-access.enabled' to 'true'");
    }

    /**
     * Host names are compared in lower case, and an IPv6 address without its brackets.
     */
    private static String normalize(String host) {
        String normalizedHost = host.trim().toLowerCase(Locale.ROOT);
        if (normalizedHost.startsWith("[") && normalizedHost.endsWith("]")) {
            return normalizedHost.substring(1, normalizedHost.length() - 1);
        }
        return normalizedHost;
    }

    /**
     * What a request was rejected for, and the value of it: its host, its origin, the client it
     * comes from, a client that cannot be known, or a request that changes something without
     * coming from the Dev UI.
     */
    record Rejection(String kind, String value) {

        static final String HOST = "host";

        static final String ORIGIN = "origin";

        static final String CLIENT = "client";

        static final String UNKNOWN_CLIENT = "unknown client";

        static final String WRITE = "write";

        /**
         * What the application log says about the rejection, with what to do about it when
         * the request was meant to be accepted.
         */
        String message() {
            return switch (this.kind) {
                case HOST -> "the host '%s' is not allowed. The Dev UI only accepts requests addressed to the local machine. "
                        .formatted(this.value) + "To allow another host, add it to 'arconia.dev.ui.allowed-hosts'.";
                case ORIGIN -> "the origin '%s' is not allowed. The Dev UI doesn't accept requests sent by the pages of other websites. "
                        .formatted(this.value) + "To allow another host, add it to 'arconia.dev.ui.allowed-hosts'.";
                case CLIENT -> "the client '%s' is another machine. The Dev UI only accepts requests that come from the local machine. "
                        .formatted(this.value) + "To allow other machines, as for an application in a container, "
                        + "set 'arconia.dev.ui.remote-access.enabled' to 'true'.";
                case UNKNOWN_CLIENT -> "the web server replaces the address of a request with the one forwarded headers claim "
                        + "('server.forward-headers-strategy=native'), so no request can be told to come from the local machine. "
                        + "Use the 'framework' strategy, or set 'arconia.dev.ui.remote-access.enabled' to 'true'.";
                case WRITE -> "a %s request changes something, and is only accepted from the Dev UI itself, with the '%s' header."
                        .formatted(this.value, WRITE_HEADER);
                default -> "the %s '%s' is not allowed.".formatted(this.kind, this.value);
            };
        }

    }

}
