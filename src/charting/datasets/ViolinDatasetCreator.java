package charting.datasets;

import java.util.ArrayList;
import java.util.List;

import org.jfree.data.Range;

import stats.NucleusStatistic;
import stats.Stats;
import weka.estimators.KernelEstimator;
import analysis.AnalysisDataset;
import charting.charts.ViolinCategoryDataset;
import charting.options.ChartOptions;
import components.CellCollection;
import components.generic.MeasurementScale;
import logging.Loggable;

public class ViolinDatasetCreator implements Loggable {
	
	private static ViolinDatasetCreator instance = null;
	
	private ViolinDatasetCreator(){

	}
	
	public static ViolinDatasetCreator getInstance(){
		if(instance==null){
			instance = new ViolinDatasetCreator();
		}
		return instance;
	}
	
	/**
	 * Get a boxplot dataset for the given statistic for each collection
	 * @param options the charting options
	 * @return
	 * @throws Exception
	 */
	public ViolinCategoryDataset createNucleusStatisticViolinDataset(ChartOptions options) {
		List<AnalysisDataset> datasets = options.getDatasets();
		NucleusStatistic stat = (NucleusStatistic) options.getStat();
		MeasurementScale scale = options.getScale();
		ViolinCategoryDataset ds = new ViolinCategoryDataset();

		
		for (int i=0; i < datasets.size(); i++) {
			CellCollection c = datasets.get(i).getCollection();
			
			String rowKey = c.getName()+"_"+i;

			// Add the boxplot values

			double[] stats = c.getNuclearStatistics(stat, scale);
			List<Number> list = new ArrayList<Number>();
			for (double d : stats) {
				list.add(new Double(d));
			}
			
			ds.add(list, rowKey, stat.toString());
			
			// Add the probability values
			
			
				

				List<Number> pdfValues = new ArrayList<Number>();

				if(list.size()>1){ // don't bother with a dataset of a single cell
					
					KernelEstimator est = NucleusDatasetCreator.getInstance().createProbabililtyKernel(  list , 0.001 );
					double min = Stats.min(list).doubleValue();
					double max = Stats.max(list).doubleValue();

					double stepSize = ( max - min ) / 100;

					for(double d=min; d<=max; d+=stepSize){

						pdfValues.add(est.getProbability(d));
					}

					
					Range r = new Range(min, max);
					ds.addProbabilityRange(r, rowKey, stat.toString());
				} else {
					Range r = new Range(list.get(0).doubleValue(), list.get(0).doubleValue());
					ds.addProbabilityRange(r, rowKey, stat.toString());
				}
				ds.addProbabilities(pdfValues, rowKey, stat.toString());
		}

		return ds;
	}
	
}
