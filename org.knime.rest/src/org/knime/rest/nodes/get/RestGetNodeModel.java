/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 *   23. Jan. 2016. (Gabor Bakos): created
 */
package org.knime.rest.nodes.get;

import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.streamable.simple.SimpleStreamableFunctionNodeModel;
import org.knime.rest.nodes.get.RestGetSettings.RequestHeaderKeyItem;

/**
 *
 * @author Gabor Bakos
 */
public class RestGetNodeModel extends SimpleStreamableFunctionNodeModel {
    private RestGetSettings m_settings;

    /**
     *
     */
    public RestGetNodeModel() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec spec) throws InvalidSettingsException {
        ColumnRearranger rearranger = new ColumnRearranger(spec);
        List<DataColumnSpec> fields = m_settings.getExtractFields().entrySet().stream().map(e -> new DataColumnSpecCreator(e.getKey(), e.getValue()).createSpec()).collect(Collectors.toList());
        DataColumnSpec[] newColumns = fields.toArray(new DataColumnSpec[fields.size()]);
        final int uriColumn = spec.findColumnIndex(m_settings.getUriColumn());
        rearranger.append(new AbstractCellFactory(newColumns) {

            @Override
            public DataCell[] getCells(final DataRow row) {
                ClientBuilder clientBuilder = ClientBuilder.newBuilder();
                Client client = clientBuilder.build();
                WebTarget target = client.target(m_settings.isUseConstantURI() ? m_settings.getConstantURI() : ((StringValue)row.getCell(uriColumn)).getStringValue());

                Builder request = target.request();
                for (final RequestHeaderKeyItem headerItem : m_settings.getRequestHeaders()) {
                    Object value;
                    switch (headerItem.getKind()) {
                        case Constant:
                            value = headerItem.getValueReference();
                            break;
                        case Column:
                            value = row.getCell(spec.findColumnIndex(headerItem.getValueReference())).toString();
                            break;
                        case FlowVariable:
                            value = getAvailableInputFlowVariables().get(headerItem.getKey()).getValueAsString();
                            break;
                            default:
                                throw new UnsupportedOperationException("Unknown: " + headerItem.getKind() + " in: " + headerItem);
                    }
                    request.header(headerItem.getKey(), value);
                }
                Response response = request.buildGet().invoke();
                Object body = response.getEntity();
                response.getHeaders();
                //TODO
                return null;
            }
        });
        return rearranger;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        new RestGetSettings().loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettingsFrom(settings);
    }

}
