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
 *   Aug 20, 2024 (lw): created
 */
package org.knime.core.util.proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.net.URI;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * 'Tinyproxy' context factory that creates {@link GlobalProxyConfig} objects,
 * based on running sidecars in the current test context.
 *
 * @see "https://github.com/tinyproxy/tinyproxy"
 */
public class TinyproxyTestContext {

    private static final Pattern PROXY_PATTERN = Pattern
        .compile("^(?:(?<protocol>\\w+)://)?(?:(?<user>.*?)(?::(?<password>.*?))?@)?(?<host>.+?):(?<port>\\d+)/?$");

    private static final String KNIME_TINYPROXY = "KNIME_TINYPROXY";

    private static final String KNIME_TINYPROXYAUTH = "KNIME_TINYPROXYAUTH";

    private static final String KNIME_TINYPROXYSTATS = "KNIME_TINYPROXYSTATS";

    /**
     * Using the magical {@link #PROXY_PATTERN} RegEx to parse out fields for creating a {@link GlobalProxyConfig}
     * configuration record.
     *
     * @param protocol the proxy protocol, necessary for constructing the config object
     * @param key the key of the proxy environment variable
     * @return {@link GlobalProxyConfig} representing the variable value
     */
    private static GlobalProxyConfig parseAsGlobalProxyConfig(final ProxyProtocol protocol, final String key) {
        // (1) Extract environment variable, from dynamically-created sidecars.
        final var proxy = System.getenv(key);

        // (2) Parse proxy value, optionally containing credentials.
        String host;
        String port;
        String user;
        String password;
        final var matcher = PROXY_PATTERN.matcher(proxy);
        if (matcher.matches() && StringUtils.isNotEmpty(host = matcher.group("host"))) {
            port = assertNonNegativePort(matcher.group("port"));
            user = matcher.group("user");
            password = matcher.group("password");

            // (3) Convert parsed fields to our `GlobalProxyConfig`.
            return new GlobalProxyConfig( //
                protocol, host, port, StringUtils.isNotEmpty(user), user, password, false, "");
        }

        throw new IllegalArgumentException("Could not parse proxy from environment variable \"%s\"".formatted(key));
    }

    private static String assertNonNegativePort(final String stringPort) {
        if (StringUtils.isEmpty(stringPort)) {
            return null;
        }
        return assertDoesNotThrow(() -> {
            final var intPort = Integer.parseInt(stringPort);
            // a negative port is invalid for a URI, it can either be empty (e.g. "http://knime.com:")
            // or must be present; we choose 'null' as placeholder which will be parsed correctly in #intPort()
            return intPort < 0 ? null : String.valueOf(intPort);
        }, "Could not to parse proxy port, thus failing to create a configuration");
    }

    private static void assertNonNullEnvVar(final String key) {
        assertThat(System.getenv(key)) //
            .as("Expected environment variable \"%s\" to be non-null", key) //
            .isNotNull();
    }

    /**
     * Returns the {@link GlobalProxyConfig} from a 'Tinyproxy' sidecar container, not requiring user authentication.
     *
     * @param protocol the proxy protocol
     * @return {@link GlobalProxyConfig}
     */
    public static GlobalProxyConfig getWithoutAuth(final ProxyProtocol protocol) {
        assertNonNullEnvVar(KNIME_TINYPROXY);
        return assertDoesNotThrow(() -> parseAsGlobalProxyConfig(protocol, KNIME_TINYPROXY),
            "Proxy configuration (without auth) could not be extracted from the environment");
    }

    /**
     * Returns the {@link GlobalProxyConfig} from a 'Tinyproxy' sidecar container, which *does* require user
     * authentication.
     *
     * @param protocol the proxy protocol
     * @return {@link GlobalProxyConfig}
     */
    public static GlobalProxyConfig getWithAuth(final ProxyProtocol protocol) {
        assertNonNullEnvVar(KNIME_TINYPROXYAUTH);
        return assertDoesNotThrow(() -> parseAsGlobalProxyConfig(protocol, KNIME_TINYPROXYAUTH),
            "Proxy configuration (with auth) could not be extracted from the environment");
    }

    /**
     * Get {@link URI} of 'Tinyproxy' statistics (valid for both containers, with and without authentication). Next to
     * listing statistics, this host is useful for testing since it is only available behind the proxy.
     *
     * @return {@link URI} pointing to the 'Tinyproxy' statistics host
     */
    public static URI getStatsURI() {
        assertNonNullEnvVar(KNIME_TINYPROXYSTATS);
        return assertDoesNotThrow(() -> URI.create(System.getenv(KNIME_TINYPROXYSTATS)),
            "Could not parse stats URI of the Tinyproxy instance from the environment");
    }
}
