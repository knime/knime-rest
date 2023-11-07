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
 *   14 Mar 2023 (leon.wenzler): created
 */
package org.knime.rest.nodes.common;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.rest.generic.EachRequestAuthentication;
import org.knime.rest.nodes.common.RestSettings.HttpMethod;
import org.knime.rest.nodes.common.proxy.ProxyMode;
import org.knime.rest.nodes.common.proxy.RestProxyConfig;

import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.Invocation.Builder;

/**
 * REST model for testing which per default configures the specified proxy in its settings.
 * Also makes the request method available (always uses GET).
 *
 * @author Leon Wenzler, KNIME AG, Konstanz, Germany
 */
public final class TestGetNodeModel extends RestNodeModel<RestSettings> {

    /**
     * REST node model always using a GET request, but configurable on which proxy to use.
     *
     * @param proxyMode NONE, LOCAL, or GLOBAL
     */
    public TestGetNodeModel(final ProxyMode proxyMode) {
        this(proxyMode, null);
    }

    /**
     * REST node model always using a GET request, but configurable on which proxy to use.
     *
     * @param proxyMode NONE, LOCAL, or GLOBAL
     * @param proxyConfig proxy config
     */
    public TestGetNodeModel(final ProxyMode proxyMode, final RestProxyConfig proxyConfig) {
        CheckUtils.checkArgument((proxyMode == ProxyMode.NONE) == (proxyConfig == null),
            "Either set proxy mode to NONE and don't specify a config object, or set a different proxy mode!");
        m_settings.getProxyManager().setProxyMode(proxyMode);
        m_settings.m_currentProxyConfig = Optional.ofNullable(proxyConfig);
        // Useful for testing: extract all response headers.
        m_settings.setExtractAllResponseFields(true);
    }

    @Override
    protected RestSettings createSettings() {
        return new RestSettings(HttpMethod.GET);
    }

    @Override
    protected Invocation invocation(final Builder request, final DataRow row, final DataTableSpec spec) {
        // Using a GET request as default.
        return request.buildGet();
    }

    /**
     * Sets the target of this REST request to the given URI string.
     *
     * @param targetURI
     */
    public void setRequestTarget(final String targetURI) {
        m_settings.setUseConstantURI(true);
        m_settings.setConstantURI(targetURI);
    }

    /**
     * Performs a simple GET request without any context.
     *
     * @throws Exception
     */
    public void makeRequest() throws Exception {
        makeRequest(null);
    }

    /**
     * Performs a simple GET request with a given authentication method.
     *
     * @throws Exception
     */
    public void makeRequest(final EachRequestAuthentication auth) throws Exception {
        reset();
        final List<EachRequestAuthentication> enabledAuthentications =
            auth != null ? List.of(auth) : Collections.emptyList();
        makeFirstCall(null, enabledAuthentications, null, null);
    }

    /**
     * Returns an array of DataCells, encapsulating the HTTP response.
     *
     * @return array of DataCells
     */
    public DataCell[] getResponses() {
        if (!m_firstCallValues.isEmpty()) {
            return m_firstCallValues.get(0);
        }
        return new DataCell[0];
    }
}
