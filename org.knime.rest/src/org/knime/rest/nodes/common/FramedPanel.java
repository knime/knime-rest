package org.knime.rest.nodes.common;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

@SuppressWarnings("java:S1699") // calls to overridable methods in constructor -- fine in swing.
public class FramedPanel extends JPanel {
    private final GridBagConstraints m_gbc;

    public FramedPanel() {
        this.setLayout(new GridBagLayout());
        m_gbc = initGridBagConstraints();

        initContents(m_gbc);

        this.setMaximumSize(new Dimension(Integer.MAX_VALUE, (int)this.getPreferredSize().getHeight()));
    }

    public void setTitle(final String title) {
        this.setBorder(BorderFactory.createTitledBorder(title));
    }

    public void addAsRow(final JComponent comp) {
        m_gbc.insets = new Insets(5, 0, 5, 0);
        m_gbc.gridx = 0;
        m_gbc.gridy++;
        this.add(comp, m_gbc);
        m_gbc.insets = new Insets(0, 0, 0, 0);
    }

    public void initContents(final GridBagConstraints gbc) {
        // overridable by subclasses
    }

    public static GridBagConstraints initGridBagConstraints() {
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = gbc.gridy = 0;
        gbc.weightx = gbc.weighty = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        return gbc;
    }

    public static void addLabeledSpinnerTo(final JComponent container, final GridBagConstraints gbc, final String label,
        final String tooltip, final JSpinner spinner) {
        gbc.gridy++;
        gbc.gridx = 0;

        gbc.weightx = 0;
        Box labelBox = Box.createHorizontalBox();
        labelBox.add(new JLabel(label));
        labelBox.add(Box.createHorizontalStrut(5));
        container.add(labelBox, gbc);

        gbc.gridx++;
        gbc.weightx = 1;
        container.add(spinner, gbc);

        labelBox.setToolTipText(tooltip);
        spinner.setToolTipText(tooltip);
    }

    public static JSpinner createSpinner(final Number value, final int step) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(value.intValue(), 0, null, step));
        ((JSpinner.DefaultEditor)spinner.getEditor()).getTextField().setColumns(4);
        return spinner;
    }

}
