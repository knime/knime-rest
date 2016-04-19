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
 *   30. Jan. 2016. (Gabor Bakos): created
 */
package org.knime.rest.nodes.post;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import javax.swing.table.AbstractTableModel;

import org.knime.core.data.DataType;
import org.knime.core.util.Pair;
import org.knime.rest.nodes.post.RestPostSettings.ResponseHeaderItem;

/**
 *
 * @author Gabor Bakos
 */
class ResponseTableModel extends AbstractTableModel implements Iterable<ResponseHeaderItem> {
    private static final long serialVersionUID = -7048890183554347405L;

    private static enum Columns {
            headerKey, outputColumn;
    }

    private final List<ResponseHeaderItem> content = new ArrayList<>();

    /**
     *
     */
    public ResponseTableModel() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRowCount() {
        return content.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getColumnCount() {
        return 2;//key, output column name + type
    }

    /**
     * {@inheritDoc}
     *
     * @throws IndexOutOfBoundsException when {@code rowIndex} or {@code columnIndex} is wrong.
     */
    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex) {
        ResponseHeaderItem setting = content.get(rowIndex);
        Columns col = Columns.values()[columnIndex];
        switch (col) {
            case headerKey://output+
                return //Pair.create(setting.getKey(), setting.getType());
                setting.getHeaderKey();
            case outputColumn:
                return Pair.create(setting.getOutputColumnName(), setting.getType());
            default:
                throw new IllegalStateException("Unknown column: ");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
        Columns col = Columns.values()[columnIndex];
        if (rowIndex >= content.size()) {
            return;
        }
        ResponseHeaderItem setting = content.get(rowIndex);
        switch (col) {
            case headerKey: {
                if (aValue instanceof String) {
                    String headerKey = (String)aValue;
                    String key = setting.getHeaderKey();
                    content.set(rowIndex,
                        new ResponseHeaderItem(headerKey, setting.getType(), setting.getOutputColumnName()));
                    if (!Objects.equals(key, headerKey)) {
                        fireTableCellUpdated(rowIndex, columnIndex);
                    }
                }
                break;
            }
            case outputColumn: {
                if (aValue instanceof Pair<?, ?>) {
                    Pair<?, ?> pair = (Pair<?, ?>)aValue;
                    Object first = pair.getFirst();
                    Object second = pair.getSecond();
                    setValueAt(first, rowIndex, columnIndex);
                    setValueAt(second, rowIndex, columnIndex);
                } else if (aValue instanceof String) {
                    String outputColumn = (String)aValue;
                    String output = setting.getOutputColumnName();
                    content.set(rowIndex,
                        new ResponseHeaderItem(setting.getHeaderKey(), setting.getType(), outputColumn));
                    if (!Objects.equals(output, outputColumn)) {
                        fireTableCellUpdated(rowIndex, columnIndex);
                    }
                } else if (aValue instanceof DataType) {
                    DataType outputType = (DataType)aValue;
                    DataType returnType = setting.getType();
                    content.set(rowIndex,
                        new ResponseHeaderItem(setting.getHeaderKey(), outputType, setting.getOutputColumnName()));
                    if (!Objects.equals(returnType, outputType)) {
                        fireTableCellUpdated(rowIndex, columnIndex);
                    }
                }
                break;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<ResponseHeaderItem> iterator() {
        return content.iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCellEditable(final int rowIndex, final int columnIndex) {
        return true;
    }

    void addRow(final ResponseHeaderItem setting) {
        content.add(setting);
        fireTableRowsInserted(content.size() - 1, content.size() - 1);
    }

    ResponseHeaderItem newRow() {
        ResponseHeaderItem ret = new ResponseHeaderItem("");
        addRow(ret);
        return ret;
    }

    void insertRow(final int rowIndex, final ResponseHeaderItem setting) {
        if (rowIndex >= 0 && rowIndex <= content.size()) {
            content.add(rowIndex, setting);
            fireTableRowsInserted(rowIndex, rowIndex);
        }
    }

    void removeRow(final int rowIndex) {
        if (rowIndex >= 0) {
            content.remove(rowIndex);
            fireTableRowsDeleted(rowIndex, rowIndex);
        }
    }

    /**
     *
     */
    void clear() {
        int origSize = content.size();
        content.clear();
        if (origSize > 0) {
            fireTableRowsDeleted(0, origSize - 1);
        }
    }
}
