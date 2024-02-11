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
 *   Feb 10, 2024 (lw): created
 */
package org.knime.rest.util;

import org.knime.base.data.filter.row.FilterRowGenerator;
import org.knime.base.data.filter.row.FilterRowTable;
import org.knime.core.data.DataRow;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableFunction;
import org.knime.core.node.streamable.StreamableOperatorInternals;

/**
 * Provides utility methods for applying an {@link FilterRowGenerator} to a table (i.e. standard
 * node execution), and to a streamable function (i.e. streaming node execution).
 *
 * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
 */
public final class RowFilterUtil {

    /**
     * Applies the given {@link FilterRowGenerator} to the buffered data table.
     * Creates a new {@link BufferedDataTable} (not filtered in-place).
     *
     * @param table buffered data table
     * @param filter row filter
     * @param exec execution context
     * @return filtered table
     * @throws CanceledExecutionException if the table creation was cancelled
     */
    public static BufferedDataTable filterBufferedDataTable(final BufferedDataTable table,
        final FilterRowGenerator filter,
        final ExecutionContext exec) throws CanceledExecutionException {
        return exec.createBufferedDataTable(new FilterRowTable(table, filter), exec);
    }

    /**
     * Wraps the given {@link StreamableFunction} in a delegating instance, which copies all method
     * functionality except for the final run of the function. There, the {@link FilterRowGenerator}
     * is applied. Only matching input rows are computed and pushed to the output.
     *
     * @param delegate function to wrap
     * @param filter row filter
     * @return function whose output is filtered
     */
    public static StreamableFunction filterStreamableFunction(final StreamableFunction delegate,
        final FilterRowGenerator filter) {
        return new RowFilteringStreamableFunction(delegate, filter);
    }

    private static class RowFilteringStreamableFunction extends StreamableFunction {

        private final StreamableFunction m_delegate;

        private final FilterRowGenerator m_filter;

        RowFilteringStreamableFunction(final StreamableFunction delegate, final FilterRowGenerator filter) {
            m_delegate = delegate;
            m_filter = filter;
        }

        @Override
        public void init(final ExecutionContext ctx) throws Exception {
            m_delegate.init(ctx);
        }

        @Override
        public DataRow compute(final DataRow inputRow, final long rowIndex) throws Exception {
            return m_delegate.compute(inputRow, rowIndex);
        }

        @Override
        public void finish() {
            m_delegate.finish();
        }

        @Override
        public StreamableOperatorInternals saveInternals() {
            return m_delegate.saveInternals();
        }

        @Override
        public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext ctx)
            throws Exception {
            final var rowInput = (RowInput)inputs[StreamableFunction.DEFAULT_INPORT_INDEX];
            final var rowOutput = (RowOutput)outputs[StreamableFunction.DEFAULT_OUTPORT_INDEX];
            init(ctx);
            try {
                DataRow inputRow;
                for (var index = 0L; (inputRow = rowInput.poll()) != null; index++) {
                    if (m_filter.isIn(inputRow)) {
                        rowOutput.push(compute(inputRow, index));
                        final long i = index;
                        final DataRow r = inputRow;
                        ctx.setMessage(() -> String.format("Row %d (\"%s\"))", i, r.getKey()));
                    }
                }
                rowInput.close();
                rowOutput.close();
            } finally {
                finish();
            }
        }
    }

    /**
     * Only utility class.
     */
    private RowFilterUtil() {
    }
}
