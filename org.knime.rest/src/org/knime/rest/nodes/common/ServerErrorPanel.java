package org.knime.rest.nodes.common;

import java.awt.GridBagConstraints;

import javax.swing.JSpinner;

public class ServerErrorPanel extends FramedPanel {
    private BooleanRadioButtonGroup m_failOrOutput;

    private DisableablePanel m_retryOnError;

    private JSpinner m_retriesSpinner;

    private JSpinner m_retryDelay;

    @Override
    public void initContents(final GridBagConstraints gbc) {
        super.initContents(gbc);
        this.setTitle("Server-side errors (HTTP 5XX)");

        m_failOrOutput = new BooleanRadioButtonGroup("Fail node execution", "Output missing value");
        this.addAsRow(m_failOrOutput);

        m_retryOnError = new DisableablePanel("Retry on error");
        this.addAsRow(m_retryOnError);

        m_retriesSpinner = createSpinner(1, 1);
        m_retryOnError.addLabeledSpinner("Number of retries",
            "Number of additional attempts that will be made after the initial request has failed", m_retriesSpinner);

        m_retryDelay = createSpinner(1, 1);
        m_retryOnError.addLabeledSpinner("Retry delay [s]", "The base delay to be applied.", m_retryDelay);

    }

    public void setFailOrOutput(final boolean value) {
        m_failOrOutput.setValue(value);
    }

    public boolean getFailOrOutput() {
        return m_failOrOutput.getValue();
    }

    public void setNumRetries(final int value) {
        m_retriesSpinner.setValue(value);
    }

    public int getNumRetries() {
        return (int)m_retriesSpinner.getValue();
    }

    public void setRetryDelay(final long value) {
        m_retryDelay.setValue(value);
    }

    public long getRetryDelay() {
        return ((Number)m_retryDelay.getValue()).longValue();
    }

    public boolean getRetryEnabled() {
        return m_retryOnError.isActive();
    }

    public void setActive(final boolean value) {
        m_retryOnError.setActive(value);
    }

}
