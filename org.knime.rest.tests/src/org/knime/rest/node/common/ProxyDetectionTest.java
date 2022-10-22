package org.knime.rest.node.common;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.Invocation.Builder;

import org.apache.cxf.jaxrs.client.WebClient;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.rest.nodes.common.RestNodeModel;
import org.knime.rest.nodes.common.RestSettings;

/**
 * Test class for {@link RestNodeModel#setProxyCredentialsIfNeeded}.
 *
 * @author Leon Wenzler, KNIME AG, Konstanz, Germany
 */
public class ProxyDetectionTest {

	private enum PropertyMode {
		CLEAR, BLANK, DUMMY;
	}

	private static DummyRestNodeModel dummyInstance = null;

	@BeforeAll
	public static void initializeModel() {
		dummyInstance = new DummyRestNodeModel();
	}

	/**
	 * Tests if a proxy is successfully detected as non-present if the
	 * System.properties are non-existent.
	 */
	@Test
	void detectNoProxyNonPresent() {
		// assumption: a proxy is detected via a the "proxySet" property and a valid
		// host
		var props = new String[] { "http.proxyHost", "http.proxySet" };
		var values = saveAndSetProperties(props, PropertyMode.CLEAR);
		var builder = dummyInstance.detectProxy();
		assertThat("Proxy host must not be set in the client.", null,
				is(WebClient.getConfig(builder).getHttpConduit().getClient().getProxyServer()));
		restoreProperties(props, values);
	}

	/**
	 * Tests if a proxy is successfully detected as non-present if the
	 * System.properties for proxies exist but are blank.
	 */
	@Test
	void detectNoProxyBlank() {
		// assumption: a proxy is detected via a the "proxySet" property and a valid
		// host
		var props = new String[] { "http.proxyHost", "http.proxySet" };
		var values = saveAndSetProperties(props, PropertyMode.BLANK);
		var builder = dummyInstance.detectProxy();
		assertThat("Proxy host must not be set in the client.", null,
				is(WebClient.getConfig(builder).getHttpConduit().getClient().getProxyServer()));
		restoreProperties(props, values);
	}

	/**
	 * Tests if a proxy is detected correctly, on the premise that System.properties
	 * are set correctly.
	 */
	@Test
	void detectProxyPresent() {
		// setting credentials will only be done if a user/pw was specified
		var props = new String[] { "http.proxyUser", "http.proxyPassword", "http.proxyHost", "http.proxyPort",
				"http.proxySet" };
		var values = saveAndSetProperties(props, PropertyMode.DUMMY);
		assertThrows(ProcessingException.class, () -> dummyInstance.detectProxy(),
				"Proxy detection should have thrown an error message because authentication is needed although synchronous client is used.");
		restoreProperties(props, values);
	}

	@AfterAll
	public static void discardModel() {
		dummyInstance = null;
	}

	private String[] saveAndSetProperties(String[] properties, PropertyMode mode) {
		var values = new String[properties.length];
		for (int i = 0; i < properties.length; i++) {
			var p = properties[i];
			values[i] = System.getProperty(p);
			switch (mode) {
			case DUMMY:
				// any dummy value, here "true" is only useful for "proxySet"
				System.setProperty(p, "true");
				break;
			case BLANK:
				System.setProperty(p, "");
			case CLEAR:
				System.clearProperty(p);
			}
		}
		return values;
	}

	private void restoreProperties(String[] properties, String[] values) {
		CheckUtils.checkArgument(properties.length == values.length, "Argument arrays must have the same length!");
		for (int i = 0; i < properties.length; i++) {
			var value = values[i];
			if (value != null) {
				System.setProperty(properties[i], value);
			} else {
				System.clearProperty(properties[i]);
			}
		}
	}

	/**
	 * Helper class for enabling access to the protected proxy method.
	 * 
	 * @author leon.wenzler
	 */
	private static class DummyRestNodeModel extends RestNodeModel<RestSettings> {

		@Override
		protected RestSettings createSettings() {
			return new RestSettings() {
			};
		}

		@Override
		protected Invocation invocation(Builder request, DataRow row, DataTableSpec spec) {
			return null;
		}

		/**
		 * Making access to {@link RestNodeModel#setProxyCredentialsIfNeeded} public and
		 * supplying with a dummy builder.
		 */
		public Builder detectProxy() {
			var webTarget = ClientBuilder.newBuilder().build().target("http://localhost");
			var config = webTarget;
			var builder = webTarget.property(getWarningMessage(), dummyInstance).request();
			super.setProxyCredentialsIfNeeded(builder);
			return builder;
		}
	}
}
