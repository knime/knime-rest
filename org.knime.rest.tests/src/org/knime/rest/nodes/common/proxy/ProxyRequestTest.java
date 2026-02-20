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

import org.apache.hc.core5.net.URIBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.core.data.DataCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.util.proxy.ProxyProtocol;
import org.knime.core.util.proxy.testing.HttpbinTestContext;
import org.knime.rest.generic.EachRequestAuthentication;
import org.knime.rest.internals.BasicAuthentication;
import org.knime.rest.internals.DigestAuthentication;
import org.knime.rest.nodes.common.HttpMockServiceFactory;
import org.knime.rest.nodes.common.PassthroughMarker;
import org.knime.rest.nodes.common.TestGetNodeModel;
import org.knime.rest.nodes.common.proxy.SystemPropertyProvider.PropertyMode;

import com.github.tomakehurst.wiremock.WireMockServer;

/**
 * Uses WireMock to host a simple forwarding proxy and tests HTTP requests with different authentication methods.
 *
 * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
 */
final class ProxyRequestTest {

    // -- SERVERS --

    private static WireMockServer mockRequestProxy;

    private static WireMockServer mockRequestServer;

    // -- CLIENTS --

    private static TestGetNodeModel directNodeModel;

    private static TestGetNodeModel proxyNodeModel;

    /**
     * Initializes all needed HTTP objects.
     *
     * @throws InvalidSettingsException
     */
    @BeforeAll
    static void initializeHttpParties() throws InvalidSettingsException {
        // Setting up the forwarding proxy and mocked server target.
        mockRequestProxy = HttpMockServiceFactory.createForwardingProxy();
        mockRequestProxy.start();
        mockRequestServer = HttpMockServiceFactory.createMockServer();
        mockRequestServer.start();

        // Configuring our REST clients.
        directNodeModel = new TestGetNodeModel(ProxyMode.NONE);

        final var proxyAddress = HttpMockServiceFactory.getBaseUri(mockRequestProxy);
        final var proxyConfig = RestProxyConfig.builder()//
            .setProtocol(ProxyProtocol.HTTP)//
            .setProxyHost(proxyAddress.getHost())//
            .setProxyPort(proxyAddress.getPort())//
            .build();
        proxyNodeModel = new TestGetNodeModel(ProxyMode.LOCAL, proxyConfig);
    }

    /**
     * Discards all HTTP objects.
     */
    @AfterAll
    static void discardHttpParties() {
        // Discarding mock server objects.
        mockRequestServer.stop();
        mockRequestServer = null;
        mockRequestProxy.stop();
        mockRequestProxy = null;

        // Discarding REST client objects.
        proxyNodeModel = null;
        directNodeModel = null;
    }

    /**
     * Checks the HTTP request for 4 criteria. 1) Went through without exception. 2) Got non-missing values. 3) Proxy
     * passthrough marker is present. 4) Got a 200 response.
     */
    private static void assertRequestFor(final TestGetNodeModel client, final String target,
        final EachRequestAuthentication auth, final String status) {
        client.setRequestTarget(target);
        assertDoesNotThrow(() -> client.makeRequest(auth),
            "Making proxied GET request should not have thrown an exception.");
        assertFalse(Arrays.stream(client.getResponses()).allMatch(DataCell::isMissing),
            "GET request should have succeeded but got only missing values as response.");
        assertThat("The HTTP request should have passed through the proxy but hasn't (no marker present).",
            PassthroughMarker.isPresentIn(client.getResponses()));
        assertThat("The HTTP request should have gotten a 200 OK response.",
            status.equals(client.getResponses()[0].toString()));
    }

    private static EachRequestAuthentication createBasicAuthentication(final String user, final String passwd) {
        final var auth = new BasicAuthentication();
        auth.setUsername(user);
        auth.setPassword(passwd);
        auth.setEnabled(true);
        return auth;
    }

    private static EachRequestAuthentication createDigestAuthentication(final String user, final String passwd) {
        final var auth = new DigestAuthentication();
        auth.setUsername(user);
        auth.setPassword(passwd);
        auth.setEnabled(true);
        return auth;
    }

    /**
     * Checks if the proxy is running. Uses a HttpURLConnection to ensure the host is reachable, independent of the used
     * client.
     *
     * @throws IOException
     * @throws UnknownHostException
     */
    @SuppressWarnings("static-method")
    @BeforeEach
    void checkProxyRunning() throws IOException {
        final var connection =
            (HttpURLConnection)HttpMockServiceFactory.getBaseUri(mockRequestProxy).toURL().openConnection();
        assertDoesNotThrow(connection::connect, "Forwarding proxy should be available but isn't.");
    }

