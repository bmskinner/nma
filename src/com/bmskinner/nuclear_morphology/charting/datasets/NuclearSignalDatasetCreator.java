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

import java.awt.Color;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import com.bmskinner.nuclear_morphology.analysis.nucleus.CurveRefolder;
import com.bmskinner.nuclear_morphology.analysis.signals.ShellAnalysisException;
import com.bmskinner.nuclear_morphology.analysis.signals.ShellDetector;
import com.bmskinner.nuclear_morphology.analysis.signals.ShellDetector.Shell;
import com.bmskinner.nuclear_morphology.analysis.signals.ShellRandomDistributionCreator;
import com.bmskinner.nuclear_morphology.analysis.signals.ShellCounter.CountType;
import com.bmskinner.nuclear_morphology.charting.charts.ConsensusNucleusChartFactory;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclear.UnavailableSignalGroupException;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.INuclearSignalOptions;
import com.bmskinner.nuclear_morphology.components.options.INuclearSignalOptions.SignalDetectionMode;
import com.bmskinner.nuclear_morphology.components.stats.SignalStatistic;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;
import com.bmskinner.nuclear_morphology.utility.AngleTools;
import com.bmskinner.nuclear_morphology.utility.ArrayConverter;
import com.bmskinner.nuclear_morphology.utility.ArrayConverter.ArrayConversionException;

import weka.estimators.KernelEstimator;

public class NuclearSignalDatasetCreator extends AbstractDatasetCreator  {
	
	public NuclearSignalDatasetCreator(){}
	
	/**
	 * Create a table of signal stats for the given list of datasets. This table
	 * covers analysis parameters for the signals
	 * @param list the AnalysisDatasets to include
	 * @return a table model
	 */
	public TableModel createSignalDetectionParametersTable(TableOptions options) {

		if( ! options.hasDatasets()){
			return createBlankTable();
		}
		
		List<IAnalysisDataset> list = options.getDatasets();
		DefaultTableModel model = new DefaultTableModel();
		
		List<Object> fieldNames = new ArrayList<Object>(0);
				
		// find the collection with the most channels
		// this defines  the number of rows
			
		int maxChannels = 0;
		for(IAnalysisDataset dataset : list){
			ICellCollection collection = dataset.getCollection();
			maxChannels = Math.max(collection.getSignalManager().getSignalGroupIDs().size(), maxChannels);
			if(collection.hasSignalGroup(ShellRandomDistributionCreator.RANDOM_SIGNAL_ID)){
				maxChannels--;
			}
		}
		
		if(maxChannels==0){
			return createBlankTable();
		}
		
			
		Object[] rowNameBlock = {
				"",
				Labels.SIGNAL_GROUP_LABEL,
				"Channel",
				"Source",
				"Threshold",
				"Min size",
				"Max fraction",
				"Min circ",
				"Max circ",
				"Detection mode"
		};

		// create the row names
		fieldNames.add("Number of signal groups");

		for(int i=0;i<maxChannels;i++){

			for(Object o : rowNameBlock){
				fieldNames.add(o);
			}
		}

		int numberOfRowsPerSignalGroup = rowNameBlock.length;
		model.addColumn("", fieldNames.toArray(new Object[0])); // separate row block for each channel

		// make a new column for each collection
		for(IAnalysisDataset dataset : list){

			List<Object> columnData = makeDetectionSettingsColumn(dataset, maxChannels, numberOfRowsPerSignalGroup);
			model.addColumn(dataset.getName(), columnData.toArray(new Object[0])); // separate row block for each channel
		}
		
        
        return model;    
    }


