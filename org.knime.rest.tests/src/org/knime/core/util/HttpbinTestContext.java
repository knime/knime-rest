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
package org.knime.core.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.net.URI;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;

/**
 * 'Httpbin' context factory that returns host names and {@link URI}s,
 * based on internal testing services providing a httpbin instance.
 *
 * @see "https://github.com/mccutchen"
 */
public class HttpbinTestContext {

    private static final String KNIME_HTTPBIN = "KNIME_HTTPBIN";

    private static void assertNonNullEnvVar(final String key) {
        assertThat(System.getenv(key)) //
            .as("Expected environment variable \"%s\" to be non-null", key) //
            .isNotNull();
    }

    /**
     * Returns the host name as of our httpbin testing instance as string.
     *
     * @return host name of httpbin testing instance
     */
    public static String getHost() {
        assertNonNullEnvVar(KNIME_HTTPBIN);
        return System.getenv(KNIME_HTTPBIN);
    }

    /**
     * Returns the {@link URI} constructed from the httpbin host name and a given scheme.
     *
     * @param scheme scheme of the constructed httpbin URI
     * @return URI of httpbin testing instance
     */
    public static URI getURI(final String scheme) {
        return assertDoesNotThrow(() -> new URIBuilder() //
            .setScheme(StringUtils.lowerCase(scheme)) //
            .setHost(getHost()) //
            .build(), "URI for httpbin testing instance could not be constructed");
    }

    /**
     * Returns the {@link HttpUriRequest} constructed from the httpbin host name, a given scheme,
     * and a valid HTTP method, such as GET, POST, etc.
     *
     * @param scheme scheme of the URI to be used for the request
     * @param method HTTP method
     * @return HTTP request to the httpbin testing instance
     */
    public static HttpUriRequest getHttpRequest(final String scheme, final String method) {
        return new HttpRequestBase() {
            {
                setURI(HttpbinTestContext.getURI(scheme));
            }

            @Override
            public String getMethod() {
                return StringUtils.upperCase(method);
            }
        };
    }
}
