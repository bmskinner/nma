/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nma.gui.dialogs.prober.settings;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.signals.SignalDetectionMode;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.gui.Labels;

@SuppressWarnings("serial")
public class SignalMethodSettingsPanel extends SettingsPanel {

    private static final String METHOD_LBL = "Method";

    private static final String FORWARD_DESC_LABEL  = Labels.Signals.FORWARD_THRESHOLDING_RADIO_LABEL;
    private static final String REVERSE_DESC_LABEL  = Labels.Signals.REVERSE_THRESHOLDING_RADIO_LABEL;
    private static final String ADAPTIVE_DESC_LABEL = Labels.Signals.ADAPTIVE_THRESHOLDING_RADIO_LABEL;

    private HashOptions options;

    private JComboBox<SignalDetectionMode> box;

    private JPanel cardPanel;

    public SignalMethodSettingsPanel(HashOptions op) {
        options = op;

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        cardPanel = createCardPanel();
        this.add(createPanel(), BorderLayout.CENTER);
        this.add(cardPanel, BorderLayout.SOUTH);

    }

    private JPanel createCardPanel() {
        CardLayout cl = new CardLayout();
        JPanel panel = new JPanel(cl);
        panel.add(new JLabel(FORWARD_DESC_LABEL), FORWARD_DESC_LABEL);
        panel.add(new JLabel(REVERSE_DESC_LABEL), REVERSE_DESC_LABEL);
        panel.add(new JLabel(ADAPTIVE_DESC_LABEL), ADAPTIVE_DESC_LABEL);

        cl.show(panel, FORWARD_DESC_LABEL);

        return panel;
    }

    private JPanel createPanel() {
        createSpinners();

        JPanel panel = new JPanel();

        List<JLabel> labels = new ArrayList<>();
        labels.add(new JLabel(METHOD_LBL));

        List<JComboBox<SignalDetectionMode>> fields = new ArrayList<>();
        fields.add(box);

        addLabelTextRows(labels, fields, panel);

        // Make the description panel

        return panel;
    }

    private void createSpinners() {

        box = new JComboBox<>(SignalDetectionMode.values());
        box.setSelectedItem(SignalDetectionMode.FORWARD);

        box.addActionListener(e -> {

            SignalDetectionMode mode = (SignalDetectionMode) box.getSelectedItem();
            options.setString(HashOptions.SIGNAL_DETECTION_MODE_KEY, mode.name());

            CardLayout cl = (CardLayout) cardPanel.getLayout();

            cl.show(cardPanel, mode.getDesc());

            fireOptionsChangeEvent();
        });

    }

    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);
        box.setEnabled(b);

    }
    
    /**
     * Set the options values and update the spinners to match
     * 
     * @param options
     */
    public void set(@NonNull HashOptions options) {
    	this.options.set(options);
    	update();
    }

}
