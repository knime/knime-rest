package org.knime.rest.nodes.common;

import java.awt.GridBagConstraints;

public class ClientErrorPanel extends FramedPanel {
    private BooleanRadioButtonGroup m_failOrOutput;

    @Override
    public void initContents(final GridBagConstraints gbc) {
        super.initContents(gbc);
        this.setTitle("Client-side errors (HTTP 4XX)");

        m_failOrOutput = new BooleanRadioButtonGroup("Fail node execution", "Output missing value");
        this.addAsRow(m_failOrOutput);
    }

    public void setFailOrOutput(final boolean value) {
        m_failOrOutput.setValue(value);
    }

    public boolean getFailOrOutput() {
        return m_failOrOutput.getValue();
    }
}
