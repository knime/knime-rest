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
package org.knime.rest.nodes.get;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import javax.swing.table.AbstractTableModel;

import org.knime.rest.nodes.get.RestGetSettings.ReferenceType;
import org.knime.rest.nodes.get.RestGetSettings.RequestHeaderKeyItem;

/**
 *
 * @author Gabor Bakos
 */
class RequestTableModel extends AbstractTableModel implements Iterable<RequestHeaderKeyItem> {
    static enum Columns {
            headerKey, value, kind/*, parameterKind*/, delete;
    }

    private final List<RequestHeaderKeyItem> m_content = new ArrayList<>();

    /**
     *
     */
    private static final long serialVersionUID = 4397800184937700474L;

    /**
     *
     */
    public RequestTableModel() {
        // TODO Auto-generated constructor stub
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
        return 3;//header key, value, value kind (, key kind for non-get)
    }

    /**
     * {@inheritDoc}
     *
     * @throws IndexOutOfBoundsException when {@code rowIndex} or {@code columnIndex} is wrong.
     */
    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex) {
        RequestHeaderKeyItem setting = m_content.get(rowIndex);
        Columns col = Columns.values()[columnIndex];
        switch (col) {
            case headerKey://output+
                return //Pair.create(setting.getKey(), setting.getType());
                setting.getKey();
            case value:
                return setting.getValueReference();
            case kind:
                return setting.getKind();
            /*case parameterKind:
                return setting.getParameterKind();*/
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
        Columns col = Columns.values()[columnIndex];
        if (rowIndex >= m_content.size()) {
            return;
        }
        RequestHeaderKeyItem setting = m_content.get(rowIndex);
        switch (col) {
            case headerKey: {
                /*if (aValue instanceof Pair<?, ?>) {
                    Pair<?, ?> pair = (Pair<?, ?>)aValue;
                    Object first = pair.getFirst();
                    Object second = pair.getSecond();
                    setValueAt(first, rowIndex, columnIndex);
                    setValueAt(second, rowIndex, columnIndex);
                } else*/ if (aValue instanceof String) {
                    String headerKey = (String)aValue;
                    String key = setting.getKey();
                    m_content.set(rowIndex,
                        new RequestHeaderKeyItem(headerKey, setting.getValueReference(), setting.getKind()));
                    if (!Objects.equals(key, headerKey)) {
                        fireTableCellUpdated(rowIndex, columnIndex);
                    }
                } /*else if (aValue instanceof OutputType) {
                    OutputType outputType = (OutputType)aValue;
                    OutputType returnType = setting.getReturnType();
                    setting.setReturnType(outputType);
                    if (!Objects.equals(returnType, outputType)) {
                        fireTableCellUpdated(rowIndex, columnIndex);
                    }
                  }*/
                break;
            }
            case value: {
                if (aValue instanceof String) {
                    String newReference = (String)aValue;
                    String reference = setting.getValueReference();
                    m_content.set(rowIndex, new RequestHeaderKeyItem(setting.getKey(), newReference, setting.getKind()));
                    if (!Objects.equals(newReference, reference)) {
                        fireTableCellUpdated(rowIndex, columnIndex);
                    }
                }
                break;
            }
            case kind: {
                if (aValue instanceof ReferenceType) {
                    ReferenceType newKind = (ReferenceType)aValue;
                    ReferenceType kind = setting.getKind();
                    m_content.set(rowIndex,
                        new RequestHeaderKeyItem(setting.getKey(), setting.getValueReference(), newKind));
                    if (newKind != kind) {
                        fireTableCellUpdated(rowIndex, columnIndex);
                    }
                }
                break;
            }
//            case parameterKind: {
//                if (aValue instanceof ParameterKind) {
//                    ParameterKind paths = (ParameterKind)aValue;
//                    ParameterKind returnPaths = setting.getParameterKind();
//                    m_content.set(rowIndex, setting);
//                    if (paths != returnPaths) {
//                        fireTableCellUpdated(rowIndex, columnIndex);
//                    }
//                }
//                break;
//            }
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

    void addRow(final RequestHeaderKeyItem setting) {
        m_content.add(setting);
        fireTableRowsInserted(m_content.size() - 1, m_content.size() - 1);
    }

    RequestHeaderKeyItem newRow() {
        final RequestHeaderKeyItem ret = new RequestHeaderKeyItem("", "", ReferenceType.Constant);
        addRow(ret);
        return ret;
    }

    void insertRow(final int rowIndex, final RequestHeaderKeyItem setting) {
        if (rowIndex >= 0 && rowIndex <= m_content.size()) {
            m_content.add(rowIndex, setting);
            fireTableRowsInserted(rowIndex, rowIndex);
        }
    }

    void removeRow(final int rowIndex) {
        if (rowIndex >= 0) {
            m_content.remove(rowIndex);
            fireTableRowsDeleted(rowIndex, rowIndex);
        }
    }

    /**
     *
     */
    void clear() {
        int origSize = m_content.size();
        m_content.clear();
        if (origSize > 0) {
            fireTableRowsDeleted(0, origSize - 1);
        }
    }
}
