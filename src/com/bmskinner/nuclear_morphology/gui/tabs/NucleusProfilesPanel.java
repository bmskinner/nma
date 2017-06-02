/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.gui.tabs;

import java.awt.BorderLayout;

import javax.swing.JTabbedPane;

import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.gui.tabs.profiles.ModalityDisplayPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.profiles.ProfileDisplayPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.profiles.VariabilityDisplayPanel;

@SuppressWarnings("serial")
public class NucleusProfilesPanel extends DetailPanel {

    private static final String MODALITY_TAB_LBL    = "Modality";
    private static final String VARIABILITY_TAB_LBL = "Variability";

    public NucleusProfilesPanel() {
        super();
        this.setLayout(new BorderLayout());
        JTabbedPane tabPanel = new JTabbedPane(JTabbedPane.TOP);

        for (ProfileType type : ProfileType.displayValues()) {

            DetailPanel panel = new ProfileDisplayPanel(type);
            this.addSubPanel(panel);
            tabPanel.addTab(type.toString(), panel);
        }

        /*
         * Create the other profile panels
         */

        DetailPanel modalityDisplayPanel = new ModalityDisplayPanel();
        DetailPanel variabilityChartPanel = new VariabilityDisplayPanel();

        this.addSubPanel(variabilityChartPanel);
        this.addSubPanel(modalityDisplayPanel);

        tabPanel.addTab(VARIABILITY_TAB_LBL, variabilityChartPanel);
        tabPanel.addTab(MODALITY_TAB_LBL, modalityDisplayPanel);

        this.add(tabPanel, BorderLayout.CENTER);

    }
}
