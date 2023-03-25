/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   15 Mar 2023 (leon.wenzler): created
 */
package org.knime.rest.nodes.common.proxy;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ConvenienceMethods;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.rest.internals.BasicAuthentication;

/**
 * Config class holding all the proxy-relevant properties. Built and validated by the {@link RestProxyConfigBuilder}.
 *
 * @author Leon Wenzler, KNIME AG, Konstanz, Germany
 */
public final class RestProxyConfig {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(RestProxyConfig.class);

    // Keys used for NodeSettings.

    static final String PROXY_SETTINGS_KEY = "Node-specific Proxy";

    private static final String PROTOCOL_KEY = "Proxy protocol";

    private static final String HOST_KEY = "Proxy host";

    private static final String PORT_KEY = "Proxy port";

    private static final String USE_AUTH_KEY = "Use proxy authentication";

    private static final String USE_EXCLUDE_HOSTS_KEY = "Exclude hosts from proxy";

    private static final String EXCLUDED_HOSTS_KEY = "Proxy-excluded hosts";

    // Settings values.

    private static final ProxyProtocol DEFAULT_PROTOCOL = ProxyProtocol.HTTPS;

    private ProxyProtocol m_protocol = DEFAULT_PROTOCOL;

    private ProxyTarget m_proxyTarget;

    private boolean m_isUseAuthentication;

    private Optional<ProxyAuthentication> m_authentication;

    private boolean m_isUseExcludeHosts;

    private String m_excludedHosts;

    private RestProxyConfig() {
    }

    /**
     * Builder for creating a valid and consistent RestProxyConfig.
     *
     * @return RestProxyConfigBuilder
     */
    public static RestProxyConfigBuilder builder() {
        return new RestProxyConfigBuilder();
    }

    /**
     * Returns the proxy protocol.
     *
     * @return ProxyProtocol
     */
    public ProxyProtocol getProtocol() {
        return m_protocol;
    }

    /**
     * Returns the proxy target, specified through host and port.
     *
     * @return proxy host address and server port
     */
    public ProxyTarget getProxyTarget() {
        return m_proxyTarget;
    }

    /**
     * Returns whether authentication is needed for the proxy.
     *
     * @return is authentication needed?
     */
    public boolean isUseAuthentication() {
        return m_isUseAuthentication;
    }

    /**
     * Returns the proxy authentication for Basic proxy authentication. User name and password are either directly
     * stored or retrieved via a CredentialsProvider.
     *
     * @return proxy authentication data
     */
    public Optional<ProxyAuthentication> getAuthentication() {
        return m_authentication;
    }

    /**
     * Returns whether some hosts should be excluded from using the proxy.
     *
     * @return should some hosts be excluded from the proxy?
     */
    public boolean isExcludeHosts() {
        return m_isUseExcludeHosts && StringUtils.isNotBlank(m_excludedHosts);
    }

    /**
     * Returns the excluded hosts, if present.
     *
     * @return excluded hosts
     */
    public Optional<String> getExcludeHosts() {
        return Optional.ofNullable(m_excludedHosts);
    }

    // Settings saving and loading.

    /**
     * Saves the internal state to {@code settings}.
     *
     * @param settings A writable {@link NodeSettingsWO}.
     */
    void saveSettings(final NodeSettingsWO settings) {
        var config = settings.addConfig(PROXY_SETTINGS_KEY);
        config.addString(PROTOCOL_KEY, m_protocol.name());
        config.addString(HOST_KEY, m_proxyTarget.host());
        config.addInt(PORT_KEY, m_proxyTarget.port());

        config.addBoolean(USE_AUTH_KEY, m_isUseAuthentication);
        m_authentication.ifPresent(auth -> {
            final var authSource = auth.getAuthenticationSource();
            authSource.saveUserConfiguration(settings);
            authSource.updateSettings();
        });

        config.addBoolean(USE_EXCLUDE_HOSTS_KEY, m_isUseExcludeHosts);
        config.addString(EXCLUDED_HOSTS_KEY, m_excludedHosts);
    }

    // String history IDs for certain node config panels.

    /** @return proxy host string history ID. */
    public static String getProxyHostStringHistoryID() {
        return getStringHistoryID(HOST_KEY);
    }

