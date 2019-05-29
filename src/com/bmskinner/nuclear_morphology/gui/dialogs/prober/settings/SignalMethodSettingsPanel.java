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
package com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.options.INuclearSignalOptions;
import com.bmskinner.nuclear_morphology.components.options.INuclearSignalOptions.SignalDetectionMode;
import com.bmskinner.nuclear_morphology.gui.Labels;

@SuppressWarnings("serial")
public class SignalMethodSettingsPanel extends SettingsPanel {

    private static final String METHOD_LBL = "Method";

    private static final String FORWARD_DESC_LABEL  = Labels.Signals.FORWARD_THRESHOLDING_RADIO_LABEL;
    private static final String REVERSE_DESC_LABEL  = Labels.Signals.REVERSE_THRESHOLDING_RADIO_LABEL;
    private static final String ADAPTIVE_DESC_LABEL = Labels.Signals.ADAPTIVE_THRESHOLDING_RADIO_LABEL;

    private INuclearSignalOptions options;

    private JComboBox<SignalDetectionMode> box;

    private JPanel cardPanel;

    public SignalMethodSettingsPanel(INuclearSignalOptions op) {
        options = op;

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        cardPanel = createCardPanel();
        this.add(createPanel(), BorderLayout.CENTER);
        this.add(cardPanel, BorderLayout.SOUTH);

    }

    private JPanel createCardPanel() {
        CardLayout cl = new CardLayout();
        JPanel cardPanel = new JPanel(cl);
        cardPanel.add(new JLabel(FORWARD_DESC_LABEL), FORWARD_DESC_LABEL);
        cardPanel.add(new JLabel(REVERSE_DESC_LABEL), REVERSE_DESC_LABEL);
        cardPanel.add(new JLabel(ADAPTIVE_DESC_LABEL), ADAPTIVE_DESC_LABEL);

        cl.show(cardPanel, FORWARD_DESC_LABEL);

        return cardPanel;
    }

    private JPanel createPanel() {
        createSpinners();

        JPanel panel = new JPanel();

        List<JLabel> labels = new ArrayList<JLabel>();
        labels.add(new JLabel(METHOD_LBL));

        List<JComboBox<SignalDetectionMode>> fields = new ArrayList<JComboBox<SignalDetectionMode>>();
        fields.add(box);

        addLabelTextRows(labels, fields, panel);

        // Make the description panel

        return panel;
    }

    private void createSpinners() {

        box = new JComboBox<SignalDetectionMode>(SignalDetectionMode.values());
        box.setSelectedItem(SignalDetectionMode.FORWARD);

        box.addActionListener(e -> {

            SignalDetectionMode mode = (SignalDetectionMode) box.getSelectedItem();
            options.setDetectionMode(mode);

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
    public void set(@NonNull INuclearSignalOptions options) {
    	this.options.set(options);
    	update();
    }

}
