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

import stats.NucleusStatistic;
import stats.SignalStatistic;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import logging.Loggable;

import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import charting.options.ChartOptions;
import analysis.AnalysisDataset;
import analysis.ProfileManager;
import components.AbstractCellularComponent;
import components.Cell;
import components.generic.BorderTag;
import components.generic.MeasurementScale;
import components.generic.BorderTag.BorderTagType;
import components.generic.ProfileType;
import components.generic.XYPoint;
import components.nuclear.NuclearSignal;
import components.nuclear.NucleusBorderSegment;
import components.nuclear.NucleusType;
import components.nuclei.Nucleus;

public class CellDatasetCreator implements Loggable {
	
	/**
	 * Create a table of stats for the given cell.
	 * @param cell the cell
	 * @return a table model
	 * @throws Exception 
	 */
	public static TableModel createCellInfoTable(Cell cell) throws Exception{

		DefaultTableModel model = new DefaultTableModel();
		
		List<Object> fieldNames = new ArrayList<Object>(0);
		List<Object> rowData 	= new ArrayList<Object>(0);
		
		DecimalFormat df = new DecimalFormat("#0.00"); 
				
		// find the collection with the most channels
		// this defines  the number of rows

		if(cell==null){
			model.addColumn("No data loaded");
			
		} else {
			
			Nucleus n = cell.getNucleus();
			
			fieldNames.add("Source image");
			rowData.add(n.getPathAndNumber());
			
			fieldNames.add("Source channel");
			rowData.add(n.getChannel());
			
			fieldNames.add("Scale (um/pixel)");
			rowData.add(n.getScale());
			
			for(NucleusStatistic stat : NucleusStatistic.values()){
				
				if(!stat.equals(NucleusStatistic.VARIABILITY)){
					
					fieldNames.add(stat.label(MeasurementScale.PIXELS)  );

					double pixel = n.getStatistic(stat, MeasurementScale.PIXELS);
					
					if(stat.isDimensionless()){
						rowData.add(df.format(pixel) );
					} else {
						double micron = n.getStatistic(stat, MeasurementScale.MICRONS);
						rowData.add(df.format(pixel) +" ("+ df.format(micron)+ " "+ stat.units(MeasurementScale.MICRONS)+")");
					}
					
				}
				
			}

			fieldNames.add("Nucleus CoM");
			rowData.add(n.getCentreOfMass().toString());
			
			fieldNames.add("Nucleus position");
			rowData.add(n.getPosition()[0]+"-"+n.getPosition()[1]);
			
			
			
			NucleusType type = NucleusType.getNucleusType(n);
			
			if(type!=null){
				for(BorderTag tag : BorderTag.values(BorderTagType.CORE)){
					fieldNames.add(type.getPoint(tag));
					int index = AbstractCellularComponent.wrapIndex(n.getBorderIndex(tag)- n.getBorderIndex(BorderTag.REFERENCE_POINT), n.getBorderLength());
					rowData.add(index);
				}
			} 

			// add info for signals
			for(int signalGroup : n.getSignalGroups()){
				
				fieldNames.add("");
				rowData.add("");
				
				fieldNames.add("Signal group");
				rowData.add(signalGroup);
				
				fieldNames.add("Signal name");
				rowData.add(n.getSignalCollection().getSignalGroupName(signalGroup));
				
				fieldNames.add("Source image");
				rowData.add(n.getSignalCollection().getSourceFile(signalGroup));
				
				fieldNames.add("Source channel");
				rowData.add(n.getSignalCollection().getSourceChannel(signalGroup));
				
				fieldNames.add("Number of signals");
				rowData.add(n.getSignalCount(signalGroup));
				
				for(NuclearSignal s : n.getSignals(signalGroup)){
					
					for(SignalStatistic stat : SignalStatistic.values()){
						
						fieldNames.add(stat.label(MeasurementScale.PIXELS)  );

						double pixel = s.getStatistic(stat, MeasurementScale.PIXELS);
						
						if(stat.isDimensionless()){
							rowData.add(df.format(pixel) );
						} else {
							double micron = s.getStatistic(stat, MeasurementScale.MICRONS);
							rowData.add(df.format(pixel) +" ("+ df.format(micron)+ " "+ stat.units(MeasurementScale.MICRONS)+")");
						}
					}
					
					fieldNames.add("Signal CoM");
					rowData.add(s.getCentreOfMass().toString());
					
				}			
				
			}
			
			model.addColumn("", fieldNames.toArray(new Object[0])); 
			model.addColumn("Info", rowData.toArray(new Object[0]));

			}
		return model;	
	}
	
	
	
	/**
	 * Create an XY dataset for the offset xy positions of the start positions of a segment 
	 * @param options the chart options
	 * @return a chart
	 */
	public static XYDataset createPositionFeatureDataset(ChartOptions options) throws Exception {

		XYDataset ds = null;
		
		if(options.isSingleDataset()){
			
			ds = createSinglePositionFeatureDataset(options);

		}
		
		if(options.isMultipleDatasets()){
			
			if(ProfileManager.segmentCountsMatch(options.getDatasets())){
			
				ds = createMultiPositionFeatureDataset(options);
			} else {
				options.log(Level.WARNING, "Unable to create multiple chart: segment counts do not match");
			}
		}

		
		return ds;
	}
	
