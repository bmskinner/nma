package com.bmskinner.nuclear_morphology.charting.charts.panels;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;
import org.jfree.data.Range;

import com.bmskinner.nuclear_morphology.analysis.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.analysis.nucleus.CollectionFilterer;
import com.bmskinner.nuclear_morphology.analysis.nucleus.CollectionFilterer.CollectionFilteringException;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.charting.charts.AbstractChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.ScatterChartFactory;
import com.bmskinner.nuclear_morphology.charting.datasets.AbstractDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.AnalysisDatasetTableCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.ScatterChartDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.VirtualCellCollection;
import com.bmskinner.nuclear_morphology.gui.GlobalOptions;
import com.bmskinner.nuclear_morphology.gui.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;
import com.bmskinner.nuclear_morphology.stats.PlottableStatistic;

@SuppressWarnings("serial")
public abstract class AbstractScatterChartPanel extends DetailPanel implements ActionListener {
	
	protected ExportableChartPanel chartPanel; // hold the charts
	protected JPanel               headerPanel; // hold buttons
	
	protected JButton gateButton;
	
	protected JComboBox<PlottableStatistic> statABox;
	protected JComboBox<PlottableStatistic> statBBox;
	
	protected ExportableTable rhoTable;
	
	public AbstractScatterChartPanel(PlottableStatistic stat){
		super();
		
		this.setLayout(new BorderLayout());
		
		headerPanel = createHeader(stat);
		
		this.add(headerPanel, BorderLayout.NORTH);
		

		JPanel tablePanel = new JPanel(new BorderLayout());
				
		TableModel model = AnalysisDatasetTableCreator.createBlankTable();
		rhoTable = new ExportableTable(model);
		rhoTable.setEnabled(false);
		tablePanel.add(rhoTable, BorderLayout.CENTER);
		
		JFreeChart chart = ScatterChartFactory.makeEmptyChart();
		
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
	
	private JPanel createHeader(PlottableStatistic stat){
		statABox = new JComboBox<PlottableStatistic>(stat.getValues());
		statBBox = new JComboBox<PlottableStatistic>(stat.getValues());
		
		statABox.addActionListener(this);
		statBBox.addActionListener(this);
		
		gateButton = new JButton("Filter visible");
		gateButton.setToolTipText("Create a sub-population based on the visible values");
		gateButton.addActionListener(this);
		gateButton.setActionCommand("Gate");
		gateButton.setEnabled(false);
		
		JPanel panel = new JPanel(new FlowLayout());
		
		panel.add(new JLabel("X axis"));
		panel.add(statABox);
		panel.add(new JLabel("Y axis"));
		panel.add(statBBox);
		
		panel.add(gateButton);
		panel.add( new JLabel("Spearman's rank correlation coefficients are shown in the table")  );
		
		
		return panel;
	}
	
	@Override
	protected void updateSingle() {
		
		PlottableStatistic statA = (PlottableStatistic) statABox.getSelectedItem();
		PlottableStatistic statB = (PlottableStatistic) statBBox.getSelectedItem();
		
		ChartOptions options = new ChartOptionsBuilder()
				.setDatasets(getDatasets())
				.addStatistic(statA)
				.addStatistic(statB)
				.setScale(GlobalOptions.getInstance().getScale())
				.setSwatch(GlobalOptions.getInstance().getSwatch())
				.setTarget(chartPanel)
				.build();
		
		
		setChart(options);
		
		
		TableOptions tableOptions = new TableOptionsBuilder()
				.setDatasets(getDatasets())
				.addStatistic(statA)
				.addStatistic(statB)
				.setScale(GlobalOptions.getInstance().getScale())
				.setTarget(rhoTable)
				.build();
		
		setTable(tableOptions);

		gateButton.setEnabled(true);
	}

	@Override
	protected void updateMultiple() {
		updateSingle();
	}

	@Override
	protected void updateNull() {

		chartPanel.setChart(AbstractChartFactory.createEmptyChart());
		rhoTable.setModel(AbstractDatasetCreator.createBlankTable());
		gateButton.setEnabled(false);
	}
	
	@Override
	public synchronized void setChartsAndTablesLoading(){
		chartPanel.setChart(AbstractChartFactory.createLoadingChart());		
		rhoTable.setModel(AbstractDatasetCreator.createLoadingTable());
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options) {
		return new ScatterChartDatasetCreator().createSpearmanCorrlationTable(options);
	}

	@Override
	protected JFreeChart createPanelChartType(ChartOptions options){
		return new ScatterChartFactory(options).createScatterChart();
	}
	
	private void gateOnVisible(){
		
		int result = getFilterDialogResult();

		if(result!=0){ // button at index 0 - continue
			return;
		}
		finer("Gating datasets on "+statABox.getSelectedItem().toString()+" and "+statBBox.getSelectedItem().toString());
		
		CollectionFilterer f = new CollectionFilterer();
		
		for(IAnalysisDataset d : getDatasets()){
			
			Range domain = getDomainBounds();
			Range range  = getRangeBounds();
			
			PlottableStatistic statA = (PlottableStatistic) statABox.getSelectedItem();
			PlottableStatistic statB = (PlottableStatistic) statBBox.getSelectedItem();
			
			try {
				
				ICellCollection stat1 = f.filter(d.getCollection(), 
						statA, 
						domain.getLowerBound(), 
						domain.getUpperBound());
				
				
				ICellCollection stat2 = f.filter( stat1,
						statB,
						range.getLowerBound(),
						range.getUpperBound());

				ICellCollection virt = new VirtualCellCollection(d, stat2.getName());	
				for(ICell c : stat2.getCells()){
					virt.addCell(c);
				}

				virt.setName("Filtered_"+statA+"_"+statB);

				d.addChildCollection(virt);
				try {
					
					d.getCollection().getProfileManager().copyCollectionOffsets(virt);
				} catch (ProfileException e) {
					warn("Error copying collection offsets for "+d.getName());
					stack("Error in offsetting", e);
					continue;
				}
				
				
			} catch (CollectionFilteringException e1) {
				stack("Unable to filter collection for "+d.getName(), e1);
				continue;
			}
			
		}
		log("Filtered datasets");
		
		finer("Firing population update request");
		fireInterfaceEvent(InterfaceMethod.REFRESH_POPULATIONS);
	}


	@Override
	public void actionPerformed(ActionEvent e) {
		
		if(e.getActionCommand().equals("Gate")){
			
			gateOnVisible();
			
		} else {
			// A stats box fired, update charts
			update(getDatasets());
		}
	}
	
	protected Range getRangeBounds(){
		return chartPanel.getChart().getXYPlot().getRangeAxis().getRange();
	}
	
	protected Range getDomainBounds(){
		return chartPanel.getChart().getXYPlot().getDomainAxis().getRange();
	}
	
	protected int getFilterDialogResult(){

		Object[] options = { "Filter collection" , "Cancel", };
		int result = JOptionPane.showOptionDialog(null, "Filter selected datasets on visible values?", "Confirm filter",

				JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,

				null, options, options[0]);
		return result;
	}

}