    /**
     * Create a column of signal group information for the given dataset. 
     * If the number of signal groups in the dataset is less than the number
     * of signal groups in the table total, then the empty spaces will be 
     * added explicitly to the column 
     * @param dataset the dataset
     * @param signalGroupCount the total number of signal groups in the table
     * @param rowsPerSignalGroup the number of rows a signal group takes up
     * @return a list of rows for a table.
     */
    private List<Object> makeDetectionSettingsColumn(IAnalysisDataset dataset, int signalGroupCount, int rowsPerSignalGroup){
                        
        List<Object> rowData = new ArrayList<Object>(0);
        rowData.add(signalGroupCount);
        
        /*
         * If the dataset is a merge, then the analysis options will be null.
         * We could loop through the merge sources until finding the merge
         * with the signal options BUT there could be conflicts with 
         * signal groups when merging.
         * 
         * For now, do not display a table if the dataset has merge sources
         */
        
        boolean isFromMerge = false;
        
        if(dataset.hasMergeSources()){
        	isFromMerge = true;
        }
        
        if( ! dataset.isRoot() && ! dataset.hasAnalysisOptions()){ // temp fix for missing options in clusters from a merge
        	isFromMerge = true;
        }
        
        if(isFromMerge){
        	
        	
        	addMergedSignalData(rowData, dataset, signalGroupCount, rowsPerSignalGroup);
            
        } else {
            
        	addNonMergedSignalData(rowData, dataset, signalGroupCount, rowsPerSignalGroup);
        }
        return rowData;
    }
    
