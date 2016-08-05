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

import charting.datasets.NucleusTableDatasetCreator;
import charting.options.ChartOptions;
import charting.options.TableOptions;
import charting.options.TableOptionsBuilder;
import charting.options.TableOptions.TableType;
import gui.GlobalOptions;
import gui.components.ExportableTable;
import gui.components.panels.MeasurementUnitSettingsPanel;
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
	protected JFreeChart createPanelChartType(ChartOptions options) throws Exception {
		return null;
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options) throws Exception{
		return NucleusTableDatasetCreator.getInstance().createAnalysisTable(options);
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
				
	
	/**
	 * Update the stats panel with data from the given datasets
	 * @param list the datasets
	 */
	private void updateStatsPanel(){

		finest("Updating stats panel");
		
//		if(this.hasDatasets()){
//			measurementUnitSettingsPanel.setEnabled(true);
//		} else {
//			measurementUnitSettingsPanel.setEnabled(false);
//		}
		
		TableOptions options = new TableOptionsBuilder()
			.setDatasets(getDatasets())
			.setType(TableType.ANALYSIS_STATS)
			.setScale(GlobalOptions.getInstance().getScale())
			.build();

		finest("Built table options");
		TableModel model = getTable(options);

		finest("Fetched table model");

		tablePopulationStats.setModel(model);

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

			TableModel model = getTable(options);
			
			tablePopulationStats.setModel(model);
			
		}catch(Exception e){
			log(Level.SEVERE, "Error creating stats panel", e);
		}
		return scrollPane;
	}

	 @Override
     public void actionPerformed(ActionEvent e) {

         try {
        	 log(Level.FINEST, "Updating nucleus stats panel");
             this.update(getDatasets());
         } catch (Exception e1) {
         	log(Level.SEVERE, "Error updating boxplot panel from action listener", e1);
         }
         
         
     }

}
