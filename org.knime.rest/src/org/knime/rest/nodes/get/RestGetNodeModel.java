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

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.RuntimeDelegate;

import org.apache.cxf.jaxrs.client.spec.ClientBuilderImpl;
import org.apache.cxf.jaxrs.impl.RuntimeDelegateImpl;
import org.knime.base.data.xml.SvgCell;
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
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.blob.BinaryObjectDataCell;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.image.png.PNGImageContent;
import org.knime.core.data.json.JSONCell;
import org.knime.core.data.uri.URIDataValue;
import org.knime.core.data.xml.XMLCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortType;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.Pair;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.rest.generic.ResponseBodyParser;
import org.knime.rest.generic.ResponseBodyParser.Default;
import org.knime.rest.nodes.get.RestGetSettings.RequestHeaderKeyItem;
import org.knime.rest.nodes.get.RestGetSettings.ResponseHeaderItem;
import org.knime.rest.util.DelegatingX509TrustManager;

/**
 *
 * @author Gabor Bakos
 */
class RestGetNodeModel extends NodeModel {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(RestGetNodeModel.class);

    private final RestGetSettings m_settings = new RestGetSettings();

    private DataColumnSpec[] m_newColumnsBasedOnFirstCall;

    private DataCell[] m_firstCallValues;

    private long m_consumedRows = 0L;

    private RowKey m_firstRow;

    private final ArrayList<ResponseHeaderItem> m_responseHeaderKeys = new ArrayList<>(),
            m_bodyColumns = new ArrayList<>();

    private boolean m_isContextSettingsFailed = false;

    //    private BinaryObjectCellFactory m_binaryObjectCellFactory;

    // contains response body parsers in the order of most specifics first, most generics last.
    private final List<ResponseBodyParser> m_responseBodyParsers = new ArrayList<>();

    private static final FromString FALLBACK = new FromString() {

        @Override
        public DataType getDataType() {
            return StringCell.TYPE;
        }

        @Override
        public DataCell createCell(final String input) {
            return new MissingCell("No cell factory was found" + input);
        }

    };

