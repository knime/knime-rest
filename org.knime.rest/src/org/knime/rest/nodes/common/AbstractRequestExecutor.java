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
 *   Jan 30, 2024 (lw): created
 */
package org.knime.rest.nodes.common;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongPredicate;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.MissingCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.ThreadLocalHTTPAuthenticator;
import org.knime.rest.util.CooldownContext;
import org.knime.rest.util.DelayPolicy;
import org.knime.rest.util.InvalidURLPolicy;
import org.knime.rest.util.StatusOnlyResponse;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Response;

/**
 * Houses the REST request execution logic for client nodes. This class is initialized
 * with the entire REST "execution context", and requires implementing {@link #createInvocationTriple(DataRow)}.
 * <p>
 * Fetching responses subject to two stages: the first call (initializing the response data format),
 * and subsequent requests, filling the data table. To provide both operations, this class also
 * defines the {@link MultiResponseHandler} interface, one of which is required upon creating
 * a {@link AbstractRequestExecutor} instance.
 * <p>
 * Additionally, this executor only conforms to the JAX-RS specification.
 * This decouples from implementation specifics, possibly allowing to replace the REST library.
 *
 * @param <S> generic type of the REST settings used
 * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
 */
public abstract class AbstractRequestExecutor<S extends RestSettings> extends AbstractCellFactory {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(AbstractRequestExecutor.class);

    private static final Pattern STATUS_IN_EXCEPTION = Pattern.compile("[1-5]\\d{2}");

    private static final long CHECK_INTERVAL_MS = 100L;

    static final DataCell[] EMPTY_RESPONSE = new DataCell[0];

    private final DataTableSpec m_tableSpec;

    private final S m_settings;

    private final MultiResponseHandler m_responseHandler;

    private final CooldownContext m_cooldownContext;

    private final ExecutionMonitor m_monitor;

    private final AtomicLong m_consumedRows;

    private long m_tableSize = 1;

    /**
     * Default constructor, stores the needed execution context.
     */
    AbstractRequestExecutor(final MultiResponseHandler handler, final DataTableSpec spec,
        final DataColumnSpec[] colSpec, final S settings, final CooldownContext cooldown,
        final ExecutionMonitor monitor, final AtomicLong consumedRows) {
        super(colSpec);
        m_tableSpec = spec;
        m_settings = settings;
        m_responseHandler = handler;
        m_cooldownContext = cooldown;
        m_monitor = Objects.requireNonNullElseGet(monitor, ExecutionMonitor::new);
        m_consumedRows = consumedRows;
    }

    /**
     * Finds the deepest {@link IOException} in the list of causes.
     *
     * @param e top-most exception
     * @return root cause being an IO exception
     */
    private static Throwable getRootCause(final Exception e) {
        Throwable rootCause = e.getCause() != null ? e.getCause() : e;
        for (var t : ExceptionUtils.getThrowableList(e)) {
            if (t instanceof IOException) {
                rootCause = t;
            }
        }
        return rootCause;
    }

    /**
     * Optionally appends a prefix to the given {@link Throwable}'s cause message,
     * detailing which {@link URL} was to be request when the exception was thrown.
     *
     * @param cause the determined error cause
     * @param currentURL URL to be requested
     * @return
     */
    private static String getReadableCauseMessage(final Throwable cause, final URL currentURL) {
        final var message = Objects.requireNonNullElse(cause.getMessage(), "");
        /*
         * Want to provide a helpful message here by appending which URL the request
         * was made to. The same is already done in CXF's HTTPConduit, but we don't
         * know where the exception is from. Hence, we add the 'helpful' message
         * only if it is not already there.
         */
        final var parts = StringUtils.split(message, " ", 3);
        final var prefix = parts.length >= 3 && "invoking".equals(parts[1]) //
            ? "" //
            : "%s invoking %s: ".formatted(cause.getClass().getSimpleName(), currentURL);
        return prefix + message;
    }

