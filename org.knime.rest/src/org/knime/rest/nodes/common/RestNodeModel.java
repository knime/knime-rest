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
 *   2016. ápr. 23. (Gabor Bakos): created
 */
package org.knime.rest.nodes.common;

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.RuntimeDelegate;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.client.spec.ClientBuilderImpl;
import org.apache.cxf.jaxrs.impl.RuntimeDelegateImpl;
import org.eclipse.e4.core.di.annotations.Execute;
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
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.Pair;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.rest.generic.EachRequestAuthentication;
import org.knime.rest.generic.ResponseBodyParser;
import org.knime.rest.generic.ResponseBodyParser.Default;
import org.knime.rest.nodes.common.RestSettings.RequestHeaderKeyItem;
import org.knime.rest.nodes.common.RestSettings.ResponseHeaderItem;
import org.knime.rest.util.DelegatingX509TrustManager;

/**
 * Common {@link NodeModel} for the REST nodes.
 *
 * @author Gabor Bakos
 * @param <S> The actual type of the {@link RestSettings}.
 */
public abstract class RestNodeModel<S extends RestSettings> extends NodeModel {

    /**
     *
     */
    private static final int MAX_RETRANSITS = 2;

    private static final NodeLogger LOGGER = NodeLogger.getLogger(RestNodeModel.class);

    private final S m_settings = createSettings();

    private DataColumnSpec[] m_newColumnsBasedOnFirstCall;

    private DataCell[] m_firstCallValues;

    private long m_consumedRows = 0L;

    private RowKey m_firstRow;

    private final ArrayList<ResponseHeaderItem> m_responseHeaderKeys = new ArrayList<>();

    private final ArrayList<ResponseHeaderItem> m_bodyColumns = new ArrayList<>();

    private boolean m_isContextSettingsFailed = false;

    private final List<ResponseBodyParser> m_responseBodyParsers = new ArrayList<>();

    static final String STATUS = "Status";

    /** Fallback for the {@link FromString} creating missing cells. */
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
     * Constructor with {@link BufferedDataTable}s in input/output ports.
     *
     * @param nrInDataPorts Number of input data ports.
     * @param nrOutDataPorts Number of output data ports.
     */
    protected RestNodeModel(final int nrInDataPorts, final int nrOutDataPorts) {
        super(nrInDataPorts, nrOutDataPorts);
        init();
    }

    /**
     * Constructor with {@link PortObject}s in input/output ports.
     *
     * @param inPortTypes The input port types.
     * @param outPortTypes The output port types.
     */
    protected RestNodeModel(final PortType[] inPortTypes, final PortType[] outPortTypes) {
        super(inPortTypes, outPortTypes);
        init();
    }

