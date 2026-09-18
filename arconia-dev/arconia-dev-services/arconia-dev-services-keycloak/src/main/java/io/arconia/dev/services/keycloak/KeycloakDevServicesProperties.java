package io.arconia.dev.services.keycloak;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.arconia.dev.services.api.config.ResourceMapping;
import io.arconia.dev.services.api.config.SharedDevServicesProperties;
import io.arconia.dev.services.api.config.VolumeMapping;

/**
 * Properties for the Keycloak Dev Services.
 */
@ConfigurationProperties(prefix = KeycloakDevServicesProperties.CONFIG_PREFIX)
public class KeycloakDevServicesProperties implements SharedDevServicesProperties {

    public static final String CONFIG_PREFIX = "arconia.dev.services.keycloak";

    public static final String DEFAULT_REALM = "arconia";

    static final String DEFAULT_ADMIN_USERNAME = "arconia";
    static final String DEFAULT_ADMIN_PASSWORD = "arconia";

    /**
     * Whether the dev service is enabled.
     */
    private boolean enabled = true;

    /**
     * Full name of the container image used in the dev service.
     */
    private String imageName = "quay.io/keycloak/keycloak:26.7";

    /**
     * Environment variables to set in the service.
     */
    private Map<String,String> environment = new HashMap<>();

    /**
     * Network aliases to assign to the dev service container.
     */
    private List<String> networkAliases = new ArrayList<>();

    /**
     * Fixed port for exposing the Keycloak HTTP port to the host.
     * When it's 0 (default), a random available port is assigned dynamically.
     */
    private int port = 0;

    /**
     * Resources from the classpath or host filesystem to copy into the container.
     * They can be files or directories that will be copied to the specified
     * destination path inside the container at startup and are immutable (read-only).
     */
    private List<ResourceMapping> resources = new ArrayList<>();

    /**
     * Whether the container used in the dev service is reused across multiple
     * applications and application restarts, relying on the Testcontainers
     * reusable containers feature. It requires enabling the feature
     * in the `~/.testcontainers.properties` file. Reused containers
     * are not stopped automatically and must be cleaned up manually.
     * Only applicable in dev mode.
     */
    private boolean reuse = false;

    /**
     * Whether the dev service is shared across multiple applications. When shared, the
     * application connects to a container started by another application if one is
     * available, instead of starting a new one. Only applicable in dev mode.
     */
    private boolean shared = true;

    /**
     * Maximum waiting time for the service to start. Keycloak needs considerably longer
     * than most services, since it boots and imports the configured realms before
     * reporting itself as ready.
     */
    private Duration startupTimeout = Duration.ofMinutes(2);

    /**
     * Files or directories to mount from the host filesystem into the container.
     * They are mounted at the specified destination path inside the container
     * at startup and are mutable (read-write). Changes in either the host
     * or the container will be immediately reflected in the other.
     */
    private List<VolumeMapping> volumes = new ArrayList<>();

    /**
     * Username of the Keycloak administrator, used to access the Admin Console.
     */
    private String adminUsername = DEFAULT_ADMIN_USERNAME;

    /**
     * Password of the Keycloak administrator, used to access the Admin Console.
     */
    private String adminPassword = DEFAULT_ADMIN_PASSWORD;

    /**
     * Configuration of the realm the application connects to.
     */
    private Realm realm = new Realm();

    /**
     * Configuration of the OAuth2 client the application authenticates as.
     */
    private Client client = new Client();

    /**
     * Ports on the host that Keycloak can reach from inside its container. Only needed
     * when Keycloak initiates calls to the application, such as OpenID Connect
     * Back-Channel Logout; the ordinary login flow does not require it, since the
     * browser performs the redirect.
     */
    private List<Integer> hostAccessiblePorts = new ArrayList<>();

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    @Override
    public Map<String, String> getEnvironment() {
        return environment;
    }

    public void setEnvironment(Map<String, String> environment) {
        this.environment = environment;
    }

    @Override
    public List<String> getNetworkAliases() {
        return networkAliases;
    }

