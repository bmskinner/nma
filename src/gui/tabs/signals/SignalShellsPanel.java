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

import gui.components.ExportableTable;
import gui.tabs.DetailPanel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;
import charting.charts.ExportableChartPanel;
import charting.charts.NuclearSignalChartFactory;
import charting.datasets.NuclearSignalDatasetCreator;
import charting.datasets.NucleusTableDatasetCreator;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import charting.options.TableOptions;
import charting.options.TableOptionsBuilder;


@SuppressWarnings("serial")
public class SignalShellsPanel extends DetailPanel {

	private ExportableChartPanel 	chartPanel; 
	private JLabel 		statusLabel  = new JLabel("Shell analysis results");
	private JButton 	newAnalysis	 = new JButton("Run new shell analysis");
	protected ExportableTable table;

	public SignalShellsPanel(){
		super();
		this.setLayout(new BorderLayout());
		
		JPanel header          = createHeader();
		JPanel mainPanel       = createChartPanel();
		JScrollPane tablePanel = createTablePanel();
		
			
		this.add(tablePanel, BorderLayout.WEST);
		this.add(mainPanel,  BorderLayout.CENTER);		
		this.add(header,     BorderLayout.NORTH);

	}
	
	private JPanel createHeader(){
		JPanel panel = new JPanel();
		panel.add(statusLabel);
		return panel;
	}
	
	private JScrollPane createTablePanel(){
		JPanel tablePanel = new JPanel(new BorderLayout());
		
		TableModel model = NucleusTableDatasetCreator.getInstance().createBlankTable();
		table = new ExportableTable(model);
		table.setEnabled(false);
		tablePanel.add(table, BorderLayout.CENTER);
		
		JScrollPane scrollPane  = new JScrollPane();
		scrollPane.setViewportView(tablePanel);
		scrollPane.setColumnHeaderView(table.getTableHeader());
		Dimension size = new Dimension(300, 200);
		scrollPane.setMinimumSize(size);
		scrollPane.setPreferredSize(size);

		return scrollPane;
	}
	
	private JPanel createChartPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		
		ChartOptions options = new ChartOptionsBuilder()
			.build();
	
		JFreeChart chart = getChart(options);
		
		chartPanel = new ExportableChartPanel(chart);
		
		panel.add(chartPanel, BorderLayout.CENTER);
		
		newAnalysis.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				fireSignalChangeEvent("RunShellAnalysis");
			}
		});
		newAnalysis.setVisible(false);
		panel.add(newAnalysis, BorderLayout.SOUTH);
		
		return panel;
	}

//	/**
//	 * Create a panel to display when a shell analysis is not available
//	 * @param showRunButton should there be an option to run a shell analysis on the dataset
//	 * @param collection the nucleus collection from the dataset
//	 * @param label the text to display on the panel
//	 * @return a panel to put in the shell tab
//	 */
//	private void makeNoShellAnalysisAvailablePanel(boolean showRunButton, CellCollection collection, String label){
//		chartPanel.setVisible(false);
//		newAnalysis.setVisible(showRunButton);
//
//		this.revalidate();
//		this.repaint();
//
//	}
	
	private void updateChartAndTable(){
		ChartOptions options = new ChartOptionsBuilder()
		.setDatasets(getDatasets())
		.setTarget(chartPanel)
		.build();

		setChart(options);
//		JFreeChart chart = getChart(options);
//
//
//		chartPanel.setChart(chart);
		chartPanel.setVisible(true);


		TableOptions tableOptions = new TableOptionsBuilder()
		.setDatasets(getDatasets())
		.build();

		TableModel model = getTable(tableOptions);
		table.setModel(model);
	}

	@Override
	protected void updateSingle() {

		updateChartAndTable();

		newAnalysis.setVisible(false);
		
		if(activeDataset().getCollection().getSignalManager().hasSignals() 
				&& ! activeDataset().getCollection().getSignalManager().hasShellResult()){
			newAnalysis.setVisible(true);
		}
		
	}

	@Override
	protected void updateMultiple() {
		
		updateChartAndTable();
		
	}

	@Override
	protected void updateNull() {
		updateChartAndTable();
		
	}
	
	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) throws Exception {
		return NuclearSignalChartFactory.getInstance().createShellChart(options);
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options) throws Exception{
		return NuclearSignalDatasetCreator.getInstance().createShellChiSquareTable(options);
	}
}
