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
 *   2016. ápr. 23. (Gabor Bakos): created
 */
package org.knime.rest.nodes.common;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
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
import org.knime.rest.generic.EnablableUserConfiguration;
import org.knime.rest.generic.UserConfiguration;
import org.knime.rest.nodes.common.RequestTableModel.Columns;
import org.knime.rest.nodes.common.RestSettings.ReferenceType;
import org.knime.rest.nodes.common.RestSettings.RequestHeaderKeyItem;
import org.knime.rest.util.ButtonCell;
import org.knime.rest.util.FixedCellEditorForComboBoxes;

/**
 *
 * @author Gabor Bakos
 * @param <S> Type of the {@link RestSettings}.
 */
public abstract class RestNodeDialog<S extends RestSettings> extends NodeDialogPane {
    /** Extension id for the request header templates. */
    protected static final String EXTENSION_ID_FOR_REQUEST_HEADER_TEMPLATES = "org.knime.rest.header.template";

    private final List<String> m_credentials = new ArrayList<>();

    private final List<String> m_flowVariables = new ArrayList<>();

    private final List<String> m_columns = new ArrayList<>();

    private final S m_settings = createSettings();

    private final JRadioButton m_constantUriOption = new JRadioButton("URI: ");

    private final JRadioButton m_uriColumnOption = new JRadioButton("URI column: ");
    {
        final ButtonGroup group = new ButtonGroup();
        group.add(m_constantUriOption);
        group.add(m_uriColumnOption);
    }

    private final StringHistoryPanel m_constantUri = new StringHistoryPanel("GET uri");

    @SuppressWarnings("unchecked")
    private final ColumnSelectionPanel m_uriColumn = new ColumnSelectionPanel(StringValue.class, URIDataValue.class);

    private final JCheckBox m_useDelay = new JCheckBox("Delay (ms): ");

    private final JSpinner m_delay = new JSpinner(new SpinnerNumberModel(Long.valueOf(0L), Long.valueOf(0L),
        Long.valueOf(30L * 60 * 1000L/*30 minutes*/), Long.valueOf(100L)));

    private final JSpinner m_concurrency =
        new JSpinner(new SpinnerNumberModel(1, 1, 16/*TODO find proper default*/, 1));

    private final JCheckBox m_sslIgnoreHostnameMismatches = new JCheckBox("Ignore hostname mismatches");

    private final JCheckBox m_sslTrustAll = new JCheckBox("Trust all certificates");

    private final JCheckBox m_failOnConnectionProblems =
        new JCheckBox("Fail on connection problems (e.g. timeout, certificate errors, ...)");

    private final JCheckBox m_failOnHttpErrors = new JCheckBox("Fail on http errors (e.g. page not found)");

    private final JCheckBox m_followRedirects = new JCheckBox("Follow redirects");

    private final JSpinner m_timeoutInSeconds =
        new JSpinner(new SpinnerNumberModel(RestSettings.DEFAULT_TIMEOUT, 1, Integer.MAX_VALUE, 1));

    private final RequestTableModel m_requestHeadersModel = new RequestTableModel();

    private final JButton m_requestAddRow = new JButton("Add header parameter");

    private final JButton m_requestEditRow = new JButton("Edit header parameter");

    private final JButton m_requestDeleteRow = new JButton("Remove header parameter");

    private final ResponseTableModel m_responseHeadersModel = new ResponseTableModel();

    private final JTable m_requestHeaders = new JTable(m_requestHeadersModel);

    private final JTable m_responseHeaders = new JTable(m_responseHeadersModel);

    private final JButton m_responseAddRow = new JButton("Add header parameter");

    private final JButton m_responseEditRow = new JButton("Edit header parameter");

    private final JButton m_responseDeleteRow = new JButton("Remove header parameter");

    private final JCheckBox m_extractAllHeaders = new JCheckBox("Extract all headers");

    private final StringHistoryPanel m_bodyColumnName = new StringHistoryPanel("GET body");

    private final JComboBox<String> m_requestHeaderKey = createEditableComboBox();

    private final JComboBox<String> m_requestHeaderValue = createEditableComboBox();

    private final JComboBox<ReferenceType> m_requestHeaderValueType = createEditableComboBox(ReferenceType.values());

    private final JComboBox<String> m_responseHeaderKey = createEditableComboBox();

    private final JTextField m_responseColumnName = new JTextField(20);

    private final JComboBox<DataType> m_responseValueType =
        new JComboBox<>(new DataType[]{StringCell.TYPE, IntCell.TYPE});

    private final List<JRadioButton> m_authenticationTabTitles = new ArrayList<>();

    private final JComboBox<String> m_requestHeaderTemplate = new JComboBox<>();

