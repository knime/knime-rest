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
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.function.FailableConsumer;
import org.apache.commons.lang3.function.FailableRunnable;
import org.eclipse.core.internal.net.ProxyData;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.CoreException;
import org.knime.core.internal.CorePlugin;
import org.knime.core.util.proxy.search.GlobalProxyStrategy.GlobalProxySearchResult;
import org.knime.core.util.proxy.testing.ProxyParameterProvider;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Adapted from {@link org.knime.core.util.proxy.search.GlobalProxyTestContext},
 * with the modification of using Eclipse-based instead of Java-based proxies.
 */
public final class EclipseProxyTestContext extends ProxyParameterProvider {

    /**
     * Used statically in all tests that are extended with {@link ProxyParameterProvider}.
     */
    static final ProxyParameterProvider INSTANCE = new EclipseProxyTestContext();

    private static void runWithStrategy(final FailableRunnable<IOException> test,
        final FailableConsumer<IProxyService, CoreException> configurer) throws IOException {
        final var bundle = FrameworkUtil.getBundle(CorePlugin.getInstance().getClass());
        // using proxy service class name as String to avoid initialization of the class
        final var tracker = new ServiceTracker<IProxyService, IProxyService>(bundle.getBundleContext(), //
            "org.eclipse.core.net.proxy.IProxyService", null);
        tracker.open();
        final var service = tracker.getService();

        // store configuration of Eclipse proxies for a later restore
        final var origProxiesEnabled = service.isProxiesEnabled();
        final var origSystemProxiesEnabled = service.isSystemProxiesEnabled();
        final var origProxyData = service.getProxyData();
        final var origNonProxiedHosts = service.getNonProxiedHosts();

        try {
            try {
                configurer.accept(service);
                test.run();
            } finally {
                // lastly restore original configuration of Eclipse proxies
                service.setProxiesEnabled(origProxiesEnabled);
                service.setSystemProxiesEnabled(origSystemProxiesEnabled);
                service.setProxyData(origProxyData);
                service.setNonProxiedHosts(origNonProxiedHosts);
            }
        } catch (CoreException e) {
            throw new IOException(e);
        } finally {
            tracker.close();
        }
    }

    @SuppressWarnings("restriction")
    private static IProxyData toIProxyData(final GlobalProxyConfig config) {
        if (config == null) {
            return null;
        }
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
     */
    @Override
    public void withEmpty(final FailableRunnable<IOException> test) throws IOException {
        runWithStrategy(test, service -> {
            service.setProxiesEnabled(false);
            service.setSystemProxiesEnabled(false);
        });
    }

    /**
     * Searches with a strategy that returns the specified {@link GlobalProxyConfig}.
     *
     * @param config which configuration should be applied
     * @param test the (failable) test to run
     * @throws IOException
     */
    @Override
    public void withConfig(final GlobalProxyConfig config, final FailableRunnable<IOException> test)
        throws IOException {
        runWithStrategy(test, service -> {
            service.setProxiesEnabled(true);
            service.setSystemProxiesEnabled(false);
            service.setProxyData(new IProxyData[]{toIProxyData(config)});
            if (config.useExcludedHosts()) {
                service.setNonProxiedHosts(ExcludedHostsTokenizer.tokenize(config.excludedHosts()));
            } else {
                service.setNonProxiedHosts(new String[0]);
            }
        });
    }

    /**
     * Searches with a strategy that returns the specified {@link GlobalProxyConfig}.
     *
     * @param a first which configuration should be applied
     * @param b second which configuration should be applied
     * @param test the (failable) test to run
     * @throws IOException
     */
    @Override
    public void withTwoResults(final GlobalProxySearchResult a, final GlobalProxySearchResult b,
        final FailableRunnable<IOException> test) throws IOException {
        final var proxiesEnabled = a.value().isPresent() || b.value().isPresent();
        final var systemProxiesEnabled = false;
        final var proxyData = ArrayUtils.addAll( //
            new IProxyData[]{toIProxyData(a.value().orElse(null))}, //
            new IProxyData[]{toIProxyData(b.value().orElse(null))} //
        );
        final var nonProxiedHosts = Stream.concat( //
            a.value().stream() //
                .filter(GlobalProxyConfig::useExcludedHosts) //
                .flatMap(cfg -> ExcludedHostsTokenizer.tokenizeAsStream(cfg.excludedHosts())), //
            b.value().stream() //
                .filter(GlobalProxyConfig::useExcludedHosts) //
                .flatMap(cfg -> ExcludedHostsTokenizer.tokenizeAsStream(cfg.excludedHosts())) //
        ).toArray(String[]::new);
        runWithStrategy(test, service -> {
            service.setProxiesEnabled(proxiesEnabled);
            service.setSystemProxiesEnabled(systemProxiesEnabled);
            service.setProxyData(proxyData);
            service.setNonProxiedHosts(nonProxiedHosts);
        });
    }
}
