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
 *   14. Feb. 2016. (Gabor Bakos): created
 */
package org.knime.rest.generic;

import java.util.Objects;

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
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.CredentialsProvider;

/**
 * Abstract implementation that can be used to extend the authentication functionality of the REST nodes.
 *
 * @author Gabor Bakos
 * @since 3.3
 */
public abstract class UsernamePasswordAuthentication extends EachRequestAuthentication {
    private final SettingsModelAuthentication m_settings;

    private DialogComponentAuthentication m_controls;

    private CredentialsProvider m_lastCredentialsProvider;

    /**
     * Constructs a UsernamePasswordAuthentication using the default settings.
     *
     * @param configName The config name.
     * @param defaultUsername The default user name.
     * @param defaultPassword The default password.
     * @param defaultCredential The default credentials key.
     */
    protected UsernamePasswordAuthentication(final String configName, final String defaultUsername,
        final String defaultPassword, final String defaultCredential) {
        super();
        m_settings = new SettingsModelAuthentication(configName, AuthenticationType.USER_PWD, defaultUsername,
            defaultPassword, defaultCredential);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadUserConfiguration(final NodeSettingsRO userSettings) throws InvalidSettingsException {
        m_settings.loadSettingsFrom(userSettings);
    }

    /**
     * {@inheritDoc}
     */
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
            m_controls = new DialogComponentAuthentication(m_settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateControls() {
        var nodeSettings = new NodeSettings("");
        m_settings.saveSettingsTo(nodeSettings);
        if (m_controls != null) {
            try {
                m_controls.loadSettingsFrom(nodeSettings, null, m_lastCredentialsProvider);
            } catch (NotConfigurableException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addControls(final JPanel panel) {
        initControls();
        panel.add(m_controls.getComponentPanel());
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
     * @return the username
     */
    public String getUsername() {
        return m_settings.getUsername();
    }

    /**
     * @param username the username to set
     */
    public void setUsername(final String username) {
        m_settings.setValues(AuthenticationType.USER_PWD, m_settings.getCredential(), username,
            m_settings.getPassword());
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return m_settings.getPassword();
    }

    /**
     * @param password the password to set
     */
    public void setPassword(final String password) {
        m_settings.setValues(AuthenticationType.USER_PWD, m_settings.getCredential(), m_settings.getUsername(),
            password);
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
    public void setUseCredentials(final boolean useCredentials) {
        m_settings.setValues(useCredentials ? AuthenticationType.CREDENTIALS : AuthenticationType.USER_PWD,
            m_settings.getCredential(), m_settings.getUsername(), m_settings.getPassword());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getClass().getName() + "[" + getUsername() + "]";
    }

    /**
     * Pair holding only a user name and a password.
     *
     * @param username the user name as String
     * @param password the password as String
     */
    protected record UsernamePasswordPair(String username, String password) {
        /**
         * Creates an authentication pair that takes into account credentials flow variables, and guarantees that both
         * values are guaranteed to be non-null, and the user name is non-empty.
         *
         * @param authentication user/pw authentication
         * @param provider credentials provider for flow variables
         * @return validated pair of user name and password
         * @throws InvalidSettingsException if user name is empty
         */
        public static UsernamePasswordPair of(final UsernamePasswordAuthentication authentication,
            final CredentialsProvider provider) throws InvalidSettingsException {
            final var credentialsName = authentication.getCredential();
            var user = authentication.getUsername();
            var passwd = authentication.getPassword();
            if (authentication.isUseCredentials() && StringUtils.isNotEmpty(credentialsName)) {
                CheckUtils.checkNotNull(provider);
                final var credentials = provider.get(credentialsName);
                user = credentials.getLogin();
                passwd = credentials.getPassword();
            }
            // Check user name and fail if empty
            if (StringUtils.isEmpty(user)) {
                throw new InvalidSettingsException("User name cannot be empty");
            }
            return new UsernamePasswordPair(user, Objects.requireNonNullElse(passwd, ""));
        }
    }
}