    private void addMergedSignalData(List<Object> rowData, IAnalysisDataset dataset, int signalGroupCount, int rowsPerSignalGroup){
    	ICellCollection collection = dataset.getCollection();
    	Collection<ISignalGroup> signalGroups = collection.getSignalManager().getSignalGroups();
    	
    	int j=0;
    	for(UUID signalGroup : collection.getSignalManager().getSignalGroupIDs()){

    		if(signalGroup.equals(ShellRandomDistributionCreator.RANDOM_SIGNAL_ID)){
    			continue;
    		}

    		try {

    			
    			Color colour = collection.getSignalGroup(signalGroup).hasColour()
    					? collection.getSignalGroup(signalGroup).getGroupColour()
    							: ColourSelecter.getColor(j);
    					
    			SignalTableCell cell = new SignalTableCell(signalGroup, 
    					collection.getSignalManager().getSignalGroupName(signalGroup),
    					colour);


    					rowData.add("");// empty row for colour
    					rowData.add(cell);  // group name


				for(int i=0; i<rowsPerSignalGroup-2;i++){ // rest are NA
					rowData.add("N/A - merge");
				}

    		} catch (UnavailableSignalGroupException e){
    			fine("Signal group "+signalGroup+" is not present in collection", e);
    		} finally {
    			j++;
    		}
        }
        
        // Add blank rows for any empty spaces in the table
        int remaining = signalGroupCount - signalGroups.size();
        for(int i=0;i<remaining;i++){
          for(int k=0; k<rowsPerSignalGroup;k++){
              rowData.add("");
          }
      }
    }

    
    private void addNonMergedSignalData(List<Object> rowData, IAnalysisDataset dataset, int signalGroupCount, int rowsPerSignalGroup){
    	
    	ICellCollection collection = dataset.getCollection();
    	int signalGroupNumber = 0; // Store the number of signal groups processed from this dataset

        int j=0;
        for(UUID signalGroup : collection.getSignalManager().getSignalGroupIDs()){
        	
        	if(signalGroup.equals(ShellRandomDistributionCreator.RANDOM_SIGNAL_ID)){
        		continue;
        	}
        	
        	try{

        		signalGroupNumber++;

        		
        		Color colour = collection.getSignalGroup(signalGroup).hasColour()
        				? collection.getSignalGroup(signalGroup).getGroupColour()
        				: ColourSelecter.getColor(j);


				SignalTableCell cell = new SignalTableCell(signalGroup, 
						collection.getSignalManager().getSignalGroupName(signalGroup),
						colour);


				INuclearSignalOptions ns = dataset.getAnalysisOptions()
						.getNuclearSignalOptions(signalGroup);

				if(ns==null){ // occurs when no signals are present? Should never occur with the new SignalGroup system
					for(int i=0; i<rowsPerSignalGroup;i++){
						rowData.add("");
					}


				} else {
					Object signalThreshold = ns.getDetectionMode().equals(SignalDetectionMode.FORWARD)
							? ns.getThreshold()
									: "Variable";

							Object signalMode = ns.getDetectionMode().equals(SignalDetectionMode.FORWARD)
									? "Forward"
											: ns.getDetectionMode().equals(SignalDetectionMode.REVERSE)
											? "Reverse"
													: "Adaptive";                    


							rowData.add("");
							rowData.add(cell);
							rowData.add(ns.getChannel());
							rowData.add(ns.getFolder());
							rowData.add(  signalThreshold );
							rowData.add(ns.getMinSize());
							rowData.add(DEFAULT_DECIMAL_FORMAT.format(ns.getMaxFraction()));
							rowData.add(DEFAULT_DECIMAL_FORMAT.format(ns.getMinCirc()));
							rowData.add(DEFAULT_DECIMAL_FORMAT.format(ns.getMaxCirc()));
							rowData.add(signalMode);
				}

        	} catch (UnavailableSignalGroupException e){
        		stack("Signal group "+signalGroup+" is not present in collection", e);
        	} finally {
        		j++;
        	}
        }
        
        /*
         * If the number of signal groups in the dataset is less than the size of the table,
         * the remainder should be filled with blank cells
         */
        
        if(signalGroupNumber < signalGroupCount){
            
            // There will be empty rows in the table. Fill the blanks
            for(int i = signalGroupNumber+1; i<=signalGroupCount;i++){
                for(int k = 0; k<rowsPerSignalGroup;k++){
                    rowData.add("");
                }
            }                
        }
    }
	
		
	/**
     * Create a histogram dataset covering the signal statistic for the given analysis datasets
	 * @param list the list of datasets
	 * @return a histogram of angles
	 * @throws Exception 
	 */
	public List<HistogramDataset> createSignalStatisticHistogramDataset(List<IAnalysisDataset> list, 
			SignalStatistic stat, MeasurementScale scale) throws ChartDatasetCreationException {
		
		List<HistogramDataset> result = new ArrayList<HistogramDataset>();
		
		for(IAnalysisDataset dataset : list){
			HistogramDataset ds = new HistogramDataset();
			ICellCollection collection = dataset.getCollection();
			
			for( UUID signalGroup : dataset.getCollection().getSignalManager().getSignalGroupIDs()){
				
				if(signalGroup.equals(ShellRandomDistributionCreator.RANDOM_SIGNAL_ID)){
					continue;
				}

				try {

					if(collection.getSignalGroup(signalGroup).isVisible()){

						if(collection.getSignalManager().hasSignals(signalGroup)){

							double[] values = collection.getSignalManager().getSignalStatistics(stat, scale, signalGroup);

							ds.addSeries("Group_"+signalGroup+"_"+collection.getName(), values, 12);
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
	public List<DefaultXYDataset> createSignalDensityHistogramDataset(List<IAnalysisDataset> list, 
			SignalStatistic stat, MeasurementScale scale) throws ChartDatasetCreationException {
		
		List<DefaultXYDataset> result = new ArrayList<DefaultXYDataset>();
		
		
		int[] minMaxRange = calculateMinAndMaxRange(list, stat, scale);
		
		for(IAnalysisDataset dataset : list){
			
			DefaultXYDataset ds = new DefaultXYDataset();
			
			ICellCollection collection = dataset.getCollection();
			
            for( UUID signalGroup : collection.getSignalManager().getSignalGroupIDs()){
            	
            	if(signalGroup.equals(ShellRandomDistributionCreator.RANDOM_SIGNAL_ID)){
            		continue;
            	}

								
                String groupLabel = "Group_"+signalGroup+"_"+stat.toString();
                
                // If the angle is always zero, the estimator will fail
                if(collection.getNucleusType().equals(NucleusType.ROUND) && stat.equals(SignalStatistic.ANGLE)){
                	
                	// Add an empty series
                	double[] xData = { 0 };
                	double[] yData = { 0 };
                	double[][] data = { xData, yData} ;
                	ds.addSeries( groupLabel, data);
                	continue;
                }
                
                double[] values = findSignalDatasetValues(dataset, stat, scale, signalGroup); 
                
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
                	est = new NucleusDatasetCreator().createProbabililtyKernel(values, 0.001);
                } catch (Exception e1) {
                	fine("Error creating probability kernel",e1);
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
	private int[] calculateMinAndMaxRange(List<IAnalysisDataset> list, SignalStatistic stat, 
			MeasurementScale scale) throws ChartDatasetCreationException {
		
		int[] result = new int[2];
		result[0] = Integer.MAX_VALUE; // holds min
		result[1] = 0; // holds max

		for(IAnalysisDataset dataset : list){
			
			for( UUID signalGroup : dataset.getCollection().getSignalManager().getSignalGroupIDs()){
				
				if(signalGroup.equals(ShellRandomDistributionCreator.RANDOM_SIGNAL_ID)){
            		continue;
            	}

				double[] values = findSignalDatasetValues(dataset, stat, scale, signalGroup); 

				NuclearHistogramDatasetCreator.updateMinMaxRange(result, values);
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
	public double[] findSignalDatasetValues(IAnalysisDataset dataset, SignalStatistic stat, 
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

		double angle = n.getStatistic(SignalStatistic.ANGLE);

		double fractionalDistance = n.getStatistic(SignalStatistic.FRACT_DISTANCE_FROM_COM);

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

			for(UUID group : collection.getSignalManager().getSignalGroupIDs()){
				
				if(group.equals(ShellRandomDistributionCreator.RANDOM_SIGNAL_ID)){
            		continue;
            	}

				finest("Signal group "+group.toString());

				try {

					if(dataset.getCollection().getSignalGroup(group).isVisible()){
						finest("Group "+group.toString()+" is visible");
						double[] xpoints = new double[collection.getSignalManager().getSignals(group).size()];
						double[] ypoints = new double[collection.getSignalManager().getSignals(group).size()];

						int signalCount = 0;
						for(INuclearSignal n : collection.getSignalManager().getSignals(group)){

							IPoint p = getXYCoordinatesForSignal(n, collection.getConsensusNucleus());

							xpoints[signalCount] = p.getX();
							ypoints[signalCount] = p.getY();
							signalCount++;

						}
						double[][] data = { xpoints, ypoints };
						ds.addSeries("Group_"+group, data);
						finest("Group "+group.toString()+" added "+signalCount+" signals");
					}

				} catch (UnavailableSignalGroupException e){
					stack("Signal group "+group+" is not present in collection", e);
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
						IPoint p = getXYCoordinatesForSignal(n, collection.getConsensusNucleus());

						// ellipses are drawn starting from x y at upper left. Provide an offset from the centre
						double offset = n.getStatistic(SignalStatistic.RADIUS);


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
	 * Create a table of signal stats for the given list of datasets. This table
	 * covers size, number of signals
	 * @param list the AnalysisDatasets to include
	 * @return a table model
	 * @throws Exception 
	 */
	public TableModel createSignalStatsTable(TableOptions options) {

		if(options==null){
			return createBlankTable();
		}
			
		return createMultiDatasetSignalStatsTable(options);
	}
		
	/**
	 * Create the signal statistics table for the given options
	 * @param options
	 * @return
	 */
	private TableModel createMultiDatasetSignalStatsTable(TableOptions options) {

		DefaultTableModel model = new DefaultTableModel();

		int signalGroupCount = getSignalGroupCount(options.getDatasets());

		finest("Selected collections have "+signalGroupCount+" signal groups");

		if(signalGroupCount<=0){
			
			finest("No signal groups to show");
			model.addColumn("No signal groups in datasets");
			return model;
		}
			
		
		// Make an instance of row names
		List<Object> rowNames = new ArrayList<Object>();
		rowNames.add("");
		rowNames.add(Labels.SIGNAL_GROUP_LABEL);
		rowNames.add("Signals");
		rowNames.add("Signals per nucleus");

		for(SignalStatistic stat : SignalStatistic.values()){
			rowNames.add(  stat.label(MeasurementScale.PIXELS) );
		}

		// Make the full column of row names for each signal group
		List<Object> firstColumn = new ArrayList<Object>(0);
		firstColumn.add("Number of signal groups");
		for(int i=0;i<signalGroupCount;i++){
			firstColumn.addAll(rowNames);
		}


		int numberOfRowsPerSignalGroup = rowNames.size();
		model.addColumn("", firstColumn.toArray(new Object[0])); // separate row block for each channel

		// make a new column for each collection
		for(IAnalysisDataset dataset : options.getDatasets()){

			List<Object> rowData = addSignalDataColumn(dataset, numberOfRowsPerSignalGroup, signalGroupCount);
			model.addColumn(dataset.getName(), rowData.toArray(new Object[0])); // separate row block for each channel
		}

		return model;	
	}
	
	/**
	 * Find the number of signal groups in a list of datasets that must be drawn 
	 * to include all groups
	 * @param list
	 * @return
	 */
	private int getSignalGroupCount(List<IAnalysisDataset> list){
		int maxSignalGroup = 0;

		for(IAnalysisDataset dataset : list){
			ICellCollection collection = dataset.getCollection();
			if(collection.getSignalManager().hasSignals()){
				maxSignalGroup = Math.max(collection.getSignalManager().getSignalGroupCount(), maxSignalGroup);
			}
		}
		return maxSignalGroup;
	}
	
	/**
	 * Add a signal column for a dataset 
	 * @param dataset the dataset to add
	 * @param numberOfRowsPerSignalGroup the number of rows each signal group occupies
	 * @param maxSignalGroup the highest signal group
	 * @return a list of table row values
	 */
	private List<Object> addSignalDataColumn(IAnalysisDataset dataset, int numberOfRowsPerSignalGroup, int maxSignalGroup){
		ICellCollection collection = dataset.getCollection();
		
		int signalGroupCount = collection.getSignalManager().getSignalGroupCount();
		
		List<Object> rowData = new ArrayList<Object>(0);
		rowData.add(signalGroupCount);

		for(UUID signalGroup : collection.getSignalManager().getSignalGroupIDs()){
			
			List<Object> temp = new ArrayList<Object>(0);
			try {
				
				
				if(signalGroup.equals(ShellRandomDistributionCreator.RANDOM_SIGNAL_ID)){
					continue;
				}


				if( collection.getSignalManager().getSignalCount(signalGroup)==0){ // Signal group has no signals
					for(int j = 0; j<numberOfRowsPerSignalGroup;j++){ // Make a blank block of cells
						temp.add("");
					}
					continue;
				}

				Color colour = collection.getSignalGroup(signalGroup).hasColour()
                        ? collection.getSignalGroup(signalGroup).getGroupColour()
					     : Color.WHITE;

				SignalTableCell cell = new SignalTableCell(signalGroup, 
						collection.getSignalManager().getSignalGroupName(signalGroup), colour);


				temp.add("");
				temp.add(cell);
				temp.add(collection.getSignalManager().getSignalCount(signalGroup));
				double signalPerNucleus = collection.getSignalManager().getSignalCountPerNucleus(signalGroup);
				temp.add(DEFAULT_DECIMAL_FORMAT.format(signalPerNucleus));

				for(SignalStatistic stat : SignalStatistic.values()){
					double pixel = collection.getSignalManager().getMedianSignalStatistic(stat, MeasurementScale.PIXELS, signalGroup);


					if(stat.isDimensionless()){
						temp.add(DEFAULT_DECIMAL_FORMAT.format(pixel) );
					} else {
						double micron = collection.getSignalManager().getMedianSignalStatistic(stat, MeasurementScale.MICRONS, signalGroup);
						temp.add(DEFAULT_DECIMAL_FORMAT.format(pixel) +" ("+ DEFAULT_DECIMAL_FORMAT.format(micron)+ " "+ stat.units(MeasurementScale.MICRONS)+")");
					}
				}

			} catch (UnavailableSignalGroupException e){
				stack("Signal group "+signalGroup+" is not present in collection", e);
				temp = new ArrayList<Object>(0);
				for(int j = 0; j<numberOfRowsPerSignalGroup;j++){ // Make a blank block of cells
					temp.add("");
				}
			} finally {
				rowData.addAll(temp);
			}

		}
		
		if(signalGroupCount < maxSignalGroup){
			
			// There will be empty rows in the table. Fill the blanks
			for(int i = signalGroupCount+1; i<=maxSignalGroup;i++){
				for(int j = 0; j<numberOfRowsPerSignalGroup;j++){
					rowData.add("");
				}
			}
			
		}
		return rowData;
	}
	
	/**
     * Create a boxplot dataset for signal statistics
     * @param options the chart options
     * @return a boxplot dataset
     * @throws Exception 
     */
    public BoxAndWhiskerCategoryDataset createSignalStatisticBoxplotDataset(ChartOptions options) throws ChartDatasetCreationException {
        
    	return createMultiDatasetSignalStatisticBoxplotDataset(options);        
    }
    
      
    /**
     * Create a boxplot dataset for signal statistics for a single analysis dataset
	 * @param dataset the AnalysisDataset to get signal info from
	 * @return a boxplot dataset
	 * @throws ChartDatasetCreationException 
	 */
    private BoxAndWhiskerCategoryDataset createMultiDatasetSignalStatisticBoxplotDataset(ChartOptions options) throws ChartDatasetCreationException {


    	ExportableBoxAndWhiskerCategoryDataset result = new ExportableBoxAndWhiskerCategoryDataset();
		SignalStatistic stat = (SignalStatistic) options.getStat();
		
 
        for(IAnalysisDataset d : options.getDatasets()){
        	
        	ICellCollection collection = d.getCollection();

        	for(UUID signalGroup : collection.getSignalManager().getSignalGroupIDs()){
        		
        		if(signalGroup.equals(ShellRandomDistributionCreator.RANDOM_SIGNAL_ID)){
            		continue;
            	}

        		double[] values = collection.getSignalManager().getSignalStatistics(stat, options.getScale(), signalGroup);
        		/*
        		 * For charting, use offset angles, otherwise the boxplots will fail on wrapped signals
        		 */
        		if(stat.equals(SignalStatistic.ANGLE)){
        			values = collection.getSignalManager().getOffsetSignalAngles(signalGroup);
        		}

        		List<Double> list = new ArrayList<Double>();
        		for(double value : values){
        			list.add(value);
        		}

        		result.add(list, "Group_"+signalGroup, collection.getName());
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
	public List<CategoryDataset> createShellBarChartDataset(ChartOptions options) throws ChartDatasetCreationException {
		
		List<CategoryDataset> result = new ArrayList<CategoryDataset>();

		for(IAnalysisDataset dataset : options.getDatasets()){
			
			ShellResultDataset ds = new ShellResultDataset();
			
			ICellCollection collection = dataset.getCollection();

			for(UUID signalGroup : collection.getSignalManager().getSignalGroupIDs()){

				// Create the random distribution
				if(signalGroup.equals(ShellRandomDistributionCreator.RANDOM_SIGNAL_ID)){
					
					if(options.isShowAnnotations()){
						addRandomShellData(ds, collection, options);
					}
				} else {
					addRealShellData(ds, collection, options, signalGroup);
				}
				
			}
			result.add(ds);
		}
		return result;
	}
	
	/**
	 * Create a consensus nucleus dataset overlaid with shells
	 * @param options the options
	 * @return a dataset
	 * @throws ChartDatasetCreationException
	 */
	public XYDataset createShellConsensusDataset(ChartOptions options) throws ChartDatasetCreationException {

		DefaultXYDataset ds = new DefaultXYDataset();
		
		// Make the shells from the consensus nucleus
		ShellDetector c;
		try {
			
			int shellCount = options.firstDataset().getCollection().getSignalManager().getShellCount();
			
			if(shellCount==0){
				throw new ChartDatasetCreationException("Cannot make dataset for zero shells");
			}
			
			c = new ShellDetector(options.firstDataset().getCollection().getConsensusNucleus(), shellCount);
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
	
	/**
	 * Create a table with columns for dataset, signal group, and the p value of a chi-square test for all
	 * shell analyses run
	 * @param options
	 * @return
	 */
	public TableModel createShellChiSquareTable(TableOptions options) {
		
		if( ! options.hasDatasets()){
			return createBlankTable();
		}
				
		DefaultTableModel model = new DefaultTableModel();
		
		DecimalFormat pFormat = new DecimalFormat("#0.000"); 
		
		Object[] columnNames = {
				
				"Dataset",
				"Signal group",
				"p"
		};
		
		model.setColumnIdentifiers(columnNames);
		
		for(IAnalysisDataset d : options.getDatasets()){
			
			for(UUID signalGroup : d.getCollection().getSignalManager().getSignalGroupIDs()){

				if(signalGroup.equals(ShellRandomDistributionCreator.RANDOM_SIGNAL_ID)){
					continue;
				}

				try {

					ISignalGroup group = d.getCollection().getSignalGroup(signalGroup);

					if(group.hasShellResult()){

						String groupName = group.getGroupName();

						IShellResult r    = group.getShellResult();

						Object[] rowData = {

								d.getName(),
								groupName,
								pFormat.format(r.getRawPValue(options.getCountType()))
						};


						model.addRow(rowData);
					}

				} catch (UnavailableSignalGroupException e){
					stack("Signal group "+signalGroup+" is not present in collection", e);
				}
			}

		}

		return model;
	}
	
}