    /**
     *
     */
    public RestGetNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE_OPTIONAL}, new PortType[]{BufferedDataTable.TYPE});
        System.setProperty(ClientBuilder.JAXRS_DEFAULT_CLIENT_BUILDER_PROPERTY, ClientBuilderImpl.class.getName());
        System.setProperty(RuntimeDelegate.JAXRS_RUNTIME_DELEGATE_PROPERTY, RuntimeDelegateImpl.class.getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        // we do not know the exact columns (like type of body) without making a REST call, so return no table spec.
        return new DataTableSpec[]{null};
    }

    /**
     * {@inheritDoc}
     *
     * @throws InvalidSettingsException
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws CanceledExecutionException, InvalidSettingsException {
        //TODO can we use dependency injection here? http://www.vogella.com/tutorials/EclipseExtensionPoint/article.html
        //        m_binaryObjectCellFactory = new BinaryObjectCellFactory(exec);
        createResponseBodyParsers(exec);
        if (inData.length > 0 && inData[0] != null) {
            if (inData[0].size() == 0) {
                //No calls to make.
                return inData;
            }
            final DataTableSpec spec = inData[0].getDataTableSpec();
            try (final CloseableRowIterator iterator = inData[0].iterator()) {
                makeFirstCall(iterator.next(), spec, exec);
            }
            m_consumedRows = 1L;
            final ColumnRearranger rearranger = createColumnRearranger(spec, exec, inData[0].size());
            return new BufferedDataTable[]{exec.createColumnRearrangeTable(inData[0], rearranger, exec)};
        }
        makeFirstCall(null/*row*/, null/*spec*/, exec);
        return createTableFromFirstCallData(exec);
    }

    /**
     * @param row
     * @param spec
     * @param exec
     */
    private void makeFirstCall(final DataRow row, final DataTableSpec spec, final ExecutionContext exec) {
        m_firstRow = row.getKey();
        final UniqueNameGenerator nameGenerator = new UniqueNameGenerator(spec == null ? new DataTableSpec() : spec);
        final Response response =
            createRequest(spec == null ? -1 : spec.findColumnIndex(m_settings.getUriColumn()), row, spec).buildGet()
                .invoke();
        final List<DataCell> cells;
        final List<DataColumnSpec> specs = new ArrayList<>();
        if (m_settings.isExtractAllResponseFields()) {
            Stream
                .concat(Stream.of(Pair.create("Status", IntCell.TYPE)),
                    response.getStringHeaders().keySet().stream().map(header -> Pair.create(header, StringCell.TYPE)))
                .forEachOrdered(pair -> {
                    m_responseHeaderKeys.add(new ResponseHeaderItem(pair.getFirst(), pair.getSecond(),
                        nameGenerator.newColumn(pair.getFirst(), pair.getSecond()).getName()));
                });
        } else {
            m_responseHeaderKeys
                .addAll(
                    m_settings
                        .getExtractFields().stream().map(rhi -> new ResponseHeaderItem(rhi.getHeaderKey(),
                            rhi.getType(), nameGenerator.newName(rhi.getOutputColumnName())))
                    .collect(Collectors.toList()));
        }
        cells = m_responseHeaderKeys.stream().map(rhi -> {
            specs.add(nameGenerator.newColumn(rhi.getOutputColumnName(), rhi.getType()));
            DataCellFactory cellFactory = rhi.getType().getCellFactory(null).orElseGet(() -> FALLBACK);
            //List<Object> values = headers.get(e.getKey());
            if ("Status".equals(rhi.getHeaderKey()) && rhi.getType().isCompatible(IntValue.class)) {
                return new IntCell(response.getStatus());
            }
            if (cellFactory instanceof FromString) {
                FromString fromString = (FromString)cellFactory;
                String value = response.getHeaderString(rhi.getHeaderKey());
                if (value == null) {
                    return DataType.getMissingCell();
                }
                return fromString.createCell(value);
            }
            return DataType.getMissingCell();
        }).collect(Collectors.toList());
        examineResponse(response);
        for (ResponseHeaderItem bodyCol : m_bodyColumns) {
            specs.add(nameGenerator.newColumn(bodyCol.getOutputColumnName(), bodyCol.getType()));
        }
        addBodyValues(cells, response);
        m_firstCallValues = cells.toArray(new DataCell[cells.size()]);
        m_newColumnsBasedOnFirstCall = specs.toArray(new DataColumnSpec[specs.size()]);
    }

    /**
     * @param exec
     */
    private void createResponseBodyParsers(final ExecutionContext exec) {
        m_responseBodyParsers.clear();
        m_responseBodyParsers.add(new Default(MediaType.APPLICATION_JSON_TYPE, JSONCell.TYPE, exec)/* {
                                                                                                   @Override
                                                                                                   public String valueDescriptor() {
                                                                                                   return "JSON";
                                                                                                   }
                                                                                                   }*/);
        m_responseBodyParsers.add(new Default(MediaType.valueOf("image/png"), PNGImageContent.TYPE, exec)/* {
                                                                                                         @Override
                                                                                                         public String valueDescriptor() {
                                                                                                         return "PNG";
                                                                                                         }
                                                                                                         }*/);
        m_responseBodyParsers.add(new Default(MediaType.APPLICATION_SVG_XML_TYPE, SvgCell.TYPE, exec)/* {
                                                                                                     @Override
                                                                                                     public String valueDescriptor() {
                                                                                                     return "XML/SVG";
                                                                                                     }
                                                                                                     }*/);
        for (MediaType mediaType : new MediaType[]{MediaType.APPLICATION_ATOM_XML_TYPE,
            MediaType.APPLICATION_XHTML_XML_TYPE, MediaType.APPLICATION_XML_TYPE, MediaType.TEXT_XML_TYPE}) {
            m_responseBodyParsers.add(new Default(mediaType, XMLCell.TYPE, exec)/* {
                                                                                @Override
                                                                                public String valueDescriptor() {
                                                                                return "XML";
                                                                                }
                                                                                }*/);
        }
        for (MediaType mediaType : new MediaType[]{MediaType.TEXT_HTML_TYPE, MediaType.TEXT_PLAIN_TYPE}) {
            m_responseBodyParsers.add(new Default(mediaType, StringCell.TYPE, exec)/* {
                                                                                   @Override
                                                                                   public String valueDescriptor() {
                                                                                   return "text";
                                                                                   }
                                                                                   }*/);
        }
        for (MediaType mediaType : new MediaType[]{MediaType.APPLICATION_OCTET_STREAM_TYPE,
            MediaType.APPLICATION_FORM_URLENCODED_TYPE, MediaType.MULTIPART_FORM_DATA_TYPE/*TODO ??*/}) {
            m_responseBodyParsers.add(new Default(mediaType, BinaryObjectDataCell.TYPE, exec)/* {
                                                                                             @Override
                                                                                             public String valueDescriptor() {
                                                                                             return "file";
                                                                                             }
                                                                                             }*/);
        }
        //everything else is a file
        m_responseBodyParsers.add(new Default(MediaType.WILDCARD_TYPE, BinaryObjectDataCell.TYPE, exec)/* {
                                                                                                       @Override
                                                                                                       public String valueDescriptor() {
                                                                                                       return "file";
                                                                                                       }
                                                                                                       }*/);

    }

    /**
     * @param exec
     * @return
     */
    private BufferedDataTable[] createTableFromFirstCallData(final ExecutionContext exec) {
        final DataTableSpec spec = new DataTableSpec(m_newColumnsBasedOnFirstCall);
        final BufferedDataContainer container = exec.createDataContainer(spec, false);
        container.addRowToTable(new DefaultRow(RowKey.createRowKey(0L), m_firstCallValues));
        container.close();
        return new BufferedDataTable[]{container.getTable()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_responseBodyParsers.clear();
        m_bodyColumns.clear();
        m_consumedRows = 0L;
        m_firstCallValues = null;
        m_firstRow = null;
        m_responseHeaderKeys.clear();
        m_newColumnsBasedOnFirstCall = null;
        m_isContextSettingsFailed = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputPortRole[] getInputPortRoles() {
        //        if (m_settings.isExtractAllResponseFields()) {
        return new InputPortRole[]{InputPortRole.NONDISTRIBUTED_STREAMABLE};
        //        } else {
        //            return new InputPortRole[]{InputPortRole.DISTRIBUTED_STREAMABLE};
        //        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.NONDISTRIBUTED};
    }

    private ColumnRearranger createColumnRearranger(final DataTableSpec spec, final ExecutionMonitor exec,
        final long tableSize) throws InvalidSettingsException {
        final ColumnRearranger rearranger = new ColumnRearranger(spec);
        final DataColumnSpec[] newColumns = createNewColumnsSpec();
        final int uriColumn = spec.findColumnIndex(m_settings.getUriColumn());
        final AbstractCellFactory factory = new AbstractCellFactory(newColumns) {
            @Override
            public DataCell[] getCells(final DataRow row) {
                if (row.getKey().equals(m_firstRow)) {
                    return m_firstCallValues;
                }
                assert m_consumedRows > 0;
                final Builder request = createRequest(uriColumn, row, spec);
                final List<DataCell> cells;
                try {
                    final Response response = request.buildGet().invoke();
                    try {
                        //MultivaluedMap<String, Object> headers = response.getHeaders();
                        //examineResponse(response);
                        //cells = m_settings.getExtractFields().stream().map(rhi -> {
                        cells = m_responseHeaderKeys.stream().map(rhi -> {
                            DataCellFactory cellFactory = rhi.getType().getCellFactory(null).orElseGet(() -> FALLBACK);
                            //List<Object> values = headers.get(e.getKey());
                            if ("Status".equals(rhi.getHeaderKey()) && rhi.getType().isCompatible(IntValue.class)) {
                                return new IntCell(response.getStatus());
                            }
                            if (cellFactory instanceof FromString) {
                                FromString fromString = (FromString)cellFactory;
                                String value = response.getHeaderString(rhi.getHeaderKey());
                                if (value == null) {
                                    return DataType.getMissingCell();
                                }
                                return fromString.createCell(value);
                            }
                            return DataType.getMissingCell();
                        }).collect(Collectors.toList());
                        addBodyValues(cells, response);
                    } finally {
                        response.close();
                    }
                    //TODO wait only when we are requesting from the same domain?
                    if (m_settings.isUseDelay()) {
                        for (long wait = m_settings.getDelay(); wait > 0; wait -= 100L) {
                            exec.setMessage("Waiting till next call: " + wait / 1000d + "s");
                            try {
                                Thread.sleep(Math.min(wait, 100L));
                            } catch (InterruptedException e) {
                                exec.checkCanceled();
                            }
                        }
                    }
                } catch (CanceledExecutionException e) {
                    //Cannot check for cancelled properly, so this workaround
                    throw new IllegalStateException(e);
                } finally {
                    m_consumedRows++;
                }
                setProgress(m_consumedRows, tableSize, row.getKey(), exec);
                return cells.toArray(new DataCell[cells.size()]);
            }
        };
        final int concurrency = Math.max(1, m_settings.getConcurrency());
        factory.setParallelProcessing(true, concurrency, 4 * concurrency);
        rearranger.append(factory);
        return rearranger;
    }

    /**
     * @param response
     */
    private void examineResponse(final Response response) {
        if (response.hasEntity()) {
            final MediaType mediaType = response.getMediaType();
            DataType type = BinaryObjectDataCell.TYPE;
            for (final ResponseBodyParser responseBodyParser : m_responseBodyParsers) {
                if (responseBodyParser.supportedMediaType().isCompatible(mediaType)) {
                    type = responseBodyParser.producedDataType();
                    break;
                }
            }
            m_bodyColumns.add(new ResponseHeaderItem(m_settings.getResponseBodyColumn(), type));
        }
    }

    /**
     * @return
     */
    private DataColumnSpec[] createNewColumnsSpec() {
        return Stream.concat(m_responseHeaderKeys.stream(), m_bodyColumns.stream())
            .map(rhi -> new DataColumnSpecCreator(rhi.getOutputColumnName(), rhi.getType()))
            .map(creator -> creator.createSpec()).toArray(n -> new DataColumnSpec[n]);
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

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // TODO Auto-generated method stub

    }

    /**
     * @return The client to be used for the request.
     */
    protected Client createClient() {
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
        return client;
    }

    /**
     * @param uriColumn
     * @param row
     * @return
     */
    @SuppressWarnings("null")
    private Builder createRequest(final int uriColumn, final DataRow row, final DataTableSpec spec) {
        final Client client = createClient();
        CheckUtils.checkState(m_settings.isUseConstantURI() || row != null,
            "Without the constant uri and input, it is not possible to call a REST service!");
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
                    throw new UnsupportedOperationException("Unknown: " + headerItem.getKind() + " in: " + headerItem);
            }
            request.header(headerItem.getKey(), value);
        }
        return request;
    }

    /**
     * @param cells
     * @param response
     */
    protected void addBodyValues(final List<DataCell> cells, final Response response) {
        m_bodyColumns.stream().forEachOrdered(rhi -> {
            if (response.hasEntity()) {
                DataType expectedType = rhi.getType();
                MediaType mediaType = response.getMediaType();
                if (mediaType == null) {
                    cells.add(DataType.getMissingCell());
                } else {
                    boolean wasAdded = false;
                    for (ResponseBodyParser parser : m_responseBodyParsers) {
                        if (expectedType.isCompatible(parser.producedDataType().getPreferredValueClass())) {
                            wasAdded = true;
                            //TODO use mediaType.getParameters().get("charset");
                            //when possible
                            cells.add(parser.create(response));
                            break;
                        }
                    }
                    if (!wasAdded) {
                        cells.add(new MissingCell("Could not parse the body because the body was "
                            + response.getMediaType() + ", but was expecting: " + expectedType.getName()));
                    }
                    //                                    if (MediaType.APPLICATION_JSON_TYPE.isCompatible(mediaType)) {
                    //                                        if (expectedType.isCompatible(JSONValue.class)) {
                    //                                            try {
                    //                                                cells.add(
                    //                                                    JSONCellFactory.create((InputStream)response.getEntity(), false));
                    //                                            } catch (final IOException e1) {
                    //                                                cells.add(new MissingCell(e1.getMessage()));
                    //                                            }
                    //                                        } else {
                    //                                            cells.add(new MissingCell("The value in the body has " + mediaType
                    //                                                + ", but was expecting a JSON value."));
                    //                                        }
                    //                                    } else if (MediaType.valueOf("image/png").isCompatible(mediaType)) {
                    //                                        if (expectedType.isCompatible(PNGImageValue.class)) {
                    //                                            try {
                    //                                                cells.add(
                    //                                                    PNGImageCellFactory.create(response.readEntity(InputStream.class)));
                    //                                            } catch (IOException e) {
                    //                                                cells.add(new MissingCell(e.getMessage()));
                    //                                            }
                    //                                        } else {
                    //                                            cells.add(new MissingCell("The value in the body has " + mediaType
                    //                                                + ", but was expecting a png value."));
                    //                                        }
                    //                                    } else if (MediaType.APPLICATION_SVG_XML_TYPE.isCompatible(mediaType)) {
                    //                                        if (expectedType.isCompatible(XMLValue.class)) {
                    //                                            try {
                    //                                                cells.add(new SvgCellFactory()
                    //                                                    .createCell(response.readEntity(InputStream.class)));
                    //                                            } catch (IOException e1) {
                    //                                                cells.add(new MissingCell(e1.getMessage()));
                    //                                            }
                    //                                        } else {
                    //                                            cells.add(new MissingCell("The value in the body has " + mediaType
                    //                                                + ", but was expecting an XML/SVG value."));
                    //                                        }
                    //                                    } else if (MediaType.APPLICATION_XML_TYPE.isCompatible(mediaType)
                    //                                        || MediaType.TEXT_XML_TYPE.isCompatible(mediaType)
                    //                                        || MediaType.APPLICATION_ATOM_XML_TYPE.isCompatible(mediaType)
                    //                                        || MediaType.APPLICATION_XHTML_XML_TYPE.isCompatible(mediaType)) {
                    //                                        if (expectedType.isCompatible(XMLValue.class)) {
                    //                                            try {
                    //                                                cells.add(XMLCellFactory.create((InputStream)response.getEntity()));
                    //                                            } catch (IOException | ParserConfigurationException | SAXException
                    //                                                    | XMLStreamException e1) {
                    //                                                cells.add(new MissingCell(e1.getMessage()));
                    //                                            }
                    //                                        } else {
                    //                                            cells.add(new MissingCell("The value in the body has " + mediaType
                    //                                                + ", but was expecting an XML value."));
                    //                                        }
                    //                                    } else if (MediaType.TEXT_PLAIN_TYPE.isCompatible(mediaType)
                    //                                        || MediaType.TEXT_HTML_TYPE.isCompatible(mediaType)) {
                    //                                        if (expectedType.isCompatible(StringValue.class)) {
                    //                                            String responseText = response.readEntity(String.class);
                    //                                            cells.add(new StringCell(responseText));
                    //                                        } else {
                    //                                            cells.add(new MissingCell("The value in the body has " + mediaType
                    //                                                + ", but was expecting a String value."));
                    //                                        }
                    //                                    } else if (MediaType.APPLICATION_OCTET_STREAM_TYPE.isCompatible(mediaType)
                    //                                        || true/*TODO make it use an extension and provide this as a fallback*/) {
                    //                                        if (expectedType.isCompatible(BinaryObjectDataValue.class)) {
                    //                                            InputStream is = response.readEntity(InputStream.class);
                    //                                            try {
                    //                                                cells.add(m_binaryObjectCellFactory.create(is));
                    //                                            } catch (IOException e) {
                    //                                                cells.add(new MissingCell(e.getMessage()));
                    //                                            }
                    //                                        } else {
                    //                                            cells.add(new MissingCell("The value in the body has " + mediaType
                    //                                                + ", but was expecting binary data value."));
                    //                                        }
                    //                                    } else {
                    //                                        cells.add(DataType.getMissingCell());
                    //                                    }
                }
            } else {
                cells.add(DataType.getMissingCell());
            }
        });
    }
}
