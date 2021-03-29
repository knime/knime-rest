package org.knime.rest.nodes.common;

import java.awt.GridBagConstraints;

import javax.swing.JPanel;
import javax.swing.JSpinner;

public class RateLimitPanel extends FramedPanel {
    private JSpinner m_delaySpinner;

    private DisableablePanel m_pauseExecution;

    @Override
    public void initContents(final GridBagConstraints gbc) {
        super.initContents(gbc);
        this.setTitle("Rate-limiting error (HTTP 429)");

        m_pauseExecution = new DisableablePanel("Pause execution");
        this.addAsRow(m_pauseExecution);

        m_delaySpinner = createSpinner(1, 10);
        m_pauseExecution.addLabeledSpinner("Pause execution [s]", "The time to wait after a rate-limit error occured.",
                m_delaySpinner);

    }

    protected void setDelay(final long value) {
        m_delaySpinner.setValue(value);
    }

    protected long getDelay() {
        return ((Number)m_delaySpinner.getValue()).longValue();
    }

    protected void setActive(final boolean value) {
        m_pauseExecution.setActive(value);
    }

    protected boolean getActive() {
        return m_pauseExecution.isActive();
    }
}
