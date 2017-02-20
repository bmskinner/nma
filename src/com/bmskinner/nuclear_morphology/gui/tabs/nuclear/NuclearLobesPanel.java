package com.bmskinner.nuclear_morphology.gui.tabs.nuclear;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.charting.charts.BoxplotChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.MorphologyChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.ViolinChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ViolinChartPanel;
import com.bmskinner.nuclear_morphology.charting.datasets.AbstractTableCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.NucleusTableCreator;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.gui.GlobalOptions;
import com.bmskinner.nuclear_morphology.gui.SignalChangeEvent;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;

@SuppressWarnings("serial")
public class NuclearLobesPanel extends DetailPanel {
	
	
	private static final String RUN_LOBE_DETECTION_LBL = "Run lobe detection";
	private JButton runLobeDetectionBtn;
	
	private ExportableChartPanel chartPanel;
	private ExportableTable table;
	
	public NuclearLobesPanel(){
		
		this.setLayout(new BorderLayout());
		
		JPanel header = createHeader();
		this.add(header, BorderLayout.NORTH);
		
		chartPanel = createMainPanel();
		this.add(chartPanel, BorderLayout.CENTER);
		
		this.add(createTablePanel(), BorderLayout.WEST);
				
	}
	
	private JPanel createHeader(){
		JPanel panel = new JPanel(new FlowLayout());
				
		runLobeDetectionBtn = new JButton(RUN_LOBE_DETECTION_LBL);
		runLobeDetectionBtn.addActionListener( a -> {
			fireSignalChangeEvent(SignalChangeEvent.LOBE_DETECTION);
		}  );
		runLobeDetectionBtn.setEnabled(false);
		
		panel.add(runLobeDetectionBtn);
		return panel;
	}
	
	private ExportableChartPanel createMainPanel(){
		JFreeChart chart = BoxplotChartFactory.makeEmptyChart();
		return new ViolinChartPanel(chart);
	}
	
	private JPanel createTablePanel(){
		JPanel panel = new JPanel(new BorderLayout());
				
		table = new ExportableTable(AbstractTableCreator.createBlankTable());
		
		JScrollPane pane = new JScrollPane(table);
		
		panel.add(pane);
		return panel;
	}
	
	
	@Override
	protected void updateSingle() {
		super.updateSingle();
		
		finest("Passing to update multiple in "+this.getClass().getName());
		updateMultiple();
		runLobeDetectionBtn.setEnabled(true);	
	}

	@Override
	protected void updateMultiple() {
		super.updateMultiple();
		runLobeDetectionBtn.setEnabled(false);	
		ChartOptions options = new ChartOptionsBuilder()
			.setDatasets(getDatasets())
			.addStatistic(PlottableStatistic.LOBE_COUNT)
			.setScale(GlobalOptions.getInstance().getScale())
			.setSwatch(GlobalOptions.getInstance().getSwatch())
			.setTarget(chartPanel)
			.build();
		
		TableOptions tableOptions = new TableOptionsBuilder()
			.addStatistic(PlottableStatistic.LOBE_COUNT)
			.setDatasets(getDatasets())
			.setTarget(table)
			.build();

		setChart(options);
		setTable(tableOptions);

		
	}

	@Override
	protected void updateNull() {
		super.updateNull();
		finest("Passing to update multiple in "+this.getClass().getName());
		updateMultiple();
	}
	
	@Override
	public void setChartsAndTablesLoading(){
		super.setChartsAndTablesLoading();
		chartPanel.setChart(MorphologyChartFactory.createLoadingChart());
		table.setModel(AbstractTableCreator.createLoadingTable());
	}
	
	@Override
	protected JFreeChart createPanelChartType(ChartOptions options){
		if(GlobalOptions.getInstance().isViolinPlots()){
			return new ViolinChartFactory(options).createStatisticPlot(CellularComponent.NUCLEUS);
		} else {
			return new BoxplotChartFactory(options).createStatisticBoxplot(CellularComponent.NUCLEUS);
		}
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options){
		return new NucleusTableCreator(options).createLobeDetectionOptionsTable();
	}
	
}
