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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.text.JTextComponent;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.uri.URIDataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionPanel;
import org.knime.core.node.util.StringHistoryPanel;
import org.knime.core.util.Pair;
import org.knime.rest.generic.UserConfiguration;
import org.knime.rest.nodes.get.RestGetSettings.ParameterKind;
import org.knime.rest.nodes.get.RestGetSettings.ReferenceType;
import org.knime.rest.nodes.get.RestGetSettings.RequestHeaderKeyItem;

/**
 *
 * @author Gabor Bakos
 */
final class RestGetNodeDialog extends NodeDialogPane {
    private static final String EXTENSION_ID_FOR_REQUEST_HEADER_TEMPLATES = "org.knime.rest.header.template";

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

    private final RequestTableModel m_requestHeadersModel = new RequestTableModel();

    private final JButton m_requestAddRow = new JButton("Add header parameter"),
            m_requestEditRow = new JButton("Edit header parameter"),
            m_requestDeleteRow = new JButton("Remove header parameter");

    private final ResponseTableModel m_responseHeadersModel = new ResponseTableModel();

    private final JTable m_requestHeaders = new JTable(m_requestHeadersModel),
            m_responseHeaders = new JTable(m_responseHeadersModel);

    private final JButton m_responseAddRow = new JButton("Add header parameter"),
            m_responseEditRow = new JButton("Edit header parameter"),
            m_responseDeleteRow = new JButton("Remove header parameter");

    private final JCheckBox m_extractAllHeaders = new JCheckBox("Extract all headers");

    private final StringHistoryPanel m_bodyColumnName = new StringHistoryPanel("GET body");

    private final JComboBox<String> m_requestHeaderKey = createEditableComboBox(),
            m_requestHeaderValue = createEditableComboBox();

    private final JComboBox<ReferenceType> m_requestHeaderValueType = new JComboBox<>(ReferenceType.values());

    private final JComboBox<ParameterKind> m_requestHeaderKeyType =
        new JComboBox<>(new ParameterKind[]{ParameterKind.Header});

    private JComboBox<String> m_responseHeaderKey = createEditableComboBox();

    private JTextField m_responseColumnName = new JTextField(20);

    private JComboBox<DataType> m_responseValueType =
        new JComboBox<DataType>(new DataType[]{StringCell.TYPE, IntCell.TYPE});

    private List<JCheckBox> m_authenticationTabTitles = new ArrayList<>();

    private JComboBox<String> m_requestHeaderTemplate = new JComboBox<>();

    //template name -> keys -> possible template values
    private List<Entry<String, List<Entry<String, ? extends List<String>>>>> m_requestTemplates = new ArrayList<>();

