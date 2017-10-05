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
 *   5. Mar. 2016. (Gabor Bakos): created
 */
package org.knime.rest.nodes.common;

import java.awt.Component;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

/**
 * A {@link TableCellEditor} and also a {@link TableCellRenderer} for button actions.
 *
 * @author Gabor Bakos
 */
class ButtonCell implements TableCellEditor, TableCellRenderer {
    private final JButton m_button, m_buttonRender;

    private final JPanel m_panel, m_panelRender;

    private final List<CellEditorListener> m_listeners = new ArrayList<>();

    private int m_row;

    /**
     * Constructs the editor.
     */
    public ButtonCell() {
        m_button = new JButton();
        m_buttonRender = new JButton();
        m_button.setFont(m_button.getFont().deriveFont(11f));
        m_buttonRender.setFont(m_buttonRender.getFont().deriveFont(10f));
        m_panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
        m_button.setBorder(null);
        m_buttonRender.setBorder(null);
        m_panelRender = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
        m_panelRender.add(m_buttonRender);
        m_panel.add(m_button);
    }

    /**
     * @param action The action to perform on click.
     */
    public void setAction(final Action action) {
        m_button.setAction(action);
        m_buttonRender.setAction(action);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getCellEditorValue() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCellEditable(final EventObject anEvent) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean shouldSelectCell(final EventObject anEvent) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean stopCellEditing() {
        m_button.setSelected(false);
        m_panel.repaint();
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancelCellEditing() {
        //Do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addCellEditorListener(final CellEditorListener l) {
        m_listeners.add(l);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeCellEditorListener(final CellEditorListener l) {
        m_listeners.remove(l);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
        final boolean hasFocus, final int row, final int column) {
        return m_panelRender;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getTableCellEditorComponent(final JTable table, final Object value, final boolean isSelected,
        final int row, final int column) {
        m_row = row;
        m_panel.repaint();
        m_button.repaint();
        return m_panel;
    }

    /**
     * @return the row
     */
    public int getRow() {
        return m_row;
    }

    /**
     * @param row the row to set
     */
    public void setRow(final int row) {
        if (row < 0) {
            m_button.transferFocus();
        }
        m_row = row;
    }

    /**
     * Repaints the component.
     */
    public void repaint() {
        m_panel.repaint();
    }

    /**
     * @return the button
     */
    public JButton getButton() {
        return m_button;
    }

    /**
     * @return the buttonRender
     */
    public JButton getButtonRender() {
        return m_buttonRender;
    }
}
