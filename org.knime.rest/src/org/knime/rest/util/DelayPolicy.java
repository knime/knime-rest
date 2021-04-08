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
 *   2 Mar 2021 (Benjamin Moser): created
 */
package org.knime.rest.util;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

import javax.ws.rs.core.Response;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.rest.nodes.common.RestNodeModel;

/**
 * Utility class for retrying a task that returns a {@link Response}. Makes a distinction between "server errors" and
 * "rate-limiting errors".
 *
 * Holds information describing how server and rate-limiting errors should be handled in terms of timeouts and retries.
 *
 * Provides a utility method to run a task and apply timeouts and retries.
 *
 * We distinguish between "retry delays" (aka "backoff"; after a server error; timeout between retries) and "cooldown
 * delays" (after a rate-limiting error, timeout before trying again). Retry delays increase exponentially with each
 * attempt, cooldown delay always stays the same.
 *
 * @see RestNodeModel#isServerError(Response)
 * @see RestNodeModel#isRateLimitError(Response)
 *
 * @noreference
 * @author Benjamin Moser
 */
public final class DelayPolicy {

    private final boolean m_retriesEnabled;

    private final boolean m_cooldownEnabled;

    /**
     * Always use exponential backoff with powers of two. This means the base timeout will double before each retry.
     *
     * @see DelayPolicy#getRetryDelayAt(int)
     */
    private static final int RETRY_MULTIPLIER = 2;

    /**
     * The delay to apply before the first retry. For subsequent retries, the delay will be increased according to
     * {@link DelayPolicy#getRetryDelayAt(int)}. Given in seconds.
     */
    private final long m_retryBaseDelay;

    /**
     * The maximum number of retries to perform for server errors. Note that rate-limiting errors are handled
     * seperately.
     */
    private final int m_retryMaxAttempts;

    /**
     * The delay to apply after a rate-limiting response was encountered. Called "cooldown" to distinguish from retry
     * delays. Given in seconds.
     */
    private final long m_cooldownPeriod;

    /**
     * Construct a new object representing how retries and delays should be handled. Additionally offers utility methods
     * to execute a {@code Callable} with retries.
     *
     * @param baseRetryDelay The delay to apply between the first request and the first retry. For each subsequent
     *            retry, the delay is determined according to {@link DelayPolicy#getRetryDelayAt(int)}
     * @param maxRetries The maximum number of retries to perform, not including the initial request.
     * @param cooldown The delay to apply after a ratelimit error has been encountered.
     * @param retriesEnabled Whether the request should be retried.
     * @param cooldownEnabled Whether on a rate-limiting response a cooldown delay should be applied.
     */
    public DelayPolicy(final long baseRetryDelay, final int maxRetries, final long cooldown,
        final boolean retriesEnabled, final boolean cooldownEnabled) {
        CheckUtils.checkArgument(maxRetries >= 0, "Retries < 0: %d", maxRetries);
        CheckUtils.checkArgument(baseRetryDelay >= 0, "Base < 0: %d", baseRetryDelay);
        CheckUtils.checkArgument(cooldown >= 0, "Cooldown < 0: %d", cooldown);
        m_retryBaseDelay = baseRetryDelay;
        m_retryMaxAttempts = maxRetries;
        m_cooldownPeriod = cooldown;
        m_retriesEnabled = retriesEnabled;
        m_cooldownEnabled = cooldownEnabled;
    }

