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
 *   Sep 25, 2019 (Simon Schmid, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.rest.nodes.webpageretriever;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.rest.nodes.common.RestNodeDialog;

/**
 * Node dialog of the Webpage Retriever node.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 */
final class WebpageRetrieverNodeDialog extends RestNodeDialog<WebpageRetrieverSettings> {

    private JCheckBox m_outputAsXMLCheckBox;

    private JCheckBox m_replaceRelativeURLSCheckBox;

    private JTextField m_outputColumnNameTextField;

    /** */
    WebpageRetrieverNodeDialog() {
        super();
        renameTab("Connection Settings", "General Settings");
        removeTab("Response Headers");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected WebpageRetrieverSettings createSettings() {
        return new WebpageRetrieverSettings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JPanel createConnectionSettingsTab() {
        final JPanel superPanel = super.createConnectionSettingsTab();
        m_bodyColumnName.setVisible(false);
        m_labelBodyColumnName.setVisible(false);

        superPanel.setBorder(BorderFactory.createTitledBorder("Connection Settings"));
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Output Settings"));
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(6, 4, 2, 4);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.weighty = 0;
        m_replaceRelativeURLSCheckBox = new JCheckBox("Replace relative URLs with absolute URLs");
        m_outputAsXMLCheckBox = new JCheckBox("Output as XML");
        m_outputColumnNameTextField = new JTextField("asd");
        m_outputColumnNameTextField.setPreferredSize(new Dimension(150, 20));
        panel.add(new JLabel("Output column name "), gbc);
        gbc.gridx++;
        panel.add(m_outputColumnNameTextField, gbc);
        gbc.gridx = 0;
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.gridwidth = 2;
        gbc.gridy++;
        panel.add(m_outputAsXMLCheckBox, gbc);
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.gridy++;
        panel.add(m_replaceRelativeURLSCheckBox, gbc);

        final JPanel panelComplete = new JPanel(new GridBagLayout());
        final GridBagConstraints gbcComplete = new GridBagConstraints();
        gbcComplete.anchor = GridBagConstraints.FIRST_LINE_START;
        gbcComplete.fill = GridBagConstraints.HORIZONTAL;
        gbcComplete.gridx = 0;
        gbcComplete.gridy = 0;
        gbcComplete.weightx = 0;
        gbcComplete.weighty = 0;
        panelComplete.add(superPanel, gbcComplete);

        gbcComplete.weightx = 1;
        gbcComplete.weighty = 1;
        gbcComplete.gridy++;
        panelComplete.add(panel, gbcComplete);
        return panelComplete;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        final WebpageRetrieverSettings nodeSettings = getSettings();
        if (m_outputColumnNameTextField.getText().trim().isEmpty()) {
            throw new InvalidSettingsException("The output column name must not be empty.");
        }
        nodeSettings.setOutputColumnName(m_outputColumnNameTextField.getText());
        nodeSettings.setReplaceRelativeURLS(m_replaceRelativeURLSCheckBox.isSelected());
        nodeSettings.setOutputAsXML(m_outputAsXMLCheckBox.isSelected());
        super.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        final WebpageRetrieverSettings nodeSettings = getSettings();
        super.loadSettingsFrom(settings, specs);
        m_outputColumnNameTextField.setText(nodeSettings.getOutputColumnName());
        m_replaceRelativeURLSCheckBox.setSelected(nodeSettings.isReplaceRelativeURLS());
        m_outputAsXMLCheckBox.setSelected(nodeSettings.isOutputAsXML());
    }
}