    /**
     *
     */
    public RestGetNodeDialog() {
        m_requestTemplates.add(new SimpleImmutableEntry<>("", new ArrayList<>()));
        IConfigurationElement[] elements =
            Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_ID_FOR_REQUEST_HEADER_TEMPLATES);
        for (Entry<String, List<IConfigurationElement>> element : Stream.of(elements)
            .collect(Collectors.groupingBy(element -> element.getDeclaringExtension().getLabel())).entrySet()) {
            List<IConfigurationElement> entries = element.getValue();
            List<Entry<String, ? extends List<String>>> entryList = new ArrayList<>();
            for (IConfigurationElement entry : entries) {
                IConfigurationElement[] values = entry.getChildren();
                entryList.add(new SimpleImmutableEntry<>(entry.getAttribute("key"),
                    Stream.of(values).filter(v -> v != null && v.getValue() != null).map(v -> v.getValue().trim())
                        .collect(Collectors.toList())));
            }
            m_requestTemplates.add(new SimpleImmutableEntry<>(element.getKey(), entryList));
        }
        addTab("Connection Settings", createConnectionSettingsTab());
        addTab("Authentication", createAuthenticationTab());
        addTab("Request Headers", createRequestHeadersTab());
        addTab("Response Headers", createResponseHeadersTab());
    }

    /**
     * @return
     */
    private static JComboBox<String> createEditableComboBox() {
        final JComboBox<String> ret = new JComboBox<>();
        ret.setEditable(true);
        return ret;
    }

    /**
     * @return
     */
    private JPanel createConnectionSettingsTab() {
        final JPanel ret = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.weighty = 0;
        ret.add(m_constantUriOption, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        ret.add(m_constantUri, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        ret.add(m_uriColumnOption, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        ret.add(m_uriColumn, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        ret.add(m_useDelay, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        ret.add(m_delay, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        ret.add(new JLabel("Concurrency: "), gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        ret.add(m_concurrency, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        final JPanel sslPanel = new JPanel();
        sslPanel.setBorder(new TitledBorder("SSL"));
        sslPanel.setLayout(new BoxLayout(sslPanel, BoxLayout.PAGE_AXIS));
        sslPanel.add(m_sslIgnoreHostnameMismatches);
        sslPanel.add(m_sslTrustAll);
        ret.add(sslPanel, gbc);
        gbc.weighty = 1;
        ret.add(new JPanel(), gbc);

        m_useDelay.addActionListener(e -> m_delay.setEnabled(m_useDelay.isSelected()));
        m_constantUriOption.addActionListener(e -> {
            m_constantUri.setEnabled(m_constantUriOption.isSelected());
            m_uriColumn.setEnabled(!m_constantUriOption.isSelected());
        });
        m_uriColumnOption.addActionListener(e -> {
            m_uriColumn.setEnabled(m_uriColumnOption.isSelected());
            m_constantUri.setEnabled(!m_uriColumnOption.isSelected());
        });
        m_uriColumnOption.setSelected(true);
        m_constantUriOption.setSelected(true);
        m_delay.setEnabled(false);
        return ret;
    }

    /**
     * @param selectedRow
     */
    @SuppressWarnings("serial")
    protected void editRequestHeader(final int selectedRow) {
        final Window windowAncestor = SwingUtilities.getWindowAncestor(getPanel());
        final Frame frame = windowAncestor instanceof Frame ? (Frame)windowAncestor : null;
        final JDialog dialog = new JDialog(frame, "Edit", true);
        dialog.setPreferredSize(new Dimension(550, 200));
        Container cp = dialog.getContentPane();
        JPanel outer = new JPanel(new BorderLayout());
        JPanel panel = new JPanel(new GridBagLayout());
        addRequestSettingControls(panel);
        m_requestHeaderKey.setSelectedItem(m_requestHeadersModel.getValueAt(selectedRow, 0));
        m_requestHeaderValue.setSelectedItem(m_requestHeadersModel.getValueAt(selectedRow, 1));
        m_requestHeaderValueType.setSelectedItem(m_requestHeadersModel.getValueAt(selectedRow, 2));
        outer.add(panel, BorderLayout.CENTER);
        JPanel controls = new JPanel();
        outer.add(controls, BorderLayout.SOUTH);
        cp.add(outer);
        controls.setLayout(new BoxLayout(controls, BoxLayout.LINE_AXIS));
        controls.add(Box.createHorizontalGlue());
        controls.add(new JButton(new AbstractAction("OK") {
            @Override
            public void actionPerformed(final ActionEvent e) {
                m_requestHeadersModel.setValueAt(m_requestHeaderKey.getSelectedItem(), selectedRow, 0);
                m_requestHeadersModel.setValueAt(m_requestHeaderValue.getSelectedItem(), selectedRow, 1);
                m_requestHeadersModel.setValueAt(m_requestHeaderValueType.getSelectedItem(), selectedRow, 2);
                //m_requestHeadersModel.setValueAt(m_requestHeaderKeyType.getSelectedItem(), selectedRow, 3);
                dialog.dispose();
            }
        }));
        final AbstractAction cancel = new AbstractAction("Cancel") {
            @Override
            public void actionPerformed(final ActionEvent e) {
                dialog.dispose();
            }
        };
        controls.add(new JButton(cancel));
        dialog.getRootPane().registerKeyboardAction(cancel, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.pack();
        m_requestHeaderKey.requestFocusInWindow();
        dialog.setVisible(true);
    }

    /**
     * @param selectedRow
     */
    @SuppressWarnings("serial")
    protected void editResponseHeader(final int selectedRow) {
        final Window windowAncestor = SwingUtilities.getWindowAncestor(getPanel());
        final Frame frame = windowAncestor instanceof Frame ? (Frame)windowAncestor : null;
        final JDialog dialog = new JDialog(frame, "Edit", true);
        dialog.setPreferredSize(new Dimension(550, 200));
        Container cp = dialog.getContentPane();
        JPanel outer = new JPanel(new BorderLayout());
        JPanel panel = new JPanel(new GridBagLayout());
        addResponseSettingControls(panel);
        m_responseHeaderKey.setSelectedItem(m_responseHeadersModel.getValueAt(selectedRow, 0));
        m_responseColumnName
            .setText((String)((Pair<?, ?>)m_responseHeadersModel.getValueAt(selectedRow, 1)).getFirst());
        m_responseValueType
            .setSelectedItem(((Pair<?, ?>)m_responseHeadersModel.getValueAt(selectedRow, 1)).getSecond());
        outer.add(panel, BorderLayout.CENTER);
        JPanel controls = new JPanel();
        outer.add(controls, BorderLayout.SOUTH);
        cp.add(outer);
        controls.setLayout(new BoxLayout(controls, BoxLayout.LINE_AXIS));
        controls.add(Box.createHorizontalGlue());
        controls.add(new JButton(new AbstractAction("OK") {
            @Override
            public void actionPerformed(final ActionEvent e) {
                m_responseHeadersModel.setValueAt(m_responseHeaderKey.getSelectedItem(), selectedRow, 0);
                m_responseHeadersModel.setValueAt(m_responseColumnName.getText(), selectedRow, 1);
                m_responseHeadersModel.setValueAt(m_responseValueType.getSelectedItem(), selectedRow, 1);
                dialog.dispose();
            }
        }));
        final AbstractAction cancel = new AbstractAction("Cancel") {
            @Override
            public void actionPerformed(final ActionEvent e) {
                dialog.dispose();
            }
        };
        controls.add(new JButton(cancel));
        dialog.getRootPane().registerKeyboardAction(cancel, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.pack();
        m_responseHeaderKey.requestFocusInWindow();
        dialog.setVisible(true);
    }

    /**
     * @param panel
     */
    protected void addRequestSettingControls(final JPanel panel) {
        //panel.setPreferredSize(new Dimension(800, 300));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(new JLabel("Header key"), gbc);
        gbc.gridx = 1;
        //m_requestHeaderKey = GUIFactory.createTextField("", 22);
        gbc.weightx = 1;
        panel.add(m_requestHeaderKey, gbc);
        gbc.gridy++;

        //m_requestHeaderValue = GUIFactory.createTextField("", 22);
        ((JTextComponent)m_requestHeaderValue.getEditor().getEditorComponent()).getDocument()
            .addDocumentListener((DocumentEditListener)(e) -> {
                /*TODO completion*/});
        ((JTextComponent)m_requestHeaderKey.getEditor().getEditorComponent()).getDocument()
            .addDocumentListener((DocumentEditListener)(e) -> {
                /*TODO completion*/});

        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(new JLabel("Header value: "), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(m_requestHeaderValue, gbc);
        gbc.gridy++;

        gbc.gridx = 0;
        panel.add(m_requestHeaderValueType, gbc);
        gbc.gridx = 1;
        panel.add(m_requestHeaderKeyType, gbc);
        gbc.gridy++;

    }

    /**
     * @param panel
     */
    protected void addResponseSettingControls(final JPanel panel) {
        //panel.setPreferredSize(new Dimension(800, 300));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(new JLabel("Header key"), gbc);
        gbc.gridx = 1;
        //m_responseHeaderKey = GUIFactory.createTextField("", 22);
        gbc.weightx = 1;
        panel.add(m_responseHeaderKey, gbc);
        gbc.gridy++;

        //m_responseColumnName = GUIFactory.createTextField("", 22);
        m_responseColumnName.getDocument().addDocumentListener((DocumentEditListener)(e) -> {
            /*TODO completion*/});
        ((JTextComponent)m_responseHeaderKey.getEditor().getEditorComponent()).getDocument()
            .addDocumentListener((DocumentEditListener)(e) -> {
                /*TODO completion*/});

        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(new JLabel("Output column name: "), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(m_responseColumnName, gbc);
        gbc.gridy++;

        gbc.gridx = 0;
        panel.add(new JLabel("Column type"), gbc);
        gbc.gridx = 1;
        panel.add(m_responseValueType, gbc);
        gbc.gridy++;

    }

    /**
     * @return
     */
    private JPanel createAuthenticationTab() {
        final JPanel ret = new JPanel();
        final JTabbedPane tabs = new JTabbedPane(SwingConstants.TOP, JTabbedPane.WRAP_TAB_LAYOUT);
        for (final EnablableUserConfiguration<UserConfiguration> euc : m_settings.getAuthorizationConfigurations()) {
            final JPanel tabPanel = new JPanel(), tabTitlePanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
            final JScrollPane scrollPane = new JScrollPane(tabPanel);
            tabs.addTab("", scrollPane);
            final JCheckBox checkBox = new JCheckBox();
            final UserConfiguration userConfiguration = euc.getUserConfiguration();
            checkBox.setAction(new AbstractAction() {
                private static final long serialVersionUID = -8514095622936885670L;

                @Override
                public void actionPerformed(final ActionEvent e) {
                    if (checkBox.isSelected()) {
                        userConfiguration.enableControls();
                    } else {
                        userConfiguration.disableControls();
                    }
                }
            });
            checkBox.setName(userConfiguration.id());
            m_authenticationTabTitles.add(checkBox);
            tabTitlePanel.add(checkBox);
            tabTitlePanel.add(new JLabel(userConfiguration.id()));
            tabs.setTabComponentAt(tabs.getTabCount() - 1, tabTitlePanel);
            userConfiguration.addControls(tabPanel);
        }
        ret.add(tabs);
        return ret;
    }

    /**
     * @return
     */
    private JPanel createRequestHeadersTab() {
        m_requestHeaders.setAutoCreateColumnsFromModel(false);
        while (m_requestHeaders.getColumnModel().getColumns().hasMoreElements()) {
            m_requestHeaders.getColumnModel()
                .removeColumn(m_requestHeaders.getColumnModel().getColumns().nextElement());
        }
        final TableColumn keyCol =
            new TableColumn(0, 67, new DefaultTableCellRenderer(), new DefaultCellEditor(m_requestHeaderKey));
        keyCol.setHeaderValue("Key");
        m_requestHeaders.getColumnModel().addColumn(keyCol);
        m_requestHeaders.getColumnModel()
            .addColumn(new TableColumn(1, 67, null, new DefaultCellEditor(m_requestHeaderValue)));
        m_requestHeaders.getColumnModel()
            .addColumn(new TableColumn(2, 40, null, new DefaultCellEditor(m_requestHeaderValueType)));
        //        m_requestHeaders.getColumnModel().addColumn(new TableColumn(3, 40, null, null));
        m_requestAddRow.addActionListener(e -> m_requestHeadersModel.newRow());
        m_requestDeleteRow.addActionListener(e -> m_requestHeadersModel.removeRow(m_requestHeaders.getSelectedRow()));
        m_requestEditRow.addActionListener(e -> editRequestHeader(m_requestHeaders.getSelectedRow()));

        m_requestHeaders.getSelectionModel().addListSelectionListener(e -> {
            final boolean hasValidSelection = !m_requestHeaders.getSelectionModel().isSelectionEmpty();
            m_requestEditRow.setEnabled(hasValidSelection);
            m_requestDeleteRow.setEnabled(hasValidSelection);
            m_requestHeaderValue.removeAllItems();
            if (hasValidSelection) {
                String key = (String)m_requestHeadersModel.getValueAt(m_requestHeaders.getSelectedRow(), 0);
                Object template = m_requestHeaderTemplate.getSelectedItem();
                m_requestTemplates.stream().filter(entry -> Objects.equals(template, entry.getKey())).findFirst()
                    .ifPresent(
                        entry -> entry.getValue().stream().filter(listEntry -> Objects.equals(key, listEntry.getKey()))
                            .findFirst().map(listEntry -> listEntry.getValue())
                            .ifPresent(values -> values.forEach(i -> m_requestHeaderValue.addItem(i))));
            }
        });
        m_requestHeaders.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editRequestHeader(m_requestHeaders.getSelectedRow());
                }
            }
        });
        for (Entry<String, ?> entry : m_requestTemplates) {
            m_requestHeaderTemplate.addItem(entry.getKey());
        }
        m_requestHeaderTemplate.addActionListener(e -> {
            String selected = (String)m_requestHeaderTemplate.getSelectedItem();
            if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(null,
                "Replace the current request header options with the template values from " + selected,
                "Change request header?", JOptionPane.YES_NO_OPTION)) {
                final List<Entry<String, ? extends List<String>>> options =
                    m_requestTemplates.stream().filter(entry -> Objects.equals(selected, entry.getKey()))
                        .map(entry -> entry.getValue()).findFirst().orElse(new ArrayList<>());
                m_requestHeadersModel.clear();
                for (final Entry<String, ? extends List<String>> keyValues : options) {
                    m_requestHeadersModel.addRow(new RequestHeaderKeyItem(keyValues.getKey(),
                        keyValues.getValue().stream().findFirst().orElse(""), ReferenceType.Constant));
                }
            }
        });

        final JPanel ret = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.weighty = 1;
        ret.add(m_requestHeaderTemplate, gbc);
        gbc.gridy++;
        m_requestHeaders.setVisible(true);
        ret.add(new JScrollPane(m_requestHeaders), gbc);
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.weighty = 0;
        ret.add(m_requestAddRow, gbc);
        gbc.gridx++;
        ret.add(m_requestDeleteRow, gbc);
        gbc.gridx++;
        ret.add(m_requestEditRow, gbc);
        m_requestHeaders.getColumnModel().getColumn(0).setHeaderValue("Key");
        m_requestHeaders.getColumnModel().getColumn(1).setHeaderValue("Value");
        m_requestHeaders.getColumnModel().getColumn(2).setHeaderValue("Value kind");
        //        m_requestHeaders.getColumnModel().getColumn(3).setHeaderValue("Key kind");
        return ret;
    }

    /**
     * @return
     */
    private JPanel createResponseHeadersTab() {
        m_responseHeaders.setAutoCreateColumnsFromModel(false);
        while (m_responseHeaders.getColumnModel().getColumns().hasMoreElements()) {
            m_responseHeaders.getColumnModel()
                .removeColumn(m_responseHeaders.getColumnModel().getColumns().nextElement());
        }
        final TableColumn keyCol =
            new TableColumn(0, 67, new DefaultTableCellRenderer(), new DefaultCellEditor(m_responseHeaderKey));
        keyCol.setHeaderValue("Key");
        m_responseHeaders.getColumnModel().addColumn(keyCol);
        m_responseHeaders.getColumnModel().addColumn(new TableColumn(1, 67, new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 8685506970523457593L;

            /**
             * {@inheritDoc}
             */
            @Override
            public Component getTableCellRendererComponent(final JTable table, final Object value,
                final boolean isSelected, final boolean hasFocus, final int row, final int column) {
                final Component orig =
                    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value instanceof Pair<?, ?>) {
                    Pair<?, ?> rawPair = (Pair<?, ?>)value;
                    Object firstObject = rawPair.getFirst(), secondObject = rawPair.getSecond();
                    if (firstObject instanceof String) {
                        String colName = (String)firstObject;
                        setText(colName);
                        if (secondObject instanceof DataType) {
                            DataType type = (DataType)secondObject;
                            setIcon(type.getIcon());
                        }
                    }
                }
                return orig;
            }
        }, new DefaultCellEditor(m_responseColumnName) {
            private static final long serialVersionUID = 6989656745155391971L;

            /**
             * {@inheritDoc}
             */
            @Override
            public Object getCellEditorValue() {
                final Object orig = super.getCellEditorValue();
                if (orig instanceof Pair<?, ?>) {
                    final Pair<?, ?> pairRaw = (Pair<?, ?>)orig;
                    if (pairRaw.getFirst() instanceof String) {
                        final String first = (String)pairRaw.getFirst();
                        return first;
                    }
                }
                return orig;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Component getTableCellEditorComponent(final JTable table, final Object value,
                final boolean isSelected, final int row, final int column) {
                if (value instanceof Pair<?, ?>) {
                    final Pair<?, ?> pairRaw = (Pair<?, ?>)value;
                    return super.getTableCellEditorComponent(table, pairRaw.getFirst(), isSelected, row, column);
                }
                return super.getTableCellEditorComponent(table, value, isSelected, row, column);
            }
        }));
        m_responseAddRow.addActionListener(e -> m_responseHeadersModel.newRow());
        m_responseDeleteRow
            .addActionListener(e -> m_responseHeadersModel.removeRow(m_responseHeaders.getSelectedRow()));
        m_responseEditRow.addActionListener(e -> editResponseHeader(m_responseHeaders.getSelectedRow()));

        final JPanel ret = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        ret.add(m_extractAllHeaders, gbc);
        m_extractAllHeaders.addActionListener(e -> {
            m_responseHeaders.setEnabled(!m_extractAllHeaders.isSelected());
            if (m_extractAllHeaders.isSelected()) {
                m_responseHeaders.getSelectionModel().clearSelection();
            }
        });
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        gbc.gridy++;
        ret.add(new JScrollPane(m_responseHeaders), gbc);
        gbc.gridy++;
        gbc.weighty = 0;
        gbc.gridwidth = 1;
        ret.add(m_responseAddRow, gbc);
        gbc.gridx++;
        ret.add(m_responseEditRow, gbc);
        gbc.gridx++;
        ret.add(m_responseDeleteRow, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 3;
        final JPanel body = new JPanel(new FlowLayout(FlowLayout.LEADING));
        body.add(new JLabel("Body column: "));
        body.add(m_bodyColumnName);
        ret.add(body, gbc);

        m_responseHeaders.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editResponseHeader(m_responseHeaders.getSelectedRow());
                }
            }
        });

        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        //TODO update settings based on the UI.
        for (JCheckBox checkBox : m_authenticationTabTitles) {
            for (final EnablableUserConfiguration<UserConfiguration> euc : m_settings
                .getAuthorizationConfigurations()) {
                if (checkBox.getName().equals(euc.getUserConfiguration().id())) {
                    euc.setEnabled(checkBox.isSelected());
                }
            }
        }
        m_settings.setUseConstantURI(m_constantUriOption.isSelected());
        m_settings.setConstantURI(m_constantUri.getSelectedString());
        m_settings.setUriColumn(m_uriColumn.getSelectedColumn());
        m_settings.setUseDelay(m_useDelay.isSelected());
        m_settings.setDelay(((Number)m_delay.getValue()).longValue());
        m_settings.setConcurrency(((Number)m_concurrency.getValue()).intValue());
        m_settings.setSslIgnoreHostNameErrors(m_sslIgnoreHostnameMismatches.isSelected());
        m_settings.setSslTrustAll(m_sslTrustAll.isSelected());
        m_settings.getRequestHeaders().clear();
        m_settings.getRequestHeaders()
            .addAll(StreamSupport.stream(m_requestHeadersModel.spliterator(), false).collect(Collectors.toList()));
        m_settings.setExtractAllResponseFields(m_extractAllHeaders.isSelected());
        m_settings.getExtractFields().clear();
        m_settings.getExtractFields()
            .addAll(StreamSupport.stream(m_responseHeadersModel.spliterator(), false).collect(Collectors.toList()));
        m_settings.setResponseBodyColumn(m_bodyColumnName.getSelectedString());
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        try {
            m_settings.loadSettingsForDialog(settings, getCredentialsNames(), specs);
        } catch (InvalidSettingsException e) {
            throw new NotConfigurableException(e.getMessage(), e);
        }
        // TODO Update UI based on settings
        m_constantUriOption.setSelected(m_settings.isUseConstantURI());
        m_constantUri.setSelectedString(m_settings.getConstantURI());
        //m_uriColumn.setSelectedColumn(m_settings.getUriColumn());
        if (specs[0] != null) {
            m_uriColumnOption.setEnabled(true);
            m_uriColumn.update(specs[0], m_settings.getUriColumn(), false, true);
        } else {
            m_uriColumnOption.setEnabled(false);
            m_uriColumn.setEnabled(false);
        }
        m_useDelay.setSelected(m_settings.isUseDelay());
        m_delay.setValue(m_settings.getDelay());
        m_concurrency.setValue(m_settings.getConcurrency());
        m_sslIgnoreHostnameMismatches.setSelected(m_settings.isSslIgnoreHostNameErrors());
        m_sslTrustAll.setSelected(m_settings.isSslTrustAll());
        m_requestHeadersModel.clear();
        for (int i = 0; i < m_settings.getRequestHeaders().size(); ++i) {
            m_requestHeadersModel.addRow(m_settings.getRequestHeaders().get(i));
        }
        m_extractAllHeaders.setSelected(m_settings.isExtractAllResponseFields());
        m_responseHeadersModel.clear();
        for (int i = 0; i < m_settings.getExtractFields().size(); ++i) {
            m_responseHeadersModel.addRow(m_settings.getExtractFields().get(i));
        }
        m_bodyColumnName.setSelectedString(m_settings.getResponseBodyColumn());
        m_settings.setResponseBodyColumn(m_bodyColumnName.getSelectedString());
        for (final EnablableUserConfiguration<UserConfiguration> euc : m_settings.getAuthorizationConfigurations()) {
            for (final JCheckBox checkBox : m_authenticationTabTitles) {
                if (checkBox.getName().equals(euc.getUserConfiguration().id())) {
                    checkBox.setSelected(euc.isEnabled());
                    checkBox.getAction().actionPerformed(null);
                }
            }
        }
    }

    @FunctionalInterface
    private interface DocumentEditListener extends DocumentListener {
        @Override
        default void changedUpdate(final DocumentEvent e) {
            handleEdit(e);
        }

        @Override
        default void insertUpdate(final DocumentEvent e) {
            handleEdit(e);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        default void removeUpdate(final DocumentEvent e) {
            handleEdit(e);
        }

        /**
         * @param e
         */
        void handleEdit(DocumentEvent e);
    }
}
