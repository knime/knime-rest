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
 *   Mar 21, 2024 (lw): created
 */
package org.knime.rest.nodes.common;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.MalformedURLException;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.MissingCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.rest.nodes.common.RestSettings.HttpMethod;
import org.knime.rest.util.CooldownContext;
import org.knime.rest.util.InvalidURLPolicy;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;

/**
 * Tests the {@link AbstractRequestExecutor} and its response handling.
 *
 * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
 */
class AbstractRequestExecutorTest {

    @Test
    void testSuccessfulResponseHandling() {
        assertDoesNotThrow(() -> new SucceedingExecutor(null).makeFirstCall(null), //
            "GET request handling failed");
    }

    @Test
    void testNoResponseHandling() {
        assertDoesNotThrow(() -> new NoOpExecutor(null).makeFirstCall(null), //
            "GET request handling failed");
    }

    @Test
    void testFailingResponseHandling() {
        // technically, a ProcessingException should be thrown but we added URL validation on configure
        // and if the executor encounters one in the settings (which we have below), it throws an IllegalStateException
        assertThrows(IllegalStateException.class, () -> new ErrorThrowingExecutor(null).makeFirstCall(null), // NOSONAR
            "Request execution should have failed with due to an invalid URL");
    }

    /**
     * Models a successful GET request execution with a 200 response.
     */
    private static class SucceedingExecutor extends AbstractRequestExecutor<RestSettings> {

        private static final String TARGET_URL = "https://httpbin.testing.knime.com/get";

        SucceedingExecutor(final DataTableSpec inSpec) {
            super(new Handler(), inSpec, new DataColumnSpec[0], createSettings(), new CooldownContext(), null,
                new AtomicLong());
        }

        private static RestSettings createSettings() {
            final var settings = new RestSettings(HttpMethod.GET);
            settings.setUseConstantURL(true);
            settings.setConstantURL(TARGET_URL);
            return settings;
        }

        @SuppressWarnings("resource")
        @Override
        public InvocationTriple createInvocationTriple(final DataRow row)
            throws InvalidSettingsException, MalformedURLException {
            final var client = ClientBuilder.newBuilder().build();
            final var target = client.target(TARGET_URL);
            return new InvocationTriple(target.request().buildGet(), target.getUri().toURL(), client);
        }

        private static void assertResponse(final Response response, final MissingCell missing) {
            assertNotNull(response, "GET request should have succeeded but got missing response");
            assertEquals(200, response.getStatus(), "The HTTP request should have gotten a 200 OK response.");
            assertNull(missing, "GET request should not have generated a missing cell as it should be successful");
        }

        private static class Handler implements MultiResponseHandler {
            @Override
            public DataCell[] handleFirstResponse(final DataTableSpec spec, final Response response,
                final MissingCell missing) {
                assertResponse(response, missing);
                return null;
            }

            @Override
            public DataCell[] handleFollowingResponse(final DataTableSpec spec, final Response response,
                final MissingCell missing) {
                assertResponse(response, missing);
                return null;
            }
        }

        @Override
        public void inspectAndThrowException(final Response response) throws ProcessingException {
            // response exception inspection is not tested here
        }
    }

    /**
     * Models request execution that performs no requests, since the request preparation fails (see
     * {@link #createInvocationTriple(DataRow)}).
     */
    private class NoOpExecutor extends AbstractRequestExecutor<RestSettings> {

        NoOpExecutor(final DataTableSpec inSpec) {
            super(new Handler(), inSpec, new DataColumnSpec[0], new RestSettings(HttpMethod.GET), new CooldownContext(),
                null, new AtomicLong());
        }

        @Override
        public InvocationTriple createInvocationTriple(final DataRow row)
            throws InvalidSettingsException, MalformedURLException {
            // using an invalid URL signal to execute nothing
            throw new MalformedURLException("this should fail");
        }

        private static void assertResponse(final Response response, final MissingCell missing) {
            assertNull(response, "GET request response should be null since no request was executed");
            assertTrue(
                missing != null && StringUtils.startsWith(missing.getError(), InvalidURLPolicy.INVALID_URL_ERROR), //
                "GET request should have generated a missing cell specifying an invalid URL as cause");
        }

        private static class Handler implements MultiResponseHandler {
            @Override
            public DataCell[] handleFirstResponse(final DataTableSpec spec, final Response response,
                final MissingCell missing) {
                assertResponse(response, missing);
                return null;
            }

            @Override
            public DataCell[] handleFollowingResponse(final DataTableSpec spec, final Response response,
                final MissingCell missing) {
                assertResponse(response, missing);
                return null;
            }
        }

        @Override
        public void inspectAndThrowException(final Response response) throws ProcessingException {
            // response exception inspection is not tested here
        }
    }

    /**
     * Models request execution that throws an exception dURLng the request, hence response handling is never reached.
     */
    private class ErrorThrowingExecutor extends AbstractRequestExecutor<RestSettings> {

        private static final String TARGET_URL = "invalidprotocol://httpbin.testing.knime.com/get";

        ErrorThrowingExecutor(final DataTableSpec inSpec) {
            super(new Handler(), inSpec, new DataColumnSpec[0], createSettings(), new CooldownContext(), null,
                new AtomicLong());
        }

        private static RestSettings createSettings() {
            final var settings = new RestSettings(HttpMethod.GET);
            settings.setUseConstantURL(true);
            settings.setConstantURL(TARGET_URL);
            // failing on invalid URLs which the TARGET_URL is one
            settings.setInvalidURLPolicy("FAIL");
            return settings;
        }

        @SuppressWarnings("resource")
        @Override
        public InvocationTriple createInvocationTriple(final DataRow row)
            throws InvalidSettingsException, MalformedURLException {
            final var client = ClientBuilder.newBuilder().build();
            final var target = client.target(RestNodeModel.validateURLString(TARGET_URL).toString());
            return new InvocationTriple(target.request().buildGet(), target.getUri().toURL(), client);
        }

        private static class Handler implements MultiResponseHandler {
            @Override
            public DataCell[] handleFirstResponse(final DataTableSpec spec, final Response response,
                final MissingCell missing) {
                throw new AssertionError("Should not have reached here as invalid URL aborts response handling");
            }

            @Override
            public DataCell[] handleFollowingResponse(final DataTableSpec spec, final Response response,
                final MissingCell missing) {
                throw new AssertionError("Should not have reached here as invalid URL aborts response handling");
            }
        }

        @Override
        public void inspectAndThrowException(final Response response) throws ProcessingException {
            // response exception inspection is not tested here
        }
    }
}
