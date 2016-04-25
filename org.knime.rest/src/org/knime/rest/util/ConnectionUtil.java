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
 *   24. Jan. 2016. (Gabor Bakos): created
 */
package org.knime.rest.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.knime.core.node.NodeLogger;
import org.osgi.framework.Bundle;

/**
 * Utilities for {@link HttpURLConnection} handling.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @author Gabor Bakos
 * @since 3.2
 */
public class ConnectionUtil {

    /**
     *
     */
    private ConnectionUtil() {
        //Prevent instantiation.
    }

    /**
     * Checks the connection and throws {@link IOException} in case it is not possible to connect.
     *
     * @param bundle The bundle of this class.
     * @param logger The {@link NodeLogger} used to log problems.
     * @param url The url to test.
     * @param connectionTimeout The timeout (to connect) in milliseconds.
     * @throws IOException When opening the connection is failed.
     */
    public static void checkConnection(final Bundle bundle/*FrameworkUtil.getBundle(getClass())*/,
        final NodeLogger logger, final URL url, final int connectionTimeout) throws IOException {
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        if (conn instanceof HttpsURLConnection) {
            ((HttpsURLConnection)conn).setHostnameVerifier((hostname, session) -> true);

            URL keystoreUrl = FileLocator.find(bundle, new Path("knime-keystore.jks"), null);
            if (keystoreUrl != null) {
                try {
                    keystoreUrl = FileLocator.toFileURL(keystoreUrl);
                    KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                    try (InputStream is = keystoreUrl.openStream()) {
                        keyStore.load(is, "knimeknime".toCharArray());
                    }
                    TrustManagerFactory localTrustManager =
                        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                    localTrustManager.init(keyStore);

                    TrustManagerFactory publicTrustManager =
                        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                    publicTrustManager.init((KeyStore)null);

                    DelegatingX509TrustManager delegator =
                        new DelegatingX509TrustManager(publicTrustManager, localTrustManager);

                    SSLContext ctx = SSLContext.getInstance("TLS");
                    ctx.init(null, new TrustManager[]{delegator}, null);
                    SSLSocketFactory sslFactory = ctx.getSocketFactory();
                    ((HttpsURLConnection)conn).setSSLSocketFactory(sslFactory);
                } catch (IOException | KeyStoreException | NoSuchAlgorithmException | KeyManagementException
                        | CertificateException ex) {
                    logger.warn(
                        "Could not set up SSL connection parameters, connection to server may fail: " + ex.getMessage(),
                        ex);
                }
            }
        }

        conn.setConnectTimeout(connectionTimeout);
        conn.connect();
        conn.disconnect();
    }

}
