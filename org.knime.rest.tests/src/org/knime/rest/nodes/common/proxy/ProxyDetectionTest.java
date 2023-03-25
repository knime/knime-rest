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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;

import javax.ws.rs.client.ClientBuilder;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.rest.nodes.common.proxy.SystemPropertyProvider.PropertyMode;

/**
 * Test class for the proxy detection in {@link RestProxyConfigManager#configureRequest()}. Tests the proxy detection by
 * the proxy settings in edge cases (empty, blank, dummy proxy host).
 *
 * @author Leon Wenzler, KNIME AG, Konstanz, Germany
 */
final class ProxyDetectionTest {

    private static RestProxyConfigManager proxyManager;

    @BeforeAll
    public static void initializeConfig() {
        proxyManager = RestProxyConfigManager.createDefaultProxyManager();
    }

    /**
     * @throws InvalidSettingsException
     */
    @SuppressWarnings("static-method")
    @Test
    void detectNoProxyNonPresent() throws InvalidSettingsException {
        // assumption: a proxy is detected via a valid host
        var props = new String[]{"http.proxyHost"};
        var values = SystemPropertyProvider.saveAndSetProperties(props, PropertyMode.CLEAR);
        var proxyConfig = RestProxyConfigManager.getGlobalConfig();
        assertThat("Proxy host must not be set in the client.", null, is(proxyConfig));
        SystemPropertyProvider.restoreProperties(props, values);
    }

    /**
     * Tests if a proxy is successfully detected as non-present if the System.properties for proxies exist but are
     * blank.
     *
     * @throws InvalidSettingsException
     */
    @SuppressWarnings("static-method")
    @Test
    void detectNoProxyBlank() throws InvalidSettingsException {
        // assumption: a proxy is detected via a valid host
        var props = new String[]{"http.proxyHost"};
        var values = SystemPropertyProvider.saveAndSetProperties(props, PropertyMode.BLANK);
        var proxyConfig = RestProxyConfigManager.getGlobalConfig();
        assertThat("Proxy host must not be set in the client.", null, is(proxyConfig));
        SystemPropertyProvider.restoreProperties(props, values);
    }

    /**
     * Tests if a proxy is detected correctly, on the premise that System.properties are set correctly.
     *
     * @throws InvalidSettingsException
     */
    @SuppressWarnings("static-method")
    @Test
    void detectProxyPresent() throws InvalidSettingsException {
        // setting credentials will only be done if a user/pw was specified
        var props = new String[]{"http.proxyUser", "http.proxyPassword", "http.proxyHost", "http.proxyPort"};
        var values = SystemPropertyProvider.saveAndSetProperties(props, PropertyMode.DUMMY, "1234");
        var proxyConfig = RestProxyConfigManager.getGlobalConfig();
        var dummyRequest = ClientBuilder.newBuilder().build().target("http://localhost").request();
        assertThat("Proxy port must be set to dummy value 1234", 1234, is(proxyConfig.getProxyTarget().port()));
        assertThrows(InvalidSettingsException.class,
            () -> proxyManager.configureRequest(Optional.of(proxyConfig), dummyRequest, false, null),
            "Proxy detection should have thrown an error message because authentication is needed"
                + " although synchronous client is used.");
        SystemPropertyProvider.restoreProperties(props, values);
    }

    @AfterAll
    public static void discardConfig() {
        proxyManager = null;
    }
}
