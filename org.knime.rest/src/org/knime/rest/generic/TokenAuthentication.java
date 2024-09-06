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
 *   Sep 6, 2024 (lw): created
 */
package org.knime.rest.generic;

import java.awt.Container;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentAuthentication;
import org.knime.core.node.defaultnodesettings.SettingsModelAuthentication;
import org.knime.core.node.defaultnodesettings.SettingsModelAuthentication.AuthenticationType;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.CredentialsProvider;

/**
 * Simplified copy of {@link UsernamePasswordAuthentication}, only supporting direct token
 * inputs or tokens supplied via credentials flow variables.
 *
 * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
 * @since 5.4
 */
public abstract class TokenAuthentication extends EachRequestAuthentication {
    private final SettingsModelAuthentication m_settings;

    private DialogComponentAuthentication m_controls;

    private CredentialsProvider m_lastCredentialsProvider;

    /**
     * Constructs a TokenAuthentication using the default settings.
     *
     * @param configName The config name.
     */
    protected TokenAuthentication(final String configName) {
        super();
        m_settings = new SettingsModelAuthentication(configName, AuthenticationType.PWD);
    }

    private static void replacePasswordTexts(final Container container) {
        for (var component : container.getComponents()) {
            if (component instanceof JLabel l) {
                l.setText(StringUtils.replace(l.getText(), "Password", "Token"));
            }
            if (component instanceof AbstractButton b) {
                b.setText(StringUtils.replace(b.getText(), "Password", "Token"));
            }
            if (component instanceof Container c) {
                replacePasswordTexts(c);
            }
        }
    }

    @Override
    public boolean hasUserConfiguration() {
        return true;
    }

    @Override
    public void saveUserConfiguration(final NodeSettingsWO userSettings) {
        m_settings.saveSettingsTo(userSettings);
    }

    /**
     * Updates the settings based on the control values (if controls were set, i.e. it was added to a panel).
     */
    @Override
    public void updateSettings() {
        if (m_controls != null) {
            try {
                var nodeSettings = new NodeSettings("");
                m_controls.saveSettingsTo(nodeSettings);
                m_settings.loadSettingsFrom(nodeSettings);
            } catch (InvalidSettingsException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @Override
    public void loadUserConfiguration(final NodeSettingsRO userSettings) throws InvalidSettingsException {
        m_settings.loadSettingsFrom(userSettings);
    }

    @Override
    public void loadUserConfigurationForDialog(final NodeSettingsRO userSettings, final PortObjectSpec[] specs,
        final CredentialsProvider credentialNames) throws NotConfigurableException {
        initControls();
        m_controls.loadSettingsFrom(userSettings, specs, credentialNames);
        m_lastCredentialsProvider = credentialNames;
    }

    @Override
    public void clearUserConfiguration() {
        m_settings.clear();
    }

    /**
     * Creates the controls with DialogComponentAuthentication.
     */
    private void initControls() {
        if (m_controls == null) {
            m_controls = new DialogComponentAuthentication(m_settings, null, //
                AuthenticationType.CREDENTIALS, AuthenticationType.PWD);
        }
    }

    @Override
    public void updateControls() {
        final var nodeSettings = new NodeSettings("");
        m_settings.saveSettingsTo(nodeSettings);
        if (m_controls != null) {
            try {
                m_controls.loadSettingsFrom(nodeSettings, null, m_lastCredentialsProvider);
            } catch (NotConfigurableException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @Override
    public void addControls(final JPanel panel) {
        initControls();
        panel.add(m_controls.getComponentPanel());
        replacePasswordTexts(panel);
    }

    /**
     * Enables or disables the underlying {@link DialogComponentAuthentication} panel.
     *
     * @param enabled Should the panel be enabled?
     */
    public void setEnabled(final boolean enabled) {
        if (m_controls != null) {
            m_controls.getModel().setEnabled(enabled);
        }
    }

    /**
     * @return the token
     */
    public String getToken() {
        return m_settings.getPassword();
    }

    /**
     * @param token the token to set
     */
    public void setToken(final String token) {
        m_settings.setValues(AuthenticationType.PWD, m_settings.getCredential(), m_settings.getUsername(),
            token);
    }

    /**
     * @return the credential
     */
    public String getCredential() {
        return m_settings.getCredential();
    }

    /**
     * @param credential the credential to set
     */
    public void setCredential(final String credential) {
        m_settings.setValues(AuthenticationType.CREDENTIALS, credential, m_settings.getUsername(),
            m_settings.getPassword());
    }

    /**
     * @return the useCredentials
     */
    public boolean isUseCredentials() {
        return m_settings.getAuthenticationType() == AuthenticationType.CREDENTIALS;
    }

    /**
     * @param useCredentials the useCredentials to set
     */
    public void setUseCredentials(final boolean useCredentials) { // NOSONAR
        m_settings.setValues(useCredentials ? AuthenticationType.CREDENTIALS : AuthenticationType.PWD,
            m_settings.getCredential(), m_settings.getUsername(), m_settings.getPassword());
    }
}
