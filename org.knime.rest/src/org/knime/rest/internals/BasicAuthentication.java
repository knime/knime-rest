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
 *   10. Febr. 2016. (Gabor Bakos): created
 */
package org.knime.rest.internals;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.ws.rs.client.Invocation.Builder;

import org.apache.cxf.common.util.Base64Utility;
import org.knime.core.data.DataRow;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.rest.generic.UsernamePasswordAuthentication;

/**
 * Basic authentication.
 *
 * @author Gabor Bakos
 */
public class BasicAuthentication extends UsernamePasswordAuthentication {
    /**
     * Constructs with the empty defaults. (This constructor is called for the automatic instantiation.)
     */
    public BasicAuthentication() {
        this("BASIC auth");
    }

    /**
     * Constructs the basic authentication with a custom config name.
     * @param configName String
     */
    public BasicAuthentication(final String configName) {
        super(configName, "", "", "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Builder updateRequest(final Builder request, final DataRow row, final CredentialsProvider credProvider,
        final Map<String, FlowVariable> flowVariables) {
        final String username = isUseCredentials() ? credProvider.get(getCredential()).getLogin() : getUsername();
        String password = isUseCredentials() ? credProvider.get(getCredential()).getPassword() : getPassword();
        if (password == null) {
            password = "";
        }
        try {
            request.header("Authorization",
                "Basic " + Base64Utility.encode((username + ":" + password).getBytes("UTF-8")));
        } catch (UnsupportedEncodingException ex) {
            // UTF-8 is supported for sure, but who knows...
            NodeLogger.getLogger(getClass()).error("Unsupported charset: " + ex.getMessage(), ex);
        }
        return request;
    }
}
