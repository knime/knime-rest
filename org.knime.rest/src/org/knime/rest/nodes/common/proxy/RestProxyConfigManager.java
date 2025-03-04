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
 *   28 Feb 2023 (leon.wenzler): created
 */
package org.knime.rest.nodes.common.proxy;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.transports.http.configuration.ProxyServerType;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.util.proxy.GlobalProxyConfig;
import org.knime.core.util.proxy.ProxyProtocol;
import org.knime.core.util.proxy.search.GlobalProxySearch;
import org.knime.rest.internals.BasicAuthentication;
import org.knime.rest.nodes.common.RestNodeDialog;
import org.knime.rest.nodes.common.proxy.RestProxyConfig.RestProxyConfigBuilder;

import jakarta.ws.rs.client.Invocation.Builder;

/**
 * Class managing proxy settings. Depending on the proxy mode, GLOBAL or LOCAL, proxy settings are taken from the System
 * properties or from the stored values. The proxy can also be turned off with the proxy mode NONE.
 *
 * @author Leon Wenzler, KNIME AG, Konstanz, Germany
 */
public final class RestProxyConfigManager {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(RestProxyConfigManager.class);

    static final String USE_PROXY_KEY = "Proxy_enabled";

    static final String PROXY_AUTH_KEY = "Proxy auth";

    private static final ProxyMode DEFAULT_PROXY_MODE = ProxyMode.GLOBAL;

    private ProxyMode m_proxyMode = DEFAULT_PROXY_MODE;

    private Optional<BasicAuthentication> m_authReference = Optional.empty();

    private RestProxyConfigManager() {
    }

    /**
     * Creates default proxy settings which are global and read the System properties's proxy configuration.
     *
     * @return KNIME-wide REST proxy settings
     */
    public static RestProxyConfigManager createDefaultProxyManager() {
        var settings = new RestProxyConfigManager();
        // Make System properties proxy configuration available.
        settings.m_proxyMode = ProxyMode.GLOBAL;
        return settings;
    }

    /**
     * Creates a settings model for the proxy mode selection. Used by the {@link RestNodeDialog}.
     *
     * @return SettingsModel for the proxy mode
     */
    public static SettingsModelString createProxyModeSettingsModel() {
        return new SettingsModelString(USE_PROXY_KEY, DEFAULT_PROXY_MODE.name());
    }

    /**
     * Creates a authentication model with internal settings for the proxy authentication. Used by the
     * {@link RestNodeDialog}.
     *
     * @return Authentication model for the proxy authentication
     */
    public static BasicAuthentication createProxyAuthSettingsModel() {
        final var auth = new BasicAuthentication(PROXY_AUTH_KEY);
        // No password is null, because an empty string is possible.
        auth.setPassword(null);
        return auth;
    }

    /**
     * Use the proxy mode key as necessary identifier that proxy config has been saved to the settings before. Enables
     * backwards compatibility.
     *
     * @return String identifier
     */
    public static String getProxyConfigIdentifier() {
        return USE_PROXY_KEY;
    }

    static <E extends Enum<E>> E safeParseEnum(final Class<E> enumClass, final String value, final E defaultValue) {
        try {
            return Enum.valueOf(enumClass, Objects.requireNonNullElse(value, ""));
        } catch (IllegalArgumentException e) {
            // NPE cannot occur, we make sure value is not null.
            LOGGER.debug(e);
            return defaultValue;
        }
    }

    /**
     * Creates a policy which bypasses proxies for all hosts.
     *
     * @param conduit existing {@link HTTPConduit}, used if non-null
     * @return HTTPClientPolicy
     */
    private static HTTPClientPolicy createProxyBypassPolicy(final HTTPConduit conduit) {
        final var policy = Objects.requireNonNullElseGet(conduit.getClient(), HTTPClientPolicy::new);
        // This policy needs a non-empty host for it to be considered by the web client.
        policy.setProxyServer("non-empty-host");
        // Ignore connections for all hosts.
        policy.setNonProxyHosts("*");
        return policy;
    }

    /**
     * Strips all semicolon-separated hosts and formats them using '|'-separators.
     *
     * @param nonProxyHosts
     * @return formatted hosts
     */
    private static String formatNonProxyHosts(final String nonProxyHosts) {
        if (nonProxyHosts == null) {
            return null;
        }
        var builder = new StringBuilder();
        var excludeHosts = nonProxyHosts.split(";");
        int i;
        for (i = 0; i < excludeHosts.length - 1; i++) {
            builder.append(excludeHosts[i].strip());
            // Non-proxy hosts must be separated by a vertical bar.
            builder.append('|');
        }
        builder.append(excludeHosts[i].strip());
        return builder.toString();
    }

    /**
     * Returns the currently selected proxy mode.
     *
     * @return ProxyMode
     */
    public ProxyMode getProxyMode() {
        return m_proxyMode;
    }

