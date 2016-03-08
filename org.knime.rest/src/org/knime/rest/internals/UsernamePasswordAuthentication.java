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
 *   2016. febr. 14. (Gabor Bakos): created
 */
package org.knime.rest.internals;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.config.base.ConfigBaseWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.rest.generic.UserConfiguration;

/**
 *
 * @author Gabor Bakos
 */
public abstract class UsernamePasswordAuthentication implements UserConfiguration {
    private static final String USERNAME = "username";

    private static final String PASSWORD = "password";
    private static final String USE_CREDENTIALS = "useCredentials";
    private static final String CREDENTIAL = "credential";

    private String m_username, m_password, m_credential;
    private boolean m_useCredentials;

    private final String m_encryptionKey;

    private final JTextField m_usernameControl = new JTextField(35);

    private final JPasswordField m_passwordControl = new JPasswordField(25);

    private final JComboBox<String> m_credentialsSelection = new JComboBox<>();

    @SuppressWarnings("serial")
    private final JCheckBox m_useCredentialsCheckBox = new JCheckBox(new AbstractAction("Use credentials") {
        @Override
        public void actionPerformed(final ActionEvent e) {
            m_usernameControl.setEnabled(!m_useCredentialsCheckBox.isSelected());
            m_passwordControl.setEnabled(!m_useCredentialsCheckBox.isSelected());
            m_credentialsSelection.setEnabled(m_useCredentialsCheckBox.isSelected());
        }
    });

    private final JLabel m_userLabel = new JLabel("User"), m_passwordLabel = new JLabel("Password"),
            m_credentialLabel = new JLabel("Credential");

    /**
     * Constructs a UsernamePasswordAuthentication using {@code encryptionKey}.
     *
     * @param encryptionKey The encryption key used to store in the settings.
     */
    protected UsernamePasswordAuthentication(final String encryptionKey) {
        super();
        m_encryptionKey = encryptionKey;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasUserConfiguration() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveUserConfiguration(final ConfigBaseWO userSettings) {
        userSettings.addString(USERNAME, getUsername());
        userSettings.addPassword(PASSWORD, m_encryptionKey, getPassword());
        userSettings.addBoolean(USE_CREDENTIALS, isUseCredentials());
        userSettings.addString(CREDENTIAL, getCredential());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadUserConfiguration(final ConfigBaseRO userSettings) throws InvalidSettingsException {
        setUsername(userSettings.getString(USERNAME));
        setPassword(userSettings.getPassword(PASSWORD, m_encryptionKey));
        setUseCredentials(userSettings.getBoolean(USE_CREDENTIALS));
        setCredential(userSettings.getString(CREDENTIAL));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadUserConfigurationForDialog(final ConfigBaseRO userSettings, final PortObjectSpec[] specs,
        final Collection<String> credentialNames) {
        setUsername(userSettings == null ? "" : userSettings.getString(USERNAME, ""));
        setPassword(userSettings == null ? "" : userSettings.getPassword(PASSWORD, m_encryptionKey, ""));
        setUseCredentials(userSettings == null ? false: userSettings.getBoolean(USE_CREDENTIALS, false));
        setCredential(userSettings == null ? null : userSettings.getString(CREDENTIAL, null));
        m_credentialsSelection.removeAllItems();
        for (String credential : credentialNames) {
            m_credentialsSelection.addItem(credential);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addControls(final JPanel panel) {
        panel.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(m_userLabel, gbc);
        gbc.gridx++;
        panel.add(m_usernameControl, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(m_passwordLabel, gbc);
        gbc.gridx++;
        panel.add(m_passwordControl, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(m_useCredentialsCheckBox, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(m_credentialLabel, gbc);
        gbc.gridx++;
        panel.add(m_credentialsSelection, gbc);
        m_useCredentialsCheckBox.getAction().actionPerformed(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void enableControls() {
        m_userLabel.setEnabled(!m_useCredentialsCheckBox.isSelected());
        m_usernameControl.setEnabled(!m_useCredentialsCheckBox.isSelected());
        m_passwordLabel.setEnabled(!m_useCredentialsCheckBox.isSelected());
        m_passwordControl.setEnabled(!m_useCredentialsCheckBox.isSelected());
        m_useCredentialsCheckBox.setEnabled(true);
        m_credentialLabel.setEnabled(m_useCredentialsCheckBox.isSelected());
        m_credentialsSelection.setEnabled(m_useCredentialsCheckBox.isSelected());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disableControls() {
        m_userLabel.setEnabled(false);
        m_usernameControl.setEnabled(false);
        m_passwordLabel.setEnabled(false);
        m_passwordControl.setEnabled(false);
        m_useCredentialsCheckBox.setEnabled(false);
        m_credentialLabel.setEnabled(false);
        m_credentialsSelection.setEnabled(false);
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return m_username;
    }

    /**
     * @param username the username to set
     */
    public void setUsername(final String username) {
        m_username = username;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return m_password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(final String password) {
        m_password = password;
    }

    /**
     * @return the credential
     */
    public String getCredential() {
        return m_credential;
    }

    /**
     * @param credential the credential to set
     */
    public void setCredential(final String credential) {
        m_credential = credential;
    }

    /**
     * @return the useCredentials
     */
    public boolean isUseCredentials() {
        return m_useCredentials;
    }

    /**
     * @param useCredentials the useCredentials to set
     */
    public void setUseCredentials(final boolean useCredentials) {
        m_useCredentials = useCredentials;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return id() + "[" + getUsername() + "]";
    }
}