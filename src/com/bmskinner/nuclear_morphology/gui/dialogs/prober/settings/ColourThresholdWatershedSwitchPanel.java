/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/

package com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;

@SuppressWarnings("serial")
public class ColourThresholdWatershedSwitchPanel extends DetectionSettingsPanel implements ActionListener {

    private static final String THRESHOLD_LBL = "Colour Threshold";
    private static final String WATERSHED_LBL = "Watershed";

    private JPanel cardPanel;

    private JRadioButton thresholdBtn = new JRadioButton(THRESHOLD_LBL);
    private JRadioButton waterBtn     = new JRadioButton(WATERSHED_LBL);
    private ButtonGroup  group        = new ButtonGroup();

    public ColourThresholdWatershedSwitchPanel(final IDetectionOptions options) {
        super(options);
        this.add(createPanel(), BorderLayout.CENTER);

    }

    private JPanel createPanel() {

        JPanel panel = new JPanel();

        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JPanel switchPanel = makeSwitchPanel();
        cardPanel = makeCardPanel();

        panel.add(switchPanel);
        panel.add(cardPanel);

        return panel;
    }

    private JPanel makeCardPanel() {
        JPanel cardPanel = new JPanel(new CardLayout());

        SettingsPanel thresholdPanel = new ColourThresholdingSettingsPanel(options);
        SettingsPanel watershedPanel = new WatershedSettingsPanel(options);

        this.addSubPanel(thresholdPanel);
        this.addSubPanel(watershedPanel);

        cardPanel.add(thresholdPanel, THRESHOLD_LBL);
        cardPanel.add(watershedPanel, WATERSHED_LBL);
        CardLayout cl = (CardLayout) (cardPanel.getLayout());
        if (options.getBoolean(IDetectionOptions.IS_USE_WATERSHED)) {
            cl.show(cardPanel, WATERSHED_LBL);
        } else {
            cl.show(cardPanel, THRESHOLD_LBL);
        }

        return cardPanel;
    }

    /**
     * A panel with the radio buttons to choose edge detection or threshold for
     * the nucleus
     * 
     * @return
     */
    private JPanel makeSwitchPanel() {
        JPanel panel = new JPanel(new FlowLayout());

        thresholdBtn.setSelected(!options.getBoolean(IDetectionOptions.IS_USE_WATERSHED));
        waterBtn.setSelected(options.getBoolean(IDetectionOptions.IS_USE_WATERSHED));
        thresholdBtn.setActionCommand(THRESHOLD_LBL);
        waterBtn.setActionCommand(WATERSHED_LBL);

        // Group the radio buttons.
        group.add(thresholdBtn);
        group.add(waterBtn);

        thresholdBtn.addActionListener(this);
        waterBtn.addActionListener(this);

        panel.add(thresholdBtn);
        panel.add(waterBtn);

        return panel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(THRESHOLD_LBL)) {
            options.setBoolean(IDetectionOptions.IS_USE_WATERSHED, false);

            CardLayout cl = (CardLayout) (cardPanel.getLayout());
            cl.show(cardPanel, THRESHOLD_LBL);

        }

        if (e.getActionCommand().equals(WATERSHED_LBL)) {
            options.setBoolean(IDetectionOptions.IS_USE_WATERSHED, true);
            CardLayout cl = (CardLayout) (cardPanel.getLayout());
            cl.show(cardPanel, WATERSHED_LBL);
        }
        fireOptionsChangeEvent();

    }

    @Override
    public void update() {
        super.update();

        isUpdating = true;
        CardLayout cl = (CardLayout) (cardPanel.getLayout());

        if (options.getBoolean(IDetectionOptions.IS_USE_WATERSHED)) {
            cl.show(cardPanel, WATERSHED_LBL);
        } else {
            cl.show(cardPanel, THRESHOLD_LBL);
        }
        isUpdating = false;

    }

    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);
        thresholdBtn.setEnabled(b);
        waterBtn.setEnabled(b);

    }

    @Override
    public void set(IDetectionOptions options) {
        this.options.set(options);
        update();

    }

}