    /**
     * Sets the proxy mode. Either GLOBAL, LOCAL or NONE.
     *
     * @param mode ProxyMode
     */
    public void setProxyMode(final ProxyMode mode) {
        m_proxyMode = mode;
    }

    /**
     * Establishes the connection to the dialog settings model.
     *
     * @param auth
     */
    public void setAuthReference(final BasicAuthentication auth) {
        m_authReference = Optional.of(auth);
    }

    /**
     * Depending on the proxy mode, uses the System properties (GLOBAL) or stored settings (LOCAL) to configure the
     * request builder client if needed. Sets the server, port, user and password to a <code>HTTPClientPolicy</code> and
     * <code>ProxyAuthorizationPolicy</code> and adds them to the client configuration.
     *
     * @param maybeConfig
     * @param request Builder that creates the invocation.
     * @param credsProvider
     * @throws InvalidSettingsException
     */
    public void configureRequest(final Optional<RestProxyConfig> maybeConfig, final Builder request, // NOSONAR
        final CredentialsProvider credsProvider) throws InvalidSettingsException {
        var conduit = WebClient.getConfig(request).getHttpConduit();

        if (maybeConfig.isEmpty()) {
            switch (m_proxyMode) {
                case LOCAL:
                    // Setting the proxy mode to local requires a proxy connection and a non-empty host.
                    throw new InvalidSettingsException(
                        "The proxy host name is blank. If node-specific proxy settings are activated, "
                            + "a host name must be specified.");
                case GLOBAL:
                    // A warning appears once per node execution, see RestNodeModel#execute().
                    return;
                case NONE:
                    // If proxy is inactive, because it is disabled, set bypass policy.
                    conduit.setClient(createProxyBypassPolicy(conduit));
                    return;
            }
        }
        final var config = maybeConfig.orElseThrow(); // must be present

        // Setting proxy credentials.
        config.getProxyTarget().configure(conduit);
        var policy = conduit.getClient();
        policy.setProxyServerType(
            config.getProtocol() == ProxyProtocol.SOCKS ? ProxyServerType.SOCKS : ProxyServerType.HTTP);
        // Previous behavior: explicitly set null when excluded hosts are empty.
        policy.setNonProxyHosts(config.getExcludeHosts() //
            .filter(x -> config.isExcludeHosts()) // set excluded hosts to null if disabled
            .map(RestProxyConfigManager::formatNonProxyHosts) //
            .orElse(null));
        conduit.setClient(policy);

        // Setting authentication data.
        if (config.isUseAuthentication()) {
            final var authentication = config.getAuthentication().orElseThrow(); // must be present
            authentication.configure(conduit, credsProvider);
            // We currently only support basic authorization.
            conduit.getProxyAuthorization().setAuthorizationType("Basic");
        } else {
            // ensure authorization fields are clear when authentication is disabled
            final var authorization = conduit.getProxyAuthorization();
            if (authorization != null) {
                authorization.setUserName(null);
                authorization.setPassword(null);
                authorization.setAuthorization(null);
            }
        }
    }

    // Configuration provider, intertwined with settings saving/loading.

    /**
     * Saves the internal state to {@code settings}.
     *
     * @param config
     *
     * @param settings A writable {@link NodeSettingsWO}.
     */
    public void saveSettings(final RestProxyConfig config, final NodeSettingsWO settings) {
        // Saving empty auth settings to initialize the fields.
        RestProxyConfigManager.createProxyAuthSettingsModel().saveUserConfiguration(settings);
        if (config != null && m_proxyMode == ProxyMode.LOCAL) {
            config.saveSettings(settings);
        }
        // Coming from the RestNodeDialog, the proxy-mode is saved by the proxyModeSettingsModel.
        settings.addBoolean(USE_PROXY_KEY, m_proxyMode != ProxyMode.NONE);
    }