    /** @return proxy port string history ID. */
    public static String getProxyPortStringHistoryID() {
        return getStringHistoryID(PORT_KEY);
    }

    /** @return excluded proxy hosts string history ID. */
    public static String getProxyExcludedHostsStringHistoryID() {
        return getStringHistoryID(EXCLUDED_HOSTS_KEY);
    }

    private static String getStringHistoryID(final String field) {
        return RestProxyConfigManager.class.getSimpleName() + "-" + field;
    }

    /**
     * Builds and validates RestProxyConfig. Does not know about the property origins.
     *
     * @author Leon Wenzler, KNIME AG, Konstanz, Germany
     */
    public static final class RestProxyConfigBuilder {

        private ProxyProtocol m_builderProtocol;

        private String m_builderHost;

        private String m_builderPortString;

        private Integer m_builderPortInt;

        private boolean m_builderIsUseAuthentication;

        private BasicAuthentication m_builderBasicAuthentication;

        private boolean m_builderIsUseExcludeHosts;

        private String m_builderExcludedHosts;

        private NodeSettingsRO m_builderNodeSettings;

        private CredentialsProvider m_builderCredentialsProvider;

        private DataTableSpec[] m_builderSpecs;

        private boolean m_useDefaultSettingsValues;

        private RestProxyConfigBuilder() {
        }

        /**
         * Stores a given the proxy protocol in the settings.
         *
         * @param protocol
         * @return builder
         */
        public RestProxyConfigBuilder setProtocol(final ProxyProtocol protocol) {
            m_builderProtocol = protocol;
            return this;
        }

        /**
         * Stores a given the proxy host in the settings.
         *
         * @param host
         * @return builder
         */
        public RestProxyConfigBuilder setProxyHost(final String host) {
            m_builderHost = host;
            return this;
        }

        /**
         * Stores a given the proxy port in the settings.
         *
         * @param port
         * @return builder
         */
        public RestProxyConfigBuilder setProxyPort(final String port) {
            m_builderPortString = port;
            return this;
        }

        /**
         * Stores a given the proxy port in the settings.
         *
         * @param port
         * @return builder
         */
        public RestProxyConfigBuilder setProxyPort(final int port) {
            m_builderPortInt = port;
            return this;
        }

        /**
         * Whether to use proxy authentication.
         *
         * @param use
         * @return builder
         */
        public RestProxyConfigBuilder setUseAuthentication(final boolean use) {
            m_builderIsUseAuthentication = use;
            return this;
        }

        /**
         * Sets the {@link BasicAuthentication} object.
         *
         * @param auth
         * @return builder
         */
        public RestProxyConfigBuilder setBasicAuthentication(final BasicAuthentication auth) {
            m_builderBasicAuthentication = auth;
            return this;
        }

        /**
         * Stores the user name for node-specific proxy settings.
         *
         * @param username
         * @return builder
         */
        public RestProxyConfigBuilder setUsername(final String username) {
            m_builderBasicAuthentication = Objects.requireNonNullElseGet(m_builderBasicAuthentication,
                RestProxyConfigManager::createProxyAuthSettingsModel);
            m_builderBasicAuthentication.setUsername(username);
            return this;
        }

        /**
         * Stores the password for node-specific proxy settings.
         *
         * @param password
         * @return builder
         */
        public RestProxyConfigBuilder setPassword(final String password) {
            m_builderBasicAuthentication = Objects.requireNonNullElseGet(m_builderBasicAuthentication,
                RestProxyConfigManager::createProxyAuthSettingsModel);
            m_builderBasicAuthentication.setPassword(password);
            return this;
        }

        /**
         * Stores the credentials ID for node-specific proxy settings.
         *
         * @param credentialsId
         * @return builder
         */
        public RestProxyConfigBuilder setCredentialsId(final String credentialsId) {
            m_builderBasicAuthentication = Objects.requireNonNullElseGet(m_builderBasicAuthentication,
                RestProxyConfigManager::createProxyAuthSettingsModel);
            m_builderBasicAuthentication.setCredential(credentialsId);
            // If credentials are set, use them.
            m_builderBasicAuthentication.setUseCredentials(true);
            return this;
        }

        /**
         * Whether to use exclude some hosts.
         *
         * @param use
         * @return builder
         */
        public RestProxyConfigBuilder setUseExcludeHosts(final boolean use) {
            m_builderIsUseExcludeHosts = use;
            return this;
        }

