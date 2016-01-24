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
 *   2016. jan. 23. (Gabor Bakos): created
 */
package org.knime.rest.nodes.get;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.uri.URIDataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionPanel;
import org.knime.core.node.util.StringHistoryPanel;
import org.knime.rest.nodes.get.RestGetSettings.RequestHeaderKeyItem;
import org.knime.rest.nodes.get.RestGetSettings.ResponseHeaderItem;

/**
 *
 * @author Gabor Bakos
 */
final class RestGetNodeDialog extends NodeDialogPane {
    private final RestGetSettings m_settings = new RestGetSettings();

    private final JRadioButton m_constantUriOption = new JRadioButton("URI: "),
            m_uriColumnOption = new JRadioButton("URI column: ");

    {
        final ButtonGroup group = new ButtonGroup();
        group.add(m_constantUriOption);
        group.add(m_uriColumnOption);
    }

    private final StringHistoryPanel m_constantUri = new StringHistoryPanel("GET uri");

    @SuppressWarnings("unchecked")
    private final ColumnSelectionPanel m_uriColumn = new ColumnSelectionPanel(StringValue.class, URIDataValue.class);

    private final JCheckBox m_useDelay = new JCheckBox("Delay: ");

    private final JSpinner m_delay = new JSpinner(new SpinnerNumberModel(Long.valueOf(0l), Long.valueOf(0L),
        Long.valueOf(30L * 60 * 1000L/*30 minutes*/), Long.valueOf(100L))),
            m_concurrency = new JSpinner(new SpinnerNumberModel(1, 1, 16/*TODO find proper default*/, 1));

    private final JCheckBox m_sslIgnoreHostnameMismatches = new JCheckBox("Ignore hostname mismatches"),
            m_sslTrustAll = new JCheckBox("Trust all certificates");

    private final DefaultTableModel m_requestHeadersModel =
        new DefaultTableModel(new String[]{"Header key", "Value", "Kind"}, 0),
            m_responseHeadersModel = new DefaultTableModel(new String[]{"Header key", "Column name"}, 0);

    private final JTable m_requestHeaders = new JTable(m_requestHeadersModel), m_responseHeaders = new JTable(m_responseHeadersModel);

    private final JCheckBox m_extractAllHeaders = new JCheckBox("Extract all headers");

    private final StringHistoryPanel m_bodyColumnName = new StringHistoryPanel("GET body");

    /**
     *
     */
    public RestGetNodeDialog() {
        addTab("Connection Settings", createConnectionSettingsTab());
        addTab("Authentication", createAuthenticationTab());
        addTab("Request Headers", createRequestHeadersTab());
        addTab("Response Headers", createResponseHeadersTab());
    }

    /**
     * @return
     */
    private JPanel createConnectionSettingsTab() {
        final JPanel ret = new JPanel();
        ret.add(m_constantUriOption);
        ret.add(m_constantUri);
        ret.add(m_uriColumnOption);
        ret.add(m_uriColumn);
        ret.add(m_useDelay);
        ret.add(m_delay);
        ret.add(new JLabel("Concurrency: "));
        ret.add(m_concurrency);
        ret.add(m_sslIgnoreHostnameMismatches);
        ret.add(m_sslTrustAll);

        m_useDelay.addActionListener(e -> m_delay.setEnabled(m_useDelay.isSelected()));
        m_constantUriOption.addActionListener(e -> {
            m_constantUri.setEnabled(m_constantUriOption.isSelected());
            m_uriColumn.setEnabled(!m_constantUriOption.isSelected());
        });
        m_uriColumnOption.addActionListener(e -> {
            m_uriColumn.setEnabled(m_uriColumnOption.isSelected());
            m_constantUri.setEnabled(!m_uriColumnOption.isSelected());
        });
        return ret;
    }

    /**
     * @return
     */
    private JPanel createAuthenticationTab() {
        //TODO
        final JPanel ret = new JPanel();
        return ret;
    }

    /**
     * @return
     */
    private JPanel createRequestHeadersTab() {
        final JPanel ret = new JPanel();
        ret.add(m_requestHeaders);
        return ret;
    }