    /**
     * Loads the internal state from {@code settings} for model load.
     *
     * @param settings The read-only {@link NodeSettingsRO}.
     * @return Optional of RestProxyConfig
     * @throws InvalidSettingsException When the state is inconsistent.
     */
    public Optional<RestProxyConfig> loadConfigFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_proxyMode = ProxyMode.fromSettings(settings);
        return getProxyConfig(getLocalConfig(settings, null, (DataTableSpec[])null).orElse(null));
    }

    /**
     * Returns the correct proxy configuration for the current proxy mode.
     *
     * @param localConfig local proxy settings (must be non-{@code null} if {@link #getProxyMode()} is
     *        {@link ProxyMode#LOCAL})
     * @return proxy configuration
     */
    public Optional<RestProxyConfig> getProxyConfig(final RestProxyConfig localConfig) {
        return getProxyConfig(localConfig, null);
    }

    /**
     * Returns the correct proxy configuration for the current proxy mode and the given {@link URI}
     *
     * @param localConfig local proxy settings (must be non-{@code null} if {@link #getProxyMode()} is
     *        {@link ProxyMode#LOCAL})
     * @param uri the URI to select the proxy for
     * @return proxy configuration
     */
    public Optional<RestProxyConfig> getProxyConfig(final RestProxyConfig localConfig, final URI uri) {
        return switch (m_proxyMode) {
            case GLOBAL -> Optional.ofNullable(getGlobalConfig(uri));
            case LOCAL -> Optional.of(localConfig);
            case NONE -> Optional.empty();
        };
    }

    /**
     * Loads the settings for the dialog with defaults.
     *
     * @param settings The read-only {@link NodeSettingsRO}.
     * @param credentialNames
     * @param specs
     * @return Optional of RestProxyConfig
     * @throws InvalidSettingsException
     */
    public Optional<RestProxyConfig> loadConfigForDialog(final NodeSettingsRO settings,
        final CredentialsProvider credentialNames, final PortObjectSpec... specs) throws InvalidSettingsException {
        m_proxyMode = ProxyMode.fromSettings(settings);
        if (m_proxyMode != ProxyMode.LOCAL) {
            // Despite the non-local proxy mode, the auth panel needs to be loaded to update the credentials chooser.
            m_authReference.ifPresent(auth -> {
                try {
                    RestProxyConfigBuilder.loadAuthenticationFromSettings(auth, settings, credentialNames, specs);
                } catch (InvalidSettingsException e) {
                    LOGGER.debug("Could not load proxy authentication from settings", e);
                }
            });
        }
        return getProxyConfig(getLocalConfig(settings, credentialNames, specs).orElse(null));
    }

    /**
     * Loads a global proxy config from {@link GlobalProxySearch}. Package scope for tests.
     *
     * @return RestProxyConfig
     */
    static RestProxyConfig getGlobalConfig() {
        return getGlobalConfig(null);
    }

    /**
     * Loads a global proxy config from {@link GlobalProxySearch#getCurrentFor(URI)}.
     * If the provided {@link URI} is not null, the proxy selection is based on the URI.
     *
     * @param uri the URI to reach via a proxy
     * @return RestProxyConfig
     */
    private static RestProxyConfig getGlobalConfig(final URI uri) {
        // select proxy for URI directly if non-null, otherwise any HTTP(S) proxy
        final var maybeProxyConfig = uri != null //
                ? GlobalProxySearch.getCurrentFor(uri) //
                : GlobalProxySearch.getCurrentFor(ProxyProtocol.HTTP, ProxyProtocol.HTTPS);
        return maybeProxyConfig //
                .map(RestProxyConfigManager::toRestProxyConfig) //
                .filter(Objects::nonNull) //
                .orElse(null);
    }

    /**
     * Converts a {@link GlobalProxyConfig} to a {@link RestProxyConfig} with identical contents.
     * Package scope for tests.
     * <p>
     * Distinction between these configs mainly stems earlier proxy-related development in "org.knime.rest"
     * (than in "org.knime.core.util.proxy") and an additional integration of {@link NodeSettings} for REST nodes.
     * These two config types might be unified at some point.
     * </p>
     *
     * @param config GlobalProxyConfig
     * @return RestProxyConfig
     */
    static RestProxyConfig toRestProxyConfig(final GlobalProxyConfig config) {
        try {
            // convert to RestProxyConfig and validate in #build() step
            final var b = RestProxyConfig.builder();
            b.setProtocol(config.protocol());
            b.setProxyHost(config.host());
            b.setProxyPort(config.port());
            b.setUseAuthentication(config.useAuthentication());
            if (config.useAuthentication()) {
                b.setUsername(config.username());
                b.setPassword(config.password());
            }
            b.setUseExcludeHosts(config.useExcludedHosts());
            if (config.useExcludedHosts()) {
                b.setExcludedHosts(config.excludedHosts());
            }
            return b.build();
        } catch (InvalidSettingsException e) { // NOSONAR
            return null;
        }
    }

    /**
     * Loads a local/node-specific proxy config from settings.
     *
     * @param settings NodeSettings
     * @param useDefaults Whether to use defaults (for node dialog).
     * @return RestProxyConfig
     * @throws InvalidSettingsException
     */
    private Optional<RestProxyConfig> getLocalConfig(final NodeSettingsRO settings,
            final CredentialsProvider credentialNames, final PortObjectSpec... specs) throws InvalidSettingsException {
        if (!settings.containsKey(RestProxyConfig.PROXY_SETTINGS_KEY)) {
            return Optional.empty();
        }

        return Optional.of(RestProxyConfig.builder()//
                .setBasicAuthentication(m_authReference.orElse(null))//
                .useNodeSettings(settings, credentialNames, specs)//
                .build());
    }
}
