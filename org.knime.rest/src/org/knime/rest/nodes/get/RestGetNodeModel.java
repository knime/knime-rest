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

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.RuntimeDelegate;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.apache.cxf.jaxrs.client.spec.ClientBuilderImpl;
import org.apache.cxf.jaxrs.impl.RuntimeDelegateImpl;
import org.knime.base.data.xml.SvgCellFactory;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellFactory;
import org.knime.core.data.DataCellFactory.FromString;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.IntValue;
import org.knime.core.data.MissingCell;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.json.JSONCellFactory;
import org.knime.core.data.uri.URIDataValue;
import org.knime.core.data.xml.XMLCellFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.streamable.simple.SimpleStreamableFunctionNodeModel;
import org.knime.rest.nodes.get.RestGetSettings.RequestHeaderKeyItem;
import org.knime.rest.util.DelegatingX509TrustManager;
import org.xml.sax.SAXException;

/**
 *
 * @author Gabor Bakos
 */
class RestGetNodeModel extends SimpleStreamableFunctionNodeModel {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(RestGetNodeModel.class);

    private final RestGetSettings m_settings = new RestGetSettings();

    /**
     *
     */
    public RestGetNodeModel() {
        super();
        System.setProperty(ClientBuilder.JAXRS_DEFAULT_CLIENT_BUILDER_PROPERTY, ClientBuilderImpl.class.getName());
        System.setProperty(RuntimeDelegate.JAXRS_RUNTIME_DELEGATE_PROPERTY, RuntimeDelegateImpl.class.getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec spec) throws InvalidSettingsException {
        ColumnRearranger rearranger = new ColumnRearranger(spec);
        List<DataColumnSpec> fields = m_settings.getExtractFields().stream()
            .map(rhi -> new DataColumnSpecCreator(rhi.getOutputColumnName(), rhi.getType()).createSpec())
            .collect(Collectors.toList());
        fields.add(new DataColumnSpecCreator(m_settings.getResponseBodyColumn(), StringCell.TYPE).createSpec());
        DataColumnSpec[] newColumns = fields.toArray(new DataColumnSpec[fields.size()]);
        final int uriColumn = spec.findColumnIndex(m_settings.getUriColumn());
        rearranger.append(new AbstractCellFactory(newColumns) {
            private boolean m_isContextSettingsFailed = false;

            @Override
            public DataCell[] getCells(final DataRow row) {
                ClientBuilder clientBuilder = ClientBuilder.newBuilder();
                if (m_settings.isSslTrustAll()) {
                    final SSLContext context;
                    try {
                        context = SSLContext.getInstance("Default");
                        context.init(null, new TrustManager[]{new DelegatingX509TrustManager()}, null);
                        clientBuilder.sslContext(context);
                    } catch (final NoSuchAlgorithmException | KeyManagementException e) {
                        if (!m_isContextSettingsFailed) {
                            LOGGER.debug("Failed to disable SSL context checks", e);
                            m_isContextSettingsFailed = true;
                        }
                    }
                }
                if (m_settings.isSslIgnoreHostNameErrors()) {
                    clientBuilder.hostnameVerifier((hostName, session) -> true);
                }
                final Client client = clientBuilder.build();
                final WebTarget target = client.target(m_settings.isUseConstantURI() ? m_settings.getConstantURI()
                    : row.getCell(uriColumn) instanceof URIDataValue
                        ? ((URIDataValue)row.getCell(uriColumn)).getURIContent().getURI().toString()
                        : ((StringValue)row.getCell(uriColumn)).getStringValue());

                final Builder request = target.request();
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
                            throw new UnsupportedOperationException(
                                "Unknown: " + headerItem.getKind() + " in: " + headerItem);
                    }
                    request.header(headerItem.getKey(), value);
                }
                final List<DataCell> cells;
                final Response response = request.buildGet().invoke();
                try {
                    //MultivaluedMap<String, Object> headers = response.getHeaders();
                    if (m_settings.isExtractAllResponseFields()) {
                        throw new UnsupportedOperationException();
                    }
                    FromString fallback = new FromString() {

                        @Override
                        public DataType getDataType() {
                            return StringCell.TYPE;
                        }

                        @Override
                        public DataCell createCell(final String input) {
                            return new MissingCell("No cell factory was found" + input);
                        }

                    };
                    cells = m_settings.getExtractFields().stream().map(rhi -> {
                        DataCellFactory factory = rhi.getType().getCellFactory(null).orElseGet(() -> fallback);
                        //List<Object> values = headers.get(e.getKey());
                        if ("Status".equals(rhi.getHeaderKey()) && rhi.getType().isCompatible(IntValue.class)) {
                            return new IntCell(response.getStatus());
                        }
                        if (factory instanceof FromString) {
                            FromString fromString = (FromString)factory;
                            String value = response.getHeaderString(rhi.getHeaderKey());
                            if (value == null) {
                                return DataType.getMissingCell();
                            }
                            return fromString.createCell(value);
                        }
                        return DataType.getMissingCell();
                    }).collect(Collectors.toList());
                    if (response.hasEntity()) {
                        MediaType mediaType = response.getMediaType();
                        if (mediaType == null) {
                            cells.add(DataType.getMissingCell());
                        } else {
                            if (MediaType.APPLICATION_JSON_TYPE.isCompatible(mediaType)) {
                                try {
                                    cells.add(JSONCellFactory.create((InputStream)response.getEntity(), false));
                                } catch (final IOException e1) {
                                    cells.add(new MissingCell(e1.getMessage()));
                                }
                            } else if (MediaType.APPLICATION_SVG_XML_TYPE.isCompatible(mediaType)) {
                                try {
                                    cells.add(new SvgCellFactory().createCell((InputStream)response.getEntity()));
                                } catch (IOException e1) {
                                    cells.add(new MissingCell(e1.getMessage()));
                                }
                            } else if (MediaType.APPLICATION_XML_TYPE.isCompatible(mediaType)
                                || MediaType.TEXT_XML_TYPE.isCompatible(mediaType)
                                || MediaType.APPLICATION_ATOM_XML_TYPE.isCompatible(mediaType)
                                || MediaType.APPLICATION_XHTML_XML_TYPE.isCompatible(mediaType)) {
                                try {
                                    cells.add(XMLCellFactory.create((InputStream)response.getEntity()));
                                } catch (IOException | ParserConfigurationException | SAXException
                                        | XMLStreamException e1) {
                                    cells.add(new MissingCell(e1.getMessage()));
                                }
                            } else if (MediaType.TEXT_PLAIN_TYPE.isCompatible(mediaType)
                                || MediaType.TEXT_HTML_TYPE.isCompatible(mediaType)) {
                                String responseText = response.readEntity(String.class);
                                cells.add(new StringCell(responseText));
                            } else if (MediaType.APPLICATION_OCTET_STREAM_TYPE.isCompatible(mediaType)) {
                                cells.add(DataType.getMissingCell());
                            } else {
                                cells.add(DataType.getMissingCell());
                            }
                        }
                    } else {
                        cells.add(DataType.getMissingCell());
                    }
                } finally {
                    response.close();
                }
                //TODO wait only when we are requesting from the same domain
                if (m_settings.isUseDelay()) {
                    for (long wait = m_settings.getDelay(); wait > 0; wait -= 100L) {
                        try {
                            Thread.sleep(Math.min(wait, 100L));
                        } catch (InterruptedException e) {
                            //TODO check for cancel
                        }
                    }
                }
                return cells.toArray(new DataCell[cells.size()]);
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
