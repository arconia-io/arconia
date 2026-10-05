package io.arconia.dev.services.core.registration;

import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;

/**
 * {@link JdbcConnectionDetails} for connecting to a relational database dev service
 * running in a container discovered from another application.
 */
@Incubating
public final class JdbcDiscoveredConnectionDetails implements JdbcConnectionDetails {

    private final String jdbcUrl;

    private final String username;

    private final String password;

    public JdbcDiscoveredConnectionDetails(String jdbcUrl, String username, String password) {
        Assert.hasText(jdbcUrl, "jdbcUrl cannot be null or empty");
        Assert.notNull(username, "username cannot be null");
        Assert.notNull(password, "password cannot be null");
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    @Override
    public String getJdbcUrl() {
        return jdbcUrl;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

}
