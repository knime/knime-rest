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
 *   10 May 2023 (leon.wenzler): created
 */
package org.knime.rest.nodes.common.proxy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.knime.core.data.DataCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.util.proxy.ProxyProtocol;
import org.knime.rest.nodes.common.HttpMockServiceFactory;
import org.knime.rest.nodes.common.PassthroughMarker;
import org.knime.rest.nodes.common.ProxyRestNodeModel;

import com.github.tomakehurst.wiremock.WireMockServer;

/**
 * Uses the mock implementations for the proxy target and proxy hosts with a ProxyMode.LOCAL configuration.
 *
 * @author Leon Wenzler, KNIME AG, Konstanz, Germany
 */
final class MockProxyTest {

    // -- SERVERS --

    private static WireMockServer mockRequestProxy;

    private static WireMockServer mockRequestServer;

    // -- CLIENTS --

    private static ProxyRestNodeModel noProxyNodeModel;

    private static ProxyRestNodeModel configuredProxyNodeModel;

    @BeforeAll
    public static void initializeMockObjects() throws InvalidSettingsException {
        // Setting up the forwarding proxy and mocked server target.
        mockRequestProxy = HttpMockServiceFactory.createForwardingProxy();
        mockRequestProxy.start();
        mockRequestServer = HttpMockServiceFactory.createMockServer();
        mockRequestServer.start();
        final var serverAdress = HttpMockServiceFactory.getBaseUri(mockRequestServer).toString();

        // Configuring our REST clients.
        noProxyNodeModel = new ProxyRestNodeModel(ProxyMode.NONE);
        noProxyNodeModel.setRequestTarget(serverAdress);

        final var proxyAdress = HttpMockServiceFactory.getBaseUri(mockRequestProxy);
        final var proxyConfig = RestProxyConfig.builder()//
            .setProtocol(ProxyProtocol.HTTP)//
            .setProxyHost(proxyAdress.getHost())//
            .setProxyPort(proxyAdress.getPort())//
            .build();
        configuredProxyNodeModel = new ProxyRestNodeModel(ProxyMode.LOCAL, proxyConfig);
        configuredProxyNodeModel.setRequestTarget(serverAdress);
    }

    /**
     * Checks if the proxy is running. Uses a HttpURLConnection to ensure the host is reachable,
     * independent of the used client.
     *
     * @throws IOException
     * @throws UnknownHostException
     */
    @SuppressWarnings("static-method")
    @Test
    void checkProxyRunning() throws IOException {
        final var connection =
            (HttpURLConnection)HttpMockServiceFactory.getBaseUri(mockRequestProxy).toURL().openConnection();
        assertDoesNotThrow(connection::connect, "Forwarding proxy should be available but isn't.");
    }

    /**
     * Checks if the server is running. Uses a HttpURLConnection to ensure the host is reachable,
     * independent of the used client.
     *
     * @throws IOException
     * @throws UnknownHostException
     */
    @SuppressWarnings("static-method")
    @Test
    void checkServerRunning() throws IOException {
        final var connection =
            (HttpURLConnection)HttpMockServiceFactory.getBaseUri(mockRequestServer).toURL().openConnection();
        assertDoesNotThrow(connection::connect, "Mock server should be available but isn't.");
    }

    /**
     * Performs a GET request with a mock proxy disabled.
     */
    @SuppressWarnings("static-method")
    @Test
    void requestWithNoProxyTest() {
        assertDoesNotThrow(() -> noProxyNodeModel.makeRequest(),
            "Making proxied GET request should not have thrown an exception.");
        // If at least one response value is not missing, the request went through.
        assertFalse(Arrays.stream(noProxyNodeModel.getResponses()).allMatch(DataCell::isMissing),
            "GET request should have succeeded but got only missing values as response.");
        // Check that the request actually went through the proxy by detecting the marker.
        assertFalse(PassthroughMarker.isPresentIn(noProxyNodeModel.getResponses()),
            "The HTTP request should NOT have passed through the proxy but has (marker present).");
    }

    /**
     * Performs a GET request with a mock proxy enabled.
     */
    @SuppressWarnings("static-method")
    @Test
    void requestWithConfiguredProxyTest() {
        assertDoesNotThrow(() -> configuredProxyNodeModel.makeRequest(),
            "Making proxied GET request should not have thrown an exception.");
        // If at least one response value is not missing, the request went through.
        assertFalse(Arrays.stream(configuredProxyNodeModel.getResponses()).allMatch(DataCell::isMissing),
            "GET request should have succeeded but got only missing values as response.");
        // Check that the request actually went through the proxy by detecting the marker.
        assertThat("The HTTP request should have passed through the proxy but hasn't (no marker present).",
            PassthroughMarker.isPresentIn(configuredProxyNodeModel.getResponses()));
    }

    @AfterAll
    public static void discardMockObjects() {
        // Discarding mock server objects.
        mockRequestServer.stop();
        mockRequestServer = null;
        mockRequestProxy.stop();
        mockRequestProxy = null;

        // Discarding REST client objects.
        noProxyNodeModel = null;
        configuredProxyNodeModel = null;
    }
}
