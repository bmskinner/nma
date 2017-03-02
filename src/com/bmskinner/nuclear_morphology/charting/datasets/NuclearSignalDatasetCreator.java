/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package com.bmskinner.nuclear_morphology.charting.datasets;

import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import weka.estimators.KernelEstimator;

import com.bmskinner.nuclear_morphology.analysis.signals.ShellAnalysisException;
import com.bmskinner.nuclear_morphology.analysis.signals.ShellCounter.CountType;
import com.bmskinner.nuclear_morphology.analysis.signals.ShellDetector;
import com.bmskinner.nuclear_morphology.analysis.signals.ShellDetector.Shell;
import com.bmskinner.nuclear_morphology.analysis.signals.ShellRandomDistributionCreator;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.nuclear.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclear.UnavailableSignalGroupException;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.utility.AngleTools;
import com.bmskinner.nuclear_morphology.utility.ArrayConverter;
import com.bmskinner.nuclear_morphology.utility.ArrayConverter.ArrayConversionException;

public class NuclearSignalDatasetCreator extends AbstractDatasetCreator<ChartOptions>  {
	
	public NuclearSignalDatasetCreator(final ChartOptions o){
		super(o);
	}
	
	
	
		
	/**
     * Create a histogram dataset covering the signal statistic for the given analysis datasets
	 * @param list the list of datasets
	 * @return a histogram of angles
	 * @throws Exception 
	 */
	public List<HistogramDataset> createSignalStatisticHistogramDataset(List<IAnalysisDataset> list, 
			PlottableStatistic stat, MeasurementScale scale) throws ChartDatasetCreationException {
		
		List<HistogramDataset> result = new ArrayList<HistogramDataset>();
		
		for(IAnalysisDataset dataset : list){
			HistogramDataset ds = new HistogramDataset();
			ICellCollection collection = dataset.getCollection();
			
			for( UUID signalGroup : dataset.getCollection().getSignalManager().getSignalGroupIDs()){

				try {

					if(collection.getSignalGroup(signalGroup).isVisible()){

						if(collection.getSignalManager().hasSignals(signalGroup)){

							double[] values = collection.getSignalManager().getSignalStatistics(stat, scale, signalGroup);

							ds.addSeries(CellularComponent.NUCLEAR_SIGNAL+"_"+signalGroup+"_"+collection.getName(), values, 12);
						}
					}

				} catch (UnavailableSignalGroupException e){
					stack("Signal group "+signalGroup+" is not present in collection", e);
				}
			}
			result.add(ds);
			
		}
		return result;
	}
	
	
	/**
	 * Make an XY dataset corresponding to the probability density of a given nuclear statistic
	 * @param list the datasets to draw
	 * @param stat the statistic to measure
	 * @return a charting dataset
	 * @throws Exception
	 */
	public List<DefaultXYDataset> createSignalDensityHistogramDataset() throws ChartDatasetCreationException {
		
		List<IAnalysisDataset> list = options.getDatasets();
		PlottableStatistic stat = options.getStat();
		MeasurementScale scale  = options.getScale();
		
		List<DefaultXYDataset> result = new ArrayList<DefaultXYDataset>();
		
		int[] minMaxRange = calculateMinAndMaxRange(list, stat, scale);
		
		for(IAnalysisDataset dataset : list){
			
			DefaultXYDataset ds = new DefaultXYDataset();
			
			ICellCollection collection = dataset.getCollection();
			
            for( UUID uuid : collection.getSignalManager().getSignalGroupIDs()){
								
                String groupLabel = CellularComponent.NUCLEAR_SIGNAL+"_"+uuid+"_"+stat.toString();
                boolean ignoreSignalGroup = false;
                
                // If the angle is always zero, the estimator will fail
                if(collection.getNucleusType().equals(NucleusType.ROUND) && stat.equals(PlottableStatistic.ANGLE)){
                	ignoreSignalGroup = true;
                }
                
                // If the group is present but empty, the estimator will fail
                if( ! collection.getSignalManager().hasSignals(uuid)){
                	ignoreSignalGroup = true;
                }
                
                // Skip if needed
                if(ignoreSignalGroup){
                	// Add an empty series
                	double[] xData = { 0 };
                	double[] yData = { 0 };
                	double[][] data = { xData, yData} ;
                	ds.addSeries( groupLabel, data);
                	continue;
                }
                
                double[] values = findSignalDatasetValues(dataset, stat, scale, uuid); 
                
                // Cannot estimate pdf with too few values
                if(values.length<3){
                	// Add an empty series
                	double[] xData = { 0 };
                	double[] yData = { 0 };
                	double[][] data = { xData, yData} ;
                	ds.addSeries( groupLabel, data);
                	continue;
                }
                
                KernelEstimator est;
                try {
                	est = new NucleusDatasetCreator(options).createProbabililtyKernel(values, 0.001);
                } catch (Exception e1) {
                	stack("Error creating probability kernel",e1);
                	throw new ChartDatasetCreationException("Cannot make probability dataset", e1);
                }
                
                double min = Arrays.stream(values).min().orElse(0); //Stats.min(values);
                double max = Arrays.stream(values).max().orElse(0); //Stats.max(values);

                int log = (int) Math.floor(  Math.log10(min)  ); // get the log scale

                int roundLog = log-1 == 0 ? log-2 : log-1;
                double roundAbs = Math.pow(10, roundLog);

                int binLog = log-2;
                double stepSize = Math.pow(10, binLog);

                // use int truncation to round to nearest 100 above max
                int maxRounded = (int) ((( (int)max + (roundAbs) ) / roundAbs ) * roundAbs);
                maxRounded = roundAbs > 1 ? maxRounded + (int) roundAbs : maxRounded + 1; // correct offsets for measures between 0-1
                int minRounded = (int) (((( (int)min + (roundAbs) ) / roundAbs ) * roundAbs  ) - roundAbs);
                minRounded = roundAbs > 1 ? minRounded - (int) roundAbs : minRounded - 1;  // correct offsets for measures between 0-1
                minRounded = minRounded < 0 ? 0 : minRounded; // ensure all measures start from at least zero


                List<Double> xValues = new ArrayList<Double>();
                List<Double> yValues = new ArrayList<Double>();
                
                for(double i=minMaxRange[0]; i<=minMaxRange[1]; i+=stepSize){
                	xValues.add(i);
                	yValues.add(est.getProbability(i));
                }

                double[] xData;
                double[] yData;

                try{

                	xData = new ArrayConverter(xValues).toDoubleArray();
                	yData = new ArrayConverter(yValues).toDoubleArray();

                } catch (ArrayConversionException e) {
                	xData = new double[0]; 
                	yData = new double[0]; 
                }
                double[][] data = { xData, yData} ;

                ds.addSeries( groupLabel, data);
                
            }
			result.add(ds);
		}

		return result;
	}
	
