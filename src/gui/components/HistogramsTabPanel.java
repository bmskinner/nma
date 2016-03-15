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
package gui.components;

import gui.components.panels.MeasurementUnitSettingsPanel;
import gui.components.panels.ProbabilityDensityCheckboxPanel;
import gui.tabs.DetailPanel;
import stats.NucleusStatistic;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import charting.charts.BoxplotChartFactory;
import charting.charts.HistogramChartFactory;
import charting.datasets.NucleusTableDatasetCreator;
import charting.options.ChartOptions;
import charting.options.TableOptions;

/**
 * This class is extended for making a panel with multiple stats histograms
 * arranged vertically
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public abstract class HistogramsTabPanel extends DetailPanel implements ActionListener {
	
	protected Map<String, SelectableChartPanel> chartPanels = new HashMap<String, SelectableChartPanel>();

	protected JPanel 		mainPanel; // hold the charts
	protected JPanel		headerPanel; // hold buttons
	protected ProbabilityDensityCheckboxPanel useDensityPanel = new ProbabilityDensityCheckboxPanel();
	protected MeasurementUnitSettingsPanel measurementUnitSettingsPanel = new MeasurementUnitSettingsPanel();

	protected JScrollPane scrollPane; // hold the main panel
	
	public HistogramsTabPanel(){
		super();

		this.setLayout(new BorderLayout());

		try {
			mainPanel = new JPanel();
			mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

			headerPanel = new JPanel(new FlowLayout());

			
			headerPanel.add(useDensityPanel);

			useDensityPanel.addActionListener(this);
			
			headerPanel.add(measurementUnitSettingsPanel);
			measurementUnitSettingsPanel.addActionListener(this);


			this.add(headerPanel, BorderLayout.NORTH);

			// add the scroll pane to the tab
			scrollPane  = new JScrollPane(mainPanel);
			this.add(scrollPane, BorderLayout.CENTER);
			
			this.setEnabled(false);
		} catch(Exception e){
			log(Level.SEVERE, "Error creating panel", e);
		}

	}
	
	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) throws Exception{
		return HistogramChartFactory.createStatisticHistogram(options);
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options) throws Exception{
		return null;
	}
	
	public void setEnabled(boolean b){
		super.setEnabled(b);
		useDensityPanel.setEnabled(b);
		measurementUnitSettingsPanel.setEnabled(b);
	}
	
	 @Override
     public void actionPerformed(ActionEvent e) {

         try {
        	 log(Level.FINEST, "Updating abstract histogram tab panel");
             this.update(getDatasets());
         } catch (Exception e1) {
         	log(Level.SEVERE, "Error updating histogram panel from action listener", e1);
         }
         
         
     }
	 	 
	 protected int getFilterDialogResult(double lower, double upper){
			DecimalFormat df = new DecimalFormat("#.##");
			Object[] options = { "Filter collection" , "Cancel", };
			int result = JOptionPane.showOptionDialog(null, "Filter between "+df.format(lower)+"-"+df.format(upper)+"?", "Confirm filter",

					JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,

					null, options, options[0]);
			return result;
		}
	 
	 
}
