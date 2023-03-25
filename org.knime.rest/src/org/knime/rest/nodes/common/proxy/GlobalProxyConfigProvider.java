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
 *   8 Mar 2023 (leon.wenzler): created
 */
package org.knime.rest.nodes.common.proxy;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

/**
 * Accesses Java's System.properties to retrieve proxy-relevant properties.
 * Returns every property as Optional<String>.
 *
 * @author Leon Wenzler, KNIME AG, Konstanz, Germany
 */
final class GlobalProxyConfigProvider {

    // Keys used to identify System properties.

    static final String HOST_KEY = "proxyHost";

    static final String PORT_KEY = "proxyPort";

    static final String USER_KEY = "proxyUser";

    static final String PASSWORD_KEY = "proxyPassword";

    static final String EXCLUDED_HOSTS_KEY = "nonProxyHosts";

    /**
     * Determine which protocol of system proxy properties should be queried.
     *
     * @return ProxyProtocol enum if present
     */
    static Optional<ProxyProtocol> getProtocol() {
        for (var p : ProxyProtocol.values()) {
            // It's discouraged to use the "proxySet" property, checking for a valid host is more robust.
            if (p.getSystemProperty(HOST_KEY).filter(StringUtils::isNotBlank).isPresent()) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }

    private static Optional<String> getProperty(final String key) {
        return getProtocol().flatMap(p -> p.getSystemProperty(key));
    }

    /**
     * Returns the host value that is present with a certain protocol. The protocol can be retrieved via
     * {@link GlobalProxyConfigProvider#getProtocol()}.
     *
     * @return Proxy host address
     */
    static Optional<String> getHost() {
        return getProperty(HOST_KEY);
    }

    /**
     * Returns the port value that is present with a certain protocol. The protocol can be retrieved via
     * {@link GlobalProxyConfigProvider#getProtocol()}.
     *
     * @return Proxy port
     */
    static Optional<String> getPort() {
        return getProperty(PORT_KEY);
    }

    /**
     * Whether to use authentication, based on the presence of the property.
     *
     * @return Use proxy authentication?
     */
    static boolean useAuthentication() {
        return getProperty(USER_KEY).isPresent() && getProperty(PASSWORD_KEY).isPresent();
    }

    /**
     * Returns the user name that is present with a certain protocol. The protocol can be retrieved via
     * {@link GlobalProxyConfigProvider#getProtocol()}.
     *
     * @return Proxy user name
     */
    static Optional<String> getUsername() {
        return getProperty(USER_KEY);
    }

    /**
     * Returns the proxy password that is present with a certain protocol. The protocol can be retrieved via
     * {@link GlobalProxyConfigProvider#getProtocol()}.
     *
     * @return Proxy password
     */
    static Optional<String> getPassword() {
        return getProperty(PASSWORD_KEY);
    }

    /**
     * Returns the excluded hosts value that is present with a certain protocol. The protocol can be retrieved via
     * {@link GlobalProxyConfigProvider#getProtocol()}.
     *
     * @return Proxy-excluded hosts
     */
    static Optional<String> getExcludedHosts() {
        return getProperty(EXCLUDED_HOSTS_KEY);
    }

    /**
     * Whether to use excluding hosts, based on the presence of the property.
     *
     * @return Use proxy-excluded hosts?
     */
    static boolean useExcludedHosts() {
        return getProperty(EXCLUDED_HOSTS_KEY).map(StringUtils::isNotBlank).orElse(false);
    }

    /**
     * Hides the constructor.
     */
    private GlobalProxyConfigProvider() {
    }
}
