package gui.tabs;

import gui.components.ProfileAlignmentOptionsPanel.ProfileAlignment;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.table.TableModel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import components.generic.BorderTag;
import components.generic.ProfileCollectionType;
import stats.NucleusStatistic;
import charting.NucleusStatsTableOptions;
import charting.TableOptions;
import charting.charts.MorphologyChartFactory;
import charting.charts.ProfileChartOptions;
import charting.datasets.NucleusTableDatasetCreator;
import analysis.AnalysisDataset;

@SuppressWarnings("serial")
public class KruskalDetailPanel  extends DetailPanel {
	
	private ChartPanel chartPanel;

	public KruskalDetailPanel(Logger programLogger ) throws Exception {
		super(programLogger);
		
		createUI();
	}
	
	private void createUI(){
		this.setLayout(new BorderLayout());
		
		JPanel headerPanel = createHeaderPanel();
		this.add(headerPanel, BorderLayout.NORTH);
				
		createChartPanel();
		
		this.add(chartPanel, BorderLayout.CENTER);
	}
	
	private void createChartPanel(){
		JFreeChart profileChart = MorphologyChartFactory.makeBlankProbabililtyChart();
		chartPanel =  new ChartPanel(profileChart);
	}
	
	private JPanel createHeaderPanel(){
		JPanel panel = new JPanel(new FlowLayout());

		panel.add(new JLabel("Kruskal-Wallis comparison of datasets (Bonferroni-corrected p-values)"));

		return panel;
		
	}
	
	/**
	 * Create a chart showing the Kruskal-Wallis p-values of comparisons
	 * between curves
	 * @return
	 */
	private void updateChartPanel() throws Exception {

		JFreeChart chart = null;


		ProfileChartOptions options = new ProfileChartOptions(getDatasets(),
				true, // normalised
				ProfileAlignment.LEFT,
				BorderTag.REFERENCE_POINT,
				true, // show markers
				ProfileCollectionType.REGULAR);
		
		options.setLogger(programLogger);


		if(getChartCache().hasChart(options)){
			chart = getChartCache().getChart(options);
		} else {
			chart = MorphologyChartFactory.makeKruskalWallisChart(options);
			getChartCache().addChart(options, chart);
		}

		chartPanel.setChart(chart);

	}
		
	/**
	 * Update the panel with data from the given datasets
	 * @throws Exception 
	 */
	@Override
	public void updateDetail() {
		programLogger.log(Level.FINE, "Updating Kruskal panel");

		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				try{
					
					// Only create a chart if datasets are available
					if(!getDatasets().isEmpty() && getDatasets()!=null){

						// Only create a chart if exactly two datasets are selected
						if(getDatasets().size()==2){
							updateChartPanel();
							
						} else {
							// null chart
							JFreeChart chart = MorphologyChartFactory.makeBlankProbabililtyChart();
							chartPanel.setChart(chart);

						}
						programLogger.log(Level.FINEST, "Updated Kruskal panel");
					} else {
						// null chart
						JFreeChart chart = MorphologyChartFactory.makeBlankProbabililtyChart();
						chartPanel.setChart(chart);
					}
				} catch (Exception e) {
					programLogger.log(Level.SEVERE, "Error making Kruskal panel", e);
				} finally {
					setUpdating(false);
				}
			}});
	} 
	

}
