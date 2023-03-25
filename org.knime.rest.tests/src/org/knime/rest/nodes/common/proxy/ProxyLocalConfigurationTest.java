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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;

import javax.ws.rs.client.ClientBuilder;

import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.knime.core.node.InvalidSettingsException;

/**
 * Test class for the proxy detection in {@link RestProxyConfigManager#configureRequest()}. Tests the correct
 * node-specific proxy configuration.
 *
 * @author Leon Wenzler, KNIME AG, Konstanz, Germany
 */
final class ProxyLocalConfigurationTest {

    private static final String PROXY_HOST = "dummy-host";

    private static final int PROXY_PORT = 65743;

    private static final String PROXY_USER = "dummy-user";

    private static final String PROXY_PW = "dummy-password";

    private static RestProxyConfigManager proxyManager;

    @BeforeAll
    public static void initializeConfigs() {
        proxyManager = RestProxyConfigManager.createDefaultProxyManager();
        proxyManager.setProxyMode(ProxyMode.LOCAL);
    }

    /**
     * Tests if the node-specific proxy is configured correctly by creating dummy RestProxySettings and reading CXF's
     * configured ProxyAuthorization afterwards.
     */
    @SuppressWarnings("static-method")
    @Test
    void testLocalProxyConfigCorrect() throws InvalidSettingsException {
        var proxyConfigCorrect = RestProxyConfig.builder()//
            .setProtocol(ProxyProtocol.HTTP)//
            .setProxyHost(PROXY_HOST)//
            .setProxyPort(PROXY_PORT)//
            .setUseAuthentication(true)//
            .setUsername(PROXY_USER)//
            .setPassword(PROXY_PW)//
            .build();
        // Creating the request template and configuring it.
        var dummyRequest = ClientBuilder.newBuilder().build().target("http://localhost").request();
        assertDoesNotThrow(
            () -> proxyManager.configureRequest(Optional.of(proxyConfigCorrect), dummyRequest, true, null),
            "Unexpected exception when configuring the REST request with the proxy");
        // Checking of all properties are there.
        var conduit = WebClient.getConfig(dummyRequest).getHttpConduit();
        assertEquals(PROXY_HOST, conduit.getClient().getProxyServer(), "Proxy hosts are not equal");
        assertEquals(PROXY_PORT, conduit.getClient().getProxyServerPort(), "Proxy ports are not equal");
        assertEquals(PROXY_USER, conduit.getProxyAuthorization().getUserName(), "Proxy users are not equal");
        assertEquals(PROXY_PW, conduit.getProxyAuthorization().getPassword(), "Proxy passwords are not equal");
    }

    /**
     * Tests the invalid configuration of no host specified.
     */
    @SuppressWarnings("static-method")
    @Test
    void testLocalProxyConfigNoHost() {
        var proxyConfigNoHost = RestProxyConfig.builder()//
            .setProtocol(ProxyProtocol.HTTP)//
            .setProxyPort(PROXY_PORT)//
            .setUseAuthentication(true)//
            .setUsername(PROXY_USER)//
            .setPassword(PROXY_PW);
        assertThrows(InvalidSettingsException.class, proxyConfigNoHost::build,
            "Configuring the proxy should have failed but hasn't (no ISE)");
    }

    /**
     * Tests the invalid configuration of no port specified.
     */
    @SuppressWarnings("static-method")
    @Test
    void testLocalProxyConfigNoPort() {
        var proxyConfigNoPort = RestProxyConfig.builder()//
            .setProtocol(ProxyProtocol.HTTP)//
            .setProxyHost(PROXY_HOST)//
            .setUseAuthentication(true)//
            .setUsername(PROXY_USER)//
            .setPassword(PROXY_PW);
        assertThrows(InvalidSettingsException.class, proxyConfigNoPort::build,
            "Configuring the proxy should have failed but hasn't (no ISE)");
    }

    /**
     * Tests the invalid configuration of no numeric port specified.
     */
    @SuppressWarnings("static-method")
    @Test
    void testLocalProxyConfigNoNumberPort() {
        var proxyConfigNoNumberPort = RestProxyConfig.builder()//
            .setProtocol(ProxyProtocol.HTTP)//
            .setProxyHost(PROXY_HOST)//
            .setProxyPort("non-numeric-port")//
            .setUseAuthentication(true)//
            .setUsername(PROXY_USER)//
            .setPassword(PROXY_PW);
        assertThrows(InvalidSettingsException.class, proxyConfigNoNumberPort::build,
            "Configuring the proxy should have failed but hasn't (no ISE)");
    }

    /**
     * Tests the invalid configuration of no password specified.
     */
    @SuppressWarnings("static-method")
    @Test
    void testLocalProxyConfigNoPassword() {
        var proxyConfigNoPwd = RestProxyConfig.builder()//
            .setProtocol(ProxyProtocol.HTTP)//
            .setProxyHost(PROXY_HOST)//
            .setProxyPort(PROXY_PORT)//
            .setUseAuthentication(true)//
            .setUsername(PROXY_USER);
        assertThrows(InvalidSettingsException.class, proxyConfigNoPwd::build,
            "Configuring the proxy should have failed but hasn't (no ISE)");
    }

    @AfterAll
    public static void discardConfigs() {
        proxyManager = null;
    }
}