        /**
         * Stores whether some proxy hosts should be excluded in the settings.
         *
         * @param excludedHosts
         * @return builder
         */
        public RestProxyConfigBuilder setExcludedHosts(final String excludedHosts) {
            m_builderExcludedHosts = excludedHosts;
            return this;
        }

        /**
         * Whether to load the properties from the NodeSettings, then build.
         *
         * @param settings
         * @param credentialNames
         * @param specs
         * @return builder
         */
        public RestProxyConfigBuilder useNodeSettings(final NodeSettingsRO settings,
            final CredentialsProvider credentialNames, final DataTableSpec... specs) {
            m_builderNodeSettings = settings;
            m_builderCredentialsProvider = credentialNames;
            m_builderSpecs = specs;
            m_useDefaultSettingsValues = specs != null;
            return this;
        }

        /**
         * Validates the set properties, then builds the RestProxyConfig. If the state is inconsistent/invalid, an
         * InvalidSettingsException is thrown.
         *
         * @return RestProxyConfig
         * @throws InvalidSettingsException
         */
        public RestProxyConfig build() throws InvalidSettingsException {
            if (m_builderNodeSettings != null) {
                loadFromSettings();
                if (m_useDefaultSettingsValues) {
                    fillEmptyFieldsWithDefaults();
                }
            }

            if (m_builderProtocol == null) {
                throw new InvalidSettingsException(
                    "If node-specific proxy settings are activated, a protocol must be specified. Select one of "
                        + ConvenienceMethods.getShortStringFrom(Arrays.stream(ProxyProtocol.values()).iterator(), 3, 3)
                        + ".");
            }
            var config = new RestProxyConfig();
            config.m_protocol = m_builderProtocol;

            config.m_proxyTarget = buildProxyTarget();
            config.m_authentication = buildProxyAuthentication();
            config.m_isUseAuthentication = m_builderIsUseAuthentication;

            // If excluded hosts were specified, register them also.
            if (StringUtils.isNotBlank(m_builderExcludedHosts)) {
                config.m_excludedHosts = m_builderExcludedHosts;
                config.m_isUseExcludeHosts = m_builderIsUseExcludeHosts;
            } else {
                if (m_builderIsUseExcludeHosts) {
                    // Exclude hosts field was not filled, but the box checked.
                    throw new InvalidSettingsException("The proxy excluding hosts is selected "
                        + "but no hosts to exclude were specified. Enter some proxy hosts.");
                }
                config.m_isUseExcludeHosts = false;
            }
            return config;
        }

        private void loadFromSettings() throws InvalidSettingsException {
            var config = m_builderNodeSettings.getConfig(PROXY_SETTINGS_KEY);
            m_builderProtocol = RestProxyConfigManager.safeParseEnum(ProxyProtocol.class,
                config.getString(PROTOCOL_KEY, null), m_builderProtocol);
            m_builderHost = config.getString(HOST_KEY, m_builderHost);
            m_builderPortInt = config.getInt(PORT_KEY, Objects.requireNonNullElse(m_builderPortInt, -1));
            m_builderIsUseAuthentication = config.getBoolean(USE_AUTH_KEY, m_builderIsUseAuthentication);
            m_builderBasicAuthentication = loadAuthenticationFromSettings(m_builderBasicAuthentication,
                m_builderNodeSettings, m_builderCredentialsProvider, m_builderSpecs);
            m_builderIsUseExcludeHosts = config.getBoolean(USE_EXCLUDE_HOSTS_KEY, m_builderIsUseExcludeHosts);
            m_builderExcludedHosts = config.getString(EXCLUDED_HOSTS_KEY, m_builderExcludedHosts);
        }

        static BasicAuthentication loadAuthenticationFromSettings(BasicAuthentication auth,
            final NodeSettingsRO settings, final CredentialsProvider credentialNames, final DataTableSpec[] specs)
            throws InvalidSettingsException {
            if (!settings.containsKey(RestProxyConfigManager.PROXY_AUTH_KEY)) {
                return null;
            }
            auth = Objects.requireNonNullElseGet(auth, RestProxyConfigManager::createProxyAuthSettingsModel);
            // Load either for dialog or for model.
            if (credentialNames != null && specs != null) {
                try {
                    auth.loadUserConfigurationForDialog(settings, specs, credentialNames);
                } catch (NotConfigurableException e) {
                    throw new InvalidSettingsException(e);
                }
            } else {
                auth.loadUserConfiguration(settings);
            }
            auth.updateControls();
            return auth;
        }

