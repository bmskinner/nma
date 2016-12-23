package com.bmskinner.nuclear_morphology.charting.datasets;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jfree.data.Range;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.generic.UnsegmentedProfileException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.stats.NucleusStatistic;
import com.bmskinner.nuclear_morphology.components.stats.SegmentStatistic;
import com.bmskinner.nuclear_morphology.components.stats.SignalStatistic;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.Max;
import com.bmskinner.nuclear_morphology.stats.Min;
import com.bmskinner.nuclear_morphology.stats.Quartile;
import com.bmskinner.nuclear_morphology.stats.Sum;
import com.bmskinner.nuclear_morphology.utility.Constants;

import weka.estimators.KernelEstimator;

public class ViolinDatasetCreator extends AbstractDatasetCreator {
		
	protected ChartOptions options;
	
	public ViolinDatasetCreator(final ChartOptions options){
		this.options = options;
	}
	
	/**
	 * Get a boxplot dataset for the given statistic for each collection
	 * @param options the charting options
	 * @return
	 * @throws Exception
	 */
	public ViolinCategoryDataset createNucleusStatisticViolinDataset() {
		List<IAnalysisDataset> datasets = options.getDatasets();
		NucleusStatistic stat = (NucleusStatistic) options.getStat();
		MeasurementScale scale = options.getScale();
		ViolinCategoryDataset ds = new ViolinCategoryDataset();

		
		for (int i=0; i < datasets.size(); i++) {
			ICellCollection c = datasets.get(i).getCollection();
			
			String rowKey = c.getName()+"_"+i;
			String colKey = stat.toString();

			// Add the boxplot values

			double[] stats = c.getMedianStatistics(stat, scale);
			List<Number> list = new ArrayList<Number>();
			for (double d : stats) {
				list.add(new Double(d));
			}
			
			ds.add(list, rowKey, colKey);
			
			addProbabilities(ds, list, rowKey, colKey);  
		}

		return ds;
	}
	
    /**
     * Create a boxplot dataset for signal statistics for a single analysis dataset
	 * @param dataset the AnalysisDataset to get signal info from
	 * @return a boxplot dataset
	 * @throws Exception 
	 */
    public ViolinCategoryDataset createSignalStatisticViolinDataset() {

    	List<IAnalysisDataset> datasets = options.getDatasets();
    	SignalStatistic stat = (SignalStatistic) options.getStat();
		MeasurementScale scale = options.getScale();
		ViolinCategoryDataset ds = new ViolinCategoryDataset();
				
 
        for(IAnalysisDataset d : datasets){
        	
        	ICellCollection collection = d.getCollection();

        	for(UUID signalGroup : collection.getSignalManager().getSignalGroupIDs()){
        		
        		if(collection.getSignalManager().hasSignals(signalGroup)){

        			double[] values = collection.getSignalManager().getSignalStatistics(stat, scale, signalGroup);

        			String rowKey = "Group_"+signalGroup;
        			String colKey = collection.getName();
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

        			ds.add(list, rowKey, colKey);

        			addProbabilities(ds, list, rowKey, colKey);     
        		}
        	}
        }
		return ds;
	}
    
    /**
	 * Create a box and whisker dataset for the desired segment statistic
	 * @param collections the datasets to include
	 * @param segName the segment to calculate for
	 * @param scale the scale
	 * @param stat the segment statistic to use
	 * @return
     * @throws ChartDatasetCreationException 
	 * @throws Exception
	 */
	public ViolinCategoryDataset createSegmentStatisticDataset() throws ChartDatasetCreationException {
		
		SegmentStatistic stat = (SegmentStatistic) options.getStat();
		
		switch(stat){
		case DISPLACEMENT:
			return createSegmentDisplacementDataset(options.getDatasets(), options.getSegPosition());
		case LENGTH:
			return createSegmentLengthDataset(options.getDatasets(), options.getSegPosition(), options.getScale());
		default:
			return null;
		}
	}
	
	
	/*
	 * 
	 * PRIVATE METHODS
	 * 
	 * 
	 */
	
