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
 *   7. Febr. 2016. (Gabor Bakos): created
 */
package org.knime.rest.generic;

import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.config.base.ConfigBaseWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.CredentialsProvider;

/**
 * User configuration options on the dialog.
 *
 * @author Gabor Bakos
 */
public abstract class UserConfiguration {
    /**
     * @return Whether there is a user configuration.
     */
    public abstract boolean hasUserConfiguration();

    /**
     * Saves the user configuration to {@code userSettings}.
     * 
     * @param userSettings A {@link ConfigBaseWO} to save the configuration.
     */
    public abstract void saveUserConfiguration(NodeSettingsWO userSettings);

    /**
     * Loads the configuration in the model (probably for validation).
     *
     * @param userSettings A {@link ConfigBaseRO} to read the configuration from.
     * @throws InvalidSettingsException When the settings are not all valid.
     */
    public abstract void loadUserConfiguration(NodeSettingsRO userSettings) throws InvalidSettingsException;

    /**
     * Loads the configuration in the dialog.
     *
     * @param userSettings A {@link ConfigBaseRO} to read the configuration from.
     * @param specs The input {@link PortObjectSpec}s.
     * @param credentialNames The {@link CredentialsProvider} names.
     * @throws NotConfigurableException When there is no option to configure this extension.
     */
    public abstract void loadUserConfigurationForDialog(NodeSettingsRO userSettings, PortObjectSpec[] specs,
        final CredentialsProvider credentialNames) throws NotConfigurableException;

    /**
     * Adds the dialog controls to {@code panel}.
     * 
     * @param panel A {@link JPanel}.
     */
    public abstract void addControls(JPanel panel);

    //    /**
    //     * Enables the controls.
    //     */
    //    void enableControls();
    //    /**
    //     * Disables the controls.
    //     */
    //    void disableControls();
    /**
     * Updates the control values based on the settings.
     * 
     * @throws NotConfigurableException When cannot be configured for some reason.
     */
    public abstract void updateControls() throws NotConfigurableException;

    /**
     * Updates the settings based on the control values.
     */
    public abstract void updateSettings();
}