        private void fillEmptyFieldsWithDefaults() {
            var defaults = new StringBuilder();
            if (m_builderProtocol == null) {
                m_builderProtocol = ProxyProtocol.HTTP;
                defaults.append("protocol=" + m_builderProtocol.name());
                defaults.append(" ");
            }
            if (StringUtils.isBlank(m_builderHost)) {
                m_builderHost = "localhost";
                defaults.append("host=" + m_builderHost);
                defaults.append(" ");
            }
            if (m_builderPortInt == null || m_builderPortInt <= 0) {
                m_builderPortInt = m_builderProtocol.getDefaultPort();
                defaults.append("port=" + m_builderPortInt);
                defaults.append(" ");
            }
            if (!defaults.isEmpty()) {
                LOGGER.debug(
                    String.format("Proxy configuration was filled with these defaults: [ %s]", defaults.toString()));
            }
        }

        /**
         * Creates a valid {@link ProxyTarget}.
         *
         * @return ProxyTarget
         * @throws InvalidSettingsException
         */
        private ProxyTarget buildProxyTarget() throws InvalidSettingsException {
            final var mustbeFilledWarning = "If node-specific proxy settings are activated, a %s must be specified.";

            // Checking the host and port to create a valid proxy target.
            if (StringUtils.isBlank(m_builderHost)) {
                throw new InvalidSettingsException(
                    "The proxy host name is blank. " + mustbeFilledWarning.formatted("host name"));
            }
            if (m_builderPortInt == null || m_builderPortInt <= 0) {
                try {
                    if (StringUtils.isBlank(m_builderPortString)) {
                        throw new InvalidSettingsException(
                            "The proxy port is blank. " + mustbeFilledWarning.formatted("port"));
                    }
                    m_builderPortInt = Integer.parseInt(m_builderPortString);
                } catch (NumberFormatException e) {
                    throw new InvalidSettingsException(
                        String.format("The port input \"%s\" does not represent a number. Enter a valid input.",
                            m_builderPortString));
                }
            }
            return new ProxyTarget(m_builderHost, m_builderPortInt);
        }

        /**
         * Creates a valid {@link ProxyAuthentication} from settings..
         *
         * @return ProxyAuthentication.
         * @throws InvalidSettingsException
         */
        private Optional<ProxyAuthentication> buildProxyAuthentication() throws InvalidSettingsException {
            if (!m_builderIsUseAuthentication || m_builderBasicAuthentication == null) {
                LOGGER.debug("Proxy configuration does not use authentication.");
                return Optional.empty();
            }
            // Checking the authentication entries to create a valid proxy authentication.
            var userSet = StringUtils.isNotEmpty(m_builderBasicAuthentication.getUsername());
            var passwordSet = m_builderBasicAuthentication.getPassword() != null;
            var credsSet = StringUtils.isNotEmpty(m_builderBasicAuthentication.getCredential());
            if (userSet && passwordSet && credsSet) {
                throw new InvalidSettingsException(
                    "Both user name and password, and a credentials ID have been specified. "
                        + "Enter only one option to select a proxy authentication type.");
            }

            // Configuring types of proxy authentication.
            if (credsSet) {
                return Optional.of(new CredentialsAuthentication(m_builderBasicAuthentication));
            }
            if (userSet) {
                if (!passwordSet) {
                    // Username has been specified but without as password.
                    throw new InvalidSettingsException(
                        "A user name was specified but a password was not. Enter both to use proxy authentication.");
                }
                return Optional.of(new UserPasswordAuthentication(m_builderBasicAuthentication));
            }
            if (passwordSet) {
                // Password has been specified but without as username.
                throw new InvalidSettingsException(
                    "A password was specified but a user name was not. Enter both to use proxy authentication.");
            }

            // No authentication field was filled, but the auth box checked.
            throw new InvalidSettingsException("The proxy using authentication is selected "
                + "but no user/password/credentials field is filled. Enter some authentication data.");
        }
    }
}