	/**
	 * Create an XYDataset of segment start positions for a single dataset
	 * @param options
	 * @return
	 * @throws Exception
	 */
	private static XYDataset createSinglePositionFeatureDataset(ChartOptions options) throws Exception{
		
		DefaultXYDataset ds = new DefaultXYDataset();
		List<XYPoint> offsetPoints = createAbsolutePositionFeatureList(options.firstDataset(), options.getSegID());

		double[] xPoints = new double[offsetPoints.size()];
		double[] yPoints = new double[offsetPoints.size()];

		for(int i=0; i<offsetPoints.size(); i++){

			xPoints[i] = offsetPoints.get(i).getX();
			yPoints[i] = offsetPoints.get(i).getY();

		}

		double[][] data = { xPoints, yPoints };

		ds.addSeries("Segment_"+options.getSegID()+"_"+options.firstDataset().getName(), data);
		return ds;
	}
	
	/**
	 * Create an XYDataset of segment start positions for multiple datasets
	 * @param options
	 * @return
	 * @throws Exception
	 */
	private static XYDataset createMultiPositionFeatureDataset(ChartOptions options) throws Exception{
		
		DefaultXYDataset ds = new DefaultXYDataset();

		for(AnalysisDataset dataset :options.getDatasets()){
			
			/*
			 * We need to convert the seg position into a seg id
			 */
			UUID segID = dataset.getCollection()
					.getProfileCollection(ProfileType.REGULAR)
					.getSegmentedProfile(BorderTag.REFERENCE_POINT)
					.getSegmentAt(  options.getSegPosition()   )
					.getID();
			
			List<XYPoint> offsetPoints = createAbsolutePositionFeatureList(dataset, segID);

			double[] xPoints = new double[offsetPoints.size()];
			double[] yPoints = new double[offsetPoints.size()];

			for(int i=0; i<offsetPoints.size(); i++){

				xPoints[i] = offsetPoints.get(i).getX();
				yPoints[i] = offsetPoints.get(i).getY();

			}

			double[][] data = { xPoints, yPoints };

			ds.addSeries("Segment_"+segID+"_"+dataset.getName(), data);
		}
		
		return ds;
	}
	
	/**
	 * Create a list of points corresponding to the start index of the segment with the given id  
	 * @param dataset
	 * @param segmentID
	 * @return
	 * @throws Exception
	 */
	public static List<XYPoint> createAbsolutePositionFeatureList(AnalysisDataset dataset, UUID segmentID) throws Exception{
		
		if(dataset==null){
			throw new IllegalArgumentException("Dataset is null");
		}
		
		if(segmentID==null){
			throw new IllegalArgumentException("Segment id is null");
		}
		
		List<XYPoint> result = new ArrayList<XYPoint>();
		
		/*
		 * Fetch the cells from the dataset, and rotate the nuclei appropriately
		 */
		for(Nucleus nucleus : dataset.getCollection().getNuclei()){
			
			// For these, only include the nuclei with explicit top and bottom tags
//			if(nucleus.hasBorderTag(BorderTag.TOP_VERTICAL) && nucleus.hasBorderTag(BorderTag.BOTTOM_VERTICAL)){
				
				
				Nucleus verticalNucleus = nucleus.getVerticallyRotatedNucleus();

				// Get the segment start position XY coordinates
				NucleusBorderSegment segment = verticalNucleus.getProfile(ProfileType.REGULAR)
													.getSegment(segmentID);
				
				XYPoint point = verticalNucleus.getBorderPoint(segment.getStartIndex());
				result.add(point);
//			}		
		}	
		return result;
	}

	
	/**
	 * Find the xy coordinates of the start point of the given segment in each nucleus,
	 * after the nuclei have been rotated to vertical. The points are offset relative to 
	 * the positions within the consensus nucleus
	 * @param dataset
	 * @param segment
	 * @return
	 * @throws Exception 
	 */
	public static List<XYPoint> createRelativePositionFeatureList(AnalysisDataset dataset, UUID segmentID) throws Exception{
		
		List<XYPoint> result = createAbsolutePositionFeatureList( dataset, segmentID);
		
		/*
		 * Don't worry about changing things if there is not consensus nucleus
		 */
		if( ! dataset.getCollection().hasConsensusNucleus()){
			return result;		
		} 

		
		/*
		 * Find the start point of the segment in the consensus nucleus
		 */
		Nucleus consensus = dataset.getCollection()
				.getConsensusNucleus()
				.getVerticallyRotatedNucleus();
		
		// Get the segment start position XY coordinates
		NucleusBorderSegment segment = consensus.getProfile(ProfileType.REGULAR)
											.getSegment(segmentID);
		
		XYPoint centrePoint = consensus.getBorderPoint(segment.getStartIndex());
		
		/*
		 * The list of XYPoints are the absolute positions in cartesian space.
		 * This should be corrected to offsets from the geometric centre of the cluster
		 */
		
//		// Calculate the average x and y positions
//		
//		double sumX = 0;
//		double sumY = 0;
//		for(XYPoint p : result){
//			
//			sumX += p.getX();
//			sumY += p.getY();
//			
//		}
//		
//		sumX /= result.size();
//		sumY /= result.size();
//		XYPoint centrePoint = new XYPoint(sumX,sumY);
		
		/*
		 * Update the result positions to be offsets to the centre 
		 */
		
		for(XYPoint p : result){

			double offsetX = p.getX() - centrePoint.getX();
			double offsetY = p.getY() - centrePoint.getY();
			
			p.setX(offsetX);
			p.setY(offsetY);

		}
		
		return result;
	}

}
