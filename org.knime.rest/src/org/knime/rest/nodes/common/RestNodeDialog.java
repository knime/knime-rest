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
 *   23. Apr. 2016. (Gabor Bakos): created
 */
package org.knime.rest.nodes.common;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListCellRenderer;
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
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
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
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ColumnSelectionPanel;
import org.knime.core.node.util.StringHistoryPanel;
import org.knime.core.util.Pair;
import org.knime.core.util.proxy.ProxyProtocol;
import org.knime.rest.generic.EnablableUserConfiguration;
import org.knime.rest.generic.UserConfiguration;
import org.knime.rest.internals.BasicAuthentication;
import org.knime.rest.nodes.common.RequestTableModel.Columns;
import org.knime.rest.nodes.common.RestSettings.HttpMethod;
import org.knime.rest.nodes.common.RestSettings.ReferenceType;
import org.knime.rest.nodes.common.RestSettings.RequestHeaderKeyItem;
import org.knime.rest.nodes.common.proxy.ProxyMode;
import org.knime.rest.nodes.common.proxy.RestProxyConfig;
import org.knime.rest.nodes.common.proxy.RestProxyConfigManager;
import org.knime.rest.util.DelayPolicy;
import org.knime.rest.util.InvalidURLPolicy;

/**
 * Common dialog for the REST nodes. It adds the following tabs by default:
 * <ul>
 * <li>Connection</li>
 * <li>Authentication</li>
 * <li>Request Headers</li>
 * <li>Response Headers</li>
 * </ul>
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

    private final JRadioButton m_constantURLOption = new JRadioButton("URL: ");

    private final JRadioButton m_urlColumnOption = new JRadioButton("URL column: ");

    private final ConnectionErrorPanel m_connectionErrorPanel = new ConnectionErrorPanel();

    private final ServerErrorPanel m_serverErrorPanel = new ServerErrorPanel();

    private final ClientErrorPanel m_clientErrorPanel = new ClientErrorPanel();

    private final RateLimitPanel m_rateLimitPanel = new RateLimitPanel();

    private final ErrorCausePanel m_errorCausePanel = new ErrorCausePanel();

    {
        final var group = new ButtonGroup();
        group.add(m_constantURLOption);
        group.add(m_urlColumnOption);
    }

    private final StringHistoryPanel m_constantURL = new StringHistoryPanel(getClass().getName());

    private final ColumnSelectionPanel m_urlColumn = new ColumnSelectionPanel(StringValue.class, URIDataValue.class);

    private final DialogComponentButtonGroup m_invalidURLPolicySelector = new DialogComponentButtonGroup( //
        InvalidURLPolicy.createSettingsModel(), //
        "Handling of invalid URLs", //
        false, //
        InvalidURLPolicy.values());

    private final JCheckBox m_useDelay = new JCheckBox("Delay (ms): ");

    private final JSpinner m_delay = new JSpinner(new SpinnerNumberModel(Long.valueOf(0L), Long.valueOf(0L),
        Long.valueOf(30L * 60 * 1000L/*30 minutes*/), Long.valueOf(100L)));

    private final JSpinner m_concurrency = new JSpinner(new SpinnerNumberModel(1, 1, 64, 1));

    private final JCheckBox m_sslIgnoreHostnameMismatches = new JCheckBox("Ignore hostname mismatches");

    private final JCheckBox m_sslTrustAll = new JCheckBox("Trust all certificates");

    private final JCheckBox m_followRedirects = new JCheckBox("Follow redirects");

    private final JCheckBox m_allowChunking = new JCheckBox("Send large data in chunks", true);

    private final JSpinner m_connectTimeoutInSeconds =
        new JSpinner(new SpinnerNumberModel(RestSettings.DEFAULT_CONNECT_TIMEOUT, 1, Integer.MAX_VALUE, 1));

    private final JSpinner m_readTimeoutInSeconds =
            new JSpinner(new SpinnerNumberModel(RestSettings.DEFAULT_READ_TIMEOUT, 1, Integer.MAX_VALUE, 1));

    private final RequestTableModel m_requestHeadersModel = new RequestTableModel();

    private final JButton m_requestAddRow = new JButton("Add header parameter");

    private final JButton m_requestEditRow = new JButton("Edit header parameter");

    private final JButton m_requestDeleteRow = new JButton("Remove header parameter");

    private final JCheckBox m_failOnMissingHeaderCheck = new JCheckBox("Fail on missing header value");

    private final ResponseTableModel m_responseHeadersModel = new ResponseTableModel();

    private final JTable m_requestHeaders = new JTable(m_requestHeadersModel);

    private final JTable m_responseHeaders = new JTable(m_responseHeadersModel);

    private final JButton m_responseAddRow = new JButton("Add header parameter");

    private final JButton m_responseEditRow = new JButton("Edit header parameter");

    private final JButton m_responseDeleteRow = new JButton("Remove header parameter");

    private final JCheckBox m_extractAllHeaders = new JCheckBox("Extract all headers");

    /** */
    protected final JLabel m_labelBodyColumnName = new JLabel("Body column: ");

    /** */
    protected final StringHistoryPanel m_bodyColumnName = new StringHistoryPanel("GET body");

    private final JComboBox<String> m_requestHeaderKey = createEditableComboBox(),
            m_requestHeaderKeyPopup = createEditableComboBox();

    private final JComboBox<String> m_requestHeaderValue = createEditableComboBox(),
            m_requestHeaderValuePopup = createEditableComboBox();

    private final JComboBox<ReferenceType> m_requestHeaderValueType = createEditableComboBox(ReferenceType.values()),
            m_requestHeaderValueTypePopup = createEditableComboBox(ReferenceType.values());

    private final JTextField m_responseHeaderKey = new JTextField(20);

    private final JTextField m_responseColumnName = new JTextField(20);

    private final JComboBox<DataType> m_responseValueType =
        new JComboBox<>(new DataType[]{StringCell.TYPE, IntCell.TYPE});

    private final Map<String, JRadioButton> m_authenticationTabTitles = new HashMap<>();

    private final JComboBox<String> m_requestHeaderTemplate = new JComboBox<>();

    private final JButton m_requestHeaderTemplateReplace = new JButton("Replace");

    private final JButton m_requestHeaderTemplateMerge = new JButton("Merge");

    private final List<Entry<String, List<Entry<String, ? extends List<String>>>>> m_requestTemplates =
        new ArrayList<>();

    private List<Entry<String, ? extends List<String>>> m_requestHeaderOptions;

    private DefaultTableCellRenderer m_responseHeaderKeyCellRenderer;

    private DefaultTableCellRenderer m_responseHeaderValueCellRenderer;

    private DefaultCellEditor m_responseValueCellEditor;

    // Proxy settings tab.

    private final DialogComponentButtonGroup m_proxyModeSelector = new DialogComponentButtonGroup(
        RestProxyConfigManager.createProxyModeSettingsModel(), null, false, ProxyMode.values());

    private final JComboBox<ProxyProtocol> m_proxyProtocolCombo =
        new JComboBox<>(ProxyProtocol.values());

    private final StringHistoryPanel m_proxyHostPanel =
        new StringHistoryPanel(RestProxyConfig.getProxyHostStringHistoryID());

    private final StringHistoryPanel m_proxyPortPanel =
        new StringHistoryPanel(RestProxyConfig.getProxyPortStringHistoryID());

    private final JCheckBox m_useAuthenticationChecker = new JCheckBox("Proxy host needs authentication");

    private final BasicAuthentication m_proxyAuthenticatorPanel = RestProxyConfigManager.createProxyAuthSettingsModel();

    private final JCheckBox m_useExcludeHostsChecker = new JCheckBox("Exclude hosts from proxy");

    private final StringHistoryPanel m_proxyExcludeHostsPanel =
        new StringHistoryPanel(RestProxyConfig.getProxyExcludedHostsStringHistoryID());

    private final boolean m_hasCredentialPort;

    /**
     * Constructs the dialog.
     * @param cfg The node creation config.
     */
    protected RestNodeDialog(final NodeCreationConfiguration cfg) {
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

        m_hasCredentialPort = cfg.getPortConfig().orElseThrow(IllegalStateException::new).getInputPortLocation()
            .get(RestNodeFactory.CREDENTIAL_GROUP_ID) != null;

        addTab("Connection", createConnectionSettingsTab());
        addTab("Authentication", createAuthenticationTab());
        addTab("Proxy", createProxySettingsTab());
        addTab("Error Handling", createErrorHandlingTab());
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
                final var res = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (res instanceof JLabel label) {
                    label.setFont(getFont().deriveFont(10f));
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

    /** A functional interface to handle all changes with a single method. */
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
        final var ret = new JPanel(new GridBagLayout());
        final var gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.weighty = 0;
        ret.add(m_constantURLOption, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        gbc.gridwidth = 2;
        ret.add(m_constantURL, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        ret.add(m_urlColumnOption, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        m_urlColumn.setBorder(null);
        gbc.gridwidth = 2;
        ret.add(m_urlColumn, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 3;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        final var urlPanel = m_invalidURLPolicySelector.getComponentPanel();
        urlPanel.setLayout(new BoxLayout(urlPanel, BoxLayout.PAGE_AXIS));
        ret.add(urlPanel, gbc);
        gbc.gridy++;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        ret.add(m_useDelay, gbc);
        gbc.gridx++;
        gbc.weightx = 0;
        final var preferredLabelSize = new Dimension(150, 20);
        final var preferredSpinnerSize = new Dimension(75, 20);
        gbc.fill = GridBagConstraints.NONE;
        m_delay.setPreferredSize(preferredSpinnerSize);
        ret.add(m_delay, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        final var concurrencyLabel = new JLabel("Concurrency: ");
        concurrencyLabel.setPreferredSize(preferredLabelSize);
        ret.add(concurrencyLabel, gbc);
        gbc.gridx++;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        m_concurrency.setPreferredSize(preferredSpinnerSize);
        ret.add(m_concurrency, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 3;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        final var sslPanel = new JPanel();
        sslPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "SSL"));
        sslPanel.setLayout(new BoxLayout(sslPanel, BoxLayout.PAGE_AXIS));
        sslPanel.add(m_sslIgnoreHostnameMismatches);
        sslPanel.add(m_sslTrustAll);
        ret.add(sslPanel, gbc);
        gbc.gridy++;
        ret.add(m_followRedirects, gbc);
        gbc.gridy++;
        ret.add(m_allowChunking, gbc);
        gbc.gridy++;
        gbc.weightx = 0;
        gbc.gridx = 0;
        final var connectTimeoutLabel = new JLabel("Connect timeout (s):");
        connectTimeoutLabel.setPreferredSize(preferredLabelSize);
        ret.add(connectTimeoutLabel, gbc);
        gbc.gridx++;
        m_connectTimeoutInSeconds.setPreferredSize(preferredSpinnerSize);
        gbc.fill = GridBagConstraints.NONE;
        ret.add(m_connectTimeoutInSeconds, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        gbc.gridx = 0;
        final var readTimeoutLabel = new JLabel("Read timeout (s):");
        readTimeoutLabel.setPreferredSize(preferredLabelSize);
        ret.add(readTimeoutLabel, gbc);
        gbc.gridx++;
        m_readTimeoutInSeconds.setPreferredSize(preferredSpinnerSize);
        gbc.fill = GridBagConstraints.NONE;
        ret.add(m_readTimeoutInSeconds, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy++;
        m_labelBodyColumnName.setPreferredSize(preferredLabelSize);
        m_settings.getMethod().filter(m -> m != HttpMethod.HEAD).ifPresent(m -> ret.add(m_labelBodyColumnName, gbc));
        gbc.gridx++;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        m_settings.getMethod().filter(m -> m != HttpMethod.HEAD).ifPresent(m -> ret.add(m_bodyColumnName, gbc));
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        ret.add(new JPanel(), gbc);

        m_useDelay.addActionListener(e -> m_delay.setEnabled(m_useDelay.isSelected()));
        m_constantURLOption.addActionListener(e -> {
            m_constantURL.setEnabled(m_constantURLOption.isSelected());
            m_urlColumn.setEnabled(!m_constantURLOption.isSelected());
        });
        m_urlColumnOption.addActionListener(e -> {
            m_urlColumn.setEnabled(m_urlColumnOption.isSelected());
            m_constantURL.setEnabled(!m_urlColumnOption.isSelected());
        });
        m_constantURLOption.setSelected(true);
        m_urlColumn.setEnabled(false);
        m_urlColumn.setRequired(false);
        m_delay.setEnabled(false);
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
        final var windowAncestor = SwingUtilities.getWindowAncestor(getPanel());
        final var frame = windowAncestor instanceof Frame f ? f : null;
        final var dialog = new JDialog(frame, "Edit", true);
        final var dimension = new Dimension(550, 200);
        dialog.setPreferredSize(dimension);
        dialog.setLocationRelativeTo(frame);
        if (frame != null) {
            dialog.setLocation(
                (int)(frame.getLocationOnScreen().getX() + frame.getWidth() / 2 - dimension.getWidth() / 2),
                (int)(frame.getLocationOnScreen().getY() + frame.getHeight() / 2 - dimension.getHeight() / 2));
        }
        final var cp = dialog.getContentPane();
        final var outer = new JPanel(new BorderLayout());
        final var panel = new JPanel(new GridBagLayout());
        addRequestSettingControls(panel);
        m_requestHeaderKeyPopup.setSelectedItem(
            m_requestHeadersModel.getValueAt(selectedRow, RequestTableModel.Columns.headerKey.ordinal()));
        m_requestHeaderValuePopup
            .setSelectedItem(m_requestHeadersModel.getValueAt(selectedRow, RequestTableModel.Columns.value.ordinal()));
        m_requestHeaderValueTypePopup
            .setSelectedItem(m_requestHeadersModel.getValueAt(selectedRow, RequestTableModel.Columns.kind.ordinal()));
        outer.add(panel, BorderLayout.CENTER);
        final var controls = new JPanel();
        outer.add(controls, BorderLayout.SOUTH);
        cp.add(outer);
        controls.setLayout(new BoxLayout(controls, BoxLayout.LINE_AXIS));
        controls.add(Box.createHorizontalGlue());
        controls.add(new JButton(new AbstractAction("OK") {
            @Override
            public void actionPerformed(final ActionEvent e) {
                Object source = e.getSource();
                if (source instanceof JComponent comp) {
                    comp.grabFocus();
                }
                SwingUtilities.invokeLater(() -> {
                    m_requestHeadersModel.setValueAt(m_requestHeaderKeyPopup.getSelectedItem(), selectedRow,
                        RequestTableModel.Columns.headerKey.ordinal());
                    m_requestHeadersModel.setValueAt(m_requestHeaderValuePopup.getSelectedItem(), selectedRow,
                        RequestTableModel.Columns.value.ordinal());
                    m_requestHeadersModel.setValueAt(m_requestHeaderValueTypePopup.getSelectedItem(), selectedRow,
                        RequestTableModel.Columns.kind.ordinal());
                    dialog.dispose();
                });
                //                m_requestHeadersModel.setValueAt(m_requestHeaderKeyPopup.getSelectedItem(), selectedRow,
                //                    RequestTableModel.Columns.headerKey.ordinal());
                //                m_requestHeadersModel.setValueAt(m_requestHeaderValuePopup.getSelectedItem(), selectedRow,
                //                    RequestTableModel.Columns.value.ordinal());
                //                m_requestHeadersModel.setValueAt(m_requestHeaderValueTypePopup.getSelectedItem(), selectedRow,
                //                    RequestTableModel.Columns.kind.ordinal());
                //                dialog.dispose();
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
        final var windowAncestor = SwingUtilities.getWindowAncestor(getPanel());
        final var frame = windowAncestor instanceof Frame f ? f : null;
        final var dialog = new JDialog(frame, "Edit", true);
        final var dimension = new Dimension(550, 200);
        dialog.setPreferredSize(dimension);
        dialog.setLocationRelativeTo(frame);
        if (frame != null) {
            dialog.setLocation(
                (int)(frame.getLocationOnScreen().getX() + frame.getWidth() / 2 - dimension.getWidth() / 2),
                (int)(frame.getLocationOnScreen().getY() + frame.getHeight() / 2 - dimension.getHeight() / 2));
        }
        final var cp = dialog.getContentPane();
        final var outer = new JPanel(new BorderLayout());
        final var panel = new JPanel(new GridBagLayout());
        addResponseSettingControls(panel);
        m_responseHeaderKey.setText(
            (String)m_responseHeadersModel.getValueAt(selectedRow, ResponseTableModel.Columns.headerKey.ordinal()));
        m_responseColumnName.setText((String)((Pair<?, ?>)m_responseHeadersModel.getValueAt(selectedRow,
            ResponseTableModel.Columns.outputColumn.ordinal())).getFirst());
        m_responseValueType.setSelectedItem(((Pair<?, ?>)m_responseHeadersModel.getValueAt(selectedRow,
            ResponseTableModel.Columns.outputColumn.ordinal())).getSecond());
        outer.add(panel, BorderLayout.CENTER);
        final var controls = new JPanel();
        outer.add(controls, BorderLayout.SOUTH);
        cp.add(outer);
        controls.setLayout(new BoxLayout(controls, BoxLayout.LINE_AXIS));
        controls.add(Box.createHorizontalGlue());
        controls.add(new JButton(new AbstractAction("OK") {
            @Override
            public void actionPerformed(final ActionEvent e) {
                m_responseHeadersModel.setValueAt(m_responseHeaderKey.getText(), selectedRow, 0);
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
        final var gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(new JLabel("Header key"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(m_requestHeaderKeyPopup, gbc);
        gbc.gridy++;

        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(new JLabel("Header value:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(m_requestHeaderValuePopup, gbc);
        gbc.gridy++;

        gbc.gridx = 0;
        panel.add(new JLabel("Type:"), gbc);
        gbc.gridx++;
        panel.add(m_requestHeaderValueTypePopup, gbc);
        gbc.gridy++;

    }

    /**
     * @param panel The panel to be used for response settings.
     */
    protected void addResponseSettingControls(final JPanel panel) {
        final var gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(new JLabel("Header key"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(m_responseHeaderKey, gbc);
        gbc.gridy++;

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
        final var ret = new JPanel();
        ret.setLayout(new BoxLayout(ret, BoxLayout.PAGE_AXIS));
        final var radioButtons = new JPanel(new FlowLayout());
        ret.add(radioButtons);
        final var tabs = new JPanel(new CardLayout());
        final var buttonGroup = new ButtonGroup();
        for (final EnablableUserConfiguration<UserConfiguration> euc : m_settings.getAuthorizationConfigurations()) {
            final var tabPanel = new JPanel();
            final var scrollPane = new JScrollPane(tabPanel);
            final var userConfiguration = euc.getUserConfiguration();
            tabs.add(euc.getName(), scrollPane);
            final var radioButton = new JRadioButton(euc.getName());
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
            m_authenticationTabTitles.put(euc.getName(), radioButton);
            userConfiguration.addControls(tabPanel);
        }

        var credentialRbName = "Credential (input port)";
        var rbCredential = new JRadioButton(credentialRbName);
        radioButtons.add(rbCredential);
        buttonGroup.add(rbCredential);

        if (m_hasCredentialPort) {
            rbCredential.setSelected(true);

            tabs.add(credentialRbName, new JPanel());
            ((CardLayout)tabs.getLayout()).show(tabs, credentialRbName);

            for (JRadioButton rb : m_authenticationTabTitles.values()) {
                rb.setEnabled(false);
            }
        } else {
            rbCredential.setEnabled(false);
        }

        ret.add(tabs);
        return ret;
    }

    private Component createProxySettingsTab() {
        // - Creating outer layout (incl. radio button proxy selector).
        final var container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.PAGE_AXIS));
        container.add(m_proxyModeSelector.getComponentPanel());

        // - Building node-specific proxy settings.
        final var localProxyPanel = new JPanel(new GridBagLayout());
        localProxyPanel.setVisible(false);
        final var gbc = FramedPanel.initGridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);
        localProxyPanel.add(new JLabel("Proxy protocol: "), gbc);
        gbc.gridx += 1;
        gbc.weightx = 1.0;
        localProxyPanel.add(m_proxyProtocolCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy += 1;
        gbc.weightx = 0.0;
        localProxyPanel.add(new JLabel("Proxy host: "), gbc);
        gbc.gridx += 1;
        gbc.weightx = 1.0;
        localProxyPanel.add(m_proxyHostPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy += 1;
        gbc.weightx = 0.0;
        localProxyPanel.add(new JLabel("Proxy port: "), gbc);
        gbc.gridx += 1;
        gbc.weightx = 1.0;
        localProxyPanel.add(m_proxyPortPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy += 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        localProxyPanel.add(new JSeparator(), gbc);

        gbc.gridy += 1;
        localProxyPanel.add(m_useAuthenticationChecker, gbc);

        gbc.gridx = 0;
        gbc.gridy += 1;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 5, 5, 5);
        gbc.weightx = 1.0;
        var authPanel = new JPanel();
        m_proxyAuthenticatorPanel.addControls(authPanel);
        localProxyPanel.add(authPanel, gbc);

        // Setting enable/disable listeners for the auth field and explicitly set the state to update listeners
        m_useAuthenticationChecker.addItemListener(e -> {
            m_proxyAuthenticatorPanel.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
            m_proxyAuthenticatorPanel.updateControls();
        });
        m_useAuthenticationChecker.setSelected(false); // does not fire event...
        m_proxyAuthenticatorPanel.setEnabled(false);

        gbc.gridx = 0;
        gbc.gridy += 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        localProxyPanel.add(new JSeparator(), gbc);

        gbc.gridy += 1;
        localProxyPanel.add(m_useExcludeHostsChecker, gbc);

        gbc.gridx = 0;
        gbc.gridy += 1;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 5, 5, 5);
        gbc.weightx = 1.0;
        // stolen from SendMailNodeDialog
        final var excludeHostsPanel = new JPanel();
        excludeHostsPanel.setLayout(new BoxLayout(excludeHostsPanel, BoxLayout.Y_AXIS));
        excludeHostsPanel.setBorder(BorderFactory.createTitledBorder(" Excluded hosts (separated by a semicolon) "));
        excludeHostsPanel.add(m_proxyExcludeHostsPanel, BorderLayout.CENTER);
        localProxyPanel.add(excludeHostsPanel, gbc);

        // Setting enable/disable listeners for the exclude hosts field.
        m_useExcludeHostsChecker.addItemListener(
            e -> m_proxyExcludeHostsPanel.setEnabled(e.getStateChange() == ItemEvent.SELECTED));
        m_useExcludeHostsChecker.setSelected(false); // does not fire event...
        m_proxyExcludeHostsPanel.setEnabled(false);

        // - Adding proxy mode button listener.
        m_proxyModeSelector.getButton(ProxyMode.LOCAL.name()).addItemListener(
            e -> localProxyPanel.setVisible(e.getStateChange() == ItemEvent.SELECTED));

        container.add(new JScrollPane(localProxyPanel), BorderLayout.PAGE_START);
        return container;
    }

    private Component createErrorHandlingTab() {
        final var container = new JPanel(new GridBagLayout());
        final var gbc = FramedPanel.initGridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        container.add(m_connectionErrorPanel, gbc);
        gbc.gridy += 1;
        container.add(m_serverErrorPanel, gbc);
        gbc.gridy += 1;
        container.add(m_clientErrorPanel, gbc);
        gbc.gridy += 1;
        container.add(m_rateLimitPanel, gbc);
        gbc.gridy += 1;
        container.add(m_errorCausePanel, gbc);
        gbc.gridy += 1;
        gbc.weighty = 1.0;
        container.add(new JLabel(), gbc);
        return container;
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
        final ActionListener updateRequestValueAlternatives = al -> updateRequestValueAlternatives(m_requestHeaderKey,
            m_requestHeaderValue, m_requestHeaderValueType, false);
        m_requestHeaderKey.addActionListener(updateRequestValueAlternatives);
        final var keyCol = new TableColumn(RequestTableModel.Columns.headerKey.ordinal(), 67,
            new DefaultTableCellRenderer(), new FixedCellEditorForComboBoxes(m_requestHeaderKey));
        keyCol.setHeaderValue("Header Key");
        m_requestHeaders.getColumnModel().addColumn(keyCol);
        m_requestHeaders.getColumnModel().addColumn(new TableColumn(RequestTableModel.Columns.value.ordinal(), 67, null,
            new FixedCellEditorForComboBoxes(m_requestHeaderValue)));
        m_requestHeaders.getColumnModel().addColumn(new TableColumn(RequestTableModel.Columns.kind.ordinal(), 40, null,
            new DefaultCellEditor(m_requestHeaderValueType)));
        m_requestHeaders.setRowHeight(20);
        m_requestHeaderValueType.addActionListener(updateRequestValueAlternatives);
        final var deleteRequestRow = new ButtonCell();
        deleteRequestRow.setAction(new AbstractAction(" X ") {
            private static final long serialVersionUID = 1369259160048695493L;

            @Override
            public void actionPerformed(final ActionEvent e) {
                if (deleteRequestRow.getRow() >= 0) {
                    m_requestHeadersModel.removeRow(deleteRequestRow.getRow());
                }
            }
        });
        final var deleteRowColumn =
            new TableColumn(RequestTableModel.Columns.delete.ordinal(), 15, deleteRequestRow, deleteRequestRow);
        deleteRowColumn.setMaxWidth(25);
        m_requestHeaders.getColumnModel().addColumn(deleteRowColumn);
        m_requestAddRow.addActionListener(e -> {
            m_requestHeadersModel.newRow();
            int last = m_requestHeadersModel.getRowCount() - 1;
            m_requestHeaders.setRowSelectionInterval(last, last);
            editRequestHeader(last);
        });
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
            for (var i = 0; i < m_requestHeadersModel.getRowCount(); ++i) {
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
        m_requestHeaderTemplateReplace.setToolTipText("Replaces the current settings with the ones from the template");
        m_requestHeaderTemplateReplace.addActionListener(e -> {
            m_requestHeadersModel.clear();
            updateRequestHeaderKeys();
            for (final Entry<String, ? extends List<String>> keyValues : m_requestHeaderOptions) {
                m_requestHeadersModel.addRow(new RequestHeaderKeyItem(keyValues.getKey(),
                    keyValues.getValue().stream().findFirst().orElse(""), ReferenceType.Constant));
            }
        });

        final var ret = new JPanel(new GridBagLayout());
        final var gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.weightx = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        final var templateLabel = new JLabel("Template:");
        templateLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        templateLabel.setVerticalAlignment(SwingConstants.CENTER);
        ret.add(templateLabel, gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 4;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        ret.add(m_requestHeaderTemplate, gbc);
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.gridx += 4;
        ret.add(m_requestHeaderTemplateMerge, gbc);
        gbc.gridx++;
        ret.add(m_requestHeaderTemplateReplace, gbc);
        gbc.gridy++;
        gbc.weighty = 1;
        gbc.gridx = 0;
        gbc.gridwidth = 7;
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
        ret.add(m_failOnMissingHeaderCheck, gbc);
        gbc.gridx++;
        gbc.weightx = .5;
        ret.add(new JPanel(), gbc);
        m_requestHeaders.getColumnModel().getColumn(RequestTableModel.Columns.headerKey.ordinal())
            .setHeaderValue("Header Key");
        m_requestHeaders.getColumnModel().getColumn(RequestTableModel.Columns.value.ordinal())
            .setHeaderValue("Header value");
        m_requestHeaders.getColumnModel().getColumn(RequestTableModel.Columns.kind.ordinal())
            .setHeaderValue("Value kind");

        final ActionListener popupListener = e -> updateRequestValueAlternatives(m_requestHeaderKeyPopup,
            m_requestHeaderValuePopup, m_requestHeaderValueTypePopup, true);
        m_requestHeaderValueTypePopup.addActionListener(popupListener);
        m_requestHeaderKeyPopup.addActionListener(popupListener);
        return ret;
    }

    /**
     * @param requestHeaderKey Request header key control.
     * @param requestHeaderValue Request header value control.
     * @param requestHeaderValueType Request header value type control.
     * @param useLocalValue Use the local value instead of the table content.
     */
    private void updateRequestValueAlternatives(final JComboBox<String> requestHeaderKey,
        final JComboBox<String> requestHeaderValue, final JComboBox<ReferenceType> requestHeaderValueType,
        final boolean useLocalValue) {
        if (m_requestHeaders.getSelectedRowCount() == 0 && !useLocalValue) {
            enableRequestHeaderChangeControls(false);
            return;
        }
        enableRequestHeaderChangeControls(true);
        final Object origValue = useLocalValue ? requestHeaderValue.getSelectedItem()
            : m_requestHeadersModel.getValueAt(m_requestHeaders.getSelectedRow(), Columns.value.ordinal());
        requestHeaderValue.removeAllItems();
        switch ((ReferenceType)requestHeaderValueType.getSelectedItem()) {
            case FlowVariable:
                for (final String flowVar : m_flowVariables) {
                    requestHeaderValue.addItem(flowVar);
                }
                break;
            case Column:
                for (final String column : m_columns) {
                    requestHeaderValue.addItem(column);
                }
                break;
            case Constant:
                if (m_requestHeaders.getSelectedRowCount() > 0) {
                    final String key =
                        (String)(useLocalValue ? requestHeaderKey.getSelectedItem() : m_requestHeadersModel
                            .getValueAt(m_requestHeaders.getSelectedRow(), Columns.headerKey.ordinal()));
                    final Object templateItem = m_requestHeaderTemplate.getSelectedItem();
                    m_requestTemplates.stream().filter(entry -> Objects.equals(templateItem, entry.getKey()))
                        .findFirst()
                        .ifPresent(entry -> entry.getValue().stream()
                            .filter(listEntry -> Objects.equals(key, listEntry.getKey())).findFirst()
                            .map(listEntry -> listEntry.getValue())
                            .ifPresent(values -> values.forEach(requestHeaderValue::addItem)));
                }
                break;
            case CredentialName, CredentialPassword:
                for (final String credential : m_credentials) {
                    requestHeaderValue.addItem(credential);
                }
                break;
            default:
                throw new IllegalStateException(
                    "Unknown reference type: " + m_requestHeaderValueType.getSelectedItem());
        }
        if (!useLocalValue) {
            m_requestHeadersModel.setValueAt(origValue, m_requestHeaders.getSelectedRow(), 1);
        }
        requestHeaderValue.setSelectedItem(origValue);
    }

    /**
     * Updates the request header keys based on the template values.
     */
    private void updateRequestHeaderKeys() {
        m_requestHeaderOptions = m_requestTemplates.stream()
            .filter(entry -> Objects.equals(m_requestHeaderTemplate.getSelectedItem(), entry.getKey()))
            .map(entry -> entry.getValue()).findFirst().orElse(new ArrayList<>());
        m_requestHeaderKey.removeAllItems();
        m_requestHeaderKeyPopup.removeAllItems();
        if (m_requestHeaderOptions != null) {
            for (final Entry<String, ? extends List<String>> keyValues : m_requestHeaderOptions) {
                String key = keyValues.getKey();
                m_requestHeaderKey.addItem(key);
                m_requestHeaderKeyPopup.addItem(key);
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
        final var actionMap = m_requestHeaders.getActionMap();
        final var delete = "deleteRow";
        final var insert = "insertRow";
        final var inputMap = m_requestHeaders.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), delete);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0), insert);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape");
        actionMap.put("escape", new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                m_requestHeaders.getColumnModel().getColumn(Columns.headerKey.ordinal()).getCellEditor()
                    .cancelCellEditing();
                m_requestHeaders.getColumnModel().getColumn(Columns.value.ordinal()).getCellEditor()
                    .cancelCellEditing();
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
     * @return The response headers tab.
     */
    @SuppressWarnings("serial")
    protected JPanel createResponseHeadersTab() {
        m_responseHeaders.setAutoCreateColumnsFromModel(false);
        m_responseHeaders.setRowHeight(20);
        while (m_responseHeaders.getColumnModel().getColumns().hasMoreElements()) {
            m_responseHeaders.getColumnModel()
                .removeColumn(m_responseHeaders.getColumnModel().getColumns().nextElement());
        }
        m_responseHeaderKeyCellRenderer = new DefaultTableCellRenderer();
        final var keyCol =
            new TableColumn(0, 67, m_responseHeaderKeyCellRenderer, new DefaultCellEditor(m_responseHeaderKey));
        keyCol.setHeaderValue("Key");
        m_responseHeaders.getColumnModel().addColumn(keyCol);
        m_responseHeaderValueCellRenderer = new DefaultTableCellRenderer() {
            /**
             * {@inheritDoc}
             */
            @Override
            public Component getTableCellRendererComponent(final JTable table, final Object value,
                final boolean isSelected, final boolean hasFocus, final int row, final int column) {
                final var orig =
                    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value instanceof Pair<?, ?> rawPair) {
                    final var firstObject = rawPair.getFirst();
                    final var secondObject = rawPair.getSecond();
                    if (firstObject instanceof String colName) {
                        setText(colName);
                        if (secondObject instanceof DataType type) {
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
                if (orig instanceof Pair<?, ?> rawPair) {
                    if (rawPair.getFirst() instanceof String) {
                        return rawPair.getFirst();
                    }
                    assert false : rawPair.getFirst();
                }
                return orig;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Component getTableCellEditorComponent(final JTable table, final Object value,
                final boolean isSelected, final int row, final int column) {
                if (value instanceof Pair<?, ?> rawPair) {
                    return super.getTableCellEditorComponent(table, rawPair.getFirst(), isSelected, row, column);
                }
                return super.getTableCellEditorComponent(table, value, isSelected, row, column);
            }
        };
        final var columnColumn =
            new TableColumn(1, 67, m_responseHeaderValueCellRenderer, m_responseValueCellEditor);
        columnColumn.setHeaderValue("Column");
        m_responseHeaders.getColumnModel().addColumn(columnColumn);
        m_responseHeaders.getSelectionModel().addListSelectionListener(e -> updateResponseHeaderControls());
        m_responseAddRow.addActionListener(e -> {
            m_responseHeadersModel.newRow();
            int last = m_responseHeadersModel.getRowCount() - 1;
            m_responseHeaders.setRowSelectionInterval(last, last);
            editResponseHeader(last);
        });
        m_responseDeleteRow
            .addActionListener(e -> m_responseHeadersModel.removeRow(m_responseHeaders.getSelectedRow()));
        m_responseEditRow.addActionListener(e -> editResponseHeader(m_responseHeaders.getSelectedRow()));

        final var ret = new JPanel(new GridBagLayout());
        final var gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 4, 2, 4);
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
        return ret;
    }

    /**
     * Updates the enabledness of response header controls.
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
        if (!m_hasCredentialPort) {
            for (final EnablableUserConfiguration<UserConfiguration> euc : m_settings
                .getAuthorizationConfigurations()) {
                euc.setEnabled(m_authenticationTabTitles.get(euc.getName()).isSelected());
            }
        }
        m_settings.setUseConstantURL(m_constantURLOption.isSelected());
        m_settings.setConstantURL(m_constantURL.getSelectedString());
        if (m_constantURLOption.isSelected()) {
            m_constantURL.commitSelectedToHistory();
        }
        m_settings.setURLColumn(m_urlColumn.getSelectedColumn());
        m_settings.setInvalidURLPolicy(((SettingsModelString)m_invalidURLPolicySelector.getModel()).getStringValue());
        m_settings.setUseDelay(m_useDelay.isSelected());
        m_settings.setDelay(((Number)m_delay.getValue()).longValue());
        m_settings.setConcurrency(((Number)m_concurrency.getValue()).intValue());
        m_settings.setSslIgnoreHostNameErrors(m_sslIgnoreHostnameMismatches.isSelected());
        m_settings.setSslTrustAll(m_sslTrustAll.isSelected());
        m_settings.setFailOnConnectionProblems(m_connectionErrorPanel.isFailOnError());
        m_settings.setFollowRedirects(m_followRedirects.isSelected());
        m_settings.setConnectTimeoutInSeconds(((Number)m_connectTimeoutInSeconds.getValue()).intValue());
        m_settings.setReadTimeoutInSeconds(((Number)m_readTimeoutInSeconds.getValue()).intValue());
        m_settings.getRequestHeaders().clear();
        m_settings.getRequestHeaders()
            .addAll(StreamSupport.stream(m_requestHeadersModel.spliterator(), false).collect(Collectors.toList()));
        m_settings.setFailOnMissingHeaders(m_failOnMissingHeaderCheck.isSelected());
        m_settings.setExtractAllResponseFields(m_extractAllHeaders.isSelected());
        m_settings.getExtractFields().clear();
        m_settings.getExtractFields()
            .addAll(StreamSupport.stream(m_responseHeadersModel.spliterator(), false).collect(Collectors.toList()));
        m_settings.setResponseBodyColumn(m_bodyColumnName.getSelectedString());
        m_settings.setAllowChunking(m_allowChunking.isSelected());
        m_bodyColumnName.commitSelectedToHistory();
        for (final EnablableUserConfiguration<UserConfiguration> euc : m_settings.getAuthorizationConfigurations()) {
            euc.getUserConfiguration().updateSettings();
        }
        var delayPolicy =
            new DelayPolicy(m_serverErrorPanel.getRetryDelay(), m_serverErrorPanel.getNumRetries(),
                m_rateLimitPanel.getDelay(), m_serverErrorPanel.isRetryEnabled(), m_rateLimitPanel.isActive());
        m_settings.setDelayPolicy(delayPolicy);
        m_settings.setFailOnClientErrors(m_clientErrorPanel.isFailOnError());
        m_settings.setFailOnServerErrors(m_serverErrorPanel.isFailOnError());
        m_settings.setOutputErrorCause(m_errorCausePanel.isSelected());

        // Proxy settings.
        final var proxyManager = m_settings.getProxyManager();
        final var proxyMode = getProxyMode();
        proxyManager.setProxyMode(proxyMode);
        if (proxyMode == ProxyMode.LOCAL) {
            m_settings.m_currentProxyConfig = Optional.of(RestProxyConfig.builder()//
                .setProtocol((ProxyProtocol)m_proxyProtocolCombo.getSelectedItem())//
                .setProxyHost(m_proxyHostPanel.getSelectedString())//
                .setProxyPort(m_proxyPortPanel.getSelectedString())//
                .setUseAuthentication(m_useAuthenticationChecker.isSelected())//
                .setBasicAuthentication(m_proxyAuthenticatorPanel)//
                .setUseExcludeHosts(m_useExcludeHostsChecker.isSelected())//
                .setExcludedHosts(m_proxyExcludeHostsPanel.getSelectedString())//
                .build());
        } else {
            // Don't save any node-specific proxy settings in case of GLOBAL or NONE mode.
            m_settings.m_currentProxyConfig = Optional.empty();
        }

        m_proxyHostPanel.commitSelectedToHistory();
        m_proxyPortPanel.commitSelectedToHistory();
        m_proxyExcludeHostsPanel.commitSelectedToHistory();

        m_settings.saveSettings(settings);
    }

    private ProxyMode getProxyMode() {
        for (var mode : ProxyMode.values()) {
            if (m_proxyModeSelector.getButton(mode.name()).isSelected()) {
                return mode;
            }
        }
        throw new IllegalStateException("No proxy mode button was selected!");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_settings.getProxyManager().setAuthReference(m_proxyAuthenticatorPanel);
        try {
            m_settings.loadSettingsForDialog(settings, getCredentialsProvider(), specs);
        } catch (InvalidSettingsException e) {
            throw new NotConfigurableException(e.getMessage(), e);
        }
        m_bodyColumnName.updateHistory();
        m_columns.clear();
        if (specs[0] != null) {
            m_columns.addAll(Arrays.asList(((DataTableSpec)specs[0]).getColumnNames()));
        }
        m_flowVariables.clear();
        m_flowVariables.addAll(getAvailableFlowVariables().keySet());
        m_credentials.clear();
        m_credentials.addAll(getCredentialsNames());
        m_constantURLOption.setSelected(m_settings.isUseConstantURL());
        m_constantURL.updateHistory();
        m_constantURL.setSelectedString(m_settings.getConstantURL());
        m_constantURL.setEnabled(m_settings.isUseConstantURL());
        if (specs[0] != null) {
            int inSpecSize = ((DataTableSpec)specs[0]).getNumColumns();
            if (inSpecSize == 0 && m_settings.getURLColumn() == null) {
                m_urlColumnOption.setEnabled(false);
                m_urlColumn.setEnabled(false);
            } else {
                m_urlColumnOption.setEnabled(true);
                m_urlColumnOption.setSelected(!m_settings.isUseConstantURL());
                m_urlColumn.setEnabled(m_urlColumnOption.isSelected());
                try {
                    m_urlColumn.update((DataTableSpec)specs[0], m_settings.getURLColumn(), false, true);
                } catch (final NotConfigurableException e) {
                    m_urlColumn.setEnabled(false);
                    m_urlColumnOption.setEnabled(false);
                }
            }
        } else {
            var dummySpec = new DataTableSpec();
            m_urlColumn.update(dummySpec, m_settings.getURLColumn(), false, true);
            if (!m_settings.isUseConstantURL()) {
                m_urlColumnOption.setSelected(true);
                m_urlColumnOption.setEnabled(true);
                m_urlColumn.setEnabled(true);
            } else {
                int nrSelectableColumns = m_urlColumn.getNrItemsInList();
                m_urlColumnOption.setEnabled(nrSelectableColumns > 0);
                m_urlColumn.setEnabled(nrSelectableColumns > 0);
            }
        }
        ((SettingsModelString)m_invalidURLPolicySelector.getModel())
            .setStringValue(m_settings.getInvalidURLPolicy().name());
        m_useDelay.setSelected(m_settings.isUseDelay());
        m_delay.setValue(m_settings.getDelay());
        m_delay.setEnabled(m_useDelay.isSelected());
        m_concurrency.setValue(m_settings.getConcurrency());
        m_sslIgnoreHostnameMismatches.setSelected(m_settings.isSslIgnoreHostNameErrors());
        m_sslTrustAll.setSelected(m_settings.isSslTrustAll());

        m_connectionErrorPanel.setFailOnError(m_settings.isFailOnConnectionProblems());
        m_serverErrorPanel.setFailOnError(m_settings.isFailOnServerErrors());
        m_clientErrorPanel.setFailOnError(m_settings.isFailOnClientErrors());

        var delayPolicy = m_settings.getDelayPolicy();

        m_serverErrorPanel.setActive(delayPolicy.isRetriesEnabled());
        m_serverErrorPanel.setRetryDelay(delayPolicy.getRetryBase());
        m_serverErrorPanel.setNumRetries(delayPolicy.getMaxRetries());

        m_rateLimitPanel.setActive(delayPolicy.isCooldownEnabled());
        m_rateLimitPanel.setDelay(delayPolicy.getCooldown());

        m_settings.isOutputErrorCause().ifPresent(m_errorCausePanel::setSelected);

        m_followRedirects.setSelected(m_settings.isFollowRedirects());
        m_connectTimeoutInSeconds.setValue(m_settings.getConnectTimeoutInSeconds());
        m_readTimeoutInSeconds.setValue(m_settings.getReadTimeoutInSeconds());
        m_requestHeadersModel.clear();
        for (var i = 0; i < m_settings.getRequestHeaders().size(); ++i) {
            m_requestHeadersModel.addRow(m_settings.getRequestHeaders().get(i));
        }
        enableRequestHeaderChangeControls(!m_settings.getRequestHeaders().isEmpty());
        m_failOnMissingHeaderCheck.setSelected(m_settings.isFailOnMissingHeaders());
        m_extractAllHeaders.setSelected(m_settings.isExtractAllResponseFields());
        m_responseHeadersModel.clear();
        for (var i = 0; i < m_settings.getExtractFields().size(); ++i) {
            m_responseHeadersModel.addRow(m_settings.getExtractFields().get(i));
        }
        m_settings.isAllowChunking().ifPresent(m_allowChunking::setSelected);

        m_bodyColumnName.setSelectedString(m_settings.getResponseBodyColumn());

        if (!m_hasCredentialPort) {
            for (final EnablableUserConfiguration<UserConfiguration> euc : m_settings
                .getAuthorizationConfigurations()) {
                euc.getUserConfiguration().updateControls();
                JRadioButton radioButton = m_authenticationTabTitles.get(euc.getName());
                radioButton.setSelected(euc.isEnabled());
                radioButton.getAction().actionPerformed(null);
            }
        }

        updateResponseHeaderControls();

        // Proxy settings.
        m_proxyModeSelector.getButton(ProxyMode.fromSettings(settings).name()).doClick();
        m_settings.getCurrentProxyConfig().ifPresent(this::configurePanelsWithProxy);
        m_proxyAuthenticatorPanel.setEnabled(m_useAuthenticationChecker.isSelected());
    }

    private void configurePanelsWithProxy(final RestProxyConfig proxyConfig) {
        m_proxyProtocolCombo.setSelectedItem(proxyConfig.getProtocol());
        m_proxyHostPanel.updateHistory();
        m_proxyHostPanel.setSelectedString(proxyConfig.getProxyTarget().host());
        m_proxyPortPanel.updateHistory();
        m_proxyPortPanel.setSelectedString(String.valueOf(proxyConfig.getProxyTarget().port()));

        m_useAuthenticationChecker.setSelected(proxyConfig.isUseAuthentication());

        final var excludeHosts = proxyConfig.isExcludeHosts();
        m_useExcludeHostsChecker.setSelected(excludeHosts);
        proxyConfig.getExcludeHosts().ifPresent(s -> {
            m_proxyExcludeHostsPanel.updateHistory();
            m_proxyExcludeHostsPanel.setSelectedString(s);
        });
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
