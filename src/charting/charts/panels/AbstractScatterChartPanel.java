package charting.charts.panels;

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

import components.CellCollection;
import components.ICellCollection;
import components.generic.MeasurementScale;
import analysis.IAnalysisDataset;
import charting.charts.ScatterChartFactory;
import charting.datasets.AnalysisDatasetTableCreator;
import charting.datasets.ScatterChartDatasetCreator;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import charting.options.TableOptions;
import charting.options.TableOptionsBuilder;
import gui.GlobalOptions;
import gui.InterfaceEvent.InterfaceMethod;
import gui.components.ExportableTable;
import gui.tabs.DetailPanel;
import stats.PlottableStatistic;

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
				.build();
		
		rhoTable.setModel(getTable(tableOptions));
		
		gateButton.setEnabled(true);
	}

	@Override
	protected void updateMultiple() {
		updateSingle();
	}

	@Override
	protected void updateNull() {
		ChartOptions options = new ChartOptionsBuilder()
			.build();
		
		setChart(options);

		
		TableOptions tableOptions = new TableOptionsBuilder()
				.setDatasets(null)
				.setScale(GlobalOptions.getInstance().getScale())
				.build();
		
		rhoTable.setModel(getTable(tableOptions));
		
		gateButton.setEnabled(false);
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options) throws Exception {
		return new ScatterChartDatasetCreator().createSpearmanCorrlationTable(options);
	}

	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) throws Exception {
		return new ScatterChartFactory(options).createScatterChart();
	}
	
	private void gateOnVisible(){
		
		int result = getFilterDialogResult();

		if(result!=0){ // button at index 0 - continue
			return;
		}
		finer("Gating datasets on "+statABox.getSelectedItem().toString()+" and "+statBBox.getSelectedItem().toString());
		
		for(IAnalysisDataset d : getDatasets()){
			
			finest("Filtering dataset "+d.getName());
			
			Range domain = getDomainBounds();
			Range range  = getRangeBounds();
			
			finer("Filtering on "+statABox.getSelectedItem().toString());
			ICellCollection stat1 = d.getCollection()
					.filterCollection((PlottableStatistic) statABox.getSelectedItem(),
							MeasurementScale.PIXELS, 
							domain.getLowerBound(), domain.getUpperBound());
			
			if(stat1 == null){
				finest("No collection returned");
				// filtering on given PlottableStatistic is not yet implemented
				continue;
			}
			
			if( ! stat1.hasCells()){
				finest("No cells returned for "+statABox.getSelectedItem().toString());
				continue;
			}
			
			
			finer("Filtering on "+statBBox.getSelectedItem().toString());
			ICellCollection stat2 = stat1
					.filterCollection((PlottableStatistic) statBBox.getSelectedItem(),
							GlobalOptions.getInstance().getScale(),
							range.getLowerBound(), range.getUpperBound());
			
			if(stat2 == null){
				finer("No collection returned");
				// filtering on given PlottableStatistic is not yet implemented
				continue;
			}
			
			if( ! stat2.hasCells()){
				finer("No cells returned for "+statBBox.getSelectedItem().toString());
				continue;
			}
			
			if( stat2.size() ==  d.getCollection().size()){
				finer("Filtered collection is same as starting collection");
				continue;
			}

			stat2.setName("Filtered_"+statABox.getSelectedItem().toString()+"_"+statBBox.getSelectedItem().toString());
			finer("Filtered "+stat2.size()+" cells");
			d.addChildCollection(stat2);
			d.getCollection().getProfileManager().copyCollectionOffsets(stat2);

			
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
