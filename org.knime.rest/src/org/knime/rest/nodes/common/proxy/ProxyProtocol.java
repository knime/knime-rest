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

import java.util.Locale;
import java.util.Optional;

/**
 * Type of protocol to use for the proxy connection, come with default ports.
 *
 * @author Leon Wenzler, KNIME AG, Konstanz, Germany
 */
public enum ProxyProtocol {
        /**
         * Uses the HTTP protocol to establish a proxy connection.
         * Some also use 3128 as default port.
         */
        HTTP(8080),
        /**
         * Uses the HTTPS protocol to establish a proxy connection. More secure than HTTP, uses SSL additionally.
         * Some also use 3128 as default port.
         */
        HTTPS(8080),
        /**
         * Uses the SOCKS protocol to establish a proxy connection. Defined in RFC 1928, can safely traverse a firewall
         * both at the TCP and UDP level.
         */
        SOCKS(1080);

    private final int m_defaultPort;

    ProxyProtocol(final int defaultPort) {
        m_defaultPort = defaultPort;
    }

    /**
     * Returns the default port per protocol.
     *
     * @return int port
     */
    int getDefaultPort() {
        return m_defaultPort;
    }

    Optional<String> getSystemProperty(final String key) {
        if (key == null) {
            return Optional.empty();
        }

        if (this == ProxyProtocol.SOCKS) {
            // For the SOCKS protocol, keys do not contain a dot and are in camel-case.
            final var protocolKey = asLowerString() + key.substring(0, 1).toUpperCase(Locale.ENGLISH) + key.substring(1);
            return Optional.ofNullable(System.getProperty(protocolKey));
        } else if (GlobalProxyConfigProvider.EXCLUDED_HOSTS_KEY.equals(key)) {
            // According to https://docs.oracle.com/javase/6/docs/technotes/guides/net/proxies.html,
            // the HTTP and HTTPS protocol use the same property for non-proxy-hosts.
            return Optional.ofNullable(System.getProperty("http." + key));
        }
        return Optional.ofNullable(System.getProperty(asLowerString() + "." + key));
    }

    /**
     * String which can be used as a prefix for System properties.
     *
     * @return protocol string
     */
    String asLowerString() {
        return name().toLowerCase(Locale.ENGLISH);
    }
}
