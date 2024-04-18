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
 *   7 Nov 2023 (leon.wenzler): created
 */
package org.knime.rest.nodes.common;

import java.util.Collections;

import org.knime.core.data.DataCell;
import org.knime.rest.nodes.common.RestSettings.HttpMethod;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.Invocation.Builder;

/**
 * REST model for testing requests with a body (always uses POST). Makes the request method available.
 *
 * @author Leon Wenzler, KNIME AG, Konstanz, Germany
 */
public final class TestPostNodeModel extends RestWithBodyNodeModel<RestWithBodySettings> {

    /**
     * Simplified REST node model always using a POST request.
     */
    public TestPostNodeModel() {
        // Useful for testing: extract all response headers.
        m_settings.setExtractAllResponseFields(true);
    }

    @Override
    protected RestWithBodySettings createSettings() {
        return new RestWithBodySettings(HttpMethod.POST);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Invocation invocationWithEntity(final Builder request, final Entity<?> entity) {
        return request.buildPost(entity);
    }

    /**
     * Sets the target of this REST request to the given URI string.
     *
     * @param targetURI
     */
    public void setRequestTarget(final String targetURI) {
        m_settings.setUseConstantURL(true);
        m_settings.setConstantURL(targetURI);
    }

    /**
     * Sets the request body of this REST request to the given body string.
     *
     * @param requestBodyAsString body
     */
    public void setRequestBody(final String requestBodyAsString) {
        m_settings.setUseConstantRequestBody(true);
        m_settings.setConstantRequestBody(requestBodyAsString);
    }

    /**
     * Sets the chunking threshold of this REST request to the given byte size.
     *
     * @param newThreshold chunking threshold in bytes
     */
    public void setChunkingThreshold(final int newThreshold) {
        chunkingThreshold = newThreshold;
    }

    /**
     * Performs a simple POST request without any context.
     *
     * @throws Exception
     */
    public void makeRequest() throws Exception {
        reset();
        makeFirstCall(null, Collections.emptyList(), null, null);
    }

    /**
     * Returns an array of DataCells, encapsulating the HTTP response.
     *
     * @return array of DataCells
     */
    public DataCell[] getResponses() {
        final var firstValues = m_parsedResponseValues.values().stream().findFirst();
        return firstValues.orElseGet(() -> AbstractRequestExecutor.EMPTY_RESPONSE);
    }
}
