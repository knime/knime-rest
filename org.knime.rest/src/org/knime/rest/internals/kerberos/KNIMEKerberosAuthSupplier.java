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
 *   Apr 25, 2019 (bjoern): created
 */
package org.knime.rest.internals.kerberos;

import java.net.URI;

import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.auth.AbstractSpnegoAuthSupplier;
import org.apache.cxf.transport.http.auth.HttpAuthHeader;
import org.apache.cxf.transport.http.auth.HttpAuthSupplier;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;
import org.knime.kerberos.api.KerberosDelegationProvider;
import org.knime.kerberos.api.KerberosDelegationProvider.KerberosDelegationCallback;

/**
 *{@link HttpAuthSupplier} implementation that uses the {@link KerberosDelegationProvider} to get the proper
 * service ticket and also supports user delegation if executed on the KNIME Server.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 * @author Tobias Koetter, KNIME GmbH
 */
public class KNIMEKerberosAuthSupplier extends AbstractSpnegoAuthSupplier implements HttpAuthSupplier {

    /**
     * Can be set on the client properties. If set to true then the kerberos oid is used instead of the default spnego
     * OID.
     */
    private static final String PROPERTY_USE_KERBEROS_OID = "auth.spnego.useKerberosOid";

    private static final String KERBEROS_OID = "1.2.840.113554.1.2.2";

    private static final String SPNEGO_OID = "1.3.6.1.5.5.2";

    @Override
    public boolean requiresRequestCaching() {
        return false;
    }

    @Override
    public String getAuthorization(final AuthorizationPolicy authPolicy, final URI currentURI, final Message message,
        final String fullHeader) {

        if (!HttpAuthHeader.AUTH_TYPE_NEGOTIATE.equals(authPolicy.getAuthorizationType())) {
            return null;
        }
        try {
            final String spn = getCompleteServicePrincipalName(currentURI);

            final boolean useKerberosOid =
                    PropertyUtils.isTrue(message.getContextualProperty(PROPERTY_USE_KERBEROS_OID));
            final Oid oid = new Oid(useKerberosOid ? KERBEROS_OID : SPNEGO_OID);

            final byte[] token = getToken(spn, oid);
            return HttpAuthHeader.AUTH_TYPE_NEGOTIATE + " " + Base64Utility.encode(token);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static byte[] getToken(final String spn, final Oid oid) throws Exception {

        return KerberosDelegationProvider.doWithConstrainedDelegationBlocking(new KerberosDelegationCallback<byte[]>() {
            @Override
            public byte[] doAuthenticated(final GSSCredential credential) throws Exception {

                final GSSManager manager = GSSManager.getInstance();
                final GSSName serverName = manager.createName(spn, null);

                final GSSContext context =
                    manager.createContext(serverName.canonicalize(oid), oid, credential, GSSContext.DEFAULT_LIFETIME);

                return context.initSecContext(new byte[0], 0, 0);
            }
        }, null);
    }
}
