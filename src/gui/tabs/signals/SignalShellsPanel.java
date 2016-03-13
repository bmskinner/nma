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
package gui.tabs.signals;

import gui.tabs.DetailPanel;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.table.TableModel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.category.StatisticalBarRenderer;
import org.jfree.data.category.CategoryDataset;

import utility.Constants;
import charting.charts.AbstractChartFactory;
import charting.datasets.NuclearSignalDatasetCreator;
import charting.options.ChartOptions;
import charting.options.TableOptions;
import components.CellCollection;
import components.nuclear.ShellResult;


@SuppressWarnings("serial")
public class SignalShellsPanel extends DetailPanel {

	private ChartPanel 	chartPanel; 
	private JLabel 		statusLabel  = new JLabel();
	private JButton 	newAnalysis	 = new JButton("Run new shell analysis");

	public SignalShellsPanel(){
		super();
		this.setLayout(new BorderLayout());
		JFreeChart shellsChart = ChartFactory.createBarChart(null, "Shell", "Percent", null);
		shellsChart.getCategoryPlot().setBackgroundPaint(Color.WHITE);
		shellsChart.getCategoryPlot().getRangeAxis().setRange(0,100);
		chartPanel = new ChartPanel(shellsChart);
		this.add(chartPanel, BorderLayout.CENTER);

		this.add(statusLabel, BorderLayout.NORTH);
		statusLabel.setVisible(false);

		newAnalysis.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				fireSignalChangeEvent("RunShellAnalysis");
			}
		});
		newAnalysis.setVisible(false);
		this.add(newAnalysis, BorderLayout.SOUTH);


	}

	/**
	 * Create a panel to display when a shell analysis is not available
	 * @param showRunButton should there be an option to run a shell analysis on the dataset
	 * @param collection the nucleus collection from the dataset
	 * @param label the text to display on the panel
	 * @return a panel to put in the shell tab
	 */
	private void makeNoShellAnalysisAvailablePanel(boolean showRunButton, CellCollection collection, String label){
		chartPanel.setVisible(false);
		statusLabel.setText(label);
		statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
		statusLabel.setVisible(true);

		newAnalysis.setVisible(showRunButton);

		this.revalidate();
		this.repaint();

	}

	@Override
	protected void updateSingle() throws Exception {
//		AnalysisDataset dataset = list.get(0);
	CellCollection collection = activeDataset().getCollection();

	if(activeDataset().hasShellResult()){ // only if there is something to display

		CategoryDataset ds = NuclearSignalDatasetCreator.createShellBarChartDataset(getDatasets());
		JFreeChart shellsChart = ChartFactory.createBarChart(null, "Outer <--- Shell ---> Interior", "Percent", ds);
		shellsChart.getCategoryPlot().setBackgroundPaint(Color.WHITE);
		shellsChart.getCategoryPlot().getRangeAxis().setRange(0,100);
		StatisticalBarRenderer rend = new StatisticalBarRenderer();
		rend.setBarPainter(new StandardBarPainter());
		rend.setShadowVisible(false);
		rend.setErrorIndicatorPaint(Color.black);
		rend.setErrorIndicatorStroke(new BasicStroke(2));
		shellsChart.getCategoryPlot().setRenderer(rend);

		for (int j = 0; j < ds.getRowCount(); j++) {
			rend.setSeriesVisibleInLegend(j, Boolean.FALSE);
			rend.setSeriesStroke(j, new BasicStroke(2));
			int index = AbstractChartFactory.getIndexFromLabel( (String) ds.getRowKey((j)));
			Color colour = activeDataset().getSignalGroupColour(index);
			rend.setSeriesPaint(j, colour);
		}	

		chartPanel.setChart(shellsChart);
		chartPanel.setVisible(true);

		statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
		String label = "";
		for(int i=1; i<=activeDataset().getHighestSignalGroup();i++){
			ShellResult r = activeDataset().getShellResult(i);
			label += "Group "+i+": p="+r.getChiSquare();
			String sig 	= r.getChiSquare() < Constants.FIVE_PERCENT_SIGNIFICANCE_LEVEL 
					? "Significantly different to random at 5% level"
							: "Not significantly different to random at 5% level";

			label += "; "+sig+"\n";

		}
		statusLabel.setText(label);
		statusLabel.setVisible(true);


		newAnalysis.setVisible(false);

	} else { // no shell analysis available

		if(collection.getSignalManager().hasSignals()){
			// if signals, offer to run
			makeNoShellAnalysisAvailablePanel(true, collection, "No shell results available"); // allow option to run analysis
		} else {
			// otherwise don't show button
			makeNoShellAnalysisAvailablePanel(false, null, "No signals in population"); // container in tab if no shell chart
		}
	}
		
	}

	@Override
	protected void updateMultiple() throws Exception {
		// Multiple populations. Do not display
		// container in tab if no shell chart
		makeNoShellAnalysisAvailablePanel(false, null, "Cannot display shell results for multiple populations");
	
		
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
