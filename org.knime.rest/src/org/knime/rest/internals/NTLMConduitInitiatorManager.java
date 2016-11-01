package org.knime.rest.internals;

import javax.ws.rs.client.Invocation.Builder;

import org.apache.cxf.BusException;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.cxf.transport.http.asyncclient.AsyncHttpTransportFactory;

/**
 * Custom ConduitInitiatorManager that uses an asynchronous HTTP transport if NTLM authentication should be used (for
 * the current thread).
 *
 * @author Luke Bullard, Eli Lilly, UK
 */
class NTLMConduitInitiatorManager implements ConduitInitiatorManager {
	/**	The Singleton **/
	private static final NTLMConduitInitiatorManager INSTANCE = new NTLMConduitInitiatorManager();

	private final ThreadLocal<Boolean> m_isNtlm = new ThreadLocal<Boolean>();

	/**
	 * Returns the singleton instance for this class.
	 *
	 * @return the singleton instance
	 */
	public static NTLMConduitInitiatorManager getInstance() {
		return INSTANCE;
	}

	/**
	 * Private constructor for singleton pattern.
	 */
	private NTLMConduitInitiatorManager() {
		// Do nothing
	}

	/**
	 * Configures the given request for NTLM authentication
	 *
	 * @param request a request, must not be <code>null</code>
	 */
	void configureForNTLM(final Builder request) {
	    m_isNtlm.set(Boolean.TRUE);
	    try {
	        WebClient.getConfig(request).getHttpConduit();
	    } finally {
	        m_isNtlm.remove();
	    }
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void registerConduitInitiator(final String name, final ConduitInitiator factory) {
		// Do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void deregisterConduitInitiator(final String name) {
		// Do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ConduitInitiator getConduitInitiator(final String name) throws BusException {
		return getConduitInitiatorForUri(null);
	}

    /**
     * Depending on the the value of isNtlm we will return either a standard {@link HTTPTransportFactory} or
     * {@link AsyncHttpTransportFactory}.
     */
	@Override
	public ConduitInitiator getConduitInitiatorForUri(final String uri) {
		Boolean b = m_isNtlm.get();
		if (b == Boolean.TRUE) {
			return new AsyncHttpTransportFactory();
		}
		return new HTTPTransportFactory();
	}
}
