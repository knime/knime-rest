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
 *   Nov 7, 2023 (leon.wenzler): created
 */
package org.knime.rest.nodes.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.knime.core.data.DataCell;
import org.knime.core.util.proxy.testing.HttpbinTestContext;

/**
 * Tests whether the chunked transfer of requests works as intended. Mimics the previous testflow at
 * 'knime-rest/AP-7397/old' but is more robust.
 * <p>
 * See AP-21405 for details on moving from testflow to unittest.
 *
 * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
 */
final class ChunkedRequestsTest {

    // retries per request
    private static final int RETRIES = 2;

    // 0.5 seconds of delay between requests
    private static final int REQUEST_DELAY = 500;

    // request body size in bytes
    private static final int BODY_SIZE = 4 << 10;

    private static final int DEFAULT_THRESHOLD = RestWithBodyNodeModel.chunkingThreshold;

    private static String jsonBody = null;

    private static TestPostNodeModel client = null;

    @BeforeAll
    static void setup() {
        // generating requests body of size 4KB
        final var bodyTemplate = "{\"%s\":\"\"}";
        final var chars = new char[BODY_SIZE];
        Arrays.fill(chars, 'a');
        jsonBody = bodyTemplate.formatted(new String(chars));

        // setting up test node model with chunk threshold of 1KB
        final var uri = new URIBuilder(HttpbinTestContext.getURI("https")).setPath("post").toString();
        client = new TestPostNodeModel();
        client.setRequestTarget(uri);
        client.setChunkingThreshold(1 << 10);
    }

    @SuppressWarnings("static-method")
    @Test
    void notChunkedIfBodyIsSmall() {
        // chunking would technically be enabled but body is too small
        assertRetryable(() -> assertChunkingForBody("{}", false), RETRIES, REQUEST_DELAY);
    }

    @SuppressWarnings("static-method")
    @Test
    void chunkedIfBodyIsLarge() {
        assertRetryable(() -> assertChunkingForBody(jsonBody, true), RETRIES, REQUEST_DELAY);
    }

    /**
     * Asserts tests that can be retried up a certain number of times. This is useful for testing REST requests, which
     * may not succeed if sent in short intervals due to rate limiting. Each retry is followed by specified delay.
     *
     * @param test the test to run
     * @param retries number of retries
     * @param delay milliseconds to wait between retries
     * @throws AssertionError the assertion error that was thrown
     */
    private static void assertRetryable(final Runnable test, final int retries, final int delay) throws AssertionError {
        AssertionError lastException = null;
        var currentTry = 0;
        do {
            try {
                test.run();
                return;
            } catch (AssertionError e1) {
                lastException = e1;
                try {
                    Thread.sleep(delay); //NOSONAR
                } catch (InterruptedException e2) {
                    throw new RuntimeException(e2);
                }
            }
        } while (currentTry++ < retries);
        throw lastException;
    }

    private static void assertChunkingForBody(final String body, final boolean expected) {
        client.setRequestBody(body);

        // request went through
        assertDoesNotThrow(() -> client.makeRequest(), "Making POST request should not have thrown an exception.");
        final List<String> responses = Arrays.stream(client.getResponses())//
            .filter(x -> Objects.nonNull(x) && !x.isMissing())//
            .map(DataCell::toString)//
            .toList();
        assertFalse(responses.isEmpty(), "POST request should have succeeded but got only missing values as response.");
        assertThat("The HTTP request should have gotten a 200 OK response.", "200".equals(responses.get(0)));

        // assert the chunking status
        final var expectedStr = expected ? "" : " not";
        final var actualStr = expected ? " not" : "";
        assertThat("Request should%s have been chunked, but was%s.".formatted(expectedStr, actualStr),
            responses.contains("chunked") == expected);
    }

    @AfterAll
    static void cleanUp() {
        jsonBody = null;
        client = null;
        RestWithBodyNodeModel.chunkingThreshold = DEFAULT_THRESHOLD;
    }
}
