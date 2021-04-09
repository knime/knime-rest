package org.knime.rest.nodes.common;

import java.awt.GridBagConstraints;

public class ConnectionErrorPanel extends FramedPanel {

    private BooleanRadioButtonGroup m_failOrOutput;

    @Override
    public void initContents(final GridBagConstraints gbc) {
        super.initContents(gbc);
        this.setTitle("Connection problems (timeouts, certificate errors, ...)");
        m_failOrOutput =
            new BooleanRadioButtonGroup("Fail on connection problems", "Output status code and error message");
        this.addAsRow(m_failOrOutput);
    }

    public void setFailOrOutput(final boolean value) {
        m_failOrOutput.setValue(value);
    }

    public boolean getFailOrOutput() {
        return m_failOrOutput.getValue();
    }
}