    /**
     * Checks if the server is running. Uses a HttpURLConnection to ensure the host is reachable, independent of the
     * used client.
     *
     * @throws IOException
     * @throws UnknownHostException
     */
    @SuppressWarnings("static-method")
    @BeforeEach
    void checkServerRunning() throws IOException {
        final var connection =
            (HttpURLConnection)HttpMockServiceFactory.getBaseUri(mockRequestServer).toURL().openConnection();
        assertDoesNotThrow(connection::connect, "Mock server should be available but isn't.");
    }

    // -- CONNECTIVITY TESTS --

    /**
     * Performs a GET request with a mock proxy disabled.
     */
    @SuppressWarnings("static-method")
    @Test
    void requestWithNoProxyTest() {
        final var serverAdress = HttpMockServiceFactory.getBaseUri(mockRequestServer).toString();
        directNodeModel.setRequestTarget(serverAdress);
        assertDoesNotThrow(() -> directNodeModel.makeRequest(),
            "Making direct GET request should not have thrown an exception.");
        // If at least one response value is not missing, the request went through.
        assertFalse(Arrays.stream(directNodeModel.getResponses()).allMatch(DataCell::isMissing),
            "GET request should have succeeded but got only missing values as response.");
        // Check that the request did not go through the proxy.
        assertFalse(PassthroughMarker.isPresentIn(directNodeModel.getResponses()),
            "The HTTP request should *NOT* have passed through the proxy but has (marker present).");
    }

    /**
     * Tests if all proxies are successfully bypassed by settings global *and* local proxy hosts to an invalid host (see
     * INVALID_PROXY_HOST).
     *
     * The request should work just fine. Test fails if any proxy is used because a connection is not possible.
     */
    @SuppressWarnings("static-method")
    @Test
    void requestWithBypassedProxyTest() {
        var props = new String[]{"http.proxyHost", "http.proxyPort"};
        var values = SystemPropertyProvider.saveAndSetProperties(props, PropertyMode.TINYPROXY);
        try {
            final var serverAdress = HttpMockServiceFactory.getBaseUri(mockRequestServer).toString();
            directNodeModel.setRequestTarget(serverAdress);
            assertDoesNotThrow(() -> directNodeModel.makeRequest(),
                    "Making a (dummy) GET request should not have thrown an exception.");
            // If at least one response value is not missing, the request went through.
            assertFalse(Arrays.stream(directNodeModel.getResponses()).allMatch(DataCell::isMissing),
                    "GET request should have succeeded but got missing values as response.");
        } finally {
            SystemPropertyProvider.restoreProperties(props, values);
        }
    }

    /**
     * Performs a GET request with a mock proxy enabled.
     */
    @SuppressWarnings("static-method")
    @Test
    void requestWithConfiguredProxyTest() {
        final var serverAdress = HttpMockServiceFactory.getBaseUri(mockRequestServer).toString();
        // 404 is expected, we are calling an invalid path on a valid server
        assertRequestFor(proxyNodeModel, serverAdress, null, "404");
    }

    // -- AUTHENTICATION TESTS --

    /**
     * Tests whether an HTTP connection without authentication can be established.
     */
    @SuppressWarnings("static-method")
    @Test
    void noAuthHttpTest() {
        final var uri = new URIBuilder(HttpbinTestContext.getURI("http")) //
            .setPath("anything").toString();
        assertRequestFor(proxyNodeModel, uri, null, "200");
    }

    /**
     * Tests whether an HTTP connection with BASIC authentication can be established.
     */
    @SuppressWarnings("static-method")
    @Test
    void basicAuthHttpTest() {
        final var user = "basicHttpUser";
        final var passwd = "basicHttpPasswd";
        final var uri = new URIBuilder(HttpbinTestContext.getURI("http")) //
            .setPath(String.format("basic-auth/%s/%s", user, passwd)).toString();
        assertRequestFor(proxyNodeModel, uri, createBasicAuthentication(user, passwd), "200");
    }

    /**
     * Tests whether an HTTP connection with HIDDEN BASIC authentication can be established.
     */
    @SuppressWarnings("static-method")
    @Test
    void hiddenBasicAuthHttpTest() {
        final var user = "hiddenBasicHttpUser";
        final var passwd = "hiddenBasicHttpPasswd";
        final var uri = new URIBuilder(HttpbinTestContext.getURI("http")) //
            .setPath(String.format("hidden-basic-auth/%s/%s", user, passwd)).toString();
        assertRequestFor(proxyNodeModel, uri, createBasicAuthentication(user, passwd), "200");
    }

    /**
     * Tests whether an HTTP connection with DIGEST authentication can be established.
     */
    @SuppressWarnings("static-method")
    @Test
    void digestAuthHttpTest() {
        final var user = "digestHttpUser";
        final var passwd = "digestHttpPasswd";
        final var uri = new URIBuilder(HttpbinTestContext.getURI("http")) //
            .setPath(String.format("digest-auth/auth/%s/%s", user, passwd)).toString();
        assertRequestFor(proxyNodeModel, uri, createDigestAuthentication(user, passwd), "200");
    }
}
