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
 *   24. Apr. 2016. (Gabor Bakos): created
 */
package org.knime.rest.nodes.common;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ColumnSelectionPanel;

/**
 * A REST node dialog for the methods that require a body for the invocations.
 * This class adds {@code Request Body} tab.
 *
 * @author Gabor Bakos
 * @param <S> The type of the settings.
 */
public abstract class RestWithBodyNodeDialog<S extends RestWithBodySettings> extends RestNodeDialog<S> {

    private final JRadioButton m_useConstantRequestBody = new JRadioButton("Use constant body");

    private final JRadioButton m_useRequestBodyColumn = new JRadioButton("Use column's content as body");
    {
        final ButtonGroup bg = new ButtonGroup();
        bg.add(m_useConstantRequestBody);
        bg.add(m_useRequestBodyColumn);
    }

    private final JTextArea m_constantRequestBody = new JTextArea();

    private final ColumnSelectionPanel m_requestBodyColumn = new ColumnSelectionPanel((String)null);

    /**
     * Constructs the dialog with the request body tab.
     * @param cfg The node creation configuration.
     */
    protected RestWithBodyNodeDialog(final NodeCreationConfiguration cfg) {
        super(cfg);
        addRequestBodyTab();
        m_requestBodyColumn.setRequired(false);
    }

    /**
     * The request body tab configuration (only if the method requires it).
     */
    protected final void addRequestBodyTab() {
        addTabAt(3, "Request Body", createRequestBodyPanel());
    }

    /**
     * @return The request body panel.
     */
    private JPanel createRequestBodyPanel() {
        final JPanel ret = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.gridx = 0;
        gbc.gridy = 0;
        ret.add(m_useRequestBodyColumn, gbc);
        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        ret.add(m_requestBodyColumn, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        ret.add(m_useConstantRequestBody, gbc);
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        ret.add(new JScrollPane(m_constantRequestBody), gbc);
        gbc.gridy++;
        m_useConstantRequestBody.addActionListener(v -> enableConstantRequestBodyColumn(true));
        m_useRequestBodyColumn.addActionListener(v -> enableConstantRequestBodyColumn(false));
        m_useConstantRequestBody.setSelected(true);
        return ret;
    }

    /**
     * @param enable The new enabledness of the body specifications. {@code true} means the constant body is enabled,
     *            column selection is disabled.
     */
    private void enableConstantRequestBodyColumn(final boolean enable) {
        m_constantRequestBody.setEnabled(enable);
        m_requestBodyColumn.setEnabled(!enable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        getSettings().setUseConstantRequestBody(m_useConstantRequestBody.isSelected());
        getSettings().setConstantRequestBody(m_constantRequestBody.getText());
        getSettings().setRequestBodyColumn(m_requestBodyColumn.getSelectedColumn());
        super.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        super.loadSettingsFrom(settings, specs);
        m_useConstantRequestBody.setSelected(getSettings().isUseConstantRequestBody());
        m_useRequestBodyColumn.setSelected(!getSettings().isUseConstantRequestBody());
        m_constantRequestBody.setText(getSettings().getConstantRequestBody());
        // if present, first spec must be the table spec - otherwise create an empty spec
        final var tableSpec = specs.length > 0 && specs[0] != null ? (DataTableSpec)specs[0] : new DataTableSpec();
        if (tableSpec.getNumColumns() > 0) {
            m_useRequestBodyColumn.setEnabled(true);
            m_requestBodyColumn.setEnabled(m_useRequestBodyColumn.isSelected());
        } else {
            int nrItemsInList = m_requestBodyColumn.getNrItemsInList();
            m_useRequestBodyColumn.setEnabled(nrItemsInList > 0);
            m_requestBodyColumn.setEnabled(nrItemsInList > 0);
        }
        m_requestBodyColumn.update(tableSpec, getSettings().getRequestBodyColumn(), false, true);
    }
}
