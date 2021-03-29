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
 *   29 Mar 2021 (Knime): created
 */
package org.knime.rest.nodes.common;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;

public class DisableablePanel extends JPanel implements ItemListener {
    private final JCheckBox m_checkbox;

    private final JPanel m_childContainer;

    private final GridBagConstraints m_gbc;

    private final GridBagConstraints m_childGBC;

    @SuppressWarnings("java:S1699") // calls to overridable methods in constructor -- fine in swing.
    public DisableablePanel(final String label) {
        this.setLayout(new GridBagLayout());
        m_gbc = FramedPanel.initGridBagConstraints();
        // padding for checkbox
        m_gbc.insets = new Insets(0, 0, 7, 0);
        m_checkbox = new JCheckBox(label);
        m_checkbox.addItemListener(this); // NOSONAR
        this.add(m_checkbox, m_gbc);
        m_gbc.insets = new Insets(0, 0, 0, 0);

        m_childContainer = new JPanel();
        m_childGBC = FramedPanel.initGridBagConstraints();
        m_childContainer.setLayout(new GridBagLayout());
        // padding applied to each element added to childContainer
        m_childGBC.insets = new Insets(0, 27, 5, 0);

        m_gbc.gridy++;
        this.add(m_childContainer, m_gbc);
        // checkbox is disabled by default, make sure child fields are disabled aswell.
        DisableablePanel.toggleChildrenRec(m_childContainer, false);
    }

    public void addChild(final JPanel child) {
        m_childGBC.gridy++;
        m_childContainer.add(child, m_childGBC);
    }

    public void addLabeledSpinner(final String label, final String tooltip, final JSpinner spinner) {
        m_childGBC.gridy++;
        m_childGBC.gridx = 0;

        m_childGBC.weightx = 0;
        Box labelBox = Box.createHorizontalBox();
        labelBox.add(new JLabel(label));
        labelBox.add(Box.createHorizontalStrut(0));
        m_childContainer.add(labelBox, m_childGBC);

        m_childGBC.gridx++;
        m_childGBC.weightx = 1;
        m_childContainer.add(spinner, m_childGBC);

        labelBox.setToolTipText(tooltip);
        spinner.setToolTipText(tooltip);
    }

    @Override
    public void itemStateChanged(final ItemEvent e) {
        // do not need to check which input triggered `e` since only one is registered to this listener.
        // disable all contained components
        toggleChildrenRec(m_childContainer, e.getStateChange() == ItemEvent.SELECTED);
    }

    private static void toggleChildrenRec(final Component comp, final boolean state) {
        Color col = state ? Color.BLACK : Color.LIGHT_GRAY;
        comp.setForeground(col);
        comp.setEnabled(state);
        // if component is a container, recurse
        if (comp instanceof Container) {
            Container cont = (Container)comp;
            Arrays.stream(cont.getComponents()).forEach(c -> toggleChildrenRec(c, state));
        }
    }

    public void setActive(final boolean value) {
        m_checkbox.setSelected(value);
        // setSelected would not call the itemStateChanged listener.
        toggleChildrenRec(m_childContainer, value);
    }

    public boolean isActive() {
        return m_checkbox.isSelected();
    }
}