	/**
	 * Calculate the minimum and maximum ranges in a list of datasets
	 * for the given stat type
	 * @param list the datasets
	 * @param stat the statistic to use
	 * @return an array with the min and max of the range
	 * @throws Exception
	 */
	private int[] calculateMinAndMaxRange(List<IAnalysisDataset> list, PlottableStatistic stat, 
			MeasurementScale scale) throws ChartDatasetCreationException {
		
		int[] result = new int[2];
		result[0] = Integer.MAX_VALUE; // holds min
		result[1] = 0; // holds max

		for(IAnalysisDataset dataset : list){
			
			for( UUID signalGroup : dataset.getCollection().getSignalManager().getSignalGroupIDs()){

				if(dataset.getCollection().getSignalManager().hasSignals(signalGroup)){
				
					double[] values = findSignalDatasetValues(dataset, stat, scale, signalGroup); 
					NuclearHistogramDatasetCreator.updateMinMaxRange(result, values);
				}

				
			}
		}
		
		return result;
	}
	
	/**
	 * Given a dataset and a stats parameter, get the values for that stat
	 * @param dataset the Analysis Dataset
	 * @param stat the statistic to fetch
	 * @param scale the scale to display at
	 * @return the array of values
	 * @throws Exception
	 */
	public double[] findSignalDatasetValues(IAnalysisDataset dataset, PlottableStatistic stat, 
			MeasurementScale scale, UUID signalGroup) throws ChartDatasetCreationException {
		
		ICellCollection collection = dataset.getCollection();			
        double[] values = collection.getSignalManager().getSignalStatistics(stat, scale, signalGroup);             			
		return values;
	}
	

	
	/**
	 * Get the XY coordinates of a given signal centre of mass on a nuclear outline
	 * @param n the signal to plos
	 * @param outline the outline to draw the signal on
	 * @return the point of the signal centre of mass
	 * @throws Exception 
	 */
	public IPoint getXYCoordinatesForSignal(INuclearSignal n, Nucleus outline) throws ChartDatasetCreationException {

		double angle = n.getStatistic(PlottableStatistic.ANGLE);

		double fractionalDistance = n.getStatistic(PlottableStatistic.FRACT_DISTANCE_FROM_COM);

		// determine the distance to the border at this angle
		double distanceToBorder = outline.getDistanceFromCoMToBorderAtAngle(angle);

		// convert to fractional distance to signal
		double signalDistance = distanceToBorder * fractionalDistance;

		// adjust X and Y because we are now counting angles from the vertical axis
		double signalX = new AngleTools().getXComponentOfAngle(signalDistance, angle-90);
		double signalY = new AngleTools().getYComponentOfAngle(signalDistance, angle-90);
		return IPoint.makeNew(signalX, signalY);
	}
	