    /**
     * Common constructor for descendent classes with the default optional input table and a single output table.
     */
    protected RestNodeModel() {
        this(new PortType[]{BufferedDataTable.TYPE_OPTIONAL}, new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * @return The newly created {@link RestSettings} (derived class {@code <S>}) instance.
     */
    protected abstract S createSettings();

    /**
     * @return the settings
     */
    protected final S getSettings() {
        return m_settings;
    }

    /**
     * Inits JAX-RS to use CXF, only called in constructors.
     */
    protected void init() {
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
    @Execute
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws CanceledExecutionException, InvalidSettingsException {
        final List<EachRequestAuthentication> enabledEachRequestAuthentications =
            m_settings.getAuthorizationConfigurations().parallelStream()
                .filter(euc -> euc.isEnabled() && euc.getUserConfiguration() instanceof EachRequestAuthentication)
                .map(euc -> (EachRequestAuthentication)euc.getUserConfiguration()).collect(Collectors.toList());
        //        m_binaryObjectCellFactory = new BinaryObjectCellFactory(exec);
        createResponseBodyParsers(exec);
        if (inData.length > 0 && inData[0] != null) {
            if (inData[0].size() == 0) {
                //No calls to make.
                return inData;
            }
            final DataTableSpec spec = inData[0].getDataTableSpec();
            try (final CloseableRowIterator iterator = inData[0].iterator()) {
                makeFirstCall(iterator.next(), enabledEachRequestAuthentications, spec, exec);
            }
            m_consumedRows = 1L;
            final ColumnRearranger rearranger =
                createColumnRearranger(enabledEachRequestAuthentications, spec, exec, inData[0].size());
            return new BufferedDataTable[]{exec.createColumnRearrangeTable(inData[0], rearranger, exec)};
        }
        makeFirstCall(null/*row*/, enabledEachRequestAuthentications, null/*spec*/, exec);
        return createTableFromFirstCallData(exec);
    }

    /**
     * Makes the first call to the servers, this will create the structure of the output table.
     *
     * @param row The input row.
     * @param enabledEachRequestAuthentications The single enabled authentication.
     * @param spec The {@link DataTableSpec}.
     * @param exec An {@link ExecutionContext}.
     * @throws ProcessingException When something went wrong client-side.
     */
    protected void makeFirstCall(final DataRow row,
        final List<EachRequestAuthentication> enabledEachRequestAuthentications, final DataTableSpec spec,
        final ExecutionContext exec) throws ProcessingException {
        m_firstRow = row == null ? null : row.getKey();
        final UniqueNameGenerator nameGenerator = new UniqueNameGenerator(spec == null ? new DataTableSpec() : spec);
        Response response;
        DataCell missing;
        try {
            response =
                invoke(invocation(createRequest(spec == null ? -1 : spec.findColumnIndex(m_settings.getUriColumn()),
                    enabledEachRequestAuthentications, row, spec), row, spec));
            missing = null;
        } catch (final ProcessingException procEx) {
            LOGGER.warn("First call failed: " + procEx.getMessage(), procEx);
            if (m_settings.isFailOnConnectionProblems()) {
                throw new IllegalStateException(procEx);
            }
            missing = new MissingCell(procEx.getMessage());
            response = null;
        }
        try {
            final List<DataCell> cells;
            final List<DataColumnSpec> specs = new ArrayList<>();
            if (m_settings.isExtractAllResponseFields()) {
                Stream
                    .concat(Stream.of(Pair.create(STATUS, IntCell.TYPE)),
                        (response == null ? Collections.<String> emptyList() : response.getStringHeaders().keySet())
                            .stream().map(header -> Pair.create(header, StringCell.TYPE)))
                    .forEachOrdered(pair -> m_responseHeaderKeys.add(new ResponseHeaderItem(pair.getFirst(),
                        pair.getSecond(), nameGenerator.newColumn(pair.getFirst(), pair.getSecond()).getName())));
            } else {
                m_responseHeaderKeys
                    .addAll(m_settings
                        .getExtractFields().stream().map(rhi -> new ResponseHeaderItem(rhi.getHeaderKey(),
                            rhi.getType(), nameGenerator.newName(rhi.getOutputColumnName())))
                        .collect(Collectors.toList()));
            }
            final Response finalResponse = response;
            checkResponse(response);
            cells = m_responseHeaderKeys.stream().map(rhi -> {
                specs.add(new DataColumnSpecCreator(rhi.getOutputColumnName(), rhi.getType()).createSpec());
                final DataCellFactory cellFactory = rhi.getType().getCellFactory(null).orElseGet(() -> FALLBACK);
                if (STATUS.equals(rhi.getHeaderKey()) && rhi.getType().isCompatible(IntValue.class)) {
                    return finalResponse == null ? DataType.getMissingCell() : new IntCell(finalResponse.getStatus());
                }
                if (cellFactory instanceof FromString) {
                    final FromString fromString = (FromString)cellFactory;
                    final String value =
                        finalResponse == null ? null : finalResponse.getHeaderString(rhi.getHeaderKey());
                    if (value == null) {
                        return DataType.getMissingCell();
                    }
                    return fromString.createCell(value);
                }
                return DataType.getMissingCell();
            }).collect(Collectors.toList());
            examineResponse(response);
            final Map<ResponseHeaderItem, String> bodyColNames = new HashMap<>();
            for (final ResponseHeaderItem bodyCol : m_bodyColumns) {
                final DataColumnSpec newCol = nameGenerator.newColumn(bodyCol.getOutputColumnName(), bodyCol.getType());
                specs.add(newCol);
                bodyColNames.put(bodyCol, newCol.getName());
            }
            for (int i = 0; i < m_bodyColumns.size(); ++i) {
                m_bodyColumns.set(i, new ResponseHeaderItem(m_bodyColumns.get(i).getHeaderKey(),
                    m_bodyColumns.get(i).getType(), bodyColNames.get(m_bodyColumns.get(i))));
            }
            addBodyValues(cells, response, missing);
            m_firstCallValues = cells.toArray(new DataCell[cells.size()]);
            m_newColumnsBasedOnFirstCall = specs.toArray(new DataColumnSpec[specs.size()]);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    /**
     * Creates an {@link Invocation} from the {@code request}.
     *
     * @param request A {@link Builder}.
     * @param row A {@link DataRow} (which might contain the body value).
     * @param spec The input {@link DataTableSpec} (to find the body column in the {@code row}).
     * @return The created {@link Invocation}.
     */
    protected abstract Invocation invocation(final Builder request, final DataRow row, final DataTableSpec spec);

    /**
     * Invokes the {@code invocation}.
     *
     * @param invocation An {@link Invocation}.
     * @return The response.
     * @throws ProcessingException Some problem client-side.
     */
    protected Response invoke(final Invocation invocation) throws ProcessingException {
        final Future<Response> responseFuture = invocation.submit();
        try {
            return responseFuture.get(m_settings.getTimeoutInSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new ProcessingException(e);
        }
    }

    /**
     * Creates the response body parsers.
     *
     * @param exec An {@link ExecutionContext}.
     */
    private void createResponseBodyParsers(final ExecutionContext exec) {
        m_responseBodyParsers.clear();
        for (final MediaType mediaType : new MediaType[]{MediaType.APPLICATION_JSON_TYPE,
            MediaType.valueOf("application/vnd.mason+json")}) {
            m_responseBodyParsers.add(new Default(mediaType, JSONCell.TYPE, exec));
        }
        m_responseBodyParsers.add(new Default(MediaType.valueOf("image/png"), PNGImageContent.TYPE, exec));
        m_responseBodyParsers.add(new Default(MediaType.APPLICATION_SVG_XML_TYPE, SvgCell.TYPE, exec));
        for (final MediaType mediaType : new MediaType[]{MediaType.APPLICATION_ATOM_XML_TYPE,
            MediaType.APPLICATION_XHTML_XML_TYPE, MediaType.APPLICATION_XML_TYPE, MediaType.TEXT_XML_TYPE}) {
            m_responseBodyParsers.add(new Default(mediaType, XMLCell.TYPE, exec));
        }
        for (final MediaType mediaType : new MediaType[]{MediaType.TEXT_HTML_TYPE, MediaType.TEXT_PLAIN_TYPE}) {
            m_responseBodyParsers.add(new Default(mediaType, StringCell.TYPE, exec));
        }
        for (final MediaType mediaType : new MediaType[]{MediaType.APPLICATION_OCTET_STREAM_TYPE,
            MediaType.APPLICATION_FORM_URLENCODED_TYPE, MediaType.MULTIPART_FORM_DATA_TYPE/*TODO ??*/}) {
            m_responseBodyParsers.add(new Default(mediaType, BinaryObjectDataCell.TYPE, exec));
        }
        //everything else is a file
        m_responseBodyParsers.add(new Default(MediaType.WILDCARD_TYPE, BinaryObjectDataCell.TYPE, exec));
    }

    /**
     * Creates the data table only based on the first call.
     *
     * @param exec An {@link ExecutionContext}.
     * @return The array of a single {@link BufferedDataTable}.
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
        return new InputPortRole[]{InputPortRole.NONDISTRIBUTED_STREAMABLE};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.NONDISTRIBUTED};
    }

    /**
     * Creates the {@link ColumnRearranger}.
     *
     * @param enabledEachRequestAuthentications The selected authentication.
     * @param spec The input {@link DataTableSpec}.
     * @param exec {@link ExecutionMonitor}.
     * @param tableSize The size of the table.
     * @return The {@link ColumnRearranger}.
     * @throws InvalidSettingsException Invalid internal state.
     */
    private ColumnRearranger createColumnRearranger(
        final List<EachRequestAuthentication> enabledEachRequestAuthentications, final DataTableSpec spec,
        final ExecutionMonitor exec, final long tableSize) throws InvalidSettingsException {
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
                final Builder request = createRequest(uriColumn, enabledEachRequestAuthentications, row, spec);
                final List<DataCell> cells;
                DataCell missing = null;
                try {
                    Response response;
                    try {
                        response = invoke(invocation(request, row, spec));
                    } catch (ProcessingException e) {
                        LOGGER.debug("Call failed: " + e.getMessage(), e);
                        if (m_settings.isFailOnConnectionProblems()) {
                            throw new IllegalStateException(e);
                        }
                        missing = new MissingCell(e.getMessage());
                        response = null;
                    }
                    try {
                        final Response finalResponse = response;
                        checkResponse(response);
                        cells = m_responseHeaderKeys.stream().map(rhi -> {
                            DataCellFactory cellFactory = rhi.getType().getCellFactory(null).orElseGet(() -> FALLBACK);
                            if (STATUS.equals(rhi.getHeaderKey()) && rhi.getType().isCompatible(IntValue.class)) {
                                return finalResponse == null ? DataType.getMissingCell()
                                    : new IntCell(finalResponse.getStatus());
                            }
                            if (cellFactory instanceof FromString) {
                                FromString fromString = (FromString)cellFactory;
                                String value =
                                    finalResponse == null ? null : finalResponse.getHeaderString(rhi.getHeaderKey());
                                if (value == null) {
                                    return DataType.getMissingCell();
                                }
                                return fromString.createCell(value);
                            }
                            return DataType.getMissingCell();
                        }).collect(Collectors.toList());
                        addBodyValues(cells, response, missing);
                    } finally {
                        if (response != null) {
                            response.close();
                        }
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
     * Checks the response code.
     *
     * @param response A {@link Response}.
     * @throws IllegalStateExcetion When the http status code is {@code 4xx} or {@code 5xx} and we have to check it.
     */
    private void checkResponse(final Response response) throws IllegalStateException {
        if (response != null && m_settings.isFailOnHttpErrors()) {
            if (response.getStatus() >= 400 && response.getStatus() < 600) {
                throw new IllegalStateException(
                    "Wrong status: " + response.getStatus() + " " + response.getStatusInfo().getReasonPhrase());
            }
        }
    }

    /**
     * Checks the {@code response} object and adds the body columns.
     *
     * @param response A {@link Response} object.
     */
    private void examineResponse(final Response response) {
        if (response == null) {
            m_bodyColumns.add(new ResponseHeaderItem(m_settings.getResponseBodyColumn(), BinaryObjectDataCell.TYPE));
        } else if (response.hasEntity()) {
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
     * @return The new column specs.
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
        createSettings().loadSettingsFrom(settings);
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
        //No internals
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        //No internals
    }

    /**
     * @return The client to be used for the request.
     */
    protected Client createClient() {
        final ClientBuilder clientBuilder = ClientBuilder.newBuilder();
        if (m_settings.isSslTrustAll()) {
            final SSLContext context;
            try {
                clientBuilder.getConfiguration();
                context = SSLContext.getInstance(/*"Default"*/"TLS");
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
        return clientBuilder.build();
    }

    /**
     * Creates the request {@link Builder} based on input parameters.
     *
     * @param uriColumn The index of URI column.
     * @param enabledEachRequestAuthentications The authentications.
     * @param row The input {@link DataRow}.
     * @param spec The input table spec.
     * @return
     */
    @SuppressWarnings("null")
    private Builder createRequest(final int uriColumn,
        final List<EachRequestAuthentication> enabledEachRequestAuthentications, final DataRow row,
        final DataTableSpec spec) {
        final Client client = createClient();
        CheckUtils.checkState(m_settings.isUseConstantURI() || row != null,
            "Without the constant uri and input, it is not possible to call a REST service!");
        final WebTarget target = client.target(m_settings.isUseConstantURI() ? m_settings.getConstantURI()
            : row.getCell(uriColumn) instanceof URIDataValue
                ? ((URIDataValue)row.getCell(uriColumn)).getURIContent().getURI().toString()
                : ((StringValue)row.getCell(uriColumn)).getStringValue());

        final Builder request = target.request();
        WebClient.getConfig(request).getHttpConduit().getClient().setAutoRedirect(m_settings.isFollowRedirects());
        WebClient.getConfig(request).getHttpConduit().getClient().setMaxRetransmits(MAX_RETRANSITS);

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
                case CredentialName:
                    value = getCredentialsProvider().get(headerItem.getKey()).getLogin();
                    break;
                case CredentialPassword:
                    value = getCredentialsProvider().get(headerItem.getKey()).getPassword();
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown: " + headerItem.getKind() + " in: " + headerItem);
            }
            request.header(headerItem.getKey(), value);
        }
        for (final EachRequestAuthentication era : enabledEachRequestAuthentications) {
            era.updateRequest(request, row, getCredentialsProvider(), getAvailableFlowVariables());
        }
        return request;
    }

    /**
     * Adds the body values.
     *
     * @param cells The input {@link DataCell}s.
     * @param response The {@link Response} object.
     * @param missing The missing cell with the error message.
     */
    protected void addBodyValues(final List<DataCell> cells, final Response response, final DataCell missing) {
        m_bodyColumns.stream().forEachOrdered(rhi -> {
            if (response != null && response.hasEntity()) {
                DataType expectedType = rhi.getType();
                MediaType mediaType = response.getMediaType();
                if (mediaType == null) {
                    cells.add(DataType.getMissingCell());
                } else {
                    boolean wasAdded = false;
                    for (ResponseBodyParser parser : m_responseBodyParsers) {
                        if (expectedType.isCompatible(parser.producedDataType().getPreferredValueClass())) {
                            if (parser.supportedMediaType().isCompatible(response.getMediaType())) {
                                wasAdded = true;
                                cells.add(parser.create(response));
                                break;
                            }
                        }
                    }
                    if (!wasAdded) {
                        cells.add(new MissingCell("Could not parse the body because the body was "
                            + response.getMediaType() + ", but was expecting: " + expectedType.getName()));
                    }
                }
            } else {
                cells.add(missing);
            }
        });
    }

    /**
     * Computes the header value.
     *
     * @param row Input {@link DataRow}.
     * @param spec Input {@link DataTableSpec}.
     * @param headerItem {@link RequestHeaderKeyItem} for the specification of how to select the value.
     * @return The transformed value.
     */
    Object computeHeaderValue(final DataRow row, final DataTableSpec spec, final RequestHeaderKeyItem headerItem) {
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
            case CredentialName:
                value = getCredentialsProvider().get(headerItem.getKey()).getLogin();
                break;
            case CredentialPassword:
                value = getCredentialsProvider().get(headerItem.getKey()).getPassword();
                break;
            default:
                throw new UnsupportedOperationException("Unknown: " + headerItem.getKind() + " in: " + headerItem);
        }
        return value;
    }
}