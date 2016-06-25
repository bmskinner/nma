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
package gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import gui.LoadingIconDialog;
import gui.components.ExportableChartPanel;
import gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jfree.chart.JFreeChart;

import components.generic.BorderTag;
import components.generic.ProfileType;
import charting.charts.MorphologyChartFactory;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import analysis.AnalysisDataset;

@SuppressWarnings("serial")
public class KruskalTestDialog  extends LoadingIconDialog {

	private AnalysisDataset dataset1;
	private AnalysisDataset dataset2;
	
	private ExportableChartPanel chartPanel;

	private JButton  runButton;

	public KruskalTestDialog(final AnalysisDataset dataset1, final AnalysisDataset dataset2){
		super();
		this.dataset1 = dataset1;
		this.dataset2 = dataset2;
		createUI();
		this.setModal(false);
		this.pack();
		this.setVisible(true);
	}

	private void createUI(){
		this.setTitle("Kruskal test: "+dataset1.getName()+" vs "+dataset2.getName());
		this.setLayout(new BorderLayout());
		this.setLocationRelativeTo(null);

		this.add(createSettingsPanel(), BorderLayout.NORTH);

		chartPanel = new ExportableChartPanel(MorphologyChartFactory.getInstance().makeEmptyChart());
		this.add(chartPanel, BorderLayout.CENTER);


	}

	private JPanel createSettingsPanel(){
		JPanel panel = new JPanel(new FlowLayout());
		
		JLabel label = new JLabel("Normalise segment lengths between datasets, and rerun the Kruskal-Wallis comparison");
		panel.add(label);

		runButton = new JButton("Run");
		runButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) { 

				Thread thr = new Thread(){
					public void run(){

						try {
							runAnalysis();
						} catch (Exception e) {
							log(Level.SEVERE, "Error testing", e);
						}
					}
				};
				thr.start();


			}
		});	
		panel.add(runButton);

		return panel;
	}

	/**
	 * Toggle wait cursor on element
	 * @param b
	 */
	private void setAnalysing(boolean b){
		if(b){
			this.setEnabled(false);
			for(Component c : this.getComponents()){
				c.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)); //new Cursor(Cursor.WAIT_CURSOR));
			}

			this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		} else {
			this.setEnabled(true);
			for(Component c : this.getComponents()){
				c.setCursor(Cursor.getDefaultCursor());
			}
			this.setCursor(Cursor.getDefaultCursor());
		}
	}

	public void setEnabled(boolean b){
		runButton.setEnabled(b);
	}

	private void runAnalysis() throws Exception {

		setAnalysing(true);
		log(Level.INFO, "Franken-normalising collections");
		// Clear the old chart
		chartPanel.setChart(MorphologyChartFactory.getInstance().makeEmptyChart());
		
		List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
		list.add(dataset1);
		list.add(dataset2);
		
		ChartOptions options = new ChartOptionsBuilder()
			.setDatasets(list)
			.setNormalised(true)
			.setAlignment(ProfileAlignment.LEFT)
			.setTag(BorderTag.REFERENCE_POINT)
			.setShowMarkers(false)
			.setProfileType(ProfileType.FRANKEN)
			.build();
				
		JFreeChart chart = MorphologyChartFactory.makeKruskalWallisChart(options, true);
		chartPanel.setChart(chart);

		setAnalysing(false);
		log(Level.INFO, "Comparison complete");
	}
}

