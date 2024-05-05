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
 *   23. Apr. 2016. (Gabor Bakos): created
 */
package org.knime.rest.nodes.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.PatternBuilder;
import org.apache.cxf.transport.http.asyncclient.AsyncHTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.eclipse.e4.core.di.annotations.Execute;
import org.knime.base.data.xml.SvgCell;
import org.knime.core.data.DataCell;
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
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortObjectInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.streamable.StreamableOperatorInternals;
import org.knime.core.node.streamable.simple.SimpleStreamableOperatorInternals;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.ClassUtils;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;
import org.knime.core.node.workflow.VariableType.BooleanType;
import org.knime.core.node.workflow.VariableType.DoubleType;
import org.knime.core.node.workflow.VariableType.IntType;
import org.knime.core.node.workflow.VariableType.LongType;
import org.knime.core.node.workflow.VariableType.StringType;
import org.knime.core.util.Pair;
import org.knime.core.util.ThreadLocalHTTPAuthenticator;
import org.knime.core.util.ThreadLocalHTTPAuthenticator.AuthenticationCloseable;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.core.util.proxy.DisabledSchemesChecker;
import org.knime.credentials.base.Credential;
import org.knime.credentials.base.CredentialPortObject;
import org.knime.credentials.base.CredentialPortObjectSpec;
import org.knime.credentials.base.oauth.api.HttpAuthorizationHeaderCredentialValue;
import org.knime.rest.generic.EachRequestAuthentication;
import org.knime.rest.generic.ResponseBodyParser;
import org.knime.rest.generic.ResponseBodyParser.Default;
import org.knime.rest.generic.ResponseBodyParser.Missing;
import org.knime.rest.internals.HttpAuthorizationHeaderAuthentication;
import org.knime.rest.nodes.common.RestSettings.HttpMethod;
import org.knime.rest.nodes.common.RestSettings.ReferenceType;
import org.knime.rest.nodes.common.RestSettings.RequestHeaderKeyItem;
import org.knime.rest.nodes.common.RestSettings.ResponseHeaderItem;
import org.knime.rest.nodes.common.proxy.ProxyMode;
import org.knime.rest.nodes.common.proxy.RestProxyConfig;
import org.knime.rest.nodes.common.proxy.RestProxyConfigManager;
import org.knime.rest.util.CooldownContext;
import org.knime.rest.util.DelayPolicy;
import org.knime.rest.util.DelegatingX509TrustManager;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Common {@link NodeModel} for the REST nodes.
 *
 * @author Gabor Bakos
 * @param <S> The actual type of the {@link RestSettings}.
 */
public abstract class RestNodeModel<S extends RestSettings> extends NodeModel {
    private static final int MAX_RETRANSMITS = 4;

    private static final NodeLogger LOGGER = NodeLogger.getLogger(RestNodeModel.class);

    /**
     * The settings of this node model.
     */
    protected final S m_settings = createSettings();

    /**
     * The specs of the new columns if no input is connected.
     */
    protected DataColumnSpec[] m_newColumnsBasedOnFirstCalls;

    // Package scope needed for tests.
    final List<DataCell[]> m_firstCallValues = new ArrayList<>();

    private long m_consumedRows;

    private List<RowKey> m_firstRows;

    private boolean m_readNonError;

    private final ArrayList<ResponseHeaderItem> m_responseHeaderKeys = new ArrayList<>();

    private final ArrayList<ResponseHeaderItem> m_bodyColumns = new ArrayList<>();

    private boolean m_isContextSettingsFailed;

    private final List<ResponseBodyParser> m_responseBodyParsers = new ArrayList<>();

    private final List<ResponseBodyParser> m_errorBodyParsers = new ArrayList<>();

    private final int m_credentialPortIdx;

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
     * Holds context information regarding rate-limiting. Constructed freshly at the beginning of each node execution.
     */
    private CooldownContext m_cooldownContext;

    /**
     * Common constructor for descendent classes with the default optional input table and a single output table.
     * @param cfg The node creating configuration
     */
    protected RestNodeModel(final NodeCreationConfiguration cfg) {
        super(cfg.getPortConfig().orElseThrow(IllegalStateException::new).getInputPorts(),
            cfg.getPortConfig().orElseThrow(IllegalStateException::new).getOutputPorts());

        m_credentialPortIdx = cfg.getPortConfig().orElseThrow(IllegalStateException::new).getInputPortLocation()
            .getOrDefault(RestNodeFactory.CREDENTIAL_GROUP_ID, new int[]{-1})[0];
    }

