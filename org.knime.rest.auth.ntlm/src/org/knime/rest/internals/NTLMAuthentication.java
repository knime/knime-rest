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
 *   10. Feb. 2016. (Gabor Bakos): created
 */
package org.knime.rest.internals;

import java.util.Map;

import javax.ws.rs.client.Invocation.Builder;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPConduitFactory;
import org.apache.cxf.transport.http.asyncclient.AsyncHTTPConduitFactory;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.knime.core.data.DataRow;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.ICredentials;
import org.knime.rest.generic.UsernamePasswordAuthentication;

/**
 * NTLM authentication. Most of the logic was discovered trial and error and using a debugger. There doesn't seem to be
 * a lot of documentation on NTLM authentication in CXF (in comparison to Kerberos, where there is a lot...).
 *
 * It seems it's absolutely essential that the http client is in async mode, which is exactly not what it is set to
 * in the RestNodeModel class.
 */
public class NTLMAuthentication extends UsernamePasswordAuthentication {
    /**
     * Constructs with the empty defaults. (This constructor is called for the automatic instantiation.)
     */
    public NTLMAuthentication() {
        super("NTLM-auth", "", "", "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Builder updateRequest(final Builder request, final DataRow row, final CredentialsProvider credProvider,
        final Map<String, FlowVariable> flowVariables) {
        final ClientConfiguration conf = WebClient.getConfig(request);
        final Map<String, Object> requestContext = conf.getRequestContext();
        conf.getBus().setExtension(new AsyncHTTPConduitFactory(conf.getBus()), HTTPConduitFactory.class);

        requestContext.put("use.async.http.conduit", Boolean.TRUE);
        requestContext.put(Credentials.class.getName(), getNTCredentials(credProvider));

        // this must happen _after_ the 'bus' is set to use an async conduit above
        // (and you would think that I know what a 'bus' or a 'conduit' is ... but I don't know)
        final HTTPConduit httpConduit = conf.getHttpConduit();

        // the more you read about CXF and NTLM/Kerberos... the more you are convinced this line is needed but it's not:
        // SpnegoAuthSupplier supplier = new SpnegoAuthSupplier();
        // httpConduit.setAuthSupplier(supplier);
        HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();
        httpClientPolicy.setConnectionTimeout(36000);
        httpClientPolicy.setAllowChunking(false);
        httpConduit.setAuthorization(null);

        httpClientPolicy.setAutoRedirect(true);

        httpConduit.setClient(httpClientPolicy);
        return request;
    }

    private Credentials getNTCredentials(final CredentialsProvider credProvider) {
        if (credProvider == null) {
            throw new IllegalArgumentException("No credentials provider provided");
        }

        String credentialsName = getCredential();
        String username = getUsername();
        String password = getPassword();

        if (!StringUtils.isEmpty(credentialsName)) {
            try {
                ICredentials cred = credProvider.get(credentialsName);
                username = cred.getLogin();
                password = cred.getPassword();
            } catch (IllegalArgumentException ex) {
                throw new IllegalStateException("Missing credentials for " + credentialsName);
            }
        }

        //  Check username & password
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            throw new IllegalStateException("Username or Password cannot be blank !");
        }
        return new NTCredentials(username, password, ObjectUtils.defaultIfNull(KNIMEConstants.getHostname(), "host"),
            "");
    }
}
