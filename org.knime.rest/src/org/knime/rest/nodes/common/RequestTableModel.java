/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
package org.knime.rest.nodes.common;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import org.knime.rest.nodes.common.RestSettings.ReferenceType;
import org.knime.rest.nodes.common.RestSettings.RequestHeaderKeyItem;

/**
 * Table model for request headers table.
 *
 * @author Gabor Bakos
 */
@SuppressWarnings("serial")
class RequestTableModel extends AbstractTableModel implements Iterable<RequestHeaderKeyItem> {
    /** Column constants. */
    enum Columns {
            /** Header keys in the request. */
            headerKey,
            /** Value reference for the request header value. */
            value,
            /** Value {@link ReferenceType} for the request header value. */
            kind,
            /** Delete row buttons. */
            delete;
    }

    private final transient List<RequestHeaderKeyItem> m_content = new ArrayList<>();

    /**
     * Constructs the model.
     */
    RequestTableModel() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRowCount() {
        return m_content.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getColumnCount() {
        return Columns.values().length - 1/*delete is not important, not a real column.*/;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IndexOutOfBoundsException when {@code rowIndex} or {@code columnIndex} is wrong.
     */
    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex) {
        final RequestHeaderKeyItem setting = m_content.get(rowIndex);
        final Columns col = Columns.values()[columnIndex];
        switch (col) {
            case headerKey:
                return setting.getKey();
            case value:
                return setting.getValueReference();
            case kind:
                return setting.getKind();
            case delete:
                return null;
            default:
                throw new IllegalStateException("Unknown column: ");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
        final Columns col = Columns.values()[columnIndex];
        if (rowIndex >= m_content.size()) {
            return;
        }
        final RequestHeaderKeyItem setting = m_content.get(rowIndex);
        switch (col) {
            case headerKey: {
                if (aValue instanceof String) {
                    final String headerKey = (String)aValue;
                    final String key = setting.getKey();
                    m_content.set(rowIndex,
                        new RequestHeaderKeyItem(headerKey, setting.getValueReference(), setting.getKind()));
                    if (!Objects.equals(key, headerKey)) {
                        fireTableCellUpdated(rowIndex, columnIndex);
                    }
                }
                break;
            }
            case value: {
                if (aValue instanceof String) {
                    final String newReference = (String)aValue;
                    final String reference = setting.getValueReference();
                    m_content.set(rowIndex,
                        new RequestHeaderKeyItem(setting.getKey(), newReference, setting.getKind()));
                    if (!Objects.equals(newReference, reference)) {
                        fireTableCellUpdated(rowIndex, columnIndex);
                    }
                }
                break;
            }
            case kind: {
                if (aValue instanceof ReferenceType) {
                    final ReferenceType newKind = (ReferenceType)aValue;
                    final ReferenceType kind = setting.getKind();
                    m_content.set(rowIndex,
                        new RequestHeaderKeyItem(setting.getKey(), setting.getValueReference(), newKind));
                    if (newKind != kind) {
                        fireTableCellUpdated(rowIndex, columnIndex);
                    }
                }
                break;
            }
            case delete: {
                break;
            }
            default:
                throw new UnsupportedOperationException();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<RequestHeaderKeyItem> iterator() {
        return m_content.iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCellEditable(final int rowIndex, final int columnIndex) {
        return true;
    }

    /**
     * Inserts a new item to the end.
     *
     * @param setting The new item.
     */
    void addRow(final RequestHeaderKeyItem setting) {
        m_content.add(setting);
        fireTableRowsInserted(m_content.size() - 1, m_content.size() - 1);
    }

    /** Inserts a new constant item. */
    RequestHeaderKeyItem newRow() {
        final RequestHeaderKeyItem ret = new RequestHeaderKeyItem("", "", ReferenceType.Constant);
        addRow(ret);
        return ret;
    }

    /** Inserts an item to the specified position. */
    void insertRow(final int rowIndex, final RequestHeaderKeyItem setting) {
        if (rowIndex >= 0 && rowIndex <= m_content.size()) {
            m_content.add(rowIndex, setting);
            fireTableRowsInserted(rowIndex, rowIndex);
        }
    }

    /** Removes the selected row. */
    void removeRow(final int rowIndex) {
        if (rowIndex >= 0) {
            m_content.remove(rowIndex);
            fireTableRowsDeleted(rowIndex, rowIndex);
        }
    }

    /**
     * Clears the {@link TableModel}.
     */
    void clear() {
        int origSize = m_content.size();
        m_content.clear();
        if (origSize > 0) {
            fireTableRowsDeleted(0, origSize - 1);
        }
    }
}