	/**
	 * Get the lengths of the given segment in the collections
	 * @param collections
	 * @param segName
	 * @return
	 * @throws ChartDatasetCreationException 
	 * @throws Exception 
	 */
	private ViolinCategoryDataset createSegmentLengthDataset(List<IAnalysisDataset> collections, int segPosition, MeasurementScale scale) throws ChartDatasetCreationException {

		ViolinCategoryDataset dataset = new ViolinCategoryDataset();

		for (int i=0; i < collections.size(); i++) {

			ICellCollection collection = collections.get(i).getCollection();
			try {
				IBorderSegment medianSeg = collection
						.getProfileCollection()
						.getSegmentAt(Tag.REFERENCE_POINT, segPosition);


				List<Number> list = new ArrayList<Number>(0);

				for(Nucleus n : collection.getNuclei()){

					IBorderSegment seg;

					seg = n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT)
							.getSegment(medianSeg.getID());



					double length = 0;
					if(seg!=null){
						int indexLength = seg.length();
						double proportionPerimeter = (double) indexLength / (double) seg.getTotalLength();
						length = n.getStatistic(NucleusStatistic.PERIMETER, scale) * proportionPerimeter;

					}
					list.add(length);

				}
				
				String rowKey = Constants.SEGMENT_PREFIX+segPosition+"_"+i;
				String colKey = Constants.SEGMENT_PREFIX+segPosition;
				dataset.add(list, rowKey, colKey);

				addProbabilities(dataset, list, rowKey, colKey);
				
			} catch (ProfileException | UnavailableBorderTagException | UnavailableProfileTypeException e) {
				fine("Error getting segmented profile", e);
				throw new ChartDatasetCreationException("Cannot get segmented profile", e);
			}		
		}

		return dataset;
	}
	
	/**
	 * Get the displacements of the given segment in the collections
	 * @param collections
	 * @param segName
	 * @return
	 * @throws ChartDatasetCreationException 
	 * @throws Exception 
	 */
	private ViolinCategoryDataset createSegmentDisplacementDataset(List<IAnalysisDataset> collections, int segPosition) throws ChartDatasetCreationException {

		ViolinCategoryDataset dataset = new ViolinCategoryDataset();

		for (int i=0; i < collections.size(); i++) {

			ICellCollection collection = collections.get(i).getCollection();
			
			IBorderSegment medianSeg;
			try {
				medianSeg = collection
						.getProfileCollection()
						.getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Quartile.MEDIAN)
						.getSegmentAt(segPosition);
			} catch (UnavailableBorderTagException | ProfileException | UnavailableProfileTypeException | UnsegmentedProfileException e) {
				fine("Unable to get segmented median profile", e);
				throw new ChartDatasetCreationException("Cannot get median profile");
			}


			List<Number> list = new ArrayList<Number>(0);

			for(Nucleus n : collection.getNuclei()){
				ISegmentedProfile profile;
				try {
					profile = n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
				} catch (ProfileException | UnavailableBorderTagException | UnavailableProfileTypeException e) {
					fine("Error getting segmented profile", e);
					throw new ChartDatasetCreationException("Cannot get segmented profile", e);
				}
				
				IBorderSegment seg = profile.getSegment(medianSeg.getID());
				
				double displacement = profile.getDisplacement(seg);
				list.add(displacement);
			}
			
			String rowKey = Constants.SEGMENT_PREFIX+segPosition+"_"+i;
			String colKey = Constants.SEGMENT_PREFIX+segPosition;

			dataset.add(list, rowKey, colKey);

			addProbabilities(dataset, list, rowKey, colKey);
		}
		return dataset;
	}
	
	protected void addProbabilities(ViolinCategoryDataset dataset, List<Number> list, Comparable<?> rowKey, Comparable<?> colKey){
		
		List<Number> pdfValues = new ArrayList<Number>();
		
		Number total = new Sum(list); // Stats.sum(list);

		if(list.size()>2 && total.doubleValue()>0){ // don't bother with a dataset of a single cell, or if the stat is not present
			
			KernelEstimator est = new NucleusDatasetCreator().createProbabililtyKernel(  list , 0.001 );
//			double min = Stats.min(list).doubleValue();
			double min = new Min(list).doubleValue();
			double max = new Max(list).doubleValue();

			double stepSize = ( max - min ) / 100;

			for(double v=min; v<=max; v+=stepSize){

				pdfValues.add(est.getProbability(v));
			}

			
			Range r = new Range(min, max);
			dataset.addProbabilityRange(r, rowKey, colKey);
		} else {
			
			if(list.isEmpty()){
				Range r = new Range(0, 0);
				dataset.addProbabilityRange(r, rowKey, colKey);
			} else {
				Range r = new Range(list.get(0).doubleValue(), list.get(0).doubleValue());
				dataset.addProbabilityRange(r, rowKey, colKey);
			}
		}
		dataset.addProbabilities(pdfValues, rowKey, colKey);
		
	}
	
	
	
}
