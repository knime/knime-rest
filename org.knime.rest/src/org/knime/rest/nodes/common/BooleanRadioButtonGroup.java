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
 */
package org.knime.rest.nodes.common;

import java.awt.GridLayout;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

/**
 * A Panel that contains two radio buttons with given labels, plus some methods to set/get the state of the radio
 * buttons according to a boolean value. Useful when the radio buttons correspond to true/false.
 *
 */
@SuppressWarnings("serial")
public class BooleanRadioButtonGroup extends JPanel {
    private final JRadioButton m_trueButton;

    private final JRadioButton m_falseButton;

    /**
     * @param trueLabel Label of the radio button
     * @param falseLabel Label of the radio button
     */
    @SuppressWarnings("java:S1699") // calls to overridable methods in constructor -- fine in swing.
    public BooleanRadioButtonGroup(final String trueLabel, final String falseLabel) {
        this.setLayout(new GridLayout(0, 1));
        m_trueButton = new JRadioButton(trueLabel);
        m_falseButton = new JRadioButton(falseLabel);

        ButtonGroup group = new ButtonGroup();
        group.add(m_trueButton);
        group.add(m_falseButton);

        this.add(m_trueButton);
        this.add(m_falseButton);
    }

    /**
     * Enable one radio button, disable the other.
     *
     * @param value
     */
    public void setValue(final boolean value) {
        m_trueButton.setSelected(value);
        m_falseButton.setSelected(!value);
    }

    /**
     * @return The state of the panel depending on which radio button is selected.
     */
    public boolean getValue() { // NOSONAR naming is adequate
        return m_trueButton.isSelected();
    }
}