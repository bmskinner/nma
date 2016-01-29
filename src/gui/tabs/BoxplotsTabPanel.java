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

import gui.components.ExportableChartPanel;
import gui.components.panels.MeasurementUnitSettingsPanel;
import gui.tabs.DetailPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * This class is extended for making a panel with multiple stats histograms
	 * arranged vertically
	 * @author bms41
	 *
	 */
	@SuppressWarnings("serial")
	public abstract class BoxplotsTabPanel extends DetailPanel implements ActionListener {
		
		protected Map<String, ExportableChartPanel> chartPanels = new HashMap<String, ExportableChartPanel>();

		protected JPanel 		mainPanel; // hold the charts
		protected JPanel		headerPanel; // hold buttons
		protected MeasurementUnitSettingsPanel measurementUnitSettingsPanel = new MeasurementUnitSettingsPanel();

		protected JScrollPane scrollPane; // hold the main panel
		
		public BoxplotsTabPanel(final Logger programLogger){
			super(programLogger);

			this.setLayout(new BorderLayout());

			try {
				mainPanel = new JPanel();
				mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));

				headerPanel = new JPanel(new FlowLayout());
				
				headerPanel.add(measurementUnitSettingsPanel);
				measurementUnitSettingsPanel.addActionListener(this);


				this.add(headerPanel, BorderLayout.NORTH);

				// add the scroll pane to the tab
				scrollPane  = new JScrollPane(mainPanel);
				this.add(scrollPane, BorderLayout.CENTER);
				
				this.setEnabled(false);
			} catch(Exception e){
				programLogger.log(Level.SEVERE, "Error creating panel", e);
			}

		}
		
		public void setEnabled(boolean b){
			super.setEnabled(b);
			measurementUnitSettingsPanel.setEnabled(b);
		}
		
		 @Override
	     public void actionPerformed(ActionEvent e) {

	         try {
	        	 programLogger.log(Level.FINEST, "Updating abstract boxplot tab panel");
	             this.update(getDatasets());
	         } catch (Exception e1) {
	         	programLogger.log(Level.SEVERE, "Error updating boxplot panel from action listener", e1);
	         }
	         
	         
	     }

		 
	}

