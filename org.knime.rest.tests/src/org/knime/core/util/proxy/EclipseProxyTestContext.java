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
 *   Mar 28, 2025 (lw): created
 */
package org.knime.core.util.proxy;

import java.io.IOException;

import org.apache.commons.lang3.function.FailableConsumer;
import org.apache.commons.lang3.function.FailableRunnable;
import org.eclipse.core.internal.net.ProxyData;
import org.eclipse.core.internal.net.ProxyManager;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.CoreException;

/**
 * Adapted from {@code org.knime.core.util.proxy.search.GlobalProxyTestContext}, with the modification of using
 * Eclipse-based instead of Java-based proxies.
 *
 * @author Leon Wenzler, KNIME AG, Konstanz, Germany
 */
public class EclipseProxyTestContext {

    private static void runWithStrategy(final FailableRunnable<IOException> test,
        final FailableConsumer<IProxyService, CoreException> configurer) throws IOException, CoreException {
        @SuppressWarnings("restriction")
        final var manager = ProxyManager.getProxyManager();

        // Store configuration of Eclipse proxies for a later restore.
        final var origProxiesEnabled = manager.isProxiesEnabled();
        final var origSystemProxiesEnabled = manager.isSystemProxiesEnabled();
        final var origProxyData = manager.getProxyData();
        final var origNonProxiedHosts = manager.getNonProxiedHosts();
        try {
            configurer.accept(manager);
            test.run();
        } finally {
            manager.setProxiesEnabled(origProxiesEnabled);
            manager.setSystemProxiesEnabled(origSystemProxiesEnabled);
            manager.setProxyData(origProxyData);
            manager.setNonProxiedHosts(origNonProxiedHosts);
        }
    }

    @SuppressWarnings("restriction")
    private static IProxyData toIProxyData(final GlobalProxyConfig config) {
        final var data = new ProxyData(config.protocol().name(), config.host(), config.intPort(), //
            config.useAuthentication(), null);
        if (config.useAuthentication()) {
            data.setUserid(config.username());
            data.setPassword(config.password());
        }
        return data;
    }

    /**
     * Searches with a strategy that returns an EMPTY signal.
     *
     * @param test the (failable) test to run
     * @throws IOException
     * @throws CoreException
     */
    public static void withEmpty(final FailableRunnable<IOException> test) throws IOException, CoreException {
        runWithStrategy(test, manager -> {
            manager.setProxiesEnabled(false);
            manager.setSystemProxiesEnabled(false);
        });
    }

    /**
     * Searches with a strategy that returns the specified {@link GlobalProxyConfig}.
     *
     * @param config which configuration should be applied
     * @param test the (failable) test to run
     * @throws IOException
     * @throws CoreException
     */
    public static void withConfig(final GlobalProxyConfig config, final FailableRunnable<IOException> test)
        throws IOException, CoreException {
        runWithStrategy(test, manager -> {
            manager.setProxiesEnabled(true);
            manager.setSystemProxiesEnabled(false);
            manager.setProxyData(new IProxyData[]{toIProxyData(config)});
            if (config.useExcludedHosts()) {
                manager.setNonProxiedHosts(ExcludedHostsTokenizer.tokenize(config.excludedHosts()));
            } else {
                manager.setNonProxiedHosts(new String[0]);
            }
        });
    }
}