    private static void closeResponse(final Response response) {
        if (response == null) {
            return;
        }
        try {
            // try to close, but there is a known NPE from within CXF that we catch here
            response.close();
        } catch (NullPointerException e) { // NOSONAR avoid CXF bug
            LOGGER.warn("Closing the HTTP response failed with exception: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a REST {@link Invocation} object from the data row input.
     * Encapsulates the row's data (e.g. in the form of a target URL), and an underlying {@link Client}.
     *
     * @param row the data row currently processed
     * @return the invocation and client object if successful
     * @throws InvalidSettingsException if the invocation object could not be created
     * @throws MalformedURLException if the invocation creation failed due to an invalid URL
     */
    public abstract InvocationTriple createInvocationTriple(final DataRow row)
            throws InvalidSettingsException, MalformedURLException;

    /**
     * Tells the executor how large the table to process is.
     * Progress updates will be adjusted accordingly.
     *
     * @param size to entire table length
     */
    public void setKnownTableSize(final long size) {
        m_tableSize = size;
    }

    /**
     * Returns the initial {@link DataTableSpec}.
     * Essential for processing the responses.
     *
     * @return the (possibly cached) current table spec
     */
    protected DataTableSpec getTableSpec() {
        return m_tableSpec;
    }

    // -- PERFORMING REQUEST CALLS --

    /**
     * Due to the multi-layer response processing, exceptions can come from anywhere.
     * For example, the {@link jdk.internal.net.http.PlainTunnelingConnection} wraps any non-200
     * response in a 'Tunnel failed, got: XXX' exception and the {@link sun.net.www.protocol.http.HttpURLConnection}
     * throws an exception for every non-407 proxy connection failure.
     * <p>
     * Since this is valuable information for the user which would get thrown away in an exception,
     * this method parses and reconstructs a response from this 'XXX' status code,
     * using the {@link StatusOnlyResponse} class.
     *
     * @param message cause message
     * @return reconstructed response, most often null
     */
    private static Response tryReconstructResponse(final String message) {
        for (var indicator : List.of("Tunnel failed, got", "Unable to tunnel through proxy. Proxy returns")) {
            if (!StringUtils.startsWith(message, indicator)) {
                continue;
            }
            final var matcher = STATUS_IN_EXCEPTION.matcher(message);
            if (matcher.find()) {
                try {
                    final var statusCode = Integer.parseInt(matcher.group(0));
                    return new StatusOnlyResponse(statusCode, message);
                } catch (NumberFormatException nfe) {
                    LOGGER.debug("Could not reconstruct response status from message", nfe);
                }
            }
        }
        return null;
    }

    /**
     * Core of the REST request execution. Create a new {@link Invocation} object from the provided
     * {@link DataRow}, as well as a {@link Client}, and performs a single, synchronous request.
     * <p>
     * Since we are using CXF currently for REST requests, the behavior of this request is strongly
     * dependent on the global configuration in the KNIMECXFBusFactory, and the local configuration in
     * {@link RestNodeModel#createClient()}.
     *
     * @param spec
     * @param settings
     * @param invocation
     * @return
     * @throws InvalidSettingsException
     * @throws ProcessingException
     */
    @SuppressWarnings("resource")
    private ResultPair performSingleRequest(final DataRow row)
            throws InvalidSettingsException, MalformedURLException, ProcessingException {
        // creating the request invocation can cause an ISE, see AP-20219
        final var triple = createInvocationTriple(row);
        Response response = null;
        MissingCell missing = null;
        try (var c = ThreadLocalHTTPAuthenticator.suppressAuthenticationPopups()) {
            /*
             * Currently, we use a sync HTTP client (AP-20585, AP-21786, AP-22983), hence the invocation is
             * always blocking, and there is no way for the user to cancel the node execution (and REST request).
             */
            response = DelayPolicy.doWithDelays(m_settings.getDelayPolicy(), m_cooldownContext,
                () -> triple.invocation.invoke(Response.class));
        } catch (ProcessingException e) {
            LOGGER.warn("Call #%s failed: %s".formatted(m_consumedRows.get() + 1, e.getMessage()), e);
            final var cause = getRootCause(e);
            if (m_settings.isFailOnConnectionProblems()) {
                throw new ProcessingException(getReadableCauseMessage(cause, triple.url()), cause);
            } else {
                response = tryReconstructResponse(cause.getMessage());
                missing = new MissingCell(cause.getMessage());
            }
        } catch (Exception e) {
            LOGGER.debug("Call #%s failed: %s".formatted(m_consumedRows.get() + 1, e.getMessage()), e);
            throw new ProcessingException(ExceptionUtils.getRootCause(e));
        } finally {
            // assumes one client per request (which seems unnecessary)
            // but this is what we currently do
            final var client = triple.client();
            if (client != null) {
                client.close();
            }
        }
        return new ResultPair(response, missing);
    }

    /**
     * First request to the REST API. This is a manual invocation of the request which can result
     * in any state, including errorneous ones. This is in constrast to {@link #makeFollowingCall(DataRow)}
     * which assumes that all unexpected exceptional cases have already been handled.
     *
     * @param row the first data row to process
     * @return list of data cells, containing the parsed data
     *
     * @throws InvalidSettingsException if something went wrong in configuration
     * @throws MalformedURLException if the URL was invalid
     */
    public DataCell[] makeFirstCall(final DataRow row) throws InvalidSettingsException, MalformedURLException {
        ResultPair result;
        try {
            result = performSingleRequest(row);
        } catch (MalformedURLException e) {
            // handle early abort due to invalid URL (throws exception if != MISSING)
            abortDueToInvalidURL(row, e);
            result = new ResultPair(null, new MissingCell(String.format("%s: %s", //
                InvalidURLPolicy.INVALID_URL_ERROR, e.getMessage())));
        }
        return firstResponseToDataCells(result);
    }

    @SuppressWarnings("resource")
    private DataCell[] firstResponseToDataCells(final ResultPair result) {
        final var response = result.response();
        try {
            return m_responseHandler.handleFirstResponse(getTableSpec(), response, result.missing());
        } finally {
            closeResponse(response);
        }
    }

    /**
     * Multi-row request execution and response handling.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public DataCell[] getCells(final DataRow row) {
        final var cacheResult = m_responseHandler.lookupResponseCache(row);
        if (cacheResult.isPresent()) {
            return cacheResult.get();
        }
        return makeFollowingCall(row);
    }

    /**
     * Next request to the REST API. Assumes to-be-skipped rows are not passed in here,
     * and required (spec) initializations have been done already.
     *
     * @param row the first data row to process
     * @return array of produced response data cells
     */
    public DataCell[] makeFollowingCall(final DataRow row) {
        ResultPair result;
        try {
            result = performSingleRequest(row);
        } catch (MalformedURLException e1) {
            // handle early abort due to invalid URL
            try {
                abortDueToInvalidURL(row, e1);
            } catch (MalformedURLException e2) {
                throw new IllegalStateException("Found a URL to skip which should have been filtered already", e2);
            }
            result = new ResultPair(null, new MissingCell(String.format("%s: %s", //
                InvalidURLPolicy.INVALID_URL_ERROR, e1.getMessage())));
        } catch (InvalidSettingsException e) {
            // should not occur since first calls would already have thrown an ISE
            throw new IllegalStateException(e);
        }
        return followingResponseToDataCells(result, row);
    }

    @SuppressWarnings("resource")
    private DataCell[] followingResponseToDataCells(final ResultPair result, final DataRow row) {
        final DataCell[] cells;
        try {
            final var response = result.response();
            try {
                cells = m_responseHandler.handleFollowingResponse(getTableSpec(), response, result.missing());
            } finally {
                closeResponse(response);
            }
            // future improvement: only when we are requesting from the same domain
            if (m_settings.isUseDelay()) {
                sleepWithMonitor(m_settings.getDelay(), t -> {
                    m_monitor.setMessage("Waiting until next call: %ss".formatted(TimeUnit.MILLISECONDS.toSeconds(t)));
                    return false; // wait entire time, no wake up condition
                });
            }
        } catch (CanceledExecutionException e) {
            // cannot check for cancelled properly, so this workaround
            throw new IllegalStateException(e);
        } finally {
            m_consumedRows.getAndIncrement();
        }
        addProgress(row);
        return cells;
    }

    // -- UTILITIES --

    /**
     * Checks whether the request was aborted early due to an invalid URL.
     * If this is the case, it is handled according to {@link RestSettings#getInvalidURLPolicy()}.
     *
     * @param row current data row
     * @param exception the exception caused by an invalid URL/URL
     * @throws MalformedURLException if aborting due to invalid URL (i.e. not handling the response)
     */
    private void abortDueToInvalidURL(final DataRow row, final MalformedURLException exception)
            throws MalformedURLException {
        if (exception == null) {
            return;
        }
        if (m_settings.getInvalidURLPolicy() == InvalidURLPolicy.FAIL) {
            if (m_settings.isUseConstantURL()) {
                // we validate the constant URL on configure, if it fails here something went wrong
                throw new IllegalStateException("The URL is invalid although it should not be", exception);
            } else {
                // is thrown up to node execution, makes node fail
                throw new ProcessingException("Found invalid URL in row: %s".formatted(row.getKey()), exception);
            }
        }
        if (m_settings.getInvalidURLPolicy() == InvalidURLPolicy.SKIP) {
            // only occurs for constant URLs, for table input, a row filter is applied prior
            throw new MalformedURLException(exception.getMessage());
        }
        // if the invalid URL policy is MISSING, do nothing and handle empty (or 'null') response
    }

    private void sleepWithMonitor(final long delay, final LongPredicate condition)
            throws CanceledExecutionException {
        m_monitor.checkCanceled();
        final var t0 = System.currentTimeMillis();
        var waited = 0L;
        while (waited < delay) {
            try {
                Thread.sleep(Math.min(delay - waited, CHECK_INTERVAL_MS));
                m_monitor.checkCanceled();
                waited = System.currentTimeMillis() - t0;
                if (condition.test(waited)) {
                    return;
                }
            } catch (InterruptedException e) { // NOSONAR
                m_monitor.checkCanceled();
                LOGGER.debug("Thread has been interrupted while waiting for next REST call to make", e);
            }

        }
    }

    private void addProgress(final DataRow row) {
        setProgress(m_consumedRows.get(), m_tableSize, RestNodeModel.getRowKey(row), m_monitor);
    }

    // -- HANDLER & RESULT DECLARATIONS --

    /**
     * Divides the REST response handling into two stages: handling the first response and initializing the
     * response data format, and producing {@link DataCell}s from subsequent responses.
     *
     * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
     */
    interface MultiResponseHandler {
        /**
         * Initializes the response data format from the first request.
         *
         * @param spec initial data table spec
         * @param response the first response
         * @param missing if a response fails, the missing cell is automatically created, and handled here as well
         * @return list of data cells, containing the parsed data
         */
        DataCell[] handleFirstResponse(final DataTableSpec spec, final Response response,
            final MissingCell missing);

        /**
         * Produces the response data as a list of {@link DataCell}s from subsequent responses. This method has
         * the be called *after* {@link #handleFirstResponse(DataTableSpec, Response, MissingCell)}, and can also
         * be called, multiple times. This allows per-row-response handling.
         *
         * @param spec initial data table spec
         * @param response the currently incoming response for that row
         * @param missing the missing cell from a failed request, handled here as well
         * @return list of data cells, containing the parsed data
         */
        DataCell[] handleFollowingResponse(final DataTableSpec spec, final Response response,
            final MissingCell missing);

        /**
         * Checks whether response data for a certain {@link DataRow} already has been retrieved,
         * for example by the {@link RowKey}. The interface method provides a default implementation,
         * always returning {@link Optional#empty()}, i.e. no cache.
         *
         * @param row data row to be looked up
         * @return the stored response data
         */
        default Optional<DataCell[]> lookupResponseCache(final DataRow row) {
            return Optional.empty();
        }
    }

    /**
     * Triple holding the entire state for a single REST request.
     *
     * @param invocation the 'executable' object, performing the request
     * @param url target URL
     * @param client underlying web client
     */
    record InvocationTriple(Invocation invocation, URL url, Client client) {
    }

    /**
     * Pair holding the the REST request result, consisting of the parsed HTTP response,
     * as well as a potential HTTP error, wrapped in a {@link MissingCell}.
     *
     * @param response the received HTTP response
     * @param missing nullable, data cell constructed from a possible error
     */
    record ResultPair(Response response, MissingCell missing) {
    }
}
