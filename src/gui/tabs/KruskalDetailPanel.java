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
import gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;
import gui.dialogs.KruskalTestDialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.table.TableModel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import analysis.profiles.ProfileManager;
import components.generic.BorderTag;
import components.generic.ProfileType;
import charting.charts.MorphologyChartFactory;
import charting.datasets.NucleusTableDatasetCreator;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import charting.options.TableOptions;

@SuppressWarnings("serial")
public class KruskalDetailPanel  extends DetailPanel {
	
	private ExportableChartPanel chartPanel;
	JButton frankenButton = new JButton("Compare frankenprofiles");

	public KruskalDetailPanel( ) throws Exception {
		super();
		
		createUI();
		
		setEnabled(false);
	}
	
	@Override
	public void setEnabled(boolean b){
		super.setEnabled(b);
		frankenButton.setEnabled(b);
		
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
		chartPanel =  new ExportableChartPanel(profileChart);
	}
	
	private JPanel createHeaderPanel(){
		JPanel panel = new JPanel(new FlowLayout());

		panel.add(new JLabel("Kruskal-Wallis comparison of datasets (Bonferroni-corrected p-values)"));
		
		frankenButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) { 
				
				Thread thr = new Thread(){
					public void run(){
						
						try {
							new KruskalTestDialog(getDatasets().get(0), getDatasets().get(1) );
						} catch (Exception e) {
							log(Level.SEVERE, "Error testing", e);
						}
					}
				};
				thr.start();
			}
			
		});	
		panel.add(frankenButton);

		return panel;
		
	}
	
	/**
	 * Create a chart showing the Kruskal-Wallis p-values of comparisons
	 * between curves
	 * @return
	 */
	private void updateChartPanel() {


		ChartOptions options = new ChartOptionsBuilder()
			.setDatasets(getDatasets())
			.setNormalised(true)
			.setAlignment(ProfileAlignment.LEFT)
			.setTag(BorderTag.REFERENCE_POINT)
			.setShowMarkers(true)
			.setProfileType(ProfileType.REGULAR)
			.build();
		

		JFreeChart chart = getChart(options);

		chartPanel.setChart(chart);

	}
	
	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) throws Exception {
		return MorphologyChartFactory.makeKruskalWallisChart(options, false);
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options) throws Exception{
		return null;
	}
	
	@Override
	protected void updateSingle() {
		updateNull();
	}
	
	@Override
	protected void updateMultiple() {
		if(getDatasets().size()==2){ // Only create a chart if exactly two datasets are selected
			
			// Only allow a franken normlisation if datasets have the same number of segments
			if(ProfileManager.segmentCountsMatch(getDatasets())){
				setEnabled(true);
			} else {
				setEnabled(false);
			}
			
		
			updateChartPanel();
			
		} else {
			updateNull();

		}
		log(Level.FINEST, "Updated Kruskal panel");
	}
	
	@Override
	protected void updateNull()  {
		setEnabled(false);
		JFreeChart chart = MorphologyChartFactory.makeBlankProbabililtyChart();
		chartPanel.setChart(chart);
	}	

}
