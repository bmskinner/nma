package gui.tabs.nuclear;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import charting.charts.ScatterChartFactory;
import charting.datasets.NucleusTableDatasetCreator;
import charting.datasets.ScatterChartDatasetCreator;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import charting.options.TableOptions;
import charting.options.TableOptionsBuilder;
import gui.components.ExportableChartPanel;
import gui.components.ExportableTable;
import gui.tabs.DetailPanel;
import stats.NucleusStatistic;

@SuppressWarnings("serial")
public class NuclearScatterChartPanel extends DetailPanel implements ActionListener {

	protected ExportableChartPanel chartPanel; // hold the charts
	protected JPanel               headerPanel; // hold buttons
	
	private JComboBox<NucleusStatistic> statABox = new JComboBox<NucleusStatistic>(NucleusStatistic.values());
	private JComboBox<NucleusStatistic> statBBox = new JComboBox<NucleusStatistic>(NucleusStatistic.values());
	
	private JLabel correlationLabel = new JLabel("Spearman's rank correlation coefficients are shown in the table");

	private ExportableTable rhoTable;
	
	public NuclearScatterChartPanel(){
		super();
		createUI();
	}
	
	
	private void createUI(){
		
		this.setLayout(new BorderLayout());
		
		JFreeChart chart = ScatterChartFactory.getInstance().createEmptyScatterChart();
		
		chartPanel = new ExportableChartPanel(chart);
		this.add(chartPanel, BorderLayout.CENTER);
		
		
		headerPanel = new JPanel(new FlowLayout());
		
		headerPanel.add(statABox);
		headerPanel.add(statBBox);
		
		statABox.addActionListener(this);
		statBBox.addActionListener(this);
		
		headerPanel.add(correlationLabel);
		
		this.add(headerPanel, BorderLayout.NORTH);
				
		JPanel tablePanel = new JPanel(new BorderLayout());
		
		tablePanel.setMinimumSize(new Dimension(300, 200));
		
		TableModel model = NucleusTableDatasetCreator.getInstance().createBlankTable();
		rhoTable = new ExportableTable(model);
		rhoTable.setEnabled(false);
		tablePanel.add(rhoTable, BorderLayout.CENTER);
		
		JScrollPane scrollPane  = new JScrollPane();
		scrollPane.setViewportView(tablePanel);
		scrollPane.setColumnHeaderView(rhoTable.getTableHeader());
		
		this.add(scrollPane, BorderLayout.WEST);
		
		
		
	}
	
	@Override
	protected void updateSingle() throws Exception {
		
		NucleusStatistic statA = (NucleusStatistic) statABox.getSelectedItem();
		NucleusStatistic statB = (NucleusStatistic) statBBox.getSelectedItem();
		
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
		
//		updateSpearmanCorrelations(options);
		
	}
	
//	private void updateSpearmanCorrelations(ChartOptions options){
//		List<Double> correlations = ScatterChartDatasetCreator
//				.getInstance()
//				.createNucleusSpearmanCorrelation(options);
//		
//		String correlationString = "Spearman's rho: ";
//		DecimalFormat df = new DecimalFormat("#0.00"); 
//		for(Double d : correlations){
//			correlationString += df.format(d)+" ";
//		}
//		correlationLabel.setText(correlationString);
//	}

	@Override
	protected void updateMultiple() throws Exception {
		updateSingle();
//		correlationLabel.setText("");
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
		return ScatterChartDatasetCreator.getInstance().createNucleusSpearmanCorrlationTable(options);
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