	/**
	 * Create a chart dataset for the centres of mass of signals in the dataset
	 * @param dataset the dataset
	 * @return
	 * @throws Exception 
	 */
	public XYDataset createSignalCoMDataset(IAnalysisDataset dataset) throws ChartDatasetCreationException {
		finer("Making signal CoM dataset");
		DefaultXYDataset ds = new DefaultXYDataset();
		ICellCollection collection = dataset.getCollection();

		if(collection.getSignalManager().hasSignals()){
			finer("Collection "+collection.getName()+" has signals");

			for(UUID uuid : collection.getSignalManager().getSignalGroupIDs()){

				try {

					if(dataset.getCollection().getSignalGroup(uuid).isVisible()){
						finest("Group "+uuid.toString()+" is visible");
						double[] xpoints = new double[collection.getSignalManager().getSignals(uuid).size()];
						double[] ypoints = new double[collection.getSignalManager().getSignals(uuid).size()];

						int signalCount = 0;
						for(INuclearSignal n : collection.getSignalManager().getSignals(uuid)){

							IPoint p = getXYCoordinatesForSignal(n, collection.getConsensus());

							xpoints[signalCount] = p.getX();
							ypoints[signalCount] = p.getY();
							signalCount++;

						}
						double[][] data = { xpoints, ypoints };
						ds.addSeries(CellularComponent.NUCLEAR_SIGNAL+"_"+uuid, data);
						finest("Group "+uuid.toString()+" added "+signalCount+" signals");
					}

				} catch (UnavailableSignalGroupException e){
					stack("Signal group "+uuid+" is not present in collection", e);
				}
			}
		}
		finer("Finished signal CoM dataset");
		return ds;
	}
	
