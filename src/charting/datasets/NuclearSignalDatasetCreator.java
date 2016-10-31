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
package charting.datasets;

import java.awt.Color;
import java.awt.Paint;
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

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import charting.options.ChartOptions;
import charting.options.TableOptions;
import stats.SignalStatistic;
import utility.AngleTools;
import utility.ArrayConverter;
import utility.ArrayConverter.ArrayConversionException;
import weka.estimators.KernelEstimator;
import analysis.IAnalysisDataset;
import analysis.nucleus.CurveRefolder;
import analysis.signals.NuclearSignalOptions;
import analysis.signals.ShellRandomDistributionCreator;
import components.ICellCollection;
import components.generic.IPoint;
import components.generic.MeasurementScale;
import components.nuclear.INuclearSignal;
import components.nuclear.ISignalGroup;
import components.nuclear.ShellResult;
import components.nuclei.Nucleus;
import gui.components.ColourSelecter;

public class NuclearSignalDatasetCreator extends AbstractDatasetCreator  {
	
	public NuclearSignalDatasetCreator(){}
	
	/**
	 * Create a table of signal stats for the given list of datasets. This table
	 * covers analysis parameters for the signals
	 * @param list the AnalysisDatasets to include
	 * @return a table model
	 */
	public TableModel createSignalDetectionParametersTable(TableOptions options){

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
		
		if(maxChannels>0){
			
			Object[] rowNameBlock = {
					"",
					"Group name",
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
		} else {
			model.addColumn("No data loaded");
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
        
        // format the numbers and make into a tablemodel
        DecimalFormat df = new DecimalFormat("#0.00"); 
        
        ICellCollection collection = dataset.getCollection();
        
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
        	
        	
        	Collection<ISignalGroup> signalGroups = collection.getSignalManager().getSignalGroups();
        	
        	int j=0;
            for(UUID signalGroup : collection.getSignalManager().getSignalGroupIDs()){
            	
            	if(signalGroup.equals(ShellRandomDistributionCreator.RANDOM_SIGNAL_ID)){
            		continue;
            	}
            	
            	SignalTableCell cell = new SignalTableCell(signalGroup, collection.getSignalManager().getSignalGroupName(signalGroup));
                
            	Paint colour = collection.getSignalGroup(signalGroup).hasColour()
                        ? collection.getSignalGroup(signalGroup).getGroupColour()
                        : ColourSelecter.getColor(j++);
                
                cell.setColor((Color) colour);
                
                rowData.add("");// empty row for colour
                rowData.add(cell);  // group name
                
                
                for(int i=0; i<rowsPerSignalGroup-2;i++){ // rest are NA
                    rowData.add("N/A - merge");
                }
            }
            
            // Add blank rows for any empty spaces in the table
            int remaining = signalGroupCount - signalGroups.size();
            for(int i=0;i<remaining;i++){
              for(int k=0; k<rowsPerSignalGroup;k++){
                  rowData.add("");
              }
          }
            
        } else {
            
            int signalGroupNumber = 0; // Store the number of signal groups processed from this dataset

            int j=0;
            for(UUID signalGroup : collection.getSignalManager().getSignalGroupIDs()){
            	
            	if(signalGroup.equals(ShellRandomDistributionCreator.RANDOM_SIGNAL_ID)){
            		continue;
            	}
            
                signalGroupNumber++;
            
                SignalTableCell cell = new SignalTableCell(signalGroup, collection.getSignalManager().getSignalGroupName(signalGroup));
                
                Paint colour = collection.getSignalGroup(signalGroup).hasColour()
                        ? collection.getSignalGroup(signalGroup).getGroupColour()
                        : ColourSelecter.getColor(j++);
                
                cell.setColor((Color) colour);
                


                NuclearSignalOptions ns = dataset.getAnalysisOptions()
                        .getNuclearSignalOptions(signalGroup);
  
                if(ns==null){ // occurs when no signals are present? Should never occur with the new SignalGroup system
                    for(int i=0; i<rowsPerSignalGroup;i++){
                        rowData.add("");
                    }
                   

                } else {
                    Object signalThreshold = ns.getDetectionMode()==NuclearSignalOptions.FORWARD
                            ? ns.getThreshold()
                            : "Variable";

                    Object signalMode = ns.getDetectionMode()==NuclearSignalOptions.FORWARD
                            ? "Forward"
                            : ns.getDetectionMode()==NuclearSignalOptions.REVERSE
                            ? "Reverse"
                            : "Adaptive";                    


                    rowData.add("");
                    rowData.add(cell);
                    rowData.add(collection.getSignalManager().getSignalChannel(signalGroup));
                    rowData.add(collection.getSignalManager().getSignalSourceFolder(signalGroup));
                    rowData.add(  signalThreshold );
                    rowData.add(ns.getMinSize());
                    rowData.add(df.format(ns.getMaxFraction()));
                    rowData.add(df.format(ns.getMinCirc()));
                    rowData.add(df.format(ns.getMaxCirc()));
                    rowData.add(signalMode);
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
        return rowData;
    }

	
		
	/**
     * Create a histogram dataset covering the signal statistic for the given analysis datasets
	 * @param list the list of datasets
	 * @return a histogram of angles
	 * @throws Exception 
	 */
	public List<HistogramDataset> createSignaStatisticHistogramDataset(List<IAnalysisDataset> list, SignalStatistic stat, MeasurementScale scale) throws Exception{
		
		List<HistogramDataset> result = new ArrayList<HistogramDataset>();
		
		for(IAnalysisDataset dataset : list){
			HistogramDataset ds = new HistogramDataset();
			ICellCollection collection = dataset.getCollection();
			
			for( UUID signalGroup : dataset.getCollection().getSignalManager().getSignalGroupIDs()){
				
				if(signalGroup.equals(ShellRandomDistributionCreator.RANDOM_SIGNAL_ID)){
            		continue;
            	}

                if(collection.getSignalGroup(signalGroup).isVisible()){
	
					if(collection.getSignalManager().hasSignals(signalGroup)){
	
                        double[] values = collection.getSignalManager().getSignalStatistics(stat, scale, signalGroup);

						ds.addSeries("Group_"+signalGroup+"_"+collection.getName(), values, 12);
					}
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
	public List<DefaultXYDataset> createSignalDensityHistogramDataset(List<IAnalysisDataset> list, SignalStatistic stat, MeasurementScale scale) throws Exception {
		
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

                double[] values = findSignalDatasetValues(dataset, stat, scale, signalGroup); 
                KernelEstimator est = new NucleusDatasetCreator().createProbabililtyKernel(values, 0.001);


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
	private int[] calculateMinAndMaxRange(List<IAnalysisDataset> list, SignalStatistic stat, MeasurementScale scale) throws Exception {
		
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
	public double[] findSignalDatasetValues(IAnalysisDataset dataset, SignalStatistic stat, MeasurementScale scale, UUID signalGroup) throws Exception {
		
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
	public IPoint getXYCoordinatesForSignal(INuclearSignal n, Nucleus outline) throws Exception{

		double angle = n.getStatistic(SignalStatistic.ANGLE);

		double fractionalDistance = n.getStatistic(SignalStatistic.FRACT_DISTANCE_FROM_COM);

		// determine the total distance to the border at this angle
		double distanceToBorder = CurveRefolder.getDistanceFromAngle(angle, outline);

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
	public XYDataset createSignalCoMDataset(IAnalysisDataset dataset) throws Exception{
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
			}
		}
		finer("Finished signal CoM dataset");
		return ds;
	}
	
	public List<Shape> createSignalRadiusDataset(IAnalysisDataset dataset, UUID signalGroup) throws Exception{

		ICellCollection collection = dataset.getCollection();
		List<Shape> result = new ArrayList<Shape>(0);
		
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
		return result;
	}
	
	/**
	 * Create a table of signal stats for the given list of datasets. This table
	 * covers size, number of signals
	 * @param list the AnalysisDatasets to include
	 * @return a table model
	 * @throws Exception 
	 */
	public TableModel createSignalStatsTable(TableOptions options) throws Exception{

		DefaultTableModel model = new DefaultTableModel();

		// Empty table
		if(options==null){
			model.addColumn("No data loaded");
			return model;
		}
			
		if(options.isSingleDataset()){
			return createSingleDatasetSignalStatsTable(options);
		}
		
		if(options.isMultipleDatasets()){
			return createMultiDatasetSignalStatsTable(options);
		}
		
		model.addColumn("No data loaded");
		return model;
	}
	
	private TableModel createSingleDatasetSignalStatsTable(TableOptions options) throws Exception {

		DefaultTableModel model = new DefaultTableModel();
		
		List<Object> fieldNames = new ArrayList<Object>(0);

		IAnalysisDataset dataset = options.firstDataset();
		
		int maxSignalGroup = 0;

		ICellCollection collection = dataset.getCollection();
		if(collection.getSignalManager().hasSignals()){
			maxSignalGroup = Math.max(collection.getSignalManager().getSignalGroupIDs().size(), maxSignalGroup);
			
			if(collection.hasSignalGroup(ShellRandomDistributionCreator.RANDOM_SIGNAL_ID)){
        		maxSignalGroup--;
        	}
		}
		
		finest("Selected collections have "+maxSignalGroup+" signal groups");

		/*
		 * Create the row names for the table
		 */
		if(maxSignalGroup>0){
			// create the row names
			fieldNames.add("Number of signal groups");

			for(int i=0;i<maxSignalGroup;i++){
				fieldNames.add("");
				fieldNames.add("Signal group");
				fieldNames.add("Signals");
				fieldNames.add("Signals per nucleus");

				for(SignalStatistic stat : SignalStatistic.values()){

					fieldNames.add(stat.label(MeasurementScale.PIXELS)  );

				}
			}

			int numberOfRowsPerSignalGroup = fieldNames.size()/(maxSignalGroup+1);
			model.addColumn("", fieldNames.toArray(new Object[0])); // separate row block for each channel

			// format the numbers and make into a tablemodel
			DecimalFormat df = new DecimalFormat("#0.00"); 


			List<Object> rowData = new ArrayList<Object>(0);
			rowData.add(collection.getSignalManager().getSignalGroupIDs().size());

            int k=0;

			for(UUID signalGroup : collection.getSignalManager().getSignalGroupIDs()){
				
				if(signalGroup.equals(ShellRandomDistributionCreator.RANDOM_SIGNAL_ID)){
            		continue;
            	}
				
				if(collection.getSignalManager().hasSignals(signalGroup)){
					
					SignalTableCell cell = new SignalTableCell(signalGroup, collection.getSignalManager().getSignalGroupName(signalGroup));
                    
					Paint colour = collection.getSignalGroup(signalGroup).hasColour()
                            ? collection.getSignalGroup(signalGroup).getGroupColour()
                            : ColourSelecter.getColor(k++);
                    
                    cell.setColor((Color) colour);
                    

					
					rowData.add("");
					rowData.add(cell);
					rowData.add(collection.getSignalManager().getSignalCount(signalGroup));
					double signalPerNucleus = (double) collection.getSignalManager().getSignalCount(signalGroup)/  (double) collection.getSignalManager().getNumberOfCellsWithNuclearSignals(signalGroup);
					rowData.add(df.format(signalPerNucleus));

					for(SignalStatistic stat : SignalStatistic.values()){
                        double pixel = collection.getSignalManager().getMedianSignalStatistic(stat, MeasurementScale.PIXELS, signalGroup);

                        if(stat.isDimensionless() || stat.isAngle()){
							rowData.add(df.format(pixel) );
						} else {
                            double micron = collection.getSignalManager().getMedianSignalStatistic(stat, MeasurementScale.MICRONS, signalGroup);
							rowData.add(df.format(pixel) +" ("+ df.format(micron)+ " "+ stat.units(MeasurementScale.MICRONS)+")");
						}
					}

				} else {

					for(int i = 0; i<numberOfRowsPerSignalGroup;i++){
						rowData.add("");
					}
				}
			}
			model.addColumn(collection.getName(), rowData.toArray(new Object[0])); // separate row block for each channel

		} else {

			finest("No signal groups to show");

			model.addColumn("No data loaded");
		}
		return model;	
	}
	
	private TableModel createMultiDatasetSignalStatsTable(TableOptions options) throws Exception {

		DefaultTableModel model = new DefaultTableModel();
		
		List<Object> fieldNames = new ArrayList<Object>(0);

		
		int maxSignalGroup = 0;
		for(IAnalysisDataset dataset : options.getDatasets()){
			ICellCollection collection = dataset.getCollection();
			if(collection.getSignalManager().hasSignals()){
				maxSignalGroup = Math.max(collection.getSignalManager().getSignalGroupIDs().size(), maxSignalGroup);
			}
		}
			

			finest("Selected collections have "+maxSignalGroup+" signal groups");

			if(maxSignalGroup>0){
				// create the row names
				fieldNames.add("Number of signal groups");
				
				for(int i=0;i<maxSignalGroup;i++){
					fieldNames.add("");
					fieldNames.add("Signal group");
					fieldNames.add("Signals");
					fieldNames.add("Signals per nucleus");
					
					for(SignalStatistic stat : SignalStatistic.values()){
						
						fieldNames.add(stat.label(MeasurementScale.PIXELS)  );

					}
				}
				
				int numberOfRowsPerSignalGroup = fieldNames.size()/(maxSignalGroup+1);
				model.addColumn("", fieldNames.toArray(new Object[0])); // separate row block for each channel
					
				// format the numbers and make into a tablemodel
				DecimalFormat df = new DecimalFormat("#0.00"); 
	
				// make a new column for each collection
				for(IAnalysisDataset dataset : options.getDatasets()){
					ICellCollection collection = dataset.getCollection();
					
					int signalGroupCount = collection.getSignalManager().getSignalGroupCount();
					
					List<Object> rowData = new ArrayList<Object>(0);
					rowData.add(signalGroupCount);
	
					for(UUID signalGroup : collection.getSignalManager().getSignalGroupIDs()){
						
						if(signalGroup.equals(ShellRandomDistributionCreator.RANDOM_SIGNAL_ID)){
		            		continue;
		            	}
						
						if(collection.getSignalManager().getSignalCount(signalGroup)==0){ // Signal group has no signals
							for(int j = 0; j<numberOfRowsPerSignalGroup;j++){ // Make a blank block of cells
								rowData.add("");
							}
							continue;
						}

						SignalTableCell cell = new SignalTableCell(signalGroup, 
								collection.getSignalManager().getSignalGroupName(signalGroup));
						
						Color colour = collection.getSignalGroup(signalGroup).hasColour()
	                                 ? collection.getSignalGroup(signalGroup).getGroupColour()
	                                 : Color.WHITE;
	                    
	                    cell.setColor(colour);
						
							rowData.add("");
							rowData.add(cell);
							rowData.add(collection.getSignalManager().getSignalCount(signalGroup));
							double signalPerNucleus = collection.getSignalManager().getSignalCountPerNucleus(signalGroup);
							rowData.add(df.format(signalPerNucleus));
							
							for(SignalStatistic stat : SignalStatistic.values()){
		                        double pixel = collection.getSignalManager().getMedianSignalStatistic(stat, MeasurementScale.PIXELS, signalGroup);


								if(stat.isDimensionless()){
									rowData.add(df.format(pixel) );
								} else {
		                            double micron = collection.getSignalManager().getMedianSignalStatistic(stat, MeasurementScale.MICRONS, signalGroup);
									rowData.add(df.format(pixel) +" ("+ df.format(micron)+ " "+ stat.units(MeasurementScale.MICRONS)+")");
								}
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
					model.addColumn(collection.getName(), rowData.toArray(new Object[0])); // separate row block for each channel
				}
			} else {

				finest("No signal groups to show");

				model.addColumn("No signal groups in datasets");
			}
		return model;	
	}
	
	/**
     * Create a boxplot dataset for signal statistics
     * @param options the chart options
     * @return a boxplot dataset
     * @throws Exception 
     */
    public BoxAndWhiskerCategoryDataset createSignalStatisticBoxplotDataset(ChartOptions options) {
        
    	return createMultiDatasetSignalStatisticBoxplotDataset(options);        
    }
    
      
    /**
     * Create a boxplot dataset for signal statistics for a single analysis dataset
	 * @param dataset the AnalysisDataset to get signal info from
	 * @return a boxplot dataset
	 * @throws Exception 
	 */
    private BoxAndWhiskerCategoryDataset createMultiDatasetSignalStatisticBoxplotDataset(ChartOptions options) {


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
		
	public List<CategoryDataset> createShellBarChartDataset(ChartOptions options){
		
		List<CategoryDataset> result = new ArrayList<CategoryDataset>();

		for(IAnalysisDataset dataset : options.getDatasets()){
			
			ShellResultDataset ds = new ShellResultDataset();
			
			ICellCollection collection = dataset.getCollection();

			for(UUID signalGroup : collection.getSignalManager().getSignalGroupIDs()){
				
				// Create the random distribution
				if(signalGroup.equals(ShellRandomDistributionCreator.RANDOM_SIGNAL_ID)){
					if(collection.getSignalGroup(signalGroup).hasShellResult()){
						ShellResult r = collection.getSignalGroup(signalGroup).getShellResult();

						for(int shell = 0; shell<r.getNumberOfShells();shell++){
							Double d = options.isShowSignals() ? r.getCounts().get(shell) : r.getMeans().get(shell)*100;
							Double std = options.isShowSignals() ? 0 : r.getStandardErrors().get(shell)*100;
							ds.add(signalGroup, -d.doubleValue(), std.doubleValue(), "Group_"+signalGroup+"_"+collection.getName(), String.valueOf(shell)); 
							// we need the string value for shell otherwise we get error
							// "the method addValue(Number, Comparable, Comparable) is ambiguous for the type DefaultCategoryDataset"
							// ditto the doublevalue for std

						}
					}
					continue;
            	}
				
				if(collection.getSignalManager().hasSignals(signalGroup)){
					
					if(collection.getSignalGroup(signalGroup).hasShellResult()){
						ShellResult r = collection.getSignalGroup(signalGroup).getShellResult();
						
						

						for(int shell = 0; shell<r.getNumberOfShells();shell++){
														
							Double d = options.isShowSignals() 
									 ? r.getCounts().get(shell) 
									 : options.isNormalised()
									 	? r.getNormalisedMeans().get(shell)
									 	: r.getMeans().get(shell);
									 	
							Double std = options.isShowSignals() 
									   ? 0 
									   : r.getStandardErrors().get(shell);
							ds.add(signalGroup, d*100, std.doubleValue()*100, "Group_"+signalGroup+"_"+collection.getName(), String.valueOf(shell)); 
							// we need the string value for shell otherwise we get error
							// "the method addValue(Number, Comparable, Comparable) is ambiguous for the type DefaultCategoryDataset"
							// ditto the doublevalue for std

						}
					}
				}
			}
			result.add(ds);
		}
		return result;
	}
	
	/**
	 * Create a table with columns for dataset, signal group, and the p value of a chi-square test for all
	 * shell analyses run
	 * @param options
	 * @return
	 */
	public TableModel createShellChiSquareTable(TableOptions options){
		
		if( ! options.hasDatasets()){
			return this.createBlankTable();
		}
		
		DefaultTableModel model = new DefaultTableModel();
		
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
				
				ISignalGroup group = d.getCollection().getSignalGroup(signalGroup);
				
				if(group.hasShellResult()){
				
					String groupName = group.getGroupName();

					ShellResult r    = group.getShellResult();

					Object[] rowData = {

							d.getName(),
							groupName,
							r.getChiSquare()
					};


					model.addRow(rowData);
				}
			}
			
		}

		return model;
	}
	
}
