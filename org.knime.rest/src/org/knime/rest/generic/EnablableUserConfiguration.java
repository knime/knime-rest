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
 *   14. Febr. 2016. (Gabor Bakos): created
 */
package org.knime.rest.generic;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.config.base.ConfigBaseWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.CredentialsProvider;

/**
 * A basic {@link UserConfiguration} wrapper that allows enabling or disabling it.
 *
 * @author Gabor Bakos
 * @param <T> the specific {@link UserConfiguration} type
 */
public class EnablableUserConfiguration<T extends UserConfiguration> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(EnablableUserConfiguration.class);

    private final T m_userConfiguration;

    private boolean m_enabled;

    private final String m_name;

    /**
     * @param type The wrapped {@link UserConfiguration}.
     * @param name The name of the configuration.
     */
    public EnablableUserConfiguration(final T type, final String name) {
        m_userConfiguration = type;
        m_name = name;
    }

    /**
     * @return the enabled
     */
    public boolean isEnabled() {
        return m_enabled;
    }

    /**
     * @param enabled the enabled to set
     */
    public void setEnabled(final boolean enabled) {
        m_enabled = enabled;
    }

    /**
     * @return the userConfiguration
     */
    public T getUserConfiguration() {
        return m_userConfiguration;
    }

    /**
     * @return the name
     */
    public String getName() {
        return m_name;
    }

    @Override
    public String toString() {
        return m_userConfiguration + " - " + m_name + " (" + m_enabled + ")";
    }

    /**
     * Saves the user configuration to {@code userSettings}.
     * If the configuration is disabled, clears the current values prior to saving in order to not save unnecessary
     * confidential information.
     *
     * @param userSettings A {@link ConfigBaseWO} to save the configuration.
     * @since 5.3
     */
    public void saveUserConfiguration(final NodeSettingsWO userSettings) {
        if (m_userConfiguration != null && m_userConfiguration.hasUserConfiguration()) {
            if (!isEnabled()) {
                // don't save and persist credentials if they are not selected
                // see ticket AP-21887
                m_userConfiguration.clearUserConfiguration();
            }
            m_userConfiguration.saveUserConfiguration(userSettings);
        }
    }

    /**
     * Loads the configuration in the model (e.g. for validation).
     * The exception is only thrown if the configuration is enabled.
     *
     * @param settings main settings object
     * @throws InvalidSettingsException
     * @since 5.3
     */
    public void loadUserConfigurationFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (m_userConfiguration == null || !m_userConfiguration.hasUserConfiguration()) {
            return;
        }
        try {
            final var userSettings = settings.getNodeSettings(getName());
            m_userConfiguration.loadUserConfiguration(userSettings);
        } catch (final InvalidSettingsException e) {
            if (isEnabled()) {
                throw e;
            }
            LOGGER.debug("Could not load (disabled) user configuration: " + e.getMessage(), e);
        }
    }

    /**
     * Loads the stored user configuration from the settings for the dialog.
     *
     * @param settings a {@link ConfigBaseRO} to read the configuration from
     * @param specs the input {@link PortObjectSpec}s
     * @param credentialNames the {@link CredentialsProvider} names
     * @throws NotConfigurableException when there is no option to configure this extension
     * @since 5.3
     */
    public void loadUserConfigurationForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs,
        final CredentialsProvider credentialNames) throws NotConfigurableException {
        if (m_userConfiguration == null || !m_userConfiguration.hasUserConfiguration()) {
            return;
        }
        try {
            final var userSettings = settings.getNodeSettings(getName());
            m_userConfiguration.loadUserConfigurationForDialog(userSettings, specs, credentialNames);
        } catch (final InvalidSettingsException ignored) { // NOSONAR
            m_userConfiguration.loadUserConfigurationForDialog(null, specs, credentialNames);
        }
    }
}
