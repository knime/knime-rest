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
package org.knime.rest.nodes.common;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Supplier;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

/**
 * Mock HTTP service factory, can create a server for mocking direct responses or forwarding proxies.
 *
 * @author Leon Wenzler, KNIME AG, Konstanz, Germany
 */
public abstract class HttpMockServiceFactory {

    private static final ClassLoader WIREMOCK_CLASSLOADER = WireMock.class.getClassLoader();

    /**
     * Creates a server that simulates mock HTTP server. Returns mock responses.
     * Uses dynamic port allocation.
     *
     * @return mock server(WireMockServer)
     */
    public static WireMockServer createMockServer() {
        return loadInWireMockContext(() -> new WireMockServer(//
            wireMockConfig()//
                .dynamicPort()));
    }

    /**
     * Creates a server that acts as a mock HTTP proxy. Just forwards the incoming request to the target host.
     * Forwarding proxy will attach a "Via" header as marker to the response.
     * Uses dynamic port allocation.
     *
     * @return forwarding proxy (WireMockServer)
     */
    @SuppressWarnings("unchecked")
    public static WireMockServer createForwardingProxy() {
        return loadInWireMockContext(() -> new WireMockServer(//
            wireMockConfig()//
                .dynamicPort()//
                .enableBrowserProxying(true)//
                .trustAllProxyTargets(true)//
                .extensions(PassthroughMarker.class)));
    }

    /**
     * Extracts the base URI at which WireMock serves.
     *
     * @param mock WireMockServer
     * @return base URI
     */
    public static URI getBaseUri(final WireMockServer mock) {
        try {
            return new URI(loadInWireMockContext(mock::baseUrl));
        } catch (URISyntaxException e) {
            // Won't happen, Wiremock will throw an exception at WireMockServer#start() if URI is invalid.
            return null;
        }
    }

    /**
     * We need to load the Wiremock configuration with its own classloader, otherwise it won't find a certain file upon
     * initializing the object.
     *
     * @return WireMockConfiguration
     */
    private static <T> T loadInWireMockContext(final Supplier<T> supplier) {
        // Setting the context classloader is necessary, otherwise Wiremock doesn't find its default 'keystore' file.
        final var previousCl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(WIREMOCK_CLASSLOADER);
        try {
            return supplier.get();
        } finally {
            Thread.currentThread().setContextClassLoader(previousCl);
        }
    }

    /**
     * Hides the constructor.
     */
    private HttpMockServiceFactory() {
    }
}
