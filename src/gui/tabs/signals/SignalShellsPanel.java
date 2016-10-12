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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
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
public class SignalShellsPanel extends DetailPanel implements ActionListener {

	private ExportableChartPanel 	chartPanel; 

	private JRadioButton proportionsBtn = new JRadioButton("Proportions");
	private JRadioButton countsBtn      = new JRadioButton("Counts");
	private ButtonGroup  buttonGroup    = new ButtonGroup();
	
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
		
		buttonGroup.add(proportionsBtn);
		buttonGroup.add(countsBtn);
		proportionsBtn.addActionListener(this);
		countsBtn.addActionListener(this);
		
		proportionsBtn.setSelected(true);
		
		panel.add(countsBtn);
		panel.add(proportionsBtn);
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

	
	private void updateChartAndTable(){
		ChartOptions options = new ChartOptionsBuilder()
		.setDatasets(getDatasets())
		.setShowSignals(countsBtn.isSelected()) // if counts is selected, show signal counts, not proportions
		.setTarget(chartPanel)
		.build();

		setChart(options);

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

	@Override
	public void actionPerformed(ActionEvent arg0) {
		updateChartAndTable();
		
	}
}
