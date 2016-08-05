package charting.datasets;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jfree.data.Range;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;

import stats.NucleusStatistic;
import stats.SignalStatistic;
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
				
				Number total = Stats.sum(list);

				if(list.size()>1 && total.doubleValue()>0){ // don't bother with a dataset of a single cell, or if the stat is not present
					
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
	
    /**
     * Create a boxplot dataset for signal statistics for a single analysis dataset
	 * @param dataset the AnalysisDataset to get signal info from
	 * @return a boxplot dataset
	 * @throws Exception 
	 */
    public ViolinCategoryDataset createSignalStatisticViolinDataset(ChartOptions options) {

    	List<AnalysisDataset> datasets = options.getDatasets();
    	SignalStatistic stat = (SignalStatistic) options.getStat();
		MeasurementScale scale = options.getScale();
		ViolinCategoryDataset ds = new ViolinCategoryDataset();
				
 
        for(AnalysisDataset d : datasets){
        	
        	CellCollection collection = d.getCollection();

        	for(UUID signalGroup : collection.getSignalManager().getSignalGroupIDs()){
        		
        		double[] values = collection.getSignalManager().getSignalStatistics(stat, scale, signalGroup);
        		
        		String rowKey = "Group_"+signalGroup;
        		/*
        		 * For charting, use offset angles, otherwise the boxplots will fail on wrapped signals
        		 */
        		if(stat.equals(SignalStatistic.ANGLE)){
        			values = collection.getSignalManager().getOffsetSignalAngles(signalGroup);
        		}
        		
        		List<Number> list = new ArrayList<Number>();
    			for (double value : values) {
    				list.add(new Double(value));
    			}
    			
    			ds.add(list, rowKey, collection.getName());

    			// Add pdf values
    			
    			List<Number> pdfValues = new ArrayList<Number>();
				
				Number total = Stats.sum(list);

				if(list.size()>1 && total.doubleValue()>0){ // don't bother with a dataset of a single cell, or if the stat is not present
					
					KernelEstimator est = NucleusDatasetCreator.getInstance().createProbabililtyKernel(  list , 0.001 );
					double min = Stats.min(list).doubleValue();
					double max = Stats.max(list).doubleValue();

					double stepSize = ( max - min ) / 100;

					for(double v=min; v<=max; v+=stepSize){

						pdfValues.add(est.getProbability(v));
					}

					
					Range r = new Range(min, max);
					ds.addProbabilityRange(r, rowKey, collection.getName());
				} else {
					Range r = new Range(list.get(0).doubleValue(), list.get(0).doubleValue());
					ds.addProbabilityRange(r, rowKey, collection.getName());
				}
				ds.addProbabilities(pdfValues, rowKey, collection.getName());
        		
        	}
        }
		return ds;
	}
	
}