    private final JButton m_requestHeaderTemplateReset = new JButton("Reset");

    private final JButton m_requestHeaderTemplateMerge = new JButton("Merge");

    private final List<Entry<String, List<Entry<String, ? extends List<String>>>>> m_requestTemplates =
        new ArrayList<>();

    private List<Entry<String, ? extends List<String>>> m_requestHeaderOptions;

    private DefaultTableCellRenderer m_responseHeaderKeyCellRenderer;

    private DefaultTableCellRenderer m_responseHeaderValueCellRenderer;

    private DefaultCellEditor m_responseValueCellEditor;

    /**
     * Constructs the dialog.
     */
    protected RestNodeDialog() {
        super();
        m_requestTemplates.add(new SimpleImmutableEntry<>("", new ArrayList<>()));
        final IConfigurationElement[] elements =
            Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_ID_FOR_REQUEST_HEADER_TEMPLATES);
        for (final Entry<String, List<IConfigurationElement>> element : Stream.of(elements)
            .collect(Collectors.groupingBy(element -> element.getDeclaringExtension().getLabel())).entrySet()) {
            final List<IConfigurationElement> entries = element.getValue();
            final List<Entry<String, ? extends List<String>>> entryList = new ArrayList<>();
            for (final IConfigurationElement entry : entries) {
                final IConfigurationElement[] values = entry.getChildren();
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
     * Creates an editable {@link JComboBox}. (With slightly smaller text.)
     *
     * @param initialValues The initial values.
     * @return The created {@link JComboBox}.
     */
    @SafeVarargs
    @SuppressWarnings("serial")
    private static <T> JComboBox<T> createEditableComboBox(final T... initialValues) {
        final JComboBox<T> ret = new JComboBox<>(initialValues);
        ret.setRenderer(new DefaultListCellRenderer() {
            /**
             * {@inheritDoc}
             */
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
                final boolean isSelected, final boolean cellHasFocus) {
                setToolTipText(value.toString());
                final Component res = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (res instanceof JLabel) {
                    final JLabel label = (JLabel)res;
                    label.setFont(getFont().deriveFont(9f));
                }
                return res;
            }
        });
        ret.setEditable(true);
        return ret;
    }

    /**
     * @return The new {@link RestSettings} object.
     */
    protected abstract S createSettings();

    /**
     * @return the settings
     */
    protected final S getSettings() {
        return m_settings;
    }

    /** A functional interface to handel all changes with a single method. */
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

    /**
     * @return Connection tab.
     */
    protected JPanel createConnectionSettingsTab() {
        final JPanel ret = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
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
        gbc.gridy++;
        ret.add(m_failOnConnectionProblems, gbc);
        gbc.gridy++;
        ret.add(m_failOnHttpErrors, gbc);
        gbc.gridy++;
        ret.add(m_followRedirects, gbc);
        gbc.gridy++;
        gbc.weightx = 0;
        gbc.gridx = 0;
        ret.add(new JLabel("Timeout (s)"), gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        ret.add(m_timeoutInSeconds, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        ret.add(new JLabel("Body column: "), gbc);
        gbc.gridx++;
        ret.add(m_bodyColumnName, gbc);
        gbc.gridy++;
        gbc.gridx = 0;
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
        m_constantUriOption.setSelected(true);
        m_uriColumn.setEnabled(false);
        m_uriColumn.setRequired(false);
        m_delay.setEnabled(false);
        m_failOnConnectionProblems.setSelected(RestSettings.DEFAULT_FAIL_ON_CONNECTION_PROBLEMS);
        m_failOnHttpErrors.setSelected(RestSettings.DEFAULT_FAIL_ON_HTTP_ERRORS);
        return ret;
    }

    /**
     * Edits the selected row ({@code selectedRow}) in the request header table.
     *
     * @param selectedRow The {@code 0}-based index of the selected row. (Do nothing in case it is negative.)
     */
    @SuppressWarnings("serial")
    protected void editRequestHeader(final int selectedRow) {
        if (selectedRow < 0) {
            return;
        }
        final Window windowAncestor = SwingUtilities.getWindowAncestor(getPanel());
        final Frame frame = windowAncestor instanceof Frame ? (Frame)windowAncestor : null;
        final JDialog dialog = new JDialog(frame, "Edit", true);
        dialog.setPreferredSize(new Dimension(550, 200));
        final Container cp = dialog.getContentPane();
        final JPanel outer = new JPanel(new BorderLayout());
        final JPanel panel = new JPanel(new GridBagLayout());
        addRequestSettingControls(panel);
        m_requestHeaderKey.setSelectedItem(
            m_requestHeadersModel.getValueAt(selectedRow, RequestTableModel.Columns.headerKey.ordinal()));
        m_requestHeaderValue
            .setSelectedItem(m_requestHeadersModel.getValueAt(selectedRow, RequestTableModel.Columns.value.ordinal()));
        m_requestHeaderValueType
            .setSelectedItem(m_requestHeadersModel.getValueAt(selectedRow, RequestTableModel.Columns.kind.ordinal()));
        outer.add(panel, BorderLayout.CENTER);
        final JPanel controls = new JPanel();
        outer.add(controls, BorderLayout.SOUTH);
        cp.add(outer);
        controls.setLayout(new BoxLayout(controls, BoxLayout.LINE_AXIS));
        controls.add(Box.createHorizontalGlue());
        controls.add(new JButton(new AbstractAction("OK") {
            @Override
            public void actionPerformed(final ActionEvent e) {
                m_requestHeadersModel.setValueAt(m_requestHeaderKey.getSelectedItem(), selectedRow,
                    RequestTableModel.Columns.headerKey.ordinal());
                m_requestHeadersModel.setValueAt(m_requestHeaderValue.getSelectedItem(), selectedRow,
                    RequestTableModel.Columns.value.ordinal());
                m_requestHeadersModel.setValueAt(m_requestHeaderValueType.getSelectedItem(), selectedRow,
                    RequestTableModel.Columns.kind.ordinal());
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
     * Edits the selected row ({@code selectedRow}) in the response header table.
     *
     * @param selectedRow The {@code 0}-based index of the selected row. (Do nothing in case it is negative.)
     */
    @SuppressWarnings("serial")
    protected void editResponseHeader(final int selectedRow) {
        if (selectedRow < 0) {
            return;
        }
        final Window windowAncestor = SwingUtilities.getWindowAncestor(getPanel());
        final Frame frame = windowAncestor instanceof Frame ? (Frame)windowAncestor : null;
        final JDialog dialog = new JDialog(frame, "Edit", true);
        dialog.setPreferredSize(new Dimension(550, 200));
        final Container cp = dialog.getContentPane();
        final JPanel outer = new JPanel(new BorderLayout());
        final JPanel panel = new JPanel(new GridBagLayout());
        addResponseSettingControls(panel);
        m_responseHeaderKey.setSelectedItem(
            m_responseHeadersModel.getValueAt(selectedRow, ResponseTableModel.Columns.headerKey.ordinal()));
        m_responseColumnName.setText((String)((Pair<?, ?>)m_responseHeadersModel.getValueAt(selectedRow,
            ResponseTableModel.Columns.outputColumn.ordinal())).getFirst());
        updateResponseValueTypes();
        m_responseValueType.setSelectedItem(((Pair<?, ?>)m_responseHeadersModel.getValueAt(selectedRow,
            ResponseTableModel.Columns.outputColumn.ordinal())).getSecond());
        outer.add(panel, BorderLayout.CENTER);
        final JPanel controls = new JPanel();
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
     * @param panel The panel to be used to add the request settings.
     */
    protected void addRequestSettingControls(final JPanel panel) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(new JLabel("Header key"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(m_requestHeaderKey, gbc);
        gbc.gridy++;

        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(new JLabel("Header value: "), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(m_requestHeaderValue, gbc);
        gbc.gridy++;

        gbc.gridx = 0;
        panel.add(m_requestHeaderValueType, gbc);
        gbc.gridy++;

    }

    /**
     * @param panel The panel to be used for response settings.
     */
    protected void addResponseSettingControls(final JPanel panel) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(new JLabel("Header key"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(m_responseHeaderKey, gbc);
        gbc.gridy++;
        //
        //        m_responseColumnName.getDocument().addDocumentListener((DocumentEditListener)(e) -> {
        //            /*TODO completion*/});
        //        ((JTextComponent)m_responseHeaderKey.getEditor().getEditorComponent()).getDocument()
        //            .addDocumentListener((DocumentEditListener)e -> {
        //                /*TODO completion*/});

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
     * @return The authentication tab.
     */
    protected JPanel createAuthenticationTab() {
        final JPanel ret = new JPanel();
        ret.setLayout(new BoxLayout(ret, BoxLayout.PAGE_AXIS));
        final JPanel radioButtons = new JPanel(new FlowLayout());
        ret.add(radioButtons);
        final JPanel tabs = new JPanel(new CardLayout());
        final ButtonGroup buttonGroup = new ButtonGroup();
        for (final EnablableUserConfiguration<UserConfiguration> euc : m_settings.getAuthorizationConfigurations()) {
            final JPanel tabPanel = new JPanel();
            final JScrollPane scrollPane = new JScrollPane(tabPanel);
            final UserConfiguration userConfiguration = euc.getUserConfiguration();
            tabs.add(euc.getName(), scrollPane);
            final JRadioButton radioButton = new JRadioButton(euc.getName());
            buttonGroup.add(radioButton);
            radioButtons.add(radioButton);
            radioButton.setAction(new AbstractAction(euc.getName()) {
                private static final long serialVersionUID = -8514095622936885670L;

                @Override
                public void actionPerformed(final ActionEvent e) {
                    final CardLayout layout = (CardLayout)tabs.getLayout();
                    if (radioButton.isSelected()) {
                        layout.show(tabs, euc.getName());
                    }
                }
            });
            radioButton.setName(euc.getName());
            m_authenticationTabTitles.add(radioButton);
            userConfiguration.addControls(tabPanel);
        }
        ret.add(tabs);
        return ret;
    }

    /**
     * @return The request headers tab.
     */
    protected JPanel createRequestHeadersTab() {
        m_requestHeaderValueType.setEditable(false);
        deleteAndInsertRowRequestHeaderActions();
        m_requestHeaders.setAutoCreateColumnsFromModel(false);
        while (m_requestHeaders.getColumnModel().getColumns().hasMoreElements()) {
            m_requestHeaders.getColumnModel()
                .removeColumn(m_requestHeaders.getColumnModel().getColumns().nextElement());
        }
        final TableColumn keyCol = new TableColumn(RequestTableModel.Columns.headerKey.ordinal(), 67,
            new DefaultTableCellRenderer(), new FixedCellEditorForComboBoxes(m_requestHeaderKey));
        keyCol.setHeaderValue("Key");
        m_requestHeaders.getColumnModel().addColumn(keyCol);
        m_requestHeaders.getColumnModel().addColumn(new TableColumn(RequestTableModel.Columns.value.ordinal(), 67, null,
            new FixedCellEditorForComboBoxes(m_requestHeaderValue)));
        m_requestHeaders.getColumnModel().addColumn(new TableColumn(RequestTableModel.Columns.kind.ordinal(), 40, null,
            new DefaultCellEditor(m_requestHeaderValueType)));
        final ActionListener updateRequestValueAlternatives = al -> {
            if (m_requestHeaders.getSelectedRowCount() == 0) {
                enableRequestHeaderChangeControls(false);
                return;
            }
            enableRequestHeaderChangeControls(true);
            final Object origValue = m_requestHeadersModel.getValueAt(m_requestHeaders.getSelectedRow(), 1);
            m_requestHeaderValue.removeAllItems();
            switch ((ReferenceType)m_requestHeaderValueType.getSelectedItem()) {
                case FlowVariable:
                    for (final String flowVar : m_flowVariables) {
                        m_requestHeaderValue.addItem(flowVar);
                    }
                    break;
                case Column:
                    for (final String column : m_columns) {
                        m_requestHeaderValue.addItem(column);
                    }
                    break;
                case Constant:
                    if (m_requestHeaders.getSelectedRowCount() > 0) {
                        final String key =
                            (String)m_requestHeadersModel.getValueAt(m_requestHeaders.getSelectedRow(), 0);
                        m_requestTemplates.stream()
                            .filter(entry -> Objects.equals(m_requestHeaderTemplate.getSelectedItem(), entry.getKey()))
                            .findFirst()
                            .ifPresent(entry -> entry.getValue().stream()
                                .filter(listEntry -> Objects.equals(key, listEntry.getKey())).findFirst()
                                .map(listEntry -> listEntry.getValue())
                                .ifPresent(values -> values.forEach(m_requestHeaderValue::addItem)));
                    }
                    break;
                case CredentialName://Intentional fall through
                case CredentialPassword:
                    for (final String credential : m_credentials) {
                        m_requestHeaderValue.addItem(credential);
                    }
                    break;
                default:
                    throw new IllegalStateException(
                        "Unknown reference type: " + m_requestHeaderValueType.getSelectedItem());
            }
            m_requestHeadersModel.setValueAt(origValue, m_requestHeaders.getSelectedRow(), 1);
            m_requestHeaderValue.setSelectedItem(origValue);
        };
        m_requestHeaderValueType.addActionListener(updateRequestValueAlternatives);
        final ButtonCell deleteRequestRow = new ButtonCell();
        deleteRequestRow.setAction(new AbstractAction(" X ") {
            private static final long serialVersionUID = 1369259160048695493L;

            @Override
            public void actionPerformed(final ActionEvent e) {
                if (deleteRequestRow.getRow() >= 0) {
                    m_requestHeadersModel.removeRow(deleteRequestRow.getRow());
                }
            }
        });
        final TableColumn deleteRowColumn =
            new TableColumn(RequestTableModel.Columns.delete.ordinal(), 15, deleteRequestRow, deleteRequestRow);
        deleteRowColumn.setMaxWidth(25);
        m_requestHeaders.getColumnModel().addColumn(deleteRowColumn);
        m_requestAddRow.addActionListener(e -> m_requestHeadersModel.newRow());
        m_requestDeleteRow.addActionListener(e -> m_requestHeadersModel.removeRow(m_requestHeaders.getSelectedRow()));
        m_requestEditRow.addActionListener(e -> editRequestHeader(m_requestHeaders.getSelectedRow()));

        m_requestHeaders.getSelectionModel().addListSelectionListener(e -> {
            final boolean hasValidSelection = !m_requestHeaders.getSelectionModel().isSelectionEmpty();
            enableRequestHeaderChangeControls(hasValidSelection);
            m_requestEditRow.setEnabled(hasValidSelection);
            m_requestDeleteRow.setEnabled(hasValidSelection);
            if (hasValidSelection) {
                updateRequestValueAlternatives.actionPerformed(null);
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
        for (final Entry<String, ?> entry : m_requestTemplates) {
            m_requestHeaderTemplate.addItem(entry.getKey());
        }
        m_requestHeaderTemplate.addActionListener(e -> updateRequestHeaderKeys());
        m_requestHeaderTemplateMerge
            .setToolTipText("Merge the template values with the current settings (adds rows not present)");
        m_requestHeaderTemplateMerge.addActionListener(e -> {
            final Set<String> keysPresent = new HashSet<>();
            for (int i = 0; i < m_requestHeadersModel.getRowCount(); ++i) {
                keysPresent.add((String)m_requestHeadersModel.getValueAt(i, 0));
            }
            updateRequestHeaderKeys();
            for (final Entry<String, ? extends List<String>> keyValues : m_requestHeaderOptions) {
                if (!keysPresent.contains(keyValues.getKey())) {
                    m_requestHeadersModel.addRow(new RequestHeaderKeyItem(keyValues.getKey(),
                        keyValues.getValue().stream().findFirst().orElse(""), ReferenceType.Constant));
                }
            }
        });
        m_requestHeaderTemplateReset.setToolTipText("Replaces the current settings with the ones from the template");
        m_requestHeaderTemplateReset.addActionListener(e -> {
            m_requestHeadersModel.clear();
            updateRequestHeaderKeys();
            for (final Entry<String, ? extends List<String>> keyValues : m_requestHeaderOptions) {
                m_requestHeadersModel.addRow(new RequestHeaderKeyItem(keyValues.getKey(),
                    keyValues.getValue().stream().findFirst().orElse(""), ReferenceType.Constant));
            }
        });

        final JPanel ret = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        ret.add(m_requestHeaderTemplate, gbc);
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.gridx += 3;
        ret.add(m_requestHeaderTemplateMerge, gbc);
        gbc.gridx++;
        ret.add(m_requestHeaderTemplateReset, gbc);
        gbc.gridy++;
        gbc.weighty = 1;
        gbc.gridx = 0;
        gbc.gridwidth = 5;
        gbc.fill = GridBagConstraints.BOTH;
        m_requestHeaders.setVisible(true);
        ret.add(new JScrollPane(m_requestHeaders), gbc);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 1;
        gbc.weighty = 0;
        gbc.weightx = .5;
        ret.add(new JPanel(), gbc);
        gbc.gridx++;
        gbc.weightx = 0;
        ret.add(m_requestAddRow, gbc);
        gbc.gridx++;
        ret.add(m_requestDeleteRow, gbc);
        gbc.gridx++;
        ret.add(m_requestEditRow, gbc);
        gbc.gridx++;
        gbc.weightx = .5;
        ret.add(new JPanel(), gbc);
        m_requestHeaders.getColumnModel().getColumn(RequestTableModel.Columns.headerKey.ordinal())
            .setHeaderValue("Key");
        m_requestHeaders.getColumnModel().getColumn(RequestTableModel.Columns.value.ordinal()).setHeaderValue("Value");
        m_requestHeaders.getColumnModel().getColumn(RequestTableModel.Columns.kind.ordinal())
            .setHeaderValue("Value kind");
        return ret;
    }

    /**
     *
     */
    private void updateRequestHeaderKeys() {
        m_requestHeaderOptions = m_requestTemplates.stream()
                .filter(entry -> Objects.equals(m_requestHeaderTemplate.getSelectedItem(), entry.getKey()))
                .map(entry -> entry.getValue()).findFirst().orElse(new ArrayList<>());
        m_requestHeaderKey.removeAllItems();
        if (m_requestHeaderOptions != null) {
            for (final Entry<String, ? extends List<String>> keyValues : m_requestHeaderOptions) {
                m_requestHeaderKey.addItem(keyValues.getKey());
            }
        }
    }

    /**
     * Enable or disable the request header controls.
     *
     * @param enable New value of enabledness.
     */
    protected void enableRequestHeaderChangeControls(final boolean enable) {
        m_requestDeleteRow.setEnabled(enable);
        m_requestEditRow.setEnabled(enable);
    }

    @SuppressWarnings("serial")
    private void deleteAndInsertRowRequestHeaderActions() {
        final ActionMap actionMap = m_requestHeaders.getActionMap();
        final String delete = "deleteRow", insert = "insertRow";
        final InputMap inputMap = m_requestHeaders.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), delete);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0), insert);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape");
        actionMap.put("escape", new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                m_requestHeaders.getColumnModel().getColumn(Columns.headerKey.ordinal()).getCellEditor().cancelCellEditing();
                m_requestHeaders.getColumnModel().getColumn(Columns.value.ordinal()).getCellEditor().cancelCellEditing();
                m_requestHeaders.getColumnModel().getColumn(Columns.kind.ordinal()).getCellEditor().cancelCellEditing();
            }
        });
        actionMap.put(insert, new AbstractAction("Insert Row") {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final boolean hasValidSelection = !m_requestHeaders.getSelectionModel().isSelectionEmpty();
                m_requestEditRow.setEnabled(hasValidSelection);
                m_requestDeleteRow.setEnabled(hasValidSelection);
                m_requestHeadersModel.addRow(new RequestHeaderKeyItem("", "", ReferenceType.Constant));
            }
        });
        actionMap.put(delete, new AbstractAction("Delete Row") {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (m_requestHeaders.getSelectionModel().getLeadSelectionIndex() >= 0) {
                    //We have to go in reverse order, because the ascending order they would point to wrong or non-existing rows
                    final IntStream selectedRows =
                        Arrays.stream(m_requestHeaders.getSelectedRows()).map(i -> -i).sorted().map(i -> -i);
                    selectedRows.forEach(m_requestHeadersModel::removeRow);
                }
                final boolean hasValidSelection = !m_requestHeaders.getSelectionModel().isSelectionEmpty();
                m_requestEditRow.setEnabled(hasValidSelection);
                m_requestDeleteRow.setEnabled(hasValidSelection);
            }
        });
    }

    /**
     * @return The respone headers tab.
     */
    @SuppressWarnings("serial")
    protected JPanel createResponseHeadersTab() {
        m_responseHeaders.setAutoCreateColumnsFromModel(false);
        while (m_responseHeaders.getColumnModel().getColumns().hasMoreElements()) {
            m_responseHeaders.getColumnModel()
                .removeColumn(m_responseHeaders.getColumnModel().getColumns().nextElement());
        }
        m_responseHeaderKeyCellRenderer = new DefaultTableCellRenderer();
        final TableColumn keyCol =
            new TableColumn(0, 67, m_responseHeaderKeyCellRenderer, new FixedCellEditorForComboBoxes(m_responseHeaderKey));
        keyCol.setHeaderValue("Key");
        m_responseHeaders.getColumnModel().addColumn(keyCol);
        m_responseHeaderValueCellRenderer = new DefaultTableCellRenderer() {
            /**
             * {@inheritDoc}
             */
            @Override
            public Component getTableCellRendererComponent(final JTable table, final Object value,
                final boolean isSelected, final boolean hasFocus, final int row, final int column) {
                final Component orig =
                    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value instanceof Pair<?, ?>) {
                    final Pair<?, ?> rawPair = (Pair<?, ?>)value;
                    final Object firstObject = rawPair.getFirst(), secondObject = rawPair.getSecond();
                    if (firstObject instanceof String) {
                        final String colName = (String)firstObject;
                        setText(colName);
                        if (secondObject instanceof DataType) {
                            final DataType type = (DataType)secondObject;
                            setIcon(type.getIcon());
                        }
                    }
                }
                return orig;
            }
        };
        m_responseValueCellEditor = new DefaultCellEditor(m_responseColumnName) {
            /**
             * {@inheritDoc}
             */
            @Override
            public Object getCellEditorValue() {
                final Object orig = super.getCellEditorValue();
                if (orig instanceof Pair<?, ?>) {
                    final Pair<?, ?> pairRaw = (Pair<?, ?>)orig;
                    if (pairRaw.getFirst() instanceof String) {
                        return pairRaw.getFirst();
                    }
                    assert false : pairRaw.getFirst();
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
        };
        m_responseHeaders.getColumnModel()
            .addColumn(new TableColumn(1, 67, m_responseHeaderValueCellRenderer, m_responseValueCellEditor));
        m_responseHeaders.getSelectionModel().addListSelectionListener(e -> updateResponseHeaderControls());
        m_responseAddRow.addActionListener(e -> m_responseHeadersModel.newRow());
        m_responseDeleteRow
            .addActionListener(e -> m_responseHeadersModel.removeRow(m_responseHeaders.getSelectedRow()));
        m_responseEditRow.addActionListener(e -> editResponseHeader(m_responseHeaders.getSelectedRow()));

        final JPanel ret = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        ret.add(m_extractAllHeaders, gbc);
        m_extractAllHeaders.addActionListener(e -> updateResponseHeaderControls());
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        gbc.weightx = 1;
        gbc.gridy++;
        ret.add(new JScrollPane(m_responseHeaders), gbc);
        gbc.gridy++;
        gbc.weighty = 0;
        gbc.weightx = .5;
        gbc.gridwidth = 1;
        ret.add(new JPanel(), gbc);
        gbc.weightx = 0;
        gbc.gridx++;
        ret.add(m_responseAddRow, gbc);
        gbc.gridx++;
        ret.add(m_responseEditRow, gbc);
        gbc.gridx++;
        ret.add(m_responseDeleteRow, gbc);
        gbc.gridx++;
        gbc.weightx = .5;
        ret.add(new JPanel(), gbc);
        gbc.gridy++;

        m_responseHeaders.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editResponseHeader(m_responseHeaders.getSelectedRow());
                }
            }
        });

        ((JTextComponent)m_responseHeaderKey.getEditor().getEditorComponent())
            .addFocusListener((FocusLostListener)e -> updateResponseValueTypes());
        return ret;
    }

    /**
     *
     */
    private void updateResponseValueTypes() {
        if (RestNodeModel.STATUS.equals(m_responseHeaderKey.getSelectedItem())) {
            if (m_responseValueType.getItemCount() < 2) {
                m_responseValueType.addItem(IntCell.TYPE);
            }
        } else {
            if (m_responseValueType.getItemCount() == 2) {
                m_responseValueType.removeItem(IntCell.TYPE);
            }
        }
    }

    /**
     * Udates the enabledness of response header controls.
     */
    private void updateResponseHeaderControls() {
        if (m_extractAllHeaders.isSelected()) {
            m_responseAddRow.setEnabled(false);
            m_responseDeleteRow.setEnabled(false);
            m_responseEditRow.setEnabled(false);
            m_responseHeaders.setEnabled(false);
            m_responseHeaderKeyCellRenderer.setEnabled(false);
            m_responseHeaderValueCellRenderer.setEnabled(false);
            m_responseValueType.setEnabled(false);
            m_responseValueCellEditor.getComponent().setEnabled(false);
            m_responseHeaders.clearSelection();
        } else {
            m_responseAddRow.setEnabled(true);
            m_responseHeaders.setEnabled(true);
            m_responseHeaderKeyCellRenderer.setEnabled(true);
            m_responseHeaderValueCellRenderer.setEnabled(true);
            m_responseValueType.setEnabled(true);
            m_responseValueCellEditor.getComponent().setEnabled(true);
            final boolean hasSelection = m_responseHeaders.getSelectedRowCount() > 0;
            m_responseDeleteRow.setEnabled(hasSelection);
            m_responseEditRow.setEnabled(hasSelection);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        for (final JRadioButton radioButton : m_authenticationTabTitles) {
            for (final EnablableUserConfiguration<UserConfiguration> euc : m_settings
                .getAuthorizationConfigurations()) {
                if (radioButton.getName().equals(euc.getName())) {
                    euc.setEnabled(radioButton.isSelected());
                }
            }
        }
        m_settings.setUseConstantURI(m_constantUriOption.isSelected());
        m_settings.setConstantURI(m_constantUri.getSelectedString());
        if (m_constantUriOption.isSelected()) {
            m_constantUri.commitSelectedToHistory();
        }
        m_settings.setUriColumn(m_uriColumn.getSelectedColumn());
        m_settings.setUseDelay(m_useDelay.isSelected());
        m_settings.setDelay(((Number)m_delay.getValue()).longValue());
        m_settings.setConcurrency(((Number)m_concurrency.getValue()).intValue());
        m_settings.setSslIgnoreHostNameErrors(m_sslIgnoreHostnameMismatches.isSelected());
        m_settings.setSslTrustAll(m_sslTrustAll.isSelected());
        m_settings.setFailOnConnectionProblems(m_failOnConnectionProblems.isSelected());
        m_settings.setFailOnHttpErrors(m_failOnHttpErrors.isSelected());
        m_settings.setFollowRedirects(m_followRedirects.isSelected());
        m_settings.setTimeoutInSeconds(((Number)m_timeoutInSeconds.getValue()).intValue());
        m_settings.getRequestHeaders().clear();
        m_settings.getRequestHeaders()
            .addAll(StreamSupport.stream(m_requestHeadersModel.spliterator(), false).collect(Collectors.toList()));
        m_settings.setExtractAllResponseFields(m_extractAllHeaders.isSelected());
        m_settings.getExtractFields().clear();
        m_settings.getExtractFields()
            .addAll(StreamSupport.stream(m_responseHeadersModel.spliterator(), false).collect(Collectors.toList()));
        m_settings.setResponseBodyColumn(m_bodyColumnName.getSelectedString());
        m_bodyColumnName.commitSelectedToHistory();
        for (final EnablableUserConfiguration<UserConfiguration> euc : m_settings.getAuthorizationConfigurations()) {
            euc.getUserConfiguration().updateSettings();
        }
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        try {
            m_settings.loadSettingsForDialog(settings, getCredentialsProvider(), specs);
        } catch (InvalidSettingsException e) {
            throw new NotConfigurableException(e.getMessage(), e);
        }
        m_bodyColumnName.updateHistory();
        m_columns.clear();
        if (specs[0] != null) {
            m_columns.addAll(Arrays.asList(specs[0].getColumnNames()));
        }
        m_flowVariables.clear();
        m_flowVariables.addAll(getAvailableFlowVariables().keySet());
        m_credentials.clear();
        m_credentials.addAll(getCredentialsNames());
        m_constantUriOption.setSelected(m_settings.isUseConstantURI());
        m_constantUri.updateHistory();
        m_constantUri.setSelectedString(m_settings.getConstantURI());
        m_constantUri.setEnabled(m_settings.isUseConstantURI());
        if (specs[0] != null) {
            m_uriColumnOption.setEnabled(true);
            m_uriColumn.setEnabled(m_uriColumnOption.isSelected());
            try {
                m_uriColumn.setRequired(false);
                m_uriColumn.update(specs[0], m_settings.getUriColumn(), false, true);
                m_uriColumn.setRequired(m_uriColumnOption.isSelected());
            } catch (final NotConfigurableException e) {
                m_uriColumn.setEnabled(false);
                m_uriColumnOption.setEnabled(false);
            }
        } else {
            m_uriColumnOption.setEnabled(false);
            m_uriColumn.setEnabled(false);
        }
        m_useDelay.setSelected(m_settings.isUseDelay());
        m_delay.setValue(m_settings.getDelay());
        m_concurrency.setValue(m_settings.getConcurrency());
        m_sslIgnoreHostnameMismatches.setSelected(m_settings.isSslIgnoreHostNameErrors());
        m_sslTrustAll.setSelected(m_settings.isSslTrustAll());
        m_failOnConnectionProblems.setSelected(m_settings.isFailOnConnectionProblems());
        m_failOnHttpErrors.setSelected(m_settings.isFailOnHttpErrors());
        m_followRedirects.setSelected(m_settings.isFollowRedirects());
        m_timeoutInSeconds.setValue(m_settings.getTimeoutInSeconds());
        m_requestHeadersModel.clear();
        for (int i = 0; i < m_settings.getRequestHeaders().size(); ++i) {
            m_requestHeadersModel.addRow(m_settings.getRequestHeaders().get(i));
        }
        enableRequestHeaderChangeControls(m_settings.getRequestHeaders().size() > 0);
        m_extractAllHeaders.setSelected(m_settings.isExtractAllResponseFields());
        m_responseHeadersModel.clear();
        for (int i = 0; i < m_settings.getExtractFields().size(); ++i) {
            m_responseHeadersModel.addRow(m_settings.getExtractFields().get(i));
        }
        m_bodyColumnName.setSelectedString(m_settings.getResponseBodyColumn());
        for (final EnablableUserConfiguration<UserConfiguration> euc : m_settings.getAuthorizationConfigurations()) {
            for (final JRadioButton radioButton : m_authenticationTabTitles) {
                euc.getUserConfiguration().updateControls();
                if (radioButton.getName().equals(euc.getName())) {
                    radioButton.setSelected(euc.isEnabled());
                    radioButton.getAction().actionPerformed(null);
                }
            }
        }
        updateResponseHeaderControls();
    }

    /**
     * {@link FocusListener} with no focus gained action.
     */
    @FunctionalInterface
    interface FocusLostListener extends FocusListener {
        /**
         * {@inheritDoc}
         */
        @Override
        default void focusGained(final FocusEvent e) {
            //Do nothing.
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean closeOnESC() {
        final Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        return !(focusOwner instanceof JComboBox<?> || focusOwner instanceof JTextComponent);
    }
}