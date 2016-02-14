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
 *   2016. febr. 10. (Gabor Bakos): created
 */
package org.knime.rest.internals;

import java.awt.GridBagConstraints;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.ws.rs.client.Invocation.Builder;

import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.transport.http.auth.DigestAuthSupplier;
import org.knime.core.data.DataRow;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.rest.generic.EachRequestAuthentication;

/**
 *
 * @author Gabor Bakos
 */
public class DigestAuthentication extends UsernamePasswordAuthentication implements EachRequestAuthentication {
    private JLabel m_realmLabel = new JLabel("Realm:");

    private JTextField m_realm = new JTextField(22);

    /**
     *
     */
    public DigestAuthentication() {
        super("Digest auth");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Builder updateRequest(final Builder request, final DataRow row, final CredentialsProvider credProvider) {
        final AuthorizationPolicy authPolicy = new AuthorizationPolicy();
        authPolicy.setUserName(getUsername());
        authPolicy.setPassword(getPassword());
        request.header("Authorization",
            new DigestAuthSupplier().getAuthorization(authPolicy, null/*uri*/, null/*message*/, null/*fullHeader*/));
        return request;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addControls(final JPanel panel) {
        super.addControls(panel);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(m_realmLabel, gbc);
        gbc.gridx++;
        panel.add(m_realm, gbc);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disableControls() {
        super.disableControls();
        m_realmLabel.setEnabled(false);
        m_realm.setEnabled(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void enableControls() {
        super.enableControls();
        m_realmLabel.setEnabled(true);
        m_realm.setEnabled(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String id() {
        return "Digest";
    }

}
