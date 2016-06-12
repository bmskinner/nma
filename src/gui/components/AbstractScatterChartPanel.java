package gui.components;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import charting.charts.ScatterChartFactory;
import charting.datasets.NucleusTableDatasetCreator;
import charting.datasets.ScatterChartDatasetCreator;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import charting.options.TableOptions;
import charting.options.TableOptionsBuilder;
import gui.tabs.DetailPanel;
import stats.PlottableStatistic;

@SuppressWarnings("serial")
public abstract class AbstractScatterChartPanel extends DetailPanel implements ActionListener {
	
	protected ExportableChartPanel chartPanel; // hold the charts
	protected JPanel               headerPanel; // hold buttons
	
	protected JLabel correlationLabel = new JLabel("Spearman's rank correlation coefficients are shown in the table");
	
	protected JComboBox<PlottableStatistic> statABox;
	protected JComboBox<PlottableStatistic> statBBox;
	
	protected ExportableTable rhoTable;
	
	public AbstractScatterChartPanel(PlottableStatistic stat){
		super();
		
		this.setLayout(new BorderLayout());
		
		statABox = new JComboBox<PlottableStatistic>(stat.getValues());
		statBBox = new JComboBox<PlottableStatistic>(stat.getValues());
		
		statABox.addActionListener(this);
		statBBox.addActionListener(this);
		
		headerPanel = new JPanel(new FlowLayout());
		
		headerPanel.add(statABox);
		headerPanel.add(statBBox);
		
		this.add(headerPanel, BorderLayout.NORTH);
		
		headerPanel.add(correlationLabel);

		JPanel tablePanel = new JPanel(new BorderLayout());
				
		TableModel model = NucleusTableDatasetCreator.getInstance().createBlankTable();
		rhoTable = new ExportableTable(model);
		rhoTable.setEnabled(false);
		tablePanel.add(rhoTable, BorderLayout.CENTER);
		
		JFreeChart chart = ScatterChartFactory.getInstance().createEmptyScatterChart();
		
		chartPanel = new ExportableChartPanel(chart);
		this.add(chartPanel, BorderLayout.CENTER);
		
		JScrollPane scrollPane  = new JScrollPane();
		scrollPane.setViewportView(tablePanel);
		scrollPane.setColumnHeaderView(rhoTable.getTableHeader());
		Dimension size = new Dimension(300, 200);
		scrollPane.setMinimumSize(size);
		scrollPane.setPreferredSize(size);
		
		this.add(scrollPane, BorderLayout.WEST);
	}
	
	@Override
	protected void updateSingle() throws Exception {
		
		PlottableStatistic statA = (PlottableStatistic) statABox.getSelectedItem();
		PlottableStatistic statB = (PlottableStatistic) statBBox.getSelectedItem();
		
		ChartOptions options = new ChartOptionsBuilder()
				.setDatasets(getDatasets())
				.addStatistic(statA)
				.addStatistic(statB)
				.build();
		
		chartPanel.setChart(getChart(options));
		
		
		TableOptions tableOptions = new TableOptionsBuilder()
				.setDatasets(getDatasets())
				.addStatistic(statA)
				.addStatistic(statB)
				.build();
		
		rhoTable.setModel(getTable(tableOptions));
		
		
	}

	@Override
	protected void updateMultiple() throws Exception {
		updateSingle();
	}

	@Override
	protected void updateNull() throws Exception {
		ChartOptions options = new ChartOptionsBuilder()
			.setDatasets(null)
			.build();
		
		chartPanel.setChart(getChart(options));
		
		TableOptions tableOptions = new TableOptionsBuilder()
				.setDatasets(null)
				.build();
		
		rhoTable.setModel(getTable(tableOptions));
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options) throws Exception {
		return ScatterChartDatasetCreator.getInstance().createSpearmanCorrlationTable(options);
	}

	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) throws Exception {
		return ScatterChartFactory.getInstance().createScatterChart(options);
	}


	@Override
	public void actionPerformed(ActionEvent arg0) {
		update(getDatasets());
	}

}
