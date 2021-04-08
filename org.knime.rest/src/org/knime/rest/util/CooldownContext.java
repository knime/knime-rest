package org.knime.rest.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Holds context information regarding rate-limiting.
 */
public class CooldownContext {
    /**
     * The timestamp at which the last rate limit cooldown was triggered.
     **/
    private final AtomicLong m_cooldownInitTimestamp = new AtomicLong(0);

    void resetCooldown() {
        m_cooldownInitTimestamp.set(System.currentTimeMillis());
    }

    /**
     *
     * @return smaller-equal zero if cooldown period has passed, else the remaining time.
     */
    long getCooldownDelta(DelayPolicy policy) {
        return (m_cooldownInitTimestamp.get() + policy.getCooldownPeriodMs()) - System.currentTimeMillis();
    }

}