    /**
     * Perform the given {@code Callable} and inspect the result, potentially retrying after some delay.
     *
     * @param policy The policy defining number of retries and sleep durations
     * @param cooldownContext Context maintained across multiple requests.
     * @param task The task that will return a {@link Response}. Will usually perform a request.
     * @return The server response obtained from the last performed attempt.
     * @throws Exception Generic exception, will be handled by the node model.
     */
    public static Response doWithDelays(final DelayPolicy policy, final CooldownContext cooldownContext, final Callable<Response> task) throws Exception {
        Response lastResponse = null;
        int retryCount = 0;
        long alreadyWaited = 0;
        // determines whether to do another request after the current one (applies to both server and ratelimit errors).
        boolean tryAgain = false;
        do {
            // rate-limit cooldown delay
            long cooldownDelta = cooldownContext.getCooldownDelta(policy);
            if (cooldownDelta > 0 && policy.isCooldownEnabled()) {
                // Accumulate time waited for rate-limit cooldown. In case the thread is
                // currently in a retry cycle but a rate-limit error was encountered,
                // we (assume that we can) subtract the time waited for the rate-limit
                // cooldown counts towards the server-error retry delay.
                alreadyWaited += cooldownDelta;
                Thread.sleep(cooldownDelta);
                // after delay, re-enter loop (without increasing attempt counter)
                // this is necessary because we want to re-check the cooldown in case
                // another task has reset it in the meantime. In that case we need to
                // sleep for another delta.
                tryAgain = policy.isCooldownEnabled();
                continue;
            }

            // retry backoff delay
            long timeout = policy.getRetryDelayAt(retryCount) - alreadyWaited;
            if (timeout > 0 && retryCount > 0 && policy.isRetriesEnabled()) {
                Thread.sleep(timeout);
            }

            // done waiting for cooldown to pass (and dont need value any longer),
            // reset for future iterations.
            alreadyWaited = 0;

            lastResponse = task.call(); // perform request

            // inspect result and handle accordingly
            if (RestNodeModel.isServerError(lastResponse)) {
                retryCount++;
                tryAgain = policy.isRetriesEnabled() && retryCount <= policy.getMaxRetries();
                // re-enter loop
            } else if (RestNodeModel.isRateLimitError(lastResponse)) {
                cooldownContext.resetCooldown(); // set cooldown init to current time
                tryAgain = policy.isCooldownEnabled();
                // continue and re-enter loop without increasing retry counter
            } else { // other error or successful
                return lastResponse; // exit loop and return
            }

        } while (tryAgain);

        // out of retries or not enabled; return last response and let downstream code path handle errors.
        return lastResponse;
    }

    /**
     * @return The timeout value to apply after the <i>i</i>-th retry.
     */
    private long getRetryDelayAt(final int i) {
        return (long)(this.getRetryBaseMs() * Math.pow(RETRY_MULTIPLIER, i));
    }

    public int getMaxRetries() {
        return m_retryMaxAttempts;
    }

    long getCooldownPeriodMs() {
        return m_cooldownPeriod * 1000;
    }

    public long getCooldown() {
        return m_cooldownPeriod;
    }

    public long getRetryBase() {
        return m_retryBaseDelay;
    }

    public long getRetryBaseMs() {
        return m_retryBaseDelay * 1000;
    }

    public boolean isRetriesEnabled() {
        return m_retriesEnabled;
    }

    public boolean isCooldownEnabled() {
        return m_cooldownEnabled;
    }

    /**
     * Load the child settings from the settings and return a new {@link DelayPolicy} (if present).
     *
     * @param settings
     * @return An optional containing the DelayPolicy if according settings are set, else an empty optional.
     */
    public static Optional<DelayPolicy> loadFromSettings(final NodeSettingsRO settings) {
        try {
            NodeSettingsRO childSettings = settings.getNodeSettings("delayPolicy");
            long base = childSettings.getLong("delayRetryBase");
            int retries = childSettings.getInt("delayMaxRetries");
            long cooldown = childSettings.getLong("delayRateLimitCooldown");
            boolean retriesEnabled = childSettings.getBoolean("delayRetriesEnabled");
            boolean cooldownEnabled = childSettings.getBoolean("delayCooldownEnabled");
            return Optional.of(new DelayPolicy(base, retries, cooldown, retriesEnabled, cooldownEnabled));
        } catch (InvalidSettingsException e) { // NOSONAR: exception handled properly, added in 4.3.2
            return Optional.empty();
        }
    }

    /**
     * Save the configuration of this {@link DelayPolicy} to the given settings object.
     *
     * @param settings
     */
    public void saveToSettings(final NodeSettingsWO settings) {
        NodeSettingsWO childSettings = settings.addNodeSettings("delayPolicy");
        childSettings.addLong("delayRetryBase", getRetryBase());
        childSettings.addInt("delayMaxRetries", getMaxRetries());
        childSettings.addLong("delayRateLimitCooldown", getCooldown());
        childSettings.addBoolean("delayRetriesEnabled", isRetriesEnabled());
        childSettings.addBoolean("delayCooldownEnabled", isCooldownEnabled());
    }

}
