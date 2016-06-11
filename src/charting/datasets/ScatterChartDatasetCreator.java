package charting.datasets;

import java.util.ArrayList;
import java.util.List;

import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import analysis.AnalysisDataset;
import charting.charts.ScatterChartFactory;
import charting.options.ChartOptions;
import components.AbstractCellularComponent;
import components.Cell;
import components.CellCollection;
import components.generic.MeasurementScale;
import components.nuclear.BorderPoint;
import stats.NucleusStatistic;
import stats.PlottableStatistic;

public class ScatterChartDatasetCreator {
	
private static ScatterChartDatasetCreator instance = null;
	
	protected ScatterChartDatasetCreator(){}
	
	/**
	 * Fetch an instance of the factory
	 * @return
	 */
	public static ScatterChartDatasetCreator getInstance(){
		if(instance==null){
			instance = new ScatterChartDatasetCreator();
		}
		return instance;
	}




	/**
	 * Get a boxplot dataset for the given statistic for each collection
	 * @param options the charting options
	 * @return
	 * @throws Exception
	 */
	public XYDataset createNucleusScatterDataset(ChartOptions options) {
		List<AnalysisDataset> datasets = options.getDatasets();
		
		List<PlottableStatistic> stats =  options.getStats();
		
		MeasurementScale scale = options.getScale();
		
		NucleusStatistic statA = (NucleusStatistic) stats.get(0);
		NucleusStatistic statB = (NucleusStatistic) stats.get(1);

		DefaultXYDataset ds = new DefaultXYDataset();

		for (int i=0; i < datasets.size(); i++) {
			
			CellCollection c = datasets.get(i).getCollection();

			// draw the segment itself
			double[] xpoints = new double[c.getNucleusCount()];
			double[] ypoints = new double[c.getNucleusCount()];
			
			List<Cell> cells = c.getCells();
			// go through each index in the segment.
			for(int j=0; j<cells.size();j++){
				
				xpoints[j] = cells.get(j).getNucleus().getStatistic(statA, scale);
				ypoints[j] = cells.get(j).getNucleus().getStatistic(statB, scale);

			}

			double[][] data = { xpoints, ypoints };
			ds.addSeries(c.getName(), data);
		}

		return ds;
	}
}