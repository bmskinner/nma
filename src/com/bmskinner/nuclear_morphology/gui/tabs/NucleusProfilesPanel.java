/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package com.bmskinner.nuclear_morphology.gui.tabs;

import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JTabbedPane;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.charting.charts.AbstractChartFactory;
import com.bmskinner.nuclear_morphology.charting.datasets.AbstractDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.gui.tabs.profiles.ModalityDisplayPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.profiles.ProfileDisplayPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.profiles.VariabilityDisplayPanel;

@SuppressWarnings("serial")
public class NucleusProfilesPanel extends DetailPanel {
	
	private Map<ProfileType, ProfileDisplayPanel> profilePanels = new HashMap<ProfileType, ProfileDisplayPanel>();
	
	VariabilityDisplayPanel	variabilityChartPanel;
	ModalityDisplayPanel 		modalityDisplayPanel;
	
	public NucleusProfilesPanel() {
		super();
		this.setLayout(new BorderLayout());
		JTabbedPane profilesTabPanel = new JTabbedPane(JTabbedPane.TOP);
		
		for(ProfileType type : ProfileType.values()){
			ProfileDisplayPanel panel = new ProfileDisplayPanel(type);
			profilePanels.put(type, panel);
			this.addSubPanel(panel);
			profilesTabPanel.addTab(type.toString(), null, panel, null);
		}
		
		/*
		 * Create the other profile panels
		 */
		
		modalityDisplayPanel  = new ModalityDisplayPanel();		
		variabilityChartPanel = new VariabilityDisplayPanel();
		this.addSubPanel(variabilityChartPanel);
		this.addSubPanel(modalityDisplayPanel);
		
		profilesTabPanel.addTab("Variability", null, variabilityChartPanel, null);
		profilesTabPanel.addTab("Modality"   , null, modalityDisplayPanel, null);
		this.add(profilesTabPanel, BorderLayout.CENTER);

	}
		
	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) {
		return null;
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options){
		return null;
	}
}
