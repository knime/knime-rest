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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.asyncclient.AsyncHTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.eclipse.e4.core.di.annotations.Execute;
import org.knime.base.data.filter.row.FilterRowGenerator;
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
import org.knime.core.data.blob.BinaryObjectDataCell;
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
import org.knime.rest.nodes.common.AbstractRequestExecutor.MultiResponseHandler;
import org.knime.rest.nodes.common.RestSettings.HttpMethod;
import org.knime.rest.nodes.common.RestSettings.ReferenceType;
import org.knime.rest.nodes.common.RestSettings.RequestHeaderKeyItem;
import org.knime.rest.nodes.common.RestSettings.ResponseHeaderItem;
import org.knime.rest.nodes.common.proxy.ProxyMode;
import org.knime.rest.nodes.common.proxy.RestProxyConfigManager;
import org.knime.rest.util.CooldownContext;
import org.knime.rest.util.DelegatingX509TrustManager;
import org.knime.rest.util.InvalidURLPolicy;
import org.knime.rest.util.RowFilterUtil;

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

    private static final NodeLogger LOGGER = NodeLogger.getLogger(RestNodeModel.class);

    private static final int MAX_RETRANSMITS = 4;

    /**
     * If the constant URL is enabled, we can safely use the row with id 'Row0',
     * it cannot be overwritten. Also compatible with the output table.
     */
    private static final RowKey CONSTANT_URL_KEY = RowKey.createRowKey(0L);

    /**
     * The settings of this node model.
     */
    protected final S m_settings = createSettings();

    /**
     * The specs of the new columns if no input is connected.
     */
    protected DataColumnSpec[] m_newColumnsBasedOnFirstCalls;

    private AtomicLong m_consumedRows = new AtomicLong(0L);

    // Package scope needed for tests.
    final Map<RowKey, DataCell[]> m_parsedResponseValues = new LinkedHashMap<>();

    private boolean m_readNonError;

    private final List<ResponseHeaderItem> m_responseHeaderKeys = new ArrayList<>();

    private final List<ResponseHeaderItem> m_bodyColumns = new ArrayList<>();

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
     * Resulting length of the row as {@code DataCell[]}.
     */
    private int m_rowLength = -1;

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
     * @param row data row to get the key for
     * @return the row's key or a constant identifier for null
     */
    static RowKey getRowKey(final DataRow row) {
        if (row == null) {
            return CONSTANT_URL_KEY;
        }
        return row.getKey();
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
        return res.getStatus() == Response.Status.TOO_MANY_REQUESTS.getStatusCode();
    }

    /**
     * @param response
     * @return
     */
    private static boolean isHttpError(final Response response) {
        return isClientError(response) || isServerError(response);
    }

    /**
     * Replaces the first element in the list by the given one. If the list is empty, it simply adds the element.
     *
     * @param list
     * @param element
     */
    private static <T> void replaceFirst(final List<T> list, final T element) {
        if (list.isEmpty()) {
            list.add(element);
        } else {
            list.set(0, element);
        }
    }

    private static DataCell[] formatDataCellsToLength(final DataCell[] cells, final int size) {
        if (cells == null || size < 0 || cells.length == size) {
            return cells;
        }
        // truncates array or pads with 'null' values
        final var newCells = Arrays.copyOf(cells, size);
        for (var i = cells.length; i < size; i++) {
            newCells[i] = DataType.getMissingCell();
        }
        return newCells;
    }

    private DataCell[] formatDataCells(final DataCell[] cells) {
        return formatDataCellsToLength(cells, m_rowLength);
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
     * @param spec the input data table spec
     * @return an {@link FilterRowGenerator} conforming to the current invalid URL policy
     */
    protected FilterRowGenerator getRowFilter(final DataTableSpec spec) {
        if (m_settings.isUseConstantURL()) {
            return row -> true;
        }
        CheckUtils.checkArgumentNotNull(spec);
        final var columnIndex = spec.findColumnIndex(m_settings.getURLColumn());
        return m_settings.getInvalidURLPolicy().createRowFilter(columnIndex);
    }

    /**
     * Retrieves the {@link URL} value for a given {@link DataRow} and its column index.
     * If parsing the URL from the stored string value fails or the URL value does not represent
     * an HTTP or HTTPS address, an exception is thrown.
     *
     * @param row data row
     * @param columnIndex index of the URL column
     * @return maybe parsed URL value
     * @throws MalformedURLException if the provided URL is not a valid HTTP(S) address
     */
    public static URL getURLFromRow(final DataRow row, final int columnIndex) throws MalformedURLException {
        CheckUtils.checkNotNull(row, "URL cannot determined, given row is null");
        final var cell = row.getCell(columnIndex);
        CheckUtils.checkArgument(!cell.isMissing(), String.format(
            "The URL cannot be missing, but it is in row: %s", row.getKey()));
        final var urlString = cell instanceof URIDataValue urlValue //
                ? urlValue.getURIContent().getURI().toString() //
                : ((StringCell)cell).getStringValue();
        return validateURLString(urlString);
    }

    static URL validateURLString(final String urlString) throws MalformedURLException {
        URL url;
        try {
            /*
             * Parsing twice since URIs do not appear to be a strict superset of URLs.
             * For example, 'https://' is a valid URL, but not URI. Also, URLs permit spaces
             * while these must be URL-encoded (%20) for URIs.
             */
            url = new URI(Objects.requireNonNull(urlString)).toURL();
        } catch (URISyntaxException | IllegalArgumentException e) { // NOSONAR cannot rethrow with MUE
            throw new MalformedURLException(StringUtils.replace(e.getMessage(), "URI", "URL"));
        }
        final var protocol = url.getProtocol();
        if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
            throw new MalformedURLException(String.format(
                "Not an HTTP(S) protocol in input URL, got protocol \"%s\" in input \"%s\"",
                protocol, urlString));
        }
        return url;
    }

    @Override
    protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec inputSpec = (DataTableSpec)inSpecs[0];
        if (!m_settings.isUseConstantURL()) {
            String urlColumn = m_settings.getURLColumn();
            if (inputSpec == null) {
                throw new InvalidSettingsException(
                    "Input table required to execute. The node is configured to use a URL from the column '" + urlColumn
                        + "' of the input table.");
            } else {
                if (!inputSpec.containsName(urlColumn)) {
                    throw new InvalidSettingsException(
                        "The configured URL column '" + urlColumn + "' is missing in the input table.");
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
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        final List<EachRequestAuthentication> enabledAuthentications = getAuthentications(getCredential(inData));
        createResponseBodyParsers(exec);
        // Issue a warning if no proxy config came in from the global settings.
        if (m_settings.getProxyManager().getProxyMode() == ProxyMode.GLOBAL
            && m_settings.getUpdatedProxyConfig(null).isEmpty()) {
            LOGGER.info("The KNIME-wide proxy settings are activated but none were specified. "
                + "Defaulting to using no proxy.");
        }
        if (inData.length == 0 || inData[0] == null) {
            // Constant URL mode.
            makeFirstCall(null/*row*/, enabledAuthentications, null/*spec*/, exec);
            return createTableFromFirstCallData(exec);
        }
        final var inTable = (BufferedDataTable)inData[0];
        if (inTable.size() == 0) {
            // No calls to make.
            return inData;
        }

        final var spec = inTable.getDataTableSpec();
        final var filteredTable = RowFilterUtil.filterBufferedDataTable(inTable, getRowFilter(spec), exec);
        try (var iterator = filteredTable.iterator()) {
            while (!m_readNonError && iterator.hasNext()) {
                makeFirstCall(iterator.next(), enabledAuthentications, spec, exec);
                m_consumedRows.getAndIncrement();

            }
        }
        final var rearranger = createColumnRearranger(enabledAuthentications, spec, exec, filteredTable.size());
        return new BufferedDataTable[]{exec.createColumnRearrangeTable( //
            filteredTable, rearranger, exec)};
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
     * Creates the data table only based on the first call. Only called if input is not connected.
     *
     * @param exec An {@link ExecutionContext}.
     * @return The array of a single {@link BufferedDataTable}.
     */
    private BufferedDataTable[] createTableFromFirstCallData(final ExecutionContext exec) {
        updateFirstCallColumnsOnHttpError(null);
        final var spec = m_newColumnsBasedOnFirstCalls != null //
                ? new DataTableSpec(m_newColumnsBasedOnFirstCalls) //
                : new DataTableSpec();
        final BufferedDataContainer container = exec.createDataContainer(spec, false);
        // contains only one "row" being the result from a single constant-URL invocation
        m_parsedResponseValues.entrySet().forEach(e -> {
            final var cells = e.getValue();
            if (!Arrays.equals(cells, AbstractRequestExecutor.EMPTY_RESPONSE)) {
                container.addRowToTable(new DefaultRow(e.getKey(), formatDataCells(cells)));
            }
        });
        container.close();
        return new BufferedDataTable[]{container.getTable()};
    }

    /**
     * @param inputSpec the input spec or null (non connected optional input)
     */
    private void updateFirstCallColumnsOnHttpError(final DataTableSpec inputSpec) {
        final var validFirstValues = m_parsedResponseValues.values().stream().findFirst() //
                .filter(v -> !Objects.isNull(m_newColumnsBasedOnFirstCalls)) //
                .filter(v -> v.length > m_newColumnsBasedOnFirstCalls.length);
        if (validFirstValues.isPresent()) {
            m_newColumnsBasedOnFirstCalls = createNewColumnsSpec(inputSpec);
        }
    }

    @Override
    protected void reset() {
        m_responseBodyParsers.clear();
        m_bodyColumns.clear();
        m_consumedRows.set(0L);
        m_parsedResponseValues.clear();
        m_readNonError = false;
        m_responseHeaderKeys.clear();
        m_newColumnsBasedOnFirstCalls = null;
        m_isContextSettingsFailed = false;
        m_errorBodyParsers.clear();
    }

    @Override
    public InputPortRole[] getInputPortRoles() {
        return new InputPortRole[]{InputPortRole.NONDISTRIBUTED_STREAMABLE};
    }

    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.NONDISTRIBUTED};
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        final var s = createSettings();
        s.loadSettingsFrom(settings);
        validateURLSettings(s);
    }

    /**
     * Check URL validity on configure instead of execute if {@link InvalidURLPolicy#FAIL}.
     * <p>
     * If {@link AbstractRequestExecutor#abortDueToInvalidURI(DataRow, MalformedURLException)} encounters
     * an invalid URL on execute (with the condition checked here), it throws an illegal state exception.
     *
     * @param settings freshly loaded {@link RestSettings}
     * @throws InvalidSettingsException if the URL is invalid
     */
    private void validateURLSettings(final S settings) throws InvalidSettingsException {
        if (settings.getInvalidURLPolicy() == InvalidURLPolicy.FAIL && settings.isUseConstantURL()) {
            if (StringUtils.isEmpty(settings.getConstantURL())) {
                throw new InvalidSettingsException("The URL cannot be empty");
            }
            try {
                validateURLString(settings.getConstantURL());
            } catch (MalformedURLException e) {
                throw new InvalidSettingsException("The URL is invalid", e);
            }
        }
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettingsFrom(settings);
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        //No internals
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        //No internals
    }

    // -- REQUEST PREPARATION --

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
            MediaType.APPLICATION_FORM_URLENCODED_TYPE, MediaType.MULTIPART_FORM_DATA_TYPE}) {
            m_responseBodyParsers.add(new Default(mediaType, BinaryObjectDataCell.TYPE, exec));
        }
        // everything else is a file
        m_responseBodyParsers.add(new Default(MediaType.WILDCARD_TYPE, BinaryObjectDataCell.TYPE, exec));

        m_errorBodyParsers.addAll(m_responseBodyParsers.stream()
            .filter(parser -> (MediaType.TEXT_PLAIN_TYPE.isCompatible(parser.supportedMediaType()) //
                    && !parser.supportedMediaType().isWildcardType()) //
                    || XMLCell.TYPE.equals(parser.producedDataType()) //
                    || JSONCell.TYPE.equals(parser.producedDataType())) //
            .map(Missing::new) //
            .collect(Collectors.toCollection(ArrayList<ResponseBodyParser>::new)));
        // error codes for the rest of the content types
        m_errorBodyParsers.add(new Default(MediaType.WILDCARD_TYPE, DataType.getMissingCell().getType(), exec) {
            @Override
            public DataCell create(final Response response) {
                return new MissingCell(response.getStatusInfo().getReasonPhrase());
            }
        });
    }

    /**
     * Creates the request {@link Builder} based on input parameters.
     *
     * @param URLColumn The index of URL column.
     * @param enabledAuthentications The authentications.
     * @param row The input {@link DataRow}.
     * @param spec The input table spec.
     * @return
     * @throws InvalidSettingsException
     */
    @SuppressWarnings({"resource"})
    private Pair<Builder, Client> createRequest(final URI targetUri, // NOSONAR leave in outer class
        final List<EachRequestAuthentication> enabledAuthentications, final DataRow row, final DataTableSpec spec)
        throws InvalidSettingsException {
        final var client = createClient();
        WebTarget target = client.target(targetUri);
        //Support relative redirects too, see https://tools.ietf.org/html/rfc7231#section-3.1.4.2
        target = target.property("http.redirect.relative.uri", true);
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

        // IMPORTANT: don't access the HttpConduit before the request has been updated by an EachRequestAuthentication!
        // Some implementations (e.g. NTLM) must configure the conduit but they cannot after it has been accessed.

        for (final EachRequestAuthentication era : enabledAuthentications) {
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
        final var optProxyConfig = m_settings.getUpdatedProxyConfig(targetUri);
        getProxyManager().configureRequest(optProxyConfig, request, getCredentialsProvider());
        return Pair.create(request, client);
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
            .map(euc -> (EachRequestAuthentication)euc.getUserConfiguration()) //
            .toList();
    }

    // -- STANDARD REQUEST EXECUTION --

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
     * Makes the first call to the servers, this will create the structure of the output table.
     *
     * @param row The input row.
     * @param enabledAuthentications The single enabled authentication.
     * @param spec The {@link DataTableSpec}.
     * @param exec An {@link ExecutionContext}.
     * @throws InvalidSettingsException if the request could not be created
     */
    protected void makeFirstCall(final DataRow row, final List<EachRequestAuthentication> enabledAuthentications,
        final DataTableSpec spec, final ExecutionContext exec) throws InvalidSettingsException {
        m_cooldownContext = new CooldownContext(); // reset context before execution.
        final var executor = new RequestExecutor(spec, enabledAuthentications, exec);
        final var rowKey = getRowKey(row);
        try {
            m_parsedResponseValues.put(rowKey, executor.makeFirstCall(row));
        } catch (MalformedURLException e) {
            // use empty response as signal that this row was processed but is to be skipped
            // when writing out to buffered data table (or being streamed)
            m_parsedResponseValues.put(rowKey, AbstractRequestExecutor.EMPTY_RESPONSE);
        }

    }

    /**
     * A REST request executor that has access to the entire configuration state of the node model.
     * Compared to the {@link AbstractRequestExecutor}, it implements the invocation creation,
     * involving utilizing currently enabled authentication methods, and validating the {@link URL}.
     *
     * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
     */
    private class RequestExecutor extends AbstractRequestExecutor<S> {

        private final List<EachRequestAuthentication> m_enabledAuthentications;

        /**
         * Implementation of the request executor.
         *
         * @param spec initial data table spec
         * @param enabledAuthentications currently available auth methods
         * @param monitor an execution monitor for progress tracking
         */
        RequestExecutor(final DataTableSpec spec, final List<EachRequestAuthentication> enabledAuthentications,
            final ExecutionMonitor monitor) {
            super(new ResponseHandler(), spec, createNewColumnsSpec(spec), m_settings, m_cooldownContext,
                monitor, m_consumedRows);
            m_enabledAuthentications = enabledAuthentications;
        }

        @SuppressWarnings("resource")
        @Override
        public InvocationTriple createInvocationTriple(final DataRow row)
                throws InvalidSettingsException, MalformedURLException {
            final var spec = getTableSpec();
            final var currentURL = getCurrentURL(spec, row);
            // need to convert to URI for request creation, CXF only accepts those
            URI currentURI;
            try {
                currentURI = currentURL.toURI();
            } catch (URISyntaxException e) { // NOSONAR cannot occur, see #validateURLString
                throw new MalformedURLException(e.getMessage());
            }
            // computing request builder and client on-demand for each new row
            // could be improved in the future by re-using one client per execution (not per row)
            final var builderClient = createRequest(currentURI, m_enabledAuthentications, row, spec);
            return new InvocationTriple(
                invocation(builderClient.getFirst(), row, spec),    // invocation
                currentURL,                                         // URL
                builderClient.getSecond());                         // web client
        }

        private URL getCurrentURL(final DataTableSpec spec, final DataRow row) throws MalformedURLException {
            CheckUtils.checkState(m_settings.isUseConstantURL() || row != null,
                    "Without the constant URL and input, it is not possible to call a REST service!");
            if (!m_settings.isUseConstantURL()) {
                final var columnIndex = spec == null ? -1 : spec.findColumnIndex(m_settings.getURLColumn());
                CheckUtils.checkArgument(row != null && columnIndex >= 0,
                        "The URL column is not present: " + m_settings.getURLColumn());
                return getURLFromRow(row, columnIndex); // NOSONAR 'row' is not null
            }
            return validateURLString(m_settings.getConstantURL());
        }
    }

    // -- STREAMING REQUEST EXECUTION --

    /**
     * Creates the {@link ColumnRearranger}.
     *
     * @param enabledAuthentications The selected authentication.
     * @param spec The input {@link DataTableSpec}.
     * @param exec {@link ExecutionMonitor}.
     * @param tableSize The size of the table.
     * @return The {@link ColumnRearranger}.
     * @throws InvalidSettingsException Invalid internal state.
     */
    private ColumnRearranger createColumnRearranger(final List<EachRequestAuthentication> enabledAuthentications,
        final DataTableSpec spec, final ExecutionMonitor exec, final long tableSize) {
        final var factory = new RequestExecutor(spec, enabledAuthentications, exec);
        factory.setKnownTableSize(tableSize);
        final int concurrency = Math.max(1, m_settings.getConcurrency());
        factory.setParallelProcessing(true, concurrency, 4 * concurrency);
        final var rearranger = new ColumnRearranger(spec);
        rearranger.append(factory);
        return rearranger;
    }

    @Override
    public StreamableOperatorInternals createInitialStreamableOperatorInternals() {
        // unused method
        return new SimpleStreamableOperatorInternals();
    }

    @Override
    public boolean iterate(final StreamableOperatorInternals internals) {
        return !m_readNonError;
    }

    @Override
    public PortObjectSpec[] computeFinalOutputSpecs(final StreamableOperatorInternals internals,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        CheckUtils.check(inSpecs.length > 0, InvalidSettingsException::new,
            () -> "Cannot find URL column, the node input is missing.");
        updateFirstCallColumnsOnHttpError(null);
        final var spec = m_newColumnsBasedOnFirstCalls != null //
                ? new DataTableSpec(m_newColumnsBasedOnFirstCalls) //
                : new DataTableSpec();
        return new PortObjectSpec[]{m_consumedRows.get() == 0 ? spec
            : createColumnRearranger(enabledAuthConfigs(), (DataTableSpec)inSpecs[0], null/*exec*/, -1L).createSpec()};
    }

    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new StreamableOperator() {
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
                        m_consumedRows.getAndIncrement();
                    }
                } else {
                    makeFirstCall(null/*row*/, authentications, null/*spec*/, exec);
                }
                updateFirstCallColumnsOnHttpError(inputSpec);
                // no more rows, so even if there are errors, m_readError should be true
                m_readNonError = true;
            }

            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
                if (m_consumedRows.get() == 0) {
                    var rowOutput = (RowOutput)outputs[0];
                    final var cells = m_parsedResponseValues.get(CONSTANT_URL_KEY);
                    if (!Arrays.equals(cells, AbstractRequestExecutor.EMPTY_RESPONSE)) {
                        rowOutput.push(new DefaultRow(CONSTANT_URL_KEY, formatDataCells(cells)));
                    }
                    rowOutput.close();
                } else {
                    CheckUtils.check(inSpecs.length > 0, InvalidSettingsException::new,
                        () -> "Cannot find URL column, the node input is missing.");
                    final var spec = (DataTableSpec)inSpecs[0];
                    final var enabledAuthentications = getAuthentications(getCredential(inputs));
                    final var rearranger = createColumnRearranger(enabledAuthentications, spec, exec, -1L);
                    RowFilterUtil.filterStreamableFunction(rearranger.createStreamableFunction(), getRowFilter(spec))
                        .runFinal(inputs, outputs, exec);
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
     * @param spec the input spec
     * @return The new column specs.
     */
    protected DataColumnSpec[] createNewColumnsSpec(final DataTableSpec spec) {
        var uniqueNameGenerator = new UniqueNameGenerator(spec);
        List<DataColumnSpec> specs = Stream.concat(m_responseHeaderKeys.stream(), m_bodyColumns.stream())
            .map(rhi -> uniqueNameGenerator.newCreator(rhi.getOutputColumnName(), rhi.getType()))
            .map(DataColumnSpecCreator::createSpec).collect(Collectors.toCollection(ArrayList<DataColumnSpec>::new));
        updateErrorCauseColumnSpec(specs, uniqueNameGenerator);
        return specs.toArray(new DataColumnSpec[0]);
    }

    // -- RESPONSE HANDLING --

    /**
     * Adds the body values.
     *
     * @param cells The input {@link DataCell}s.
     * @param response The {@link Response} object.
     * @param missing The missing cell with the error message.
     */
    protected void addBodyValuesToCells(final List<DataCell> cells, final Response response, final DataCell missing) {
        m_bodyColumns.stream().forEachOrdered(rhi -> {
            if (response != null) {
                DataType expectedType = rhi.getType();
                var mediaType = response.getMediaType();
                if (mediaType == null) {
                    cells.add(new MissingCell("Response does not have a media type"));
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
     * Add a column for error causes (String descriptions) to the given list of <code>DataColumnSpec</code>s, which will
     * ultimately determine the node's output column specification. Do this only if the respective setting is enabled.
     * Use the given <code>nameGenerator</code> to find a name for the new column.
     *
     * @param specs The list of <code>DataColumnSpec</code>s to add the newly created column spec to.
     * @param nameGenerator The generator to use for finding a name for the column to be added.
     */
    protected void updateErrorCauseColumnSpec(final List<DataColumnSpec> specs,
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
    private void addErrorCauseToCells(final List<DataCell> cells) { // NOSONAR leave in outer class
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
     * A REST implements the multi-stage response handling, dividing into handling the first,
     * and then all following responses.
     * <p>
     * The first (successful) response initializes the response data format (see
     * {@link RestNodeModel#m_responseHeaderKeys} and further responses fill up the cached
     * parsed values (see {@link RestNodeModel#m_parsedResponseValues}).
     *
     * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
     */
    private class ResponseHandler implements MultiResponseHandler {
        @Override
        public DataCell[] handleFirstResponse(final DataTableSpec spec, final Response response,
            final MissingCell missing) {
            final var nameGenerator = new UniqueNameGenerator(spec == null ? new DataTableSpec() : spec);
            // determine response headers to be used for output columns where we overwrite their "specification"
            // when response keys are empty (first request) or of size 1 (previous request was not successful)
            if (m_responseHeaderKeys.size() <= 1) {
                m_responseHeaderKeys.clear();
                if (m_settings.isExtractAllResponseFields()) {
                    Stream
                        .concat(Stream.of(Pair.create(STATUS, IntCell.TYPE)),
                            (response == null ? Collections.<String> emptyList() : response.getStringHeaders().keySet())
                                .stream().map(header -> new Pair<String, DataType>(header, StringCell.TYPE)))
                        .forEachOrdered(pair -> m_responseHeaderKeys.add(new ResponseHeaderItem(pair.getFirst(),
                            pair.getSecond(), nameGenerator.newColumn(pair.getFirst(), pair.getSecond()).getName())));
                } else {
                    m_responseHeaderKeys.addAll(
                        m_settings.getExtractFields().stream().map(rhi -> new ResponseHeaderItem(rhi.getHeaderKey(),
                            rhi.getType(), nameGenerator.newName(rhi.getOutputColumnName()))).toList());
                }
            }
            // parse content from response (data cells and specs)
            final List<DataCell> cells = m_responseHeaderKeys.stream() //
                    .map(rhi -> extractHeaderAsCell(response, rhi)) //
                    .collect(Collectors.toCollection(ArrayList<DataCell>::new));
            final List<DataColumnSpec> specs = m_responseHeaderKeys.stream() //
                    .map(rhi -> new DataColumnSpecCreator(rhi.getOutputColumnName(), rhi.getType()).createSpec()) //
                    .collect(Collectors.toCollection(ArrayList<DataColumnSpec>::new));
            // perform checks on HTTP status and headers
            checkHeadersAndUpdateBodyValues(response);
            final var httpError = checkResponseStatus(response);
            final Map<ResponseHeaderItem, String> bodyColNames = new HashMap<>();
            if (!httpError) {
                for (final ResponseHeaderItem bodyCol : m_bodyColumns) {
                    final DataColumnSpec newCol =
                        nameGenerator.newColumn(bodyCol.getOutputColumnName(), bodyCol.getType());
                    specs.add(newCol);
                    bodyColNames.put(bodyCol, newCol.getName());
                }
                for (var i = 0; i < m_bodyColumns.size(); ++i) {
                    // replace header name with deduplicated value from corresponding bodyColName
                    m_bodyColumns.set(i, new ResponseHeaderItem(m_bodyColumns.get(i).getHeaderKey(),
                        m_bodyColumns.get(i).getType(), bodyColNames.get(m_bodyColumns.get(i))));
                }
            } else {
                if (response != null && response.getEntity() instanceof InputStream) {
                    replaceFirst(m_bodyColumns, new ResponseHeaderItem(m_bodyColumns.get(0).getHeaderKey(),
                        BinaryObjectDataCell.TYPE, m_bodyColumns.get(0).getHeaderKey()));
                }
            }
            addBodyValuesToCells(cells, response, missing);
            addErrorCauseToCells(cells);
            if (!m_readNonError && !httpError) {
                // first time reading a successful response
                m_readNonError = true;
                m_rowLength = cells.size();
            }
            m_newColumnsBasedOnFirstCalls = specs.toArray(new DataColumnSpec[specs.size()]);
            return cells.toArray(DataCell[]::new);
        }

        @Override
        public DataCell[] handleFollowingResponse(final DataTableSpec spec, final Response response,
            final MissingCell missing) {
            CheckUtils.checkState(m_consumedRows.get() > 0,
                "First response has not been processed yet, cannot continue");
            checkResponseStatus(response);
            final List<DataCell> cells = m_responseHeaderKeys.stream() //
                    .map(rhi -> extractHeaderAsCell(response, rhi)) //
                    .collect(Collectors.toCollection(ArrayList<DataCell>::new));
            addBodyValuesToCells(cells, response, missing);
            addErrorCauseToCells(cells);
            return cells.toArray(DataCell[]::new);
        }

        @Override
        public Optional<DataCell[]> lookupResponseCache(final DataRow row) {
            return Optional.ofNullable(formatDataCells(m_parsedResponseValues.get(getRowKey(row))));
        }

        /**
         * Checks the response code, returns {@code true} if the status code is 4XX or 5XX.
         *
         * @param response the {@link Response} object
         * @return if HTTP error was detected
         * @throws IllegalStateException if a 4XX status is detected and {@link RestSettings#isFailOnClientErrors()}
         * or a 5XX status is detected {@link RestSettings#isFailOnServerErrors()} is enabled
         */
        private boolean checkResponseStatus(final Response response) {
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
         * Checks the {@code response} object and adds the body columns.
         *
         * @param response A {@link Response} object.
         */
        private void checkHeadersAndUpdateBodyValues(final Response response) {
            if (response == null) {
                replaceFirst(m_bodyColumns,
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
                replaceFirst(m_bodyColumns, new ResponseHeaderItem(m_settings.getResponseBodyColumn(), type));
            }
        }

        private DataCell extractHeaderAsCell(final Response response, final ResponseHeaderItem rhi) {
            if (STATUS.equals(rhi.getHeaderKey()) && rhi.getType().isCompatible(IntValue.class)) {
                return response == null ? DataType.getMissingCell() : new IntCell(response.getStatus());
            }
            if (rhi.getType().isCompatible(IntValue.class)) {
                try {
                    return response == null ? DataType.getMissingCell()
                        : new IntCell(Integer.parseInt(response.getHeaderString(rhi.getHeaderKey())));
                } catch (RuntimeException e) { //NOSONAR
                    return new MissingCell(e.getMessage());
                }
            }
            final var cellFactory = rhi.getType().getCellFactory(null).orElseGet(() -> FALLBACK);
            if (cellFactory instanceof FromString fromString) {
                final String value = response == null ? null : response.getHeaderString(rhi.getHeaderKey());
                if (value != null) {
                    return fromString.createCell(value);
                }
            }
            return DataType.getMissingCell();
        }
    }
}
