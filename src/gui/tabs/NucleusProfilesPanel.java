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
package gui.tabs;

import gui.tabs.profiles.ModalityDisplayPanel;
import gui.tabs.profiles.ProfileDisplayPanel;
import gui.tabs.profiles.VariabilityDisplayPanel;
import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JTabbedPane;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;
import charting.datasets.NucleusTableDatasetCreator;
import charting.options.ChartOptions;
import charting.options.TableOptions;
import components.generic.ProfileType;

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
		
		profilesTabPanel.addTab("Variability", null, variabilityChartPanel, null);
		profilesTabPanel.addTab("Modality", null, modalityDisplayPanel, null);
		this.add(profilesTabPanel, BorderLayout.CENTER);

	}
	
	@Override
	protected void updateSingle() throws Exception {
		updateMultiple();
	}
	
	@Override
	protected void updateMultiple() throws Exception {
		
		for(ProfileType type : profilePanels.keySet()){
			profilePanels.get(type).update(getDatasets());
			log(Level.FINEST, "Updated "+type.toString()+" profile panel");
		}
		
		variabilityChartPanel.update(getDatasets());
		log(Level.FINEST, "Updated variabililty panel");
		
		modalityDisplayPanel.update(getDatasets());
		log(Level.FINEST, "Updated modality panel");
	}
	
	@Override
	protected void updateNull() throws Exception {
		updateMultiple();
	}
	
	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) throws Exception {
		return null;
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options) throws Exception{
		return null;
	}
}
