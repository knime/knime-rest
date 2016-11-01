package org.knime.rest.internals;

import java.net.InetAddress;
import java.util.Map;

import javax.ws.rs.client.Invocation.Builder;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.knime.core.data.DataRow;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.ICredentials;
import org.knime.rest.generic.UsernamePasswordAuthentication;

/**
 * NTLM Authentication.
 *
 * @author Luke Bullard, Eli Lilly, UK
 */
public class NTLMAuthentication extends UsernamePasswordAuthentication {
    private static final String HOST;

    static {
        String hostName = "host";
        try {
            hostName = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (Exception ex) {
            // Do nothing
        }
        HOST = hostName;
    }

    /**
     * Constructs with the empty defaults. (This constructor is called for the automatic instantiation.)
     */
    public NTLMAuthentication() {
        super("NTLM auth", "", "", "");

        Bus bus = BusFactory.getThreadDefaultBus();
        bus.setExtension(NTLMConduitInitiatorManager.getInstance(), ConduitInitiatorManager.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Builder updateRequest(final Builder request, final DataRow row, final CredentialsProvider credProvider,
        final Map<String, FlowVariable> flowVariables) {
        NTLMConduitInitiatorManager.getInstance().configureForNTLM(request);
        ClientConfiguration conf = WebClient.getConfig(request);

        conf.getHttpConduit().setAuthorization(null);
        conf.getRequestContext().put("use.async.http.conduit", Boolean.TRUE);
        conf.getRequestContext().put(Credentials.class.getName(), getNTCredentials(credProvider));

        HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();
        httpClientPolicy.setAllowChunking(false);
        httpClientPolicy.setAutoRedirect(true);
        httpClientPolicy.setMaxRetransmits(1);

        conf.getHttpConduit().setClient(httpClientPolicy);


        return request;
    }

    /**
     * Gets a Credentials object populated with either the values from the {@link ICredentials} object or the typed in
     * username and password fields.
     *
     * @param credProvider the {@link org.apache.http.client.CredentialsProvider} to use
     * @return a credentials object to be used in the NTLM call
     */
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

        return new NTCredentials(username, password, HOST, "");
    }
}
