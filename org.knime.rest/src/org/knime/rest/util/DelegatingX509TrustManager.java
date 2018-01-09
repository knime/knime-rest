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
 *   24. Jan. 2016. (Gabor Bakos): created
 */
package org.knime.rest.util;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Trust manager that delegates checks to several real trust managers. All delegates are tried until a positive answer
 * is returned or there are not more delegates to try.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
public class DelegatingX509TrustManager implements X509TrustManager {
    private final List<X509TrustManager> m_delegates;

    /**
     * Creates a new delegating trust manager.
     *
     * @param delegates a list of trust managers to which requests are delegated.
     */
    public DelegatingX509TrustManager(final TrustManagerFactory... delegates) {
        m_delegates = Stream.of(delegates).flatMap(f -> Stream.of(f.getTrustManagers()))
            .filter(m -> m instanceof X509TrustManager).map(m -> (X509TrustManager)m).collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkClientTrusted(final X509Certificate[] arg0, final String arg1) throws CertificateException {
        CertificateException ex = null;
        for (X509TrustManager d : m_delegates) {
            try {
                d.checkClientTrusted(arg0, arg1);
                return;
            } catch (CertificateException e) {
                ex = e;
            }
        }
        if (ex != null) {
            throw ex;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkServerTrusted(final X509Certificate[] arg0, final String arg1) throws CertificateException {
        CertificateException ex = null;
        for (X509TrustManager d : m_delegates) {
            try {
                d.checkServerTrusted(arg0, arg1);
                return;
            } catch (CertificateException e) {
                ex = e;
            }
        }
        if (ex != null) {
            throw ex;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public X509Certificate[] getAcceptedIssuers() {
        List<X509Certificate> certs = new ArrayList<>();
        for (X509TrustManager d : m_delegates) {
            certs.addAll(Arrays.asList(d.getAcceptedIssuers()));
        }

        return certs.toArray(new X509Certificate[certs.size()]);
    }
}
