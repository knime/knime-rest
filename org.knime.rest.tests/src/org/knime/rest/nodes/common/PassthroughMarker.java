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
 *   May 11, 2023 (Leon Wenzler, KNIME AG, Konstanz, Germany): created
 */
package org.knime.rest.nodes.common;

import java.util.Arrays;

import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;

/**
 * Transforms a HTTP response by attaching a marker which can be used to detect a proxy presence.
 *
 * @author Leon Wenzler, KNIME AG, Konstanz, Germany
 */
public final class PassthroughMarker extends ResponseTransformer {

    private static final String VIA = "Via";

    /**
     * The marker attached at responses - just the fully qualified class name.
     *
     * @return marker string
     */
    private static String getMarkerString() {
        return PassthroughMarker.class.getName();
    }

    @Override
    public String getName() {
        return getMarkerString();
    }

    @Override
    public Response transform(final Request request, final Response response, final FileSource files,
        final Parameters parameters) {
        // Same response, but with an added "Via" header.
        return Response.response()//
            .like(response)//
            .headers(response.getHeaders().plus(new HttpHeader(VIA, getMarkerString())))//
            .build();
    }
    
    /**
     * Checks if the marker is present in the given array,
     * which represents the parsed response contents.
     *
     * @param responseArray
     * @return whether the parsed response content contains a marker
     */
    public static boolean isPresentIn(final Object[] responseArray) {
        return Arrays.stream(responseArray).map(Object::toString).anyMatch(getMarkerString()::equals);
    }
}
