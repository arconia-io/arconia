package io.arconia.dev.services.core;

import java.time.Duration;
import java.util.List;

import io.arconia.dev.services.api.config.JdbcDevServicesProperties;

/**
 * Configuration properties for a relational database dev service under test.
 */
public class TestJdbcDevServicesProperties extends TestDevServicesProperties implements JdbcDevServicesProperties {

    private String username = "user";

    private String password = "password";

    private String dbName = "testdb";

    private List<String> initScriptPaths = List.of();

    public TestJdbcDevServicesProperties withUsername(String username) {
        TestJdbcDevServicesProperties copy = copy();
        copy.username = username;
        return copy;
    }

    public TestJdbcDevServicesProperties withPassword(String password) {
        TestJdbcDevServicesProperties copy = copy();
        copy.password = password;
        return copy;
    }

    public TestJdbcDevServicesProperties withDbName(String dbName) {
        TestJdbcDevServicesProperties copy = copy();
        copy.dbName = dbName;
        return copy;
    }

    public TestJdbcDevServicesProperties withInitScriptPaths(List<String> initScriptPaths) {
        TestJdbcDevServicesProperties copy = copy();
        copy.initScriptPaths = initScriptPaths;
        return copy;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getDbName() {
        return dbName;
    }

    @Override
    public List<String> getInitScriptPaths() {
        return initScriptPaths;
    }

    @Override
    public TestJdbcDevServicesProperties withStartupTimeout(Duration startupTimeout) {
        return (TestJdbcDevServicesProperties) super.withStartupTimeout(startupTimeout);
    }

    @Override
    protected TestJdbcDevServicesProperties copy() {
        TestJdbcDevServicesProperties copy = new TestJdbcDevServicesProperties();
        copyInto(copy);
        copy.username = username;
        copy.password = password;
        copy.dbName = dbName;
        copy.initScriptPaths = initScriptPaths;
        return copy;
    }

}
