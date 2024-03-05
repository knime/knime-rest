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
 *   Jun 14, 2023 (Leon Wenzler, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.rest.nodes.common.proxy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.knime.core.data.DataCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.util.proxy.ProxyProtocol;
import org.knime.rest.generic.EachRequestAuthentication;
import org.knime.rest.internals.BasicAuthentication;
import org.knime.rest.internals.DigestAuthentication;
import org.knime.rest.nodes.common.HttpMockServiceFactory;
import org.knime.rest.nodes.common.PassthroughMarker;
import org.knime.rest.nodes.common.TestGetNodeModel;

import com.github.tomakehurst.wiremock.WireMockServer;

/**
 * Uses WireMock to host a simple forwarding proxy and tests HTTP requests with different authentication methods.
 *
 * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
 */
public class AuthenticationWithProxyTest {

    private static WireMockServer forwardingProxy;

    private static TestGetNodeModel proxyNodeModel;

    /**
     * Initializes all needed HTTP objects.
     *
     * @throws InvalidSettingsException
     */
    @BeforeAll
    public static void initializeHttpParties() throws InvalidSettingsException {
        // Setting up the forwarding proxy.
        forwardingProxy = HttpMockServiceFactory.createForwardingProxy();
        forwardingProxy.start();

        // Configuring our REST client.
        final var proxyAdress = HttpMockServiceFactory.getBaseUri(forwardingProxy);
        final var proxyConfig = RestProxyConfig.builder()//
            .setProtocol(ProxyProtocol.HTTP)//
            .setProxyHost(proxyAdress.getHost())//
            .setProxyPort(proxyAdress.getPort())//
            .build();
        proxyNodeModel = new TestGetNodeModel(ProxyMode.LOCAL, proxyConfig);
    }

    /**
     * Tests whether an HTTP connection without authentication can be established.
     */
    @Test
    void noAuthHttpTest() {
        assertRequestFor("http://httpbin.testing.knime.com/anything", null);
    }

    /**
     * Tests whether an HTTP connection with BASIC authentication can be established.
     */
    @Test
    void basicAuthHttpTest() {
        final var user = "basicHttpUser";
        final var passwd = "basicHttpPasswd";
        assertRequestFor(String.format("http://httpbin.testing.knime.com/basic-auth/%s/%s", user, passwd),
            createBasicAuthentication(user, passwd));
    }

    /**
     * Tests whether an HTTP connection with HIDDEN BASIC authentication can be established.
     */
    @Test
    void hiddenBasicAuthHttpTest() {
        final var user = "hiddenBasicHttpUser";
        final var passwd = "hiddenBasicHttpPasswd";
        assertRequestFor(String.format("http://httpbin.testing.knime.com/hidden-basic-auth/%s/%s", user, passwd),
            createBasicAuthentication(user, passwd));
    }

    /**
     * Tests whether an HTTP connection with DIGEST authentication can be established.
     */
    @Test
    void digestAuthHttpTest() {
        final var user = "digestHttpUser";
        final var passwd = "digestHttpPasswd";
        assertRequestFor(String.format("http://httpbin.testing.knime.com/digest-auth/auth/%s/%s", user, passwd),
            createDigestAuthentication(user, passwd));
    }

    /**
     * Checks the HTTP request for 4 criteria:
     *   1) Went through without exception.
     *   2) Got non-missing values.
     *   3) Proxy passthrough marker is present.
     *   4) Got a 200 response.
     */
    private static void assertRequestFor(final String target, final EachRequestAuthentication auth) {
        proxyNodeModel.setRequestTarget(target);
        assertDoesNotThrow(() -> proxyNodeModel.makeRequest(auth),
            "Making proxied GET request should not have thrown an exception.");
        assertFalse(Arrays.stream(proxyNodeModel.getResponses()).allMatch(DataCell::isMissing),
            "GET request should have succeeded but got only missing values as response.");
        assertThat("The HTTP request should have passed through the proxy but hasn't (no marker present).",
            PassthroughMarker.isPresentIn(proxyNodeModel.getResponses()));
        assertThat("The HTTP request should have gotten a 200 OK response.",
            "200".equals(proxyNodeModel.getResponses()[0].toString()));
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
     * Discards all HTTP objects.
     */
    @AfterAll
    public static void discardHttpParties() {
        // Discarding mock server objects.
        forwardingProxy.stop();
        forwardingProxy = null;

        // Discarding REST client.
        proxyNodeModel = null;
    }
}