	public List<Shape> createSignalRadiusDataset(IAnalysisDataset dataset, UUID signalGroup) throws ChartDatasetCreationException {

		ICellCollection collection = dataset.getCollection();
		List<Shape> result = new ArrayList<Shape>(0);
		
		try {
			if(collection.getSignalGroup(signalGroup).isVisible()){
				if(collection.getSignalManager().hasSignals(signalGroup)){

					for(INuclearSignal n : collection.getSignalManager().getSignals(signalGroup)){
						IPoint p = getXYCoordinatesForSignal(n, collection.getConsensus());

						// ellipses are drawn starting from x y at upper left. Provide an offset from the centre
						double offset = n.getStatistic(PlottableStatistic.RADIUS);


						result.add(new Ellipse2D.Double(p.getX()-offset, p.getY()-offset, offset*2, offset*2));
					}

				}
			}

		} catch (UnavailableSignalGroupException e){
			stack("Signal group "+signalGroup+" is not present in collection", e);
		}
		return result;
	}
	
	

	
	/**
     * Create a boxplot dataset for signal statistics
     * @param options the chart options
     * @return a boxplot dataset
     * @throws Exception 
     */
    public BoxAndWhiskerCategoryDataset createSignalStatisticBoxplotDataset() throws ChartDatasetCreationException {
        
    	return createMultiDatasetSignalStatisticBoxplotDataset();        
    }
    
      
    /**
     * Create a boxplot dataset for signal statistics for a single analysis dataset
	 * @param dataset the AnalysisDataset to get signal info from
	 * @return a boxplot dataset
	 * @throws ChartDatasetCreationException 
	 */
    private BoxAndWhiskerCategoryDataset createMultiDatasetSignalStatisticBoxplotDataset() throws ChartDatasetCreationException {


    	ExportableBoxAndWhiskerCategoryDataset result = new ExportableBoxAndWhiskerCategoryDataset();
    	PlottableStatistic stat = options.getStat();
		
 
        for(IAnalysisDataset d : options.getDatasets()){
        	
        	ICellCollection collection = d.getCollection();

        	for(UUID signalGroup : collection.getSignalManager().getSignalGroupIDs()){

        		double[] values = collection.getSignalManager().getSignalStatistics(stat, options.getScale(), signalGroup);
        		/*
        		 * For charting, use offset angles, otherwise the boxplots will fail on wrapped signals
        		 */
        		if(stat.equals(PlottableStatistic.ANGLE)){
        			values = collection.getSignalManager().getOffsetSignalAngles(signalGroup);
        		}

        		List<Double> list = new ArrayList<Double>();
        		for(double value : values){
        			list.add(value);
        		}

        		result.add(list, CellularComponent.NUCLEAR_SIGNAL+"_"+signalGroup, collection.getName());
        	}
        }
		return result;
	}
		
	/**
	 * Create a list of shell result datasets for each analysis dataset in the given options
	 * @param options
	 * @return
	 * @throws ChartDatasetCreationException
	 */
	public List<CategoryDataset> createShellBarChartDataset() throws ChartDatasetCreationException {
//		ChartOptions op = (ChartOptions) options;
		List<CategoryDataset> result = new ArrayList<CategoryDataset>();

		for(IAnalysisDataset dataset : options.getDatasets()){
			
			ShellResultDataset ds = new ShellResultDataset();
			
			ICellCollection collection = dataset.getCollection();
			
			if(collection.hasSignalGroup(ShellRandomDistributionCreator.RANDOM_SIGNAL_ID)){
				if(options.isShowAnnotations()){
					addRandomShellData(ds, collection, options);
				}
			}

			for(UUID signalGroup : collection.getSignalManager().getSignalGroupIDs()){

				addRealShellData(ds, collection, options, signalGroup);
			}
			result.add(ds);
		}
		return result;
	}
	
