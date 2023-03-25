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

import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.util.ButtonGroupEnumInterface;

/**
 * Are the proxy settings taken from the AP's global settings, configured and set by a dialog or should no proxy be
 * used?
 *
 * @author Leon Wenzler, KNIME AG, Konstanz, Germany
 */
public enum ProxyMode implements ButtonGroupEnumInterface {
        /**
         * Takes the proxy configuration from the KNIME platform. This is passed via Java System.properties, and
         * implemented in {@link GlobalProxyConfiguration}.
         */
        GLOBAL("Use KNIME-wide proxy settings", "The node uses the same proxy as the KNIME platform."),
        /**
         * Uses local, node-specific proxy settings. These are stored in {@link RestProxyConfig}.
         */
        LOCAL("Use node-specific proxy settings", "The node uses the proxy configured below."),
        /**
         * Bypasses all proxies and uses a direction connection.
         */
        NONE("Direct connection", "The node does not use a proxy for its connection.");

    private final String m_text;

    private final String m_tooltip;

    ProxyMode(final String text, final String tooltip) {
        m_text = text;
        m_tooltip = tooltip;
    }

    /**
     * Identifies the proxy mode from the settings.
     *
     * @param settings NodeSettings
     * @return ProxyMode
     */
    public static ProxyMode fromSettings(final ConfigBaseRO settings) {
        var proxyEnabled = settings.getBoolean(RestProxyConfigManager.USE_PROXY_KEY, true);
        var containProxySettings = settings.containsKey(RestProxyConfig.PROXY_SETTINGS_KEY);
        if (proxyEnabled) {
            return containProxySettings ? ProxyMode.LOCAL : ProxyMode.GLOBAL;
        }
        return ProxyMode.NONE;
    }

    @Override
    public String getText() {
        return m_text;
    }

    @Override
    public String getActionCommand() {
        return name();
    }

    @Override
    public String getToolTip() {
        return m_tooltip;
    }

    @Override
    public boolean isDefault() {
        return this == GLOBAL;
    }
}
