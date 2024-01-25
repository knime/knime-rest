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
 *   Jun 15, 2023 (Alexander Bondaletov, Redfield SE): created
 */
package org.knime.rest.internals;

import java.io.IOException;
import java.util.Map;

import javax.swing.JPanel;

import org.knime.core.data.DataRow;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.credentials.base.oauth.api.HttpAuthorizationHeaderCredentialValue;
import org.knime.rest.generic.EachRequestAuthentication;

import jakarta.ws.rs.client.Invocation.Builder;

/**
 * {@link EachRequestAuthentication} implementation that uses {@link HttpAuthorizationHeaderCredentialValue} credential
 * from the credential port.
 *
 * @author Alexander Bondaletov, Redfield SE
 */
public class HttpAuthorizationHeaderAuthentication extends EachRequestAuthentication {

    private final HttpAuthorizationHeaderCredentialValue m_credential;

    /**
     * @param credential The credential.
     *
     */
    public HttpAuthorizationHeaderAuthentication(final HttpAuthorizationHeaderCredentialValue credential) {
        m_credential = credential;
    }

    @Override
    public Builder updateRequest(final Builder request, final DataRow row, final CredentialsProvider credProvider,
        final Map<String, FlowVariable> flowVariables) {

        try {
            request.header("Authorization", m_credential.getAuthScheme() + " " + m_credential.getAuthParameters());
        } catch (IOException e) {
            NodeLogger.getLogger(getClass()).error(e.getMessage(), e);
        }

        return request;
    }

    @Override
    public boolean hasUserConfiguration() {
        return false;
    }

    @Override
    public void saveUserConfiguration(final NodeSettingsWO userSettings) {
        //no user configuration
    }

    @Override
    public void loadUserConfiguration(final NodeSettingsRO userSettings) throws InvalidSettingsException {
        //no user configuration
    }

    @Override
    public void loadUserConfigurationForDialog(final NodeSettingsRO userSettings, final PortObjectSpec[] specs,
        final CredentialsProvider credentialNames) throws NotConfigurableException {
        //no user configuration
    }

    @Override
    public void clearUserConfiguration() {
        //no user configuration
    }

    @Override
    public void addControls(final JPanel panel) {
        //no user configuration
    }

    @Override
    public void updateControls() throws NotConfigurableException {
        //no user configuration
    }

    @Override
    public void updateSettings() {
        //no user configuration
    }

}
