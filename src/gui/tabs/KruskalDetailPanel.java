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

import gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import components.generic.BorderTag;
import components.generic.ProfileType;
import charting.charts.MorphologyChartFactory;
import charting.charts.ProfileChartOptions;

@SuppressWarnings("serial")
public class KruskalDetailPanel  extends DetailPanel {
	
	private ChartPanel chartPanel;

	public KruskalDetailPanel(Logger programLogger ) throws Exception {
		super(programLogger);
		
		createUI();
	}
	
	private void createUI(){
		this.setLayout(new BorderLayout());
		
		JPanel headerPanel = createHeaderPanel();
		this.add(headerPanel, BorderLayout.NORTH);
				
		createChartPanel();
		
		this.add(chartPanel, BorderLayout.CENTER);
	}
	
	private void createChartPanel(){
		JFreeChart profileChart = MorphologyChartFactory.makeBlankProbabililtyChart();
		chartPanel =  new ChartPanel(profileChart);
	}
	
	private JPanel createHeaderPanel(){
		JPanel panel = new JPanel(new FlowLayout());

		panel.add(new JLabel("Kruskal-Wallis comparison of datasets (Bonferroni-corrected p-values)"));

		return panel;
		
	}
	
	/**
	 * Create a chart showing the Kruskal-Wallis p-values of comparisons
	 * between curves
	 * @return
	 */
	private void updateChartPanel() throws Exception {

		JFreeChart chart = null;


		ProfileChartOptions options = new ProfileChartOptions(getDatasets(),
				true, // normalised
				ProfileAlignment.LEFT,
				BorderTag.REFERENCE_POINT,
				true, // show markers
				ProfileType.REGULAR);
		
		options.setLogger(programLogger);


		if(getChartCache().hasChart(options)){
			chart = getChartCache().getChart(options);
		} else {
			chart = MorphologyChartFactory.makeKruskalWallisChart(options);
			getChartCache().addChart(options, chart);
		}

		chartPanel.setChart(chart);

	}
		
	/**
	 * Update the panel with data from the given datasets
	 * @throws Exception 
	 */
	@Override
	public void updateDetail() {
		programLogger.log(Level.FINE, "Updating Kruskal panel");

		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				try{
					
					// Only create a chart if datasets are available
					if(!getDatasets().isEmpty() && getDatasets()!=null){

						// Only create a chart if exactly two datasets are selected
						if(getDatasets().size()==2){
							updateChartPanel();
							
						} else {
							// null chart
							JFreeChart chart = MorphologyChartFactory.makeBlankProbabililtyChart();
							chartPanel.setChart(chart);

						}
						programLogger.log(Level.FINEST, "Updated Kruskal panel");
					} else {
						// null chart
						JFreeChart chart = MorphologyChartFactory.makeBlankProbabililtyChart();
						chartPanel.setChart(chart);
					}
				} catch (Exception e) {
					programLogger.log(Level.SEVERE, "Error making Kruskal panel", e);
				} finally {
					setUpdating(false);
				}
			}});
	} 
	

}
