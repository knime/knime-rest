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
 *   Aug 10, 2024 (lw): created
 */
package org.knime.core.util.proxy;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.knime.core.util.Pair;
import org.knime.core.util.proxy.apache.ProxyConnectionReuseStrategy;
import org.knime.core.util.proxy.apache.ProxyCredentialsProvider;
import org.knime.core.util.proxy.apache.ProxyHttpClients;
import org.knime.core.util.proxy.apache.ProxyHttpRoutePlanner;
import org.knime.core.util.proxy.testing.HttpbinTestContext;
import org.knime.core.util.proxy.testing.ProxyParameterProvider;
import org.knime.core.util.proxy.testing.ProxyTestContext;
import org.knime.core.util.proxy.testing.TinyproxyTestContext;

/**
 * Tests the effect of {@link ProxyHttpRoutePlanner}, {@link ProxyCredentialsProvider}, and
 * {@link ProxyConnectionReuseStrategy} on standard Apache {@link HttpClient}s.
 */
class ApacheHttpClientProxyTest {

    @RegisterExtension
    private static final ProxyParameterProvider TEST_CONTEXT = EclipseProxyTestContext.INSTANCE;

    private static final int TIMEOUT_SECONDS = 60;

    private static CloseableHttpClient createClient(final Consumer<HttpClientBuilder> configurer) {
        final var builder = HttpClients.custom().setDefaultRequestConfig(RequestConfig.custom() //
            .setConnectTimeout(1000 * TIMEOUT_SECONDS) //
            .setConnectionRequestTimeout(1000 * TIMEOUT_SECONDS) //
            .setSocketTimeout(1000 * TIMEOUT_SECONDS) //
            .build());
        configurer.accept(builder);
        return builder.build();
    }

    // -- TESTS --

    @TestTemplate
    void testRoutePlanner(final ProxyTestContext context) throws IOException {
        // only visible behind the internal proxy
        final var target = new HttpGet(TinyproxyTestContext.getStatsURI());
        final var proxy = TinyproxyTestContext.getWithoutAuth(ProxyProtocol.HTTP);

        // test that the target host is not reachable without proxy
        context.withEmpty(() -> {
            try (var client = createClient(b -> b.setRoutePlanner(ProxyHttpRoutePlanner.INSTANCE));
                    var response = client.execute(target)) {
                throw new AssertionError("Should not have reached here, request should throw an UnknownHostException");
            } catch (UnknownHostException e) { // NOSONAR - this should happen

            }
        });

        // test regular routing over proxy (target is reachable now)
        context.withConfig(proxy, () -> {
                try (var client = createClient(b -> b.setRoutePlanner(ProxyHttpRoutePlanner.INSTANCE));
                        var response = client.execute(target)) {
                    assertThat(response.getStatusLine().getStatusCode()) //
                        .as("Proxy was not successfully found and routed through") //
                        .isEqualTo(200);
                }
            });

        // test that a wrong proxy config results in non-routeability
        context.withConfig(
            new GlobalProxyConfig(ProxyProtocol.HTTP, proxy.host(), "1111", false, null, null, false, null), () -> {
                try (var client = createClient(b -> b.setRoutePlanner(ProxyHttpRoutePlanner.INSTANCE));
                        var response = client.execute(target)) {
                    throw new AssertionError(
                        "Should not have reached here, request should throw a HttpHostConnectException");
                } catch (HttpHostConnectException e) { // NOSONAR - this should happen

                }
            });
    }

    @TestTemplate
    void testCredentialsProvider(final ProxyTestContext context) throws IOException {
        // this target is always reachable (not only behind proxy)
        final var target = HttpbinTestContext.getHttpRequest("https", "GET");
        final var proxy = TinyproxyTestContext.getWithAuth(ProxyProtocol.HTTPS);

        // test that connections are not re-used when alternatingly querying unauthenticated and authenticated proxies
        try (var client = createClient(b -> ProxyHttpClients.customForBuilder(b, target.getURI()))) {
            context.withConfig(proxy, () -> {
                try (var response = client.execute(target)) {
                    assertThat(response.getStatusLine().getStatusCode()) //
                        .as("Proxy was not successfully found, authenticated at, and routed through") //
                        .isEqualTo(200);
                }
            });
        }
    }

    @TestTemplate
    void testNoConnectionReuse(final ProxyTestContext context) throws IOException {
        // this target is always reachable (not only behind proxy)
        final var target = HttpbinTestContext.getHttpRequest("https", "GET");
        final var authenticatedConfig = TinyproxyTestContext.getWithAuth(ProxyProtocol.HTTPS);
        final var unauthenticatedConfig = new GlobalProxyConfig( //
            authenticatedConfig.protocol(), //
            authenticatedConfig.host(), //
            authenticatedConfig.port(), //
            false, null, null, false, null);

        // test that connections are not re-used when alternatingly querying unauthenticated and authenticated proxies
        try (var client = createClient(b -> ProxyHttpClients.customForBuilder(b, target.getURI()) //
            .setMaxConnTotal(20) //
            .setMaxConnPerRoute(20) //
            .setConnectionTimeToLive(-1, TimeUnit.MILLISECONDS))) {

            // run back-to-back to test that credentials caching
            for (var test : List.of(Pair.create(authenticatedConfig, 200), Pair.create(unauthenticatedConfig, 407))) {
                final var proxy = test.getFirst();
                final var statusCode = test.getSecond();

                context.withConfig(proxy, () -> {
                    try (var response = client.execute(target)) {
                        assertThat(response.getStatusLine().getStatusCode()) //
                            .as("Proxy was not successfully found, authenticated at, and routed through") //
                            .isEqualTo(statusCode);
                    }
                });
            }
        }
    }
}
