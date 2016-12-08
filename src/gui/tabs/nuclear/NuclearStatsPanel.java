package gui.tabs.nuclear;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import charting.datasets.AbstractDatasetCreator;
import charting.datasets.AnalysisDatasetTableCreator;
import charting.options.ChartOptions;
import charting.options.TableOptions;
import charting.options.TableOptionsBuilder;
import charting.options.DefaultTableOptions.TableType;
import gui.GlobalOptions;
import gui.components.ExportableTable;
import gui.tabs.DetailPanel;

@SuppressWarnings("serial")
public class NuclearStatsPanel extends DetailPanel implements ActionListener {
	
	private ExportableTable tablePopulationStats;
		
	public NuclearStatsPanel(){
		super();
		
		this.setLayout(new BorderLayout());
		
		JScrollPane statsPanel = createStatsPanel();
		
		JPanel headerPanel = new JPanel(new FlowLayout());

		this.add(headerPanel, BorderLayout.NORTH);

		this.add(statsPanel, BorderLayout.CENTER);

	}
	
	@Override
	protected JFreeChart createPanelChartType(ChartOptions options){
		return null;
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options){
		
		return new AnalysisDatasetTableCreator(options).createAnalysisTable();
	}
	
	@Override
	protected void updateSingle() {
		finest("Passing to update multiple");
		updateMultiple();
	}
	

	@Override
	protected void updateMultiple() {
		super.updateMultiple();
		finest("Updating analysis stats panel");
		updateStatsPanel();
		finest("Updated analysis stats panel");
	}
	
	@Override
	protected void updateNull() {
		super.updateNull();
		finest("Passing to update multiple");
		updateMultiple();
	}
		
	
	@Override
	public void setChartsAndTablesLoading(){
		super.setChartsAndTablesLoading();
		tablePopulationStats.setModel(AbstractDatasetCreator.createLoadingTable());
	}
	
	/**
	 * Update the stats panel with data from the given datasets
	 * @param list the datasets
	 */
	private void updateStatsPanel(){

		finest("Updating stats panel");
		
		TableOptions options = new TableOptionsBuilder()
			.setDatasets(getDatasets())
			.setType(TableType.ANALYSIS_STATS)
			.setScale(GlobalOptions.getInstance().getScale())
			.setTarget(tablePopulationStats)
			.build();
		
		setTable(options);
		finest("Set table model");
		
	}
	
	private JScrollPane createStatsPanel(){
		JScrollPane scrollPane = new JScrollPane();
		try {

			
			JPanel panel = new JPanel();

			panel.setLayout(new BorderLayout(0, 0));

			tablePopulationStats = new ExportableTable();
			panel.add(tablePopulationStats, BorderLayout.CENTER);
			tablePopulationStats.setEnabled(false);

			scrollPane.setViewportView(panel);
			scrollPane.setColumnHeaderView(tablePopulationStats.getTableHeader());
			
			TableOptions options = new TableOptionsBuilder()
				.setDatasets(null)
				.setType(TableType.ANALYSIS_STATS)
				.build();

			TableModel model = new AnalysisDatasetTableCreator(options).createAnalysisTable();
			
			tablePopulationStats.setModel(model);
			
		} catch(Exception e){
			warn("Error making nuclear stats panel");
			stack("Error creating stats panel", e);
		}
		return scrollPane;
	}

	 @Override
     public void actionPerformed(ActionEvent e) {

         try {
        	 finest("Updating nucleus stats panel");
             this.update(getDatasets());
         } catch (Exception e1) {
         	stack("Error updating boxplot panel from action listener", e1);
         }
         
         
     }

}