    /**
     * @return
     */
    private JPanel createResponseHeadersTab() {
        final JPanel ret = new JPanel();
        ret.add(m_extractAllHeaders);
        ret.add(m_responseHeaders);
        ret.add(m_bodyColumnName);
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        //TODO update settings based on the UI.
        m_settings.setUseConstantURI(m_constantUriOption.isSelected());
        m_settings.setConstantURI(m_constantUri.getSelectedString());
        m_settings.setUriColumn(m_uriColumn.getSelectedColumn());
        m_settings.setUseDelay(m_useDelay.isSelected());
        m_settings.setDelay(((Number)m_delay.getValue()).longValue());
        m_settings.setConcurrency(((Number)m_concurrency.getValue()).intValue());
        m_settings.setSslIgnoreHostNameErrors(m_sslIgnoreHostnameMismatches.isSelected());
        m_settings.setSslTrustAll(m_sslTrustAll.isSelected());
        m_settings.getRequestHeaders().clear();
        final List<RequestHeaderKeyItem> headers = new ArrayList<>(m_requestHeaders.getRowCount());
        for (int i = 0; i < m_requestHeaders.getRowCount(); ++i) {
            headers.add((RequestHeaderKeyItem)m_requestHeaders.getModel().getValueAt(i, 0));
        }
        m_settings.getRequestHeaders().addAll(headers);
        m_settings.setExtractAllResponseFields(m_extractAllHeaders.isSelected());
        final List<ResponseHeaderItem> responseItems = new ArrayList<>(m_responseHeaders.getRowCount());
        for (int i = 0; i < m_responseHeaders.getRowCount(); ++i) {
            responseItems.add((ResponseHeaderItem)m_responseHeaders.getModel().getValueAt(i, 0));
        }
        m_settings.setResponseBodyColumn(m_bodyColumnName.getSelectedString());
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs) throws NotConfigurableException {
        try {
            m_settings.loadSettingsForDialog(settings, specs);
        } catch (InvalidSettingsException e) {
            throw new NotConfigurableException(e.getMessage(), e);
        }
        // TODO Update UI based on settings
        m_constantUriOption.setSelected(m_settings.isUseConstantURI());
        m_constantUri.setSelectedString(m_settings.getConstantURI());
        m_uriColumn.setSelectedColumn(m_settings.getUriColumn());
        m_useDelay.setSelected(m_settings.isUseDelay());
        m_delay.setValue(m_settings.getDelay());
        m_concurrency.setValue(m_settings.getConcurrency());
        m_sslIgnoreHostnameMismatches.setSelected(m_settings.isSslIgnoreHostNameErrors());
        m_sslTrustAll.setSelected(m_settings.isSslTrustAll());
        m_requestHeadersModel.setRowCount(0);
        for (int i = 0; i < m_settings.getRequestHeaders().size(); ++i) {
            RequestHeaderKeyItem[] items = new RequestHeaderKeyItem[m_requestHeadersModel.getColumnCount()];
            Arrays.fill(items, m_settings.getRequestHeaders().get(i));
            m_requestHeadersModel.addRow(items);
        }
        m_extractAllHeaders.setSelected(m_settings.isExtractAllResponseFields());
        m_responseHeadersModel.setRowCount(0);
        for (int i = 0; i < m_settings.getExtractFields().size(); ++i) {
            ResponseHeaderItem[] items = new ResponseHeaderItem[m_responseHeadersModel.getColumnCount()];
            Arrays.fill(items, m_settings.getExtractFields().get(i));
            m_responseHeadersModel.addRow(items);
        }
        final List<ResponseHeaderItem> responseItems = new ArrayList<>(m_responseHeaders.getRowCount());
        for (int i = 0; i < m_responseHeaders.getRowCount(); ++i) {
            responseItems.add((ResponseHeaderItem)m_responseHeaders.getModel().getValueAt(i, 0));
        }
        m_bodyColumnName.setSelectedString(m_settings.getResponseBodyColumn());
        m_settings.setResponseBodyColumn(m_bodyColumnName.getSelectedString());
    }
}
