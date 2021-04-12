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
 *   7 May 2016 (Gabor Bakos): created
 */
package org.knime.rest.nodes.common;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;

/**
 * Fixing the {@link DefaultCellEditor} behaviour when the wrapped {@link JComboBox} is editable and editing is not
 * finished, but clicking on a different cell. It disables single click edit, but allows double-click and editing by F2.
 *
 * @author Gabor Bakos
 */
@SuppressWarnings("serial")
final class FixedCellEditorForComboBoxes extends DefaultCellEditor {
    /**
     * @param comboBox {@link JComboBox} to wrap.
     */
    public FixedCellEditorForComboBoxes(final JComboBox<String> comboBox) {
        super(comboBox);
        final EditorDelegate oldDelegate = delegate;
        delegate = new EditorDelegate() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void setValue(final Object v) {
                oldDelegate.setValue(v);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean shouldSelectCell(final EventObject anEvent) {
                return oldDelegate.shouldSelectCell(anEvent);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean stopCellEditing() {
                return oldDelegate.stopCellEditing();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Object getCellEditorValue() {
                return comboBox.isEditable() && comboBox.isEnabled() ? comboBox.getEditor().getItem()
                    : comboBox.getSelectedItem();
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCellEditable(final EventObject e) {
        //Based on http://stackoverflow.com/a/29175884/1502148
        if (super.isCellEditable(e)) {
            if (e instanceof MouseEvent) {
                MouseEvent me = (MouseEvent)e;
                return me.getClickCount() >= 2;
            }
            if (e instanceof KeyEvent) {
                KeyEvent ke = (KeyEvent)e;
                return ke.getKeyCode() == KeyEvent.VK_F2;
            }
        }
        return false;
    }
}
