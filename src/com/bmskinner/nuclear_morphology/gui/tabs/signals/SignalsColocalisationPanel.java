package com.bmskinner.nuclear_morphology.gui.tabs.signals;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.charting.charts.AbstractChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.ViolinChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.charting.datasets.AbstractDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.NuclearSignalDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptionsBuilder;
import com.bmskinner.nuclear_morphology.gui.GlobalOptions;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;

/**
 * Show the minimum distances between signals within a dataset
 * @author ben
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class SignalsColocalisationPanel extends DetailPanel {
	
	private static final String HEADER_LBL = "Pairwise distances between the closest signal pairs";
	private static final String TABLE_TOOLTIP = "Median distance between closest signal pairs";
	
//	private ExportableTable table;			// table for analysis parameters
	
	private ExportableChartPanel violinChart;


	public SignalsColocalisationPanel(){
		super();
		this.setLayout(new BorderLayout());
		
		JPanel header    = createHeader();
		JPanel mainPanel = createMainPanel();
		
		
		this.add(header,    BorderLayout.NORTH);
		this.add(mainPanel, BorderLayout.CENTER);
	}
	
	/**
	 * Create the header panel
	 * @return
	 */
	private JPanel createHeader(){
		JPanel panel = new JPanel();
		
		JLabel label = new JLabel(HEADER_LBL);
		
		panel.add(label);
		return panel;
	}
	
	/**
	 * Create the main panel with charts and tables
	 * @return
	 */
	private JPanel createMainPanel(){
		JPanel panel = new JPanel( new BorderLayout());
		
//		table  = new ExportableTable(AbstractDatasetCreator.createBlankTable());
//		table.setToolTipText(TABLE_TOOLTIP);
//
//		table.setEnabled(false);
//		JScrollPane scrollPane = new JScrollPane(table);
//		panel.add(scrollPane, BorderLayout.WEST);
		
		violinChart = new ExportableChartPanel( AbstractChartFactory.createEmptyChart());
		panel.add(violinChart, BorderLayout.CENTER);
		
		return panel;
	}
		
	@Override
	protected void updateSingle() {
		
//		TableOptions options = new TableOptionsBuilder()
//			.setDatasets(getDatasets())
//			.setScale(GlobalOptions.getInstance().getScale())
//			.setTarget(table)
//			.build();
//		
//		setTable(options);	
		
		ChartOptions chartOptions = new ChartOptionsBuilder()
				.setDatasets(getDatasets())
				.setScale(GlobalOptions.getInstance().getScale())
				.setTarget(violinChart)
				.build();
		
		setChart(chartOptions);
		
	}

	@Override
	protected void updateMultiple() {
//		table.setModel(AbstractDatasetCreator.createBlankTable());		
		
		ChartOptions chartOptions = new ChartOptionsBuilder()
				.setDatasets(getDatasets())
				.setScale(GlobalOptions.getInstance().getScale())
				.setTarget(violinChart)
				.build();
		
		setChart(chartOptions);
	}

	@Override
	protected void updateNull() {
//		table.setModel(AbstractDatasetCreator.createBlankTable());		
		violinChart.setChart(AbstractChartFactory.createEmptyChart());
	}
	
	@Override
	public void setChartsAndTablesLoading(){
		super.setChartsAndTablesLoading();
//		table.setModel(AbstractDatasetCreator.createLoadingTable());	
		violinChart.setChart(AbstractChartFactory.createLoadingChart());
		
	}
	
	
	@Override
	protected JFreeChart createPanelChartType(ChartOptions options){
		return new ViolinChartFactory(options).createSignalColocalisationViolinChart();
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options){
		return new NuclearSignalDatasetCreator().createSignalColocalisationTable(options);
	}
}