    public void setNetworkAliases(List<String> networkAliases) {
        this.networkAliases = networkAliases;
    }

    @Override
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public List<ResourceMapping> getResources() {
        return resources;
    }

    public void setResources(List<ResourceMapping> resources) {
        this.resources = resources;
    }

    @Override
    public boolean isReuse() {
        return reuse;
    }

    public void setReuse(boolean reuse) {
        this.reuse = reuse;
    }

    @Override
    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }

    @Override
    public Duration getStartupTimeout() {
        return startupTimeout;
    }

    public void setStartupTimeout(Duration startupTimeout) {
        this.startupTimeout = startupTimeout;
    }

    @Override
    public List<VolumeMapping> getVolumes() {
        return volumes;
    }

    public void setVolumes(List<VolumeMapping> volumes) {
        this.volumes = volumes;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public Realm getRealm() {
        return realm;
    }

    public void setRealm(Realm realm) {
        this.realm = realm;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public List<Integer> getHostAccessiblePorts() {
        return hostAccessiblePorts;
    }

    public void setHostAccessiblePorts(List<Integer> hostAccessiblePorts) {
        this.hostAccessiblePorts = hostAccessiblePorts;
    }

    /**
     * Configuration of the realm the application connects to.
     * <p>
     * A realm comes from one of two places. Either it is generated from this configuration,
     * which is the default and covers the common case of wanting a few users with a few roles,
     * or it is imported from realm files, which takes over completely for realms too elaborate
     * to express here. Generation can also be turned off altogether, leaving Keycloak with only
     * its `master` realm for an application that configures Spring Security itself.
     */
    public static class Realm {

        /**
         * Name of the realm the application connects to. When empty, it is derived from the
         * first realm import file, or defaults to `arconia` when no realm file is configured.
         * Set it explicitly to connect to a realm this application does not import itself,
         * such as one imported by another application sharing the same dev service.
         */
        private String name = "";

        /**
         * Whether a realm is generated from this configuration. When disabled and no realm
         * import path is configured, Keycloak starts with only its `master` realm and the dev
         * service contributes no OAuth2 configuration, leaving the application's own Spring
         * Security configuration untouched.
         */
        private boolean create = true;

        /**
         * Paths to Keycloak realm files to import at startup, resolved from the classpath
         * or the host filesystem. A path can be prefixed with `classpath:` or `file:` to
         * select the location explicitly; otherwise the classpath is searched first and
         * the host filesystem second. When set, the realm files are imported as they are
         * and no realm is generated.
         */
        private List<String> importPaths = new ArrayList<>();

        /**
         * Users to create in the generated realm, as a map of username to user. A user whose
         * password is all it needs can be given as the password alone, as in
         * `alice: alice-password`. When empty, two default users are created: `isabella`
         * and `bjorn`.
         */
        private Map<String,User> users = new LinkedHashMap<>();

        /**
         * Roles to create in the generated realm, as a map of role name to the usernames
         * holding it. A role with no users is created but assigned to nobody. When both this
         * and `users` are empty, the default `admin` and `user` roles are created.
         */
        private Map<String,List<String>> roles = new LinkedHashMap<>();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isCreate() {
            return create;
        }

        public void setCreate(boolean create) {
            this.create = create;
        }

        public List<String> getImportPaths() {
            return importPaths;
        }

        public void setImportPaths(List<String> importPaths) {
            this.importPaths = importPaths;
        }

        public Map<String,User> getUsers() {
            return users;
        }

        public void setUsers(Map<String,User> users) {
            this.users = users;
        }

        public Map<String,List<String>> getRoles() {
            return roles;
        }

        public void setRoles(Map<String,List<String>> roles) {
            this.roles = roles;
        }

        /**
         * A user of the generated realm.
         * <p>
         * Only the password is required, which is why the compact form
         * `alice: alice-password` is accepted alongside the full one: {@link #of(String)} is the
         * static factory Spring's conversion service looks for when binding a scalar to an
         * object, so a user that needs nothing but a password stays a single line.
         * <p>
         * The remaining fields are an identity, not embellishment. Keycloak's default user
         * profile marks the email address and both names as required, and refuses to issue a
         * token for a user missing any of them with the decidedly unhelpful
         * `Account is not fully set up`. Leaving them empty is therefore not the same as leaving
         * them out: the dev service derives them from the username instead, so the compact form
         * still produces a user that can log in.
         * <p>
         * Roles are deliberately absent. They are configured the other way round, under `roles`,
         * mapping a role to the users holding it, which is the direction one usually thinks in
         * and keeps each assignment in a single place.
         */
        public static class User {

            /**
             * Password of the user. Required.
             */
            private String password = "";

            /**
             * First name of the user. When empty, it is derived from the username.
             */
            private String firstName = "";

            /**
             * Last name of the user. When empty, it is derived from the username.
             */
            private String lastName = "";

            /**
             * Email address of the user. When empty, it is derived from the username.
             */
            private String email = "";

            /**
             * A user with the given password and an identity derived from its username, which is
             * what the compact configuration form binds to.
             */
            public static User of(String password) {
                User user = new User();
                user.setPassword(password);
                return user;
            }

            public String getPassword() {
                return password;
            }

            public void setPassword(String password) {
                this.password = password;
            }

            public String getFirstName() {
                return firstName;
            }

            public void setFirstName(String firstName) {
                this.firstName = firstName;
            }

            public String getLastName() {
                return lastName;
            }

            public void setLastName(String lastName) {
                this.lastName = lastName;
            }

            public String getEmail() {
                return email;
            }

            public void setEmail(String email) {
                this.email = email;
            }

        }

    }

    /**
     * Configuration of the OAuth2 client the application authenticates as.
     * <p>
     * The client governs two things that are easy to conflate. It is declared in the generated
     * realm from {@code id} and {@code secret}, which applies to every application, including one
     * that is purely a resource server: a realm needs a client for tokens to be minted against,
     * whether or not this application performs the login. And when Spring Security's OAuth2
     * Client is on the classpath, the same values back the client registration the dev service
     * registers with Spring Security, so the client declared in the realm and the one the
     * application authenticates as are the same by construction.
     */
    public static class Client {

        /**
         * The client registered in the realm generated when no realm import path is configured.
         */
        public static final String DEFAULT_CLIENT_ID = "arconia-app";

        /**
         * The secret of the client registered in the realm generated when no realm import path
         * is configured.
         */
        public static final String DEFAULT_CLIENT_SECRET = "arconia-secret";

        /**
         * The scopes requested when none are configured. The `openid` scope is what makes the
         * flow an OpenID Connect login rather than a plain OAuth2 authorization.
         */
        public static final List<String> DEFAULT_SCOPES = List.of("openid", "profile", "email");

        /**
         * Whether the dev service declares a client. When disabled, the client is left out of
         * the generated realm and no Spring Security client registration is registered. The two
         * always agree, because a registration pointing at a client that was never created is
         * worse than no registration at all.
         */
        private boolean enabled = true;

        /**
         * Identifier of the Keycloak client the application authenticates as. It must exist in
         * the realm in use, which is a given for the generated realm, and the reason this must
         * be set explicitly when realm files are imported: the clients a realm file declares are
         * not inspected.
         */
        private String id = DEFAULT_CLIENT_ID;

        /**
         * Secret of the Keycloak client the application authenticates with. Leave it empty for
         * a public client, in which case the client is registered with no client authentication
         * instead.
         */
        private String secret = DEFAULT_CLIENT_SECRET;

        /**
         * Identifier of the Spring Security client registration the dev service registers. It
         * appears in the login URL, as in `/oauth2/authorization/{registration-id}`.
         */
        private String registrationId = "keycloak";

        /**
         * Scopes the client registration requests.
         */
        private List<String> scopes = new ArrayList<>(DEFAULT_SCOPES);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public String getRegistrationId() {
            return registrationId;
        }

        public void setRegistrationId(String registrationId) {
            this.registrationId = registrationId;
        }

        public List<String> getScopes() {
            return scopes;
        }

        public void setScopes(List<String> scopes) {
            this.scopes = scopes;
        }

    }

}
