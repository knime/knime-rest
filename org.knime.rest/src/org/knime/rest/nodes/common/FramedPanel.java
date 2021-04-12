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
 */
package org.knime.rest.nodes.common;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

/**
 * Base class for panels in the "Error Handling" tab.
 */
@SuppressWarnings({"java:S1699", "serial"}) // calls to overridable methods in constructor -- fine in swing.
abstract class FramedPanel extends JPanel {
    private final GridBagConstraints m_gbc;

    FramedPanel() {
        this.setLayout(new GridBagLayout());
        m_gbc = initGridBagConstraints();
    }

    final void setTitle(final String title) {
        this.setBorder(BorderFactory.createTitledBorder(title));
    }

    final void addAsRow(final JComponent comp) {
        m_gbc.insets = new Insets(5, 0, 5, 0);
        m_gbc.gridx = 0;
        m_gbc.gridy++;
        this.add(comp, m_gbc);
        m_gbc.insets = new Insets(0, 0, 0, 0);
    }

    static GridBagConstraints initGridBagConstraints() {
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        return gbc;
    }

    static void addLabeledSpinnerTo(final JComponent container, final GridBagConstraints gbc, final String label,
        final String tooltip, final JSpinner spinner) {
        gbc.gridy++;
        gbc.gridx = 0;

        gbc.weightx = 0;
        Box labelBox = Box.createHorizontalBox();
        labelBox.add(new JLabel(label));
        labelBox.add(Box.createHorizontalStrut(5));
        container.add(labelBox, gbc);

        gbc.gridx++;
        gbc.weightx = 1;
        container.add(spinner, gbc);

        labelBox.setToolTipText(tooltip);
        spinner.setToolTipText(tooltip);
    }

    static JSpinner createSpinner(final Number value, final int step) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(value.intValue(), 0, null, step));
        ((JSpinner.DefaultEditor)spinner.getEditor()).getTextField().setColumns(4);
        return spinner;
    }

}