	/**
	 * Create a consensus nucleus dataset overlaid with shells. Requires a single dataset
	 * in the options.
	 * @param options the options
	 * @return a chart dataset
	 * @throws ChartDatasetCreationException if the IAnalysisDataset has no shell results or the dataset count is not 1
	 */
	public XYDataset createShellConsensusDataset() throws ChartDatasetCreationException {

		if(! options.isSingleDataset()){
			throw new ChartDatasetCreationException("Single dataset required");
		}
		
		DefaultXYDataset ds = new DefaultXYDataset();
		
		// Make the shells from the consensus nucleus
		ShellDetector c;
		try {
			
			int shellCount = options.firstDataset().getCollection().getSignalManager().getShellCount();
			
			if(shellCount==0){
				throw new ChartDatasetCreationException("Cannot make dataset for zero shells");
			}
			
			c = new ShellDetector(options.firstDataset().getCollection().getConsensus(), shellCount);
		} catch (ShellAnalysisException e) {
			stack("Error making shells in consensus", e);
			throw new ChartDatasetCreationException("Error making shells", e);
		}

		
		// Draw the shells
		int shellNumber = 0;	
		for(Shell shell : c.getShells()){
			
			Polygon p = shell.toPolygon();
			
			double[] xpoints = new double[p.npoints+1];
			double[] ypoints = new double[p.npoints+1];
			
			for(int i=0; i<p.npoints;i++){
				
				xpoints[i] = p.xpoints[i];
				ypoints[i] = p.ypoints[i];
			}
			// complete the line
			xpoints[p.npoints] = xpoints[0];
			ypoints[p.npoints] = ypoints[0];
			
			double[][] data = { xpoints, ypoints };
			ds.addSeries("Shell_"+shellNumber, data);
			shellNumber++;
			
		}
		
		
		
		return ds;
		
	}
	
	
	/**
	 * Add the simulated random data from the given collection to the result dataset
	 * @param ds the dataset to add values to
	 * @param collection the cell collection to take random shell data from
	 * @param options the chart options
	 */
	private void addRandomShellData(ShellResultDataset ds, ICellCollection collection, ChartOptions options){
		// Create the random distribution

		try {

			UUID signalGroup = ShellRandomDistributionCreator.RANDOM_SIGNAL_ID;

			// Choose between signal or nucleus level analysis
			CountType type = options.getCountType();
			
			boolean isNormalised = options.isNormalised();
			
			if(collection.getSignalGroup(signalGroup).hasShellResult()){
				IShellResult r = collection.getSignalGroup(signalGroup).getShellResult();

				for(int shell = 0; shell<r.getNumberOfShells();shell++){
					Double d = isNormalised
							 ? r.getNormalisedMeans(type).get(shell)*100
							 : r.getRawMeans(type).get(shell)*100;
					
					Double std = isNormalised
							 ? r.getNormalisedStandardErrors(type).get(shell)*100
							 : r.getRawStandardErrors(type).get(shell)*100;

					ds.add(signalGroup, -d.doubleValue(), std.doubleValue(), "Group_"+signalGroup+"_"+collection.getName(), String.valueOf(shell)); 
							// we need the string value for shell otherwise we get error
							// "the method addValue(Number, Comparable, Comparable) is ambiguous for the type DefaultCategoryDataset"
							// ditto the doublevalue for std

				}
			}
		}catch(UnavailableSignalGroupException e){
			stack("Error getting random signal group", e);
		}
	}
	
	/**
	 * Add the real shell data from the given collection to the result dataset
	 * @param ds the dataset to add values to
	 * @param collection the cell collection to take shell data from
	 * @param options the chart options
	 */
	private void addRealShellData(ShellResultDataset ds, ICellCollection collection, ChartOptions options, UUID signalGroup){

		try {
			
			// Choose between signal or nucleus level analysis
			CountType type = options.getCountType();

			// Choose whether to display signals or pixel counts
//			boolean showSignals = options.isShowSignals();
			
			boolean isNormalised = options.isNormalised();

			if(collection.getSignalManager().hasSignals(signalGroup)){

				if(collection.getSignalGroup(signalGroup).hasShellResult()){
					IShellResult r = collection.getSignalGroup(signalGroup).getShellResult();

					for(int shell = 0; shell<r.getNumberOfShells();shell++){

						Double d = isNormalised
								 ? r.getNormalisedMeans(type).get(shell)
							     : r.getRawMeans(type).get(shell);

						Double std = isNormalised
								   ? r.getNormalisedStandardErrors(type).get(shell)
								   : r.getRawStandardErrors(type).get(shell);
						ds.add(signalGroup, d*100, std.doubleValue()*100, "Group_"+signalGroup+"_"+collection.getName(), String.valueOf(shell)); 
						// we need the string value for shell otherwise we get error
						// "the method addValue(Number, Comparable, Comparable) is ambiguous for the type DefaultCategoryDataset"
						// ditto the doublevalue for std

					}
				}
			}
		}catch(UnavailableSignalGroupException e){
			stack("Error getting random signal group", e);
		}
	}
	
	
	
	
	
}
