package gui.tabs.nuclear;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import charting.charts.ScatterChartFactory;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import charting.options.TableOptions;
import gui.components.ExportableChartPanel;
import gui.tabs.DetailPanel;
import stats.NucleusStatistic;

@SuppressWarnings("serial")
public class NuclearScatterChartPanel extends DetailPanel implements ActionListener {

	protected ExportableChartPanel chartPanel; // hold the charts
	protected JPanel               headerPanel; // hold buttons
	
	private JComboBox<NucleusStatistic> statABox = new JComboBox<NucleusStatistic>(NucleusStatistic.values());
	private JComboBox<NucleusStatistic> statBBox = new JComboBox<NucleusStatistic>(NucleusStatistic.values());
	
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
		
		this.add(headerPanel, BorderLayout.NORTH);
		
		
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
		
	}

	@Override
	protected TableModel createPanelTableType(TableOptions options) throws Exception {
		return null;
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
