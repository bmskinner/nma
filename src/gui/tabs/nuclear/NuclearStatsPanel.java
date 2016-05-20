package gui.tabs.nuclear;

import java.awt.BorderLayout;
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
import gui.components.ExportableTable;
import gui.tabs.DetailPanel;

@SuppressWarnings("serial")
public class NuclearStatsPanel extends DetailPanel {
	
	private ExportableTable tablePopulationStats;
	
	public NuclearStatsPanel(){
		super();
		
		this.setLayout(new BorderLayout());
		
		JScrollPane statsPanel = createStatsPanel();

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
	protected void updateSingle() throws Exception {
		updateMultiple() ;
	}
	

	@Override
	protected void updateMultiple() throws Exception {
		
		updateStatsPanel();
		log(Level.FINEST, "Updated analysis stats panel");
	}
	
	@Override
	protected void updateNull() throws Exception {
		updateMultiple() ;
	}
				
	
	/**
	 * Update the stats panel with data from the given datasets
	 * @param list the datasets
	 */
	private void updateStatsPanel(){
		try{

			TableOptions options = new TableOptionsBuilder()
			.setDatasets(getDatasets())
			.setType(TableType.ANALYSIS_STATS)
			.build();

			TableModel model = getTable(options);


			tablePopulationStats.setModel(model);
		} catch(Exception e){
			log(Level.SEVERE, "Error updating stats panel", e);
		}
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

}
