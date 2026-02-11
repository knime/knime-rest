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
 *   14 Mar 2023 (leon.wenzler): created
 */
package org.knime.rest.nodes.common.proxy;

import java.util.Locale;
import java.util.Optional;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.Strings;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.proxy.GlobalProxyConfig;
import org.knime.core.util.proxy.ProxyProtocol;
import org.knime.core.util.proxy.testing.TinyproxyTestContext;

/**
 * Allows to set and reset dummy system properties. More specifically, allows to CLEAR, make BLANK, or provide DUMMY
 * values for a subset of Java System properties.
 * Used for temporarily setting JVM-wide proxy properties for proxy tests.
 *
 * @author Leon Wenzler, KNIME AG, Konstanz, Germany
 */
final class SystemPropertyProvider {

    static final ProxyProtocol DEFAULT_PROTOCOL = ProxyProtocol.HTTP;

    enum PropertyMode {
            CLEAR, BLANK, DUMMY, TINYPROXY;
    }

    static String[] saveAndSetProperties(final String[] properties, final PropertyMode mode) {
        // Any dummy value works, here "1" is useful because some system properties get parsed into a number
        // and this does not immediately raise a NumberFormatException.
        return saveAndSetProperties(properties, mode, "1");
    }

    /**
     * Depending on the property mode, the given property keys are exchanged for their dummy values.
     * Returns the previously set values as String array.
     *
     * @param properties Property keys to exchange
     * @param mode How should the properties be exchanged?
     * @param dummyValue The replacement dummy value
     * @return Previously set properties
     */
    static String[] saveAndSetProperties(final String[] properties, final PropertyMode mode, final String dummyValue) {
        var values = new String[properties.length];
        for (int i = 0; i < properties.length; i++) {
            var p = properties[i];
            values[i] = System.getProperty(p);
            switch (mode) {
                case TINYPROXY:
                    System.setProperty(p, getTinyproxyProperty(p).orElse(dummyValue));
                    break;
                case DUMMY:
                    System.setProperty(p, dummyValue);
                    break;
                case BLANK:
                    System.setProperty(p, "");
                    break;
                case CLEAR:
                    System.clearProperty(p);
                    break;
            }
        }
        return values;
    }

    private static Optional<String> getTinyproxyProperty(final String property) {
        var parts = property.split("\\.", 2);
        if (parts.length < 2) {
            // must be SOCKS, e.g. the "socksProxyHost" property
            parts = new String[]{ //
                property.substring(0, 5), //
                property.substring(5) //
            };
        }
        final var protocol = EnumUtils.getEnumIgnoreCase(ProxyProtocol.class, //
            parts[0].toUpperCase(Locale.ENGLISH), DEFAULT_PROTOCOL);
        final GlobalProxyConfig proxy;
        if (Strings.CI.containsAny(property, "user", "password")) {
            proxy = TinyproxyTestContext.getWithAuth(protocol);
        } else {
            proxy = TinyproxyTestContext.getWithoutAuth(protocol);
        }
        if ("proxyHost".equalsIgnoreCase(parts[1])) {
            return Optional.of(proxy.host());
        }
        if ("proxyPort".equalsIgnoreCase(parts[1])) {
            // chooses the default port if `proxy#port` is null
            return Optional.of(String.valueOf(proxy.intPort()));
        }
        if (Strings.CI.contains(parts[1], "user")) {
            // null if `proxy#useAuthentication` is false
            return Optional.ofNullable(proxy.username());
        }
        if (Strings.CI.contains(parts[1], "password")) {
            // null if `proxy#useAuthentication` is false
            return Optional.ofNullable(proxy.password());
        }
        return Optional.empty();
    }

    /**
     * Given a set of property keys and corresponding values, this resets the properties to their values.
     * Used in conjunction with {@link SystemPropertyProvider#saveAndSetProperties(String[], PropertyMode, String)}.
     *
     * @param properties Property keys to exchange
     * @param values The original property values
     */
    static void restoreProperties(final String[] properties, final String[] values) {
        CheckUtils.checkArgument(properties.length == values.length, "Argument arrays must have the same length!");
        for (int i = 0; i < properties.length; i++) {
            var value = values[i];
            if (value != null) {
                System.setProperty(properties[i], value);
            } else {
                System.clearProperty(properties[i]);
            }
        }
    }
}