    /**
     * Default constructor for proxy test..
     */
    protected RestNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE_OPTIONAL}, new PortType[]{BufferedDataTable.TYPE});

        m_credentialPortIdx = -1;
    }

    /**
     * @param res The server response
     * @return true iff the response corresponds to a 5XX HTTP status code.
     */
    public static boolean isServerError(final Response res) {
        return res == null || res.getStatusInfo().getFamily() == Response.Status.Family.SERVER_ERROR;
    }

    /**
     * @param res The server response
     * @return true iff the response corresponds to a 4XX HTTP status code
     */
    public static boolean isClientError(final Response res) {
        return res == null || res.getStatusInfo().getFamily() == Response.Status.Family.CLIENT_ERROR;
    }

    /**
     *
     * @param res The server response
     * @return true iff the response corresponds to a 429 (rate-limiting) HTTP status code.
     */
    public static boolean isRateLimitError(final Response res) {
        return res.getStatus() == 429;
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
     * @return the proxy manager
     */
    protected RestProxyConfigManager getProxyManager() {
        return m_settings.getProxyManager();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec inputSpec = (DataTableSpec)inSpecs[0];
        if (!m_settings.isUseConstantURI()) {
            String uriColumn = m_settings.getUriColumn();
            if (inputSpec == null) {
                throw new InvalidSettingsException(
                    "Input table required to execute. The node is configured to use a URL from the column '" + uriColumn
                        + "' of the input table.");
            } else {
                if (!inputSpec.containsName(uriColumn)) {
                    throw new InvalidSettingsException(
                        "The configured URL column '" + uriColumn + "' is missing in the input table.");
                }
            }
        }

        final List<RequestHeaderKeyItem> requestHeaders = m_settings.getRequestHeaders();
        final Map<String, FlowVariable> availableFlowVariables = getAvailableFlowVariables();
        for (final RequestHeaderKeyItem requestHeader : requestHeaders) {
            final var referenceType = requestHeader.getKind();
            if (referenceType == ReferenceType.FlowVariable) {
                final String requestHeaderFlowVariable = requestHeader.getValueReference();
                if (!availableFlowVariables.containsKey(requestHeaderFlowVariable)) {
                    throw new InvalidSettingsException(
                        "The request header '" + requestHeader.getKey() + "' is configured to use a flow variable '"
                            + requestHeaderFlowVariable + "' which is no longer present.");
                }
            }

            if (referenceType == ReferenceType.Column) {
                final String requestHeaderColumnName = requestHeader.getValueReference();
                if (inputSpec == null) {
                    throw new InvalidSettingsException(
                        "Input table required to execute. The request header '" + requestHeader.getKey() + "' is "
                            + "configured to use a column '" + requestHeaderColumnName + "' from the input table.");
                } else if (!inputSpec.containsName(requestHeaderColumnName)) {
                    throw new InvalidSettingsException(
                        "The request header '" + requestHeader.getKey() + "' is configured to use a column '"
                            + requestHeaderColumnName + "' which is no longer present in the input table.");
                }
            }
        }

        if (m_credentialPortIdx != -1) {
            var type = ((CredentialPortObjectSpec)inSpecs[m_credentialPortIdx]).getCredentialType().orElse(null);
            if (type != null
                && !HttpAuthorizationHeaderCredentialValue.class.isAssignableFrom(type.getCredentialClass())) {
                throw new InvalidSettingsException("Unsupported credential type: " + type.getName());

            }
        }

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
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec)
        throws Exception {
        BufferedDataTable inTable = (BufferedDataTable)inData[0];
        final List<EachRequestAuthentication> enabledEachRequestAuthentications =
            getAuthentications(getCredential(inData));
        createResponseBodyParsers(exec);
        // Issue a warning if no proxy config came in from the global settings.
        if (m_settings.getProxyManager().getProxyMode() == ProxyMode.GLOBAL
            && m_settings.getUpdatedProxyConfig(null).isEmpty()) {
            LOGGER.info("The KNIME-wide proxy settings are activated but none were specified. "
                + "Defaulting to using no proxy.");
        }
        if (inData.length > 0 && inData[0] != null) {
            if (inTable.size() == 0) {
                //No calls to make.
                return new BufferedDataTable[]{inTable};
            }
            final var spec = inTable.getDataTableSpec();
            try (final CloseableRowIterator iterator = inTable.iterator()) {
                while (!m_readNonError && iterator.hasNext()) {
                    makeFirstCall(iterator.next(), enabledEachRequestAuthentications, spec, exec);
                    m_consumedRows++;
                }
            }
            final var rearranger =
                createColumnRearranger(enabledEachRequestAuthentications, spec, exec, inTable.size());
            return new BufferedDataTable[]{exec.createColumnRearrangeTable(inTable, rearranger, exec)};
        }
        makeFirstCall(null/*row*/, enabledEachRequestAuthentications, null/*spec*/, exec);
        return createTableFromFirstCallData(exec);
    }

    private HttpAuthorizationHeaderCredentialValue getCredential(final PortObject[] portObjects) {
        if (m_credentialPortIdx > -1) {
            return getCredential((CredentialPortObject)portObjects[m_credentialPortIdx]);
        } else {
            return null;
        }
    }

    private static HttpAuthorizationHeaderCredentialValue getCredential(final CredentialPortObject portObject) {
        return (HttpAuthorizationHeaderCredentialValue)portObject.getCredential(Credential.class).orElseThrow(
            () -> new IllegalStateException("Credential is not available. Please re-execute the authenticator node."));
    }

    /**
     * Makes the first call to the servers, this will create the structure of the output table.
     *
     * @param row The input row.
     * @param enabledEachRequestAuthentications The single enabled authentication.
     * @param spec The {@link DataTableSpec}.
     * @param exec An {@link ExecutionContext}.
     * @throws Exception if something went wrong during the call
     */
    protected void makeFirstCall(final DataRow row,
        final List<EachRequestAuthentication> enabledEachRequestAuthentications, final DataTableSpec spec,
        final ExecutionContext exec) throws Exception {
        m_cooldownContext = new CooldownContext();  // reset context before execution.
        if (row == null) {
            m_firstRows = null;
        } else {
            if (m_firstRows == null) {
                m_firstRows = new ArrayList<>();
            }
            m_firstRows.add(row.getKey());
        }
        Response response;
        DataCell missing;
        Pair<Builder, Client> requestBuilderPair =
            createRequest(spec == null ? -1 : spec.findColumnIndex(m_settings.getUriColumn()),
                enabledEachRequestAuthentications, row, spec);
        try {
            var invocation = invocation(requestBuilderPair.getFirst(), row, spec);
            response =
                DelayPolicy.doWithDelays(m_settings.getDelayPolicy(), m_cooldownContext, () -> invoke(invocation));
            requestBuilderPair.getSecond().close();
            missing = null;
        } catch (ProcessingException procEx) {
            LOGGER.warn("Call #" + (m_consumedRows + 1) + " failed: " + procEx.getMessage(), procEx);

            var throwables = ExceptionUtils.getThrowableList(procEx);
            var rootCause = throwables.get(throwables.size() - 1);
            for (var t : throwables) {
                if (t instanceof IOException) {
                    rootCause = t;
                    break;
                }
            }

            if (m_settings.isFailOnConnectionProblems()) {
                throw (rootCause instanceof Exception ex) ? ex : new ProcessingException(rootCause);
            } else {
                missing = new MissingCell(rootCause.getMessage());
                response = null;
            }
        } finally {
            var client = requestBuilderPair.getSecond();
            client.close();
        }
        try {
            final List<DataCell> cells;
            final List<DataColumnSpec> specs = new ArrayList<>();
            final var nameGenerator = new UniqueNameGenerator(spec == null ? new DataTableSpec() : spec);
            if (m_responseHeaderKeys.isEmpty()) {
                if (m_settings.isExtractAllResponseFields()) {
                    Stream
                        .concat(Stream.of(Pair.create(STATUS, IntCell.TYPE)),
                            (response == null ? Collections.<String> emptyList() : response.getStringHeaders().keySet())
                                .stream().map(header -> new Pair<String, DataType>(header, StringCell.TYPE)))
                        .forEachOrdered(pair -> m_responseHeaderKeys.add(new ResponseHeaderItem(pair.getFirst(),
                            pair.getSecond(), nameGenerator.newColumn(pair.getFirst(), pair.getSecond()).getName())));
                } else {
                    m_responseHeaderKeys.addAll(m_settings
                        .getExtractFields().stream().map(rhi -> new ResponseHeaderItem(rhi.getHeaderKey(),
                            rhi.getType(), nameGenerator.newName(rhi.getOutputColumnName())))
                        .toList());
                }
            }
            final var finalResponse = response;
            cells = m_responseHeaderKeys.stream().map(rhi -> {
                specs.add(new DataColumnSpecCreator(rhi.getOutputColumnName(), rhi.getType()).createSpec());
                if (STATUS.equals(rhi.getHeaderKey()) && rhi.getType().isCompatible(IntValue.class)) {
                    return finalResponse == null ? DataType.getMissingCell() : new IntCell(finalResponse.getStatus());
                }
                if (rhi.getType().isCompatible(IntValue.class)) {
                    try {
                        return finalResponse == null ? DataType.getMissingCell()
                            : new IntCell(Integer.parseInt(finalResponse.getHeaderString(rhi.getHeaderKey())));
                    } catch (RuntimeException e) { //NOSONAR
                        return new MissingCell(e.getMessage());
                    }
                }
                final var cellFactory = rhi.getType().getCellFactory(null).orElseGet(() -> FALLBACK);
                if (cellFactory instanceof FromString fromString) {
                    final String value =
                        finalResponse == null ? null : finalResponse.getHeaderString(rhi.getHeaderKey());
                    if (value != null) {
                        return fromString.createCell(value);
                    }
                }
                return DataType.getMissingCell();
            }).collect(Collectors.toCollection(ArrayList<DataCell>::new));
            examineResponse(response);
            final var httpError = checkResponse(response);
            final Map<ResponseHeaderItem, String> bodyColNames = new HashMap<>();
            if (!httpError) {
                for (final ResponseHeaderItem bodyCol : m_bodyColumns) {
                    final DataColumnSpec newCol =
                        nameGenerator.newColumn(bodyCol.getOutputColumnName(), bodyCol.getType());
                    specs.add(newCol);
                    bodyColNames.put(bodyCol, newCol.getName());
                }
                for (var i = 0; i < m_bodyColumns.size(); ++i) {
                    m_bodyColumns.set(i, new ResponseHeaderItem(m_bodyColumns.get(i).getHeaderKey(),
                        m_bodyColumns.get(i).getType(), bodyColNames.get(m_bodyColumns.get(i))));
                }
            } else {
                if (response != null && response.getEntity() instanceof InputStream) {
                    replace(m_bodyColumns, new ResponseHeaderItem(m_bodyColumns.get(0).getHeaderKey(),
                        BinaryObjectDataCell.TYPE, m_bodyColumns.get(0).getHeaderKey()));
                }
            }
            m_readNonError = !httpError;
            addBodyValues(cells, response, missing);
            maybeAddErrorCause(cells);
            m_firstCallValues.add(cells.toArray(new DataCell[cells.size()]));
            m_newColumnsBasedOnFirstCalls = specs.toArray(new DataColumnSpec[specs.size()]);
        } finally {
            if (response != null) {
                try {
                    // try to close, but there is a known NPE from within CXF that we catch here
                    response.close();
                } catch (NullPointerException e) { // NOSONAR avoid CXF bug
                    LOGGER.warn("Closing the HTTP response failed with exception: " + e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Add a column for error causes (String descriptions) to the given list of <code>DataColumnSpec</code>s, which will
     * ultimately determine the node's output column specification. Do this only if the respective setting is enabled.
     * Use the given <code>nameGenerator</code> to find a name for the new column.
     *
     * @param specs The list of <code>DataColumnSpec</code>s to add the newly created column spec to.
     * @param nameGenerator The generator to use for finding a name for the column to be added.
     */
    protected void maybeAddErrorCauseColSpec(final List<DataColumnSpec> specs,
        final UniqueNameGenerator nameGenerator) {
        if (m_settings.isOutputErrorCause().orElse(RestSettings.DEFAULT_OUTPUT_ERROR_CAUSE)) {
            final DataColumnSpec errorCauseCol = nameGenerator.newColumn("Error Cause", StringCell.TYPE);
            specs.add(errorCauseCol);
        }
    }

    /**
     * Construct a String cell containing the error cause description if there was an error while performing the
     * request. Do this only if the respective user setting is enabled. In case the request was successful, a missing
     * value is added.
     *
     * @param cells A list of cells which will ultimately determine the output for the current row.
     */
    private void maybeAddErrorCause(final List<DataCell> cells) {
        if (m_settings.isOutputErrorCause().orElse(RestSettings.DEFAULT_OUTPUT_ERROR_CAUSE)) {
            List<String> errorCauses = cells.stream()//
                .flatMap(cell -> ClassUtils.castOptional(MissingCell.class, cell).stream())//
                .map(MissingCell::getError)//
                .toList();
            if (!errorCauses.isEmpty()) {
                String errorCausesReadable;
                if (errorCauses.stream().allMatch(Objects::isNull)) {
                    // We assume missing values always correspond to some error.
                    errorCausesReadable = "Unknown error";
                } else {
                    // However, there are error scenarios in which only some columns have a missing value with a cause.
                    // In this case we want to avoid printing "null" for the missing values with no associated cause,
                    // but still do print all non-null causes.
                    errorCausesReadable =
                        errorCauses.stream().filter(Objects::nonNull).collect(Collectors.joining("\n"));
                }
                cells.add(new StringCell(errorCausesReadable));
            } else {
                cells.add(DataType.getMissingCell());
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
        try (AuthenticationCloseable c = ThreadLocalHTTPAuthenticator.suppressAuthenticationPopups()) {
            final Future<Response> responseFuture = invocation.submit(Response.class);
            try {
                return responseFuture.get(m_settings.getTimeoutInSeconds(), TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) { //NOSONAR
                throw new ProcessingException(e);
            }
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
        m_responseBodyParsers.add(new Default(MediaType.valueOf("image/svg+xml"), SvgCell.TYPE, exec));
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

        m_errorBodyParsers.addAll(m_responseBodyParsers.stream()
            .filter(parser -> (MediaType.TEXT_PLAIN_TYPE.isCompatible(parser.supportedMediaType()) &&
                !parser.supportedMediaType().isWildcardType()) ||
                XMLCell.TYPE.equals(parser.producedDataType()) ||
                JSONCell.TYPE.equals(parser.producedDataType()))
            .map(Missing::new)//
            .toList());
        // Error codes for the rest of the content types
        m_errorBodyParsers.add(new Default(MediaType.WILDCARD_TYPE, DataType.getMissingCell().getType(), exec) {
            @Override
            public DataCell create(final Response response) {
                return new MissingCell(response.getStatusInfo().getReasonPhrase());
            }
        });
    }

    /**
     * Creates the data table only based on the first call. Only called if input is not connected.
     *
     * @param exec An {@link ExecutionContext}.
     * @return The array of a single {@link BufferedDataTable}.
     */
    private BufferedDataTable[] createTableFromFirstCallData(final ExecutionContext exec) {
        updateFirstCallColumnsOnHttpError(null);
        final var spec = new DataTableSpec(m_newColumnsBasedOnFirstCalls);
        final BufferedDataContainer container = exec.createDataContainer(spec, false);
        for (var i = 0; i < m_firstCallValues.size(); i++) {
            container.addRowToTable(new DefaultRow(RowKey.createRowKey((long)i), m_firstCallValues.get(i)));
        }
        container.close();
        return new BufferedDataTable[]{container.getTable()};
    }

    /**
     * @param inputSpec the input spec or null (non connected optional input)
     *
     */
    private void updateFirstCallColumnsOnHttpError(final DataTableSpec inputSpec) {
        if (!m_firstCallValues.isEmpty() && m_firstCallValues.get(0).length > m_newColumnsBasedOnFirstCalls.length) {
            m_newColumnsBasedOnFirstCalls = createNewColumnsSpec(inputSpec);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_responseBodyParsers.clear();
        m_bodyColumns.clear();
        m_consumedRows = 0L;
        m_firstCallValues.clear();
        m_firstRows = null;
        m_readNonError = false;
        m_responseHeaderKeys.clear();
        m_newColumnsBasedOnFirstCalls = null;
        m_isContextSettingsFailed = false;
        m_errorBodyParsers.clear();
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
        final ExecutionMonitor exec, final long tableSize) {
        final DataColumnSpec[] newColumns = createNewColumnsSpec(spec);
        final int uriColumn = spec.findColumnIndex(m_settings.getUriColumn());

        final AbstractCellFactory factory = new AbstractCellFactory(newColumns) {
            @Override
            public DataCell[] getCells(final DataRow row) {
                int firstRowIdx = m_firstRows.indexOf(row.getKey());
                if (firstRowIdx >= 0) {
                    return m_firstCallValues.get(firstRowIdx);
                }
                assert m_consumedRows > 0;
                final List<DataCell> cells;
                Pair<Builder, Client> requestBuilder = null;
                DataCell missing = null;
                try {
                    Response response;
                    try {
                        // Creating the request can cause an ISE, see AP-20219.
                        requestBuilder = createRequest(uriColumn, enabledEachRequestAuthentications, row, spec);
                        var invocation = invocation(requestBuilder.getFirst(), row, spec);
                        response = DelayPolicy.doWithDelays(m_settings.getDelayPolicy(), m_cooldownContext,
                            () -> invoke(invocation));
                    } catch (ProcessingException e) {
                        LOGGER.debug("Call failed: " + e.getMessage(), e);

                        var throwables = ExceptionUtils.getThrowableList(e);
                        var rootCause = throwables.get(throwables.size() - 1);
                        for (Throwable t : throwables) {
                            if (t instanceof IOException) {
                                rootCause = t;
                                break;
                            }
                        }

                        if (m_settings.isFailOnConnectionProblems()) {
                            throw new RuntimeException(rootCause); //NOSONAR
                        } else {
                            missing = new MissingCell(rootCause.getMessage());
                            response = null;
                        }
                    } catch (Exception e) {
                        LOGGER.debug("Call failed: " + e.getMessage(), e);
                        Throwable cause = ExceptionUtils.getRootCause(e);
                        throw new RuntimeException(cause); //NOSONAR
                    }
                    try {
                        final var finalResponse = response;
                        checkResponse(response);
                        cells = m_responseHeaderKeys.stream().map(rhi -> {
                            if (STATUS.equals(rhi.getHeaderKey()) && rhi.getType().isCompatible(IntValue.class)) {
                                return finalResponse == null ? DataType.getMissingCell()
                                    : new IntCell(finalResponse.getStatus());
                            }
                            final var cellFactory = rhi.getType().getCellFactory(null).orElseGet(() -> FALLBACK);
                            if (cellFactory instanceof FromString fromString) {
                                String value =
                                    finalResponse == null ? null : finalResponse.getHeaderString(rhi.getHeaderKey());
                                if (value != null) {
                                    return fromString.createCell(value);
                                }
                            }
                            return DataType.getMissingCell();
                        }).collect(Collectors.toCollection(ArrayList<DataCell>::new));
                        addBodyValues(cells, response, missing);
                        maybeAddErrorCause(cells);
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
                            } catch (InterruptedException e) { //NOSONAR
                                exec.checkCanceled();
                            }
                        }
                    }
                } catch (CanceledExecutionException e) {
                    //Cannot check for cancelled properly, so this workaround
                    throw new IllegalStateException(e);
                } finally {
                    m_consumedRows++;
                    if (requestBuilder != null) {
                        requestBuilder.getSecond().close();
                    }
                }
                if (exec != null) {
                    setProgress(m_consumedRows, tableSize, row.getKey(), exec);
                }
                return cells.toArray(new DataCell[cells.size()]);
            }

        };
        final int concurrency = Math.max(1, m_settings.getConcurrency());
        factory.setParallelProcessing(true, concurrency, 4 * concurrency);
        final var rearranger = new ColumnRearranger(spec);
        rearranger.append(factory);
        return rearranger;
    }

    /**
     * <p>Unused</p>
     * {@inheritDoc}
     */
    @Override
    public StreamableOperatorInternals createInitialStreamableOperatorInternals() {
        return new SimpleStreamableOperatorInternals();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean iterate(final StreamableOperatorInternals internals) {
        return !m_readNonError;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PortObjectSpec[] computeFinalOutputSpecs(final StreamableOperatorInternals internals,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        CheckUtils.check(inSpecs.length > 0, InvalidSettingsException::new,
            () -> "Cannot find URI column, the node input is missing.");
        return new PortObjectSpec[]{m_firstRows == null ? new DataTableSpec(m_newColumnsBasedOnFirstCalls)
            : createColumnRearranger(enabledAuthConfigs(), (DataTableSpec)inSpecs[0], null/*exec*/, -1L).createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new StreamableOperator() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void runIntermediate(final PortInput[] inputs, final ExecutionContext exec) throws Exception {
                createResponseBodyParsers(exec);
                DataTableSpec inputSpec = null;
                var authentications = getAuthentications(getCredential(inputs));
                if (inputs.length > 0 && inputs[0] instanceof RowInput input) {
                    inputSpec = input.getDataTableSpec();
                    DataRow row;
                    while (!m_readNonError && (row = input.poll()) != null) {
                        makeFirstCall(row, authentications, inputSpec, exec);
                        m_consumedRows++;
                    }
                } else {
                    makeFirstCall(null/*row*/, authentications, null/*spec*/, exec);
                }
                updateFirstCallColumnsOnHttpError(inputSpec);
                //No more rows, so even if there are errors, m_readError should be true:
                m_readNonError = true;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
                if (m_firstRows == null) {
                    var rowOutput = (RowOutput)outputs[0];
                    rowOutput.push(new DefaultRow(RowKey.createRowKey(0L), m_firstCallValues.get(0)));
                    rowOutput.close();
                } else {
                    CheckUtils.check(inSpecs.length > 0, InvalidSettingsException::new,
                        () -> "Cannot find URI column, the node input is missing.");
                    createColumnRearranger(getAuthentications(getCredential(inputs)), (DataTableSpec)inSpecs[0], exec,
                        -1L).createStreamableFunction().runFinal(inputs, outputs, exec);
                }
            }
        };
    }

    private HttpAuthorizationHeaderCredentialValue getCredential(final PortInput[] inputs) {
        if (m_credentialPortIdx > -1) {
            return getCredential((CredentialPortObject)((PortObjectInput)inputs[m_credentialPortIdx]).getPortObject());
        } else {
            return null;
        }
    }

    /**
     * Checks the response code.
     *
     * @param response A {@link Response}.
     * @return http error was returned.
     * @throws IllegalStateExcetion When the http status code is {@code 4xx} or {@code 5xx} and we have to check it.
     */
    private boolean checkResponse(final Response response) {
        // a null response means something went wrong, return an error being detected
        if (response == null) {
            return true;
        }
        boolean isServerError = isServerError(response);
        boolean isClientError = isClientError(response);
        // check whether to fail on error and extract cause
        if ((m_settings.isFailOnClientErrors() && isClientError)
            || (m_settings.isFailOnServerErrors() && isServerError)) {
            // throw exception, will not be caught in callee and cause node to fail (i think)
            Object entity = response.getEntity();
            if (entity instanceof InputStream is) {
                try {
                    getLogger().debug("Failed location: " + response.getLocation());
                    getLogger().debug(IOUtils.toString(is,
                        Charset.isSupported("UTF-8") ? StandardCharsets.UTF_8 : Charset.defaultCharset()));
                } catch (final IOException | NullPointerException e) { // NOSONAR - NPE in IOUtils#toString (CXF bug)
                    getLogger().debug(e.getMessage(), e);
                }
            } else {
                getLogger().debug(entity);
            }
            // only thrown if m_settings.isFailOnHttpError
            throw new IllegalStateException(
                "Wrong status: " + response.getStatus() + " " + response.getStatusInfo().getReasonPhrase());
        }
        // check if a needed System property is missing, added as part of AP-20585
        if (response.getStatus() == Response.Status.PROXY_AUTHENTICATION_REQUIRED.getStatusCode()
            && DisabledSchemesChecker.AuthenticationScheme.BASIC.isDisabled()) {
            // a 407 response and BASIC auth being disabled points indicates the missing property
            // to resolve it, jdk.http.auth.tunneling.disabledSchemes="" needs to be set
            setWarningMessage(DisabledSchemesChecker.FAQ_MESSAGE);
        }
        return isServerError || isClientError;
    }

    /**
     * @param response
     * @return
     */
    private static boolean isHttpError(final Response response) {
        return response == null || (response.getStatus() >= 400 && response.getStatus() < 600);
    }

    /**
     * Checks the {@code response} object and adds the body columns.
     *
     * @param response A {@link Response} object.
     */
    private void examineResponse(final Response response) {
        if (response == null) {
            replace(m_bodyColumns,
                new ResponseHeaderItem(m_settings.getResponseBodyColumn(), BinaryObjectDataCell.TYPE));
        } else {
            boolean isHttpError = isHttpError(response);
            final var mediaType = response.getMediaType();
            DataType type = BinaryObjectDataCell.TYPE;
            for (final ResponseBodyParser responseBodyParser : m_responseBodyParsers) {
                if (!isHttpError && responseBodyParser.supportedMediaType().isCompatible(mediaType)) {
                    type = responseBodyParser.producedDataType();
                    break;
                }
            }
            replace(m_bodyColumns, new ResponseHeaderItem(m_settings.getResponseBodyColumn(), type));
        }
    }

    /**
     * @param bodyColumns
     * @param rhi
     */
    private static void replace(final List<ResponseHeaderItem> bodyColumns, final ResponseHeaderItem rhi) {
        if (bodyColumns.isEmpty()) {
            bodyColumns.add(rhi);
        } else {
            bodyColumns.set(0, rhi);
        }
    }

    /**
     * @param spec the input spec
     * @return The new column specs.
     */
    protected DataColumnSpec[] createNewColumnsSpec(final DataTableSpec spec) {
        var uniqueNameGenerator = new UniqueNameGenerator(spec);
        List<DataColumnSpec> specs = Stream.concat(m_responseHeaderKeys.stream(), m_bodyColumns.stream())
                .map(rhi -> uniqueNameGenerator.newCreator(rhi.getOutputColumnName(), rhi.getType()))
                .map(DataColumnSpecCreator::createSpec)
                .collect(Collectors.toCollection(ArrayList<DataColumnSpec>::new));
        maybeAddErrorCauseColSpec(specs, uniqueNameGenerator);
        return specs.toArray(new DataColumnSpec[0]);
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
        final var clientBuilder = ClientBuilder.newBuilder();

        // Per default, the sync client is used. With AP-17297 it was assumed that the async client
        // needs to be used due to a bug, but a CXF bump to v4 fixed this.
        // (this can be overwritten by system property 'org.apache.cxf.transport.http.async.usePolicy'
        // (see CXF documentation)
        clientBuilder.property(AsyncHTTPConduit.USE_ASYNC, Boolean.FALSE);

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
            clientBuilder.hostnameVerifier((hostName, session) -> true); //NOSONAR
        }
        clientBuilder.property(org.apache.cxf.message.Message.CONNECTION_TIMEOUT,
            m_settings.getTimeoutInSeconds() * 1000L);
        clientBuilder.property(org.apache.cxf.message.Message.RECEIVE_TIMEOUT,
            m_settings.getTimeoutInSeconds() * 1000L);
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
     * @throws InvalidSettingsException
     */
    @SuppressWarnings({"null", "resource"})
    private Pair<Builder, Client> createRequest(final int uriColumn,
        final List<EachRequestAuthentication> enabledEachRequestAuthentications, final DataRow row,
        final DataTableSpec spec) throws InvalidSettingsException {

        final var client = createClient();
        CheckUtils.checkState(m_settings.isUseConstantURI() || row != null,
            "Without the constant uri and input, it is not possible to call a REST service!");

        final String targetUri;
        if (m_settings.isUseConstantURI()) {
            targetUri = m_settings.getConstantURI();
        } else {
            CheckUtils.checkArgument(row != null && uriColumn >= 0,
                "The URI column is not present: " + m_settings.getUriColumn());
            final var cell = row.getCell(uriColumn);
            CheckUtils.checkArgument(!cell.isMissing(), "The URI cannot be missing, but it is in row: " + row.getKey());
            targetUri = cell instanceof URIDataValue uriValue ? uriValue.getURIContent().getURI().toString()
                : ((StringValue)cell).getStringValue();
        }

        WebTarget target = client.target(targetUri);
        //Support relative redirects too, see https://tools.ietf.org/html/rfc7231#section-3.1.4.2
        target = target.property("http.redirect.relative.uri", true);

        final var optProxyConfig = m_settings.getUpdatedProxyConfig(safeParseURI(targetUri));
        // AP-20749: this prevents long living SelectorManager threads that can lead to out of memory errors
        if (!usesHttpsThroughAuthenticatedProxy(target.getUri(), optProxyConfig.orElse(null))) {
            target = target.property("force.urlconnection.http.conduit", true);
        }

        // IMPORTANT: don't access the HttpConduit before the request has been updated by an EachRequestAuthentication!
        // Some implementations (e.g. NTLM) must configure the conduit but they cannot after it has been accessed.

        final Builder request = target.request();

        // AP-20968: for DELETE requests, the HttpClientHTTPConduit sends a 'Content-Type: text/xml' header
        // when no content type was specified. We avoid this by explicitly setting to '*/*'.
        // This default is overwritten below if request headers were specified in the node dialog.
        if (m_settings.getMethod().orElse(null) == HttpMethod.DELETE) {
            request.header(HttpHeaders.CONTENT_TYPE, "*/*");
        }

        for (final RequestHeaderKeyItem headerItem : m_settings.getRequestHeaders()) {
            var value = extractHeaderValue(row, spec, headerItem);
            // If a specified request header has no value, the REST node execution fails with an ISE.
            if (Objects.isNull(value) && m_settings.isFailOnMissingHeaders()) {
                client.close();
                throw new InvalidSettingsException("The value of request header \"" + headerItem.getKey()
                    + "\" is not available. Enter a non-empty value.");
            }
            request.header(headerItem.getKey(), value);
        }

        for (final EachRequestAuthentication era : enabledEachRequestAuthentications) {
            era.updateRequest(request, row, getCredentialsProvider(), getAvailableFlowVariables());
        }

        final var clientConfig = WebClient.getConfig(request);

        HTTPClientPolicy clientPolicy = clientConfig.getHttpConduit().getClient();
        // Set HTTP version to 1.1 because by default HTTP/2 will be used. It's a) not supported by some sites
        // and b) doesn't make sense in our context.
        clientPolicy.setVersion("1.1");
        clientPolicy.setAutoRedirect(m_settings.isFollowRedirects());
        clientPolicy.setMaxRetransmits(MAX_RETRANSMITS);

        // Configures the proxy credentials for the request builder if needed.
        getProxyManager().configureRequest(optProxyConfig, request, getCredentialsProvider());
        return Pair.create(request, client);
    }

    /**
     * Tries to parse a {@link URI} from string input and returns {@code null} if it fails.
     * Does not throw any exception.
     *
     * @param uriString URI input as string
     * @return URI if parsing was successful, otherwise null
     */
    private static URI safeParseURI(final String uriString) {
        if (uriString == null) {
            return null;
        }
        try {
            return new URI(uriString);
        } catch (URISyntaxException e) {
            LOGGER.debug(
                "Creating a URI for input \"%s\" failed, using null URI for proxy selection".formatted(uriString), e);
            return null;
        }
    }

    /**
     * Checks whether the current request needs HTTPS tunneling over an authenticated proxy. This usecase is not covered
     * by the old {@link java.net.HttpURLConnection}-based {@link HTTPConduit}, so we absolutely have to use the new one
     * that leads to memory problems (see AP-20749).
     *
     * @param uri request URI
     * @param proxyConfig proxy configuration
     * @return {@code true} if new conduit has to be used, {@code false} otherwise
     */
    private static boolean usesHttpsThroughAuthenticatedProxy(final URI uri, final RestProxyConfig proxyConfig) {
        if (!uri.getScheme().equals("https")                     // not HTTPS
                || proxyConfig == null                           // no proxy settings available
                || !proxyConfig.isUseAuthentication()) {         // proxy not authenticated
            return false;
        }

        // taken from https://github.com/apache/cxf/blob/7e088f4a/rt/transports/http/src/main/java/org/apache/cxf/transport/http/ProxyFactory.java#L130-L138 // NOSONAR
        if (proxyConfig.isExcludeHosts()) {
            // Try to match the URL hostname with the exclusion pattern
            final var pattern = PatternBuilder.build(proxyConfig.getExcludeHosts().orElseThrow());
            if (pattern.matcher(uri.getHost()).matches()) {
                // Excluded hostname -> no proxy
                return false;
            }
        }
        return true;
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
            if (response != null) {
                DataType expectedType = rhi.getType();
                var mediaType = response.getMediaType();
                if (mediaType == null) {
                    cells.add(new MissingCell("Response doesn't have a media type"));
                } else if (missing != null) {
                    cells.add(missing);
                } else {
                    var wasAdded = false;
                    for (ResponseBodyParser parser : isHttpError(response) ? m_errorBodyParsers
                        : m_responseBodyParsers) {
                        if (parser.producedDataType().isCompatible(expectedType.getPreferredValueClass())
                            && parser.supportedMediaType().isCompatible(response.getMediaType())) {
                            wasAdded = true;
                            cells.add(parser.create(response));
                            break;
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
    String extractHeaderValue(final DataRow row, final DataTableSpec spec, final RequestHeaderKeyItem headerItem) {
        final var ref = headerItem.getValueReference();
        final var supportedTypes = new VariableType[]{BooleanType.INSTANCE, DoubleType.INSTANCE, IntType.INSTANCE,
            LongType.INSTANCE, StringType.INSTANCE};

        return switch (headerItem.getKind()) {
            case Constant -> ref;
            case Column -> row.getCell(spec.findColumnIndex(ref)).isMissing() ? null
                : row.getCell(spec.findColumnIndex(ref)).toString();
            case FlowVariable -> getAvailableFlowVariables(supportedTypes).get(ref).getValueAsString();
            case CredentialName -> getCredentialsProvider().get(ref).getLogin();
            case CredentialPassword -> getCredentialsProvider().get(ref).getPassword();
        };
    }

    private List<EachRequestAuthentication>
        getAuthentications(final HttpAuthorizationHeaderCredentialValue credential) {
        if (credential != null) {
            return List.of(new HttpAuthorizationHeaderAuthentication(credential));
        } else {
            return enabledAuthConfigs();
        }
    }

    /**
     * @return The enabled {@link EachRequestAuthentication}s.
     */
    private List<EachRequestAuthentication> enabledAuthConfigs() {
        return m_settings.getAuthorizationConfigurations().parallelStream()
            .filter(euc -> euc.isEnabled() && euc.getUserConfiguration() instanceof EachRequestAuthentication)
            .map(euc -> (EachRequestAuthentication)euc.getUserConfiguration()).toList();
    }
}
