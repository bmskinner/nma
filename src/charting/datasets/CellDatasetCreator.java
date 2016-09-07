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
import gui.GlobalOptions;
import gui.components.ColourSelecter;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import logging.Loggable;

import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import charting.options.ChartOptions;
import charting.options.TableOptions;
import analysis.AnalysisDataset;
import components.AbstractCellularComponent;
import components.Cell;
import components.generic.BorderTag;
import components.generic.BorderTagObject;
import components.generic.ProfileType;
import components.generic.XYPoint;
import components.nuclear.BorderPoint;
import components.nuclear.NuclearSignal;
import components.nuclear.NucleusBorderSegment;
import components.nuclear.NucleusType;
import components.nuclear.SignalGroup;
import components.nuclei.Nucleus;

public class CellDatasetCreator implements Loggable {
	
	private static CellDatasetCreator instance = null;
	
	private CellDatasetCreator(){}
	
	public static CellDatasetCreator getInstance(){
		if(instance==null){
			instance = new CellDatasetCreator();
		}
		return instance;
	}
	
	/**
	 * Create a table of stats for the given cell.
	 * @param cell the cell
	 * @return a table model
	 * @throws Exception 
	 */
	public TableModel createCellInfoTable(TableOptions options) {
		
		if( ! options.hasDatasets()){
			return NucleusTableDatasetCreator.getInstance().createBlankTable();
		}
		
		if(options.isMultipleDatasets()){
			return NucleusTableDatasetCreator.getInstance().createBlankTable();
		}

		Cell cell = options.getCell();
		
		if(cell==null){
			return NucleusTableDatasetCreator.getInstance().createBlankTable();
		}
		
		AnalysisDataset d = options.firstDataset();
		DefaultTableModel model = new DefaultTableModel();
		
		List<Object> fieldNames = new ArrayList<Object>(0);
		List<Object> rowData 	= new ArrayList<Object>(0);
						
		// find the collection with the most channels
		// this defines  the number of rows
			
		Nucleus n = cell.getNucleus();

		fieldNames.add("Source image file");
		rowData.add(n.getPathAndNumber());

		fieldNames.add("Source image name");
		rowData.add(n.getSourceFileName());

		fieldNames.add("Source channel");
		rowData.add(n.getChannel());
		
		fieldNames.add("Angle window prop.");
		rowData.add(n.getWindowProportion(ProfileType.ANGLE));
		
		fieldNames.add("Angle window size");
		rowData.add(n.getWindowSize(ProfileType.ANGLE));

		fieldNames.add("Scale (pixels/um)");
		rowData.add(n.getScale());

		addNuclearStatisticsToTable(fieldNames, rowData, n);
		
		fieldNames.add("Original bounding width");
		rowData.add(n.getBounds().getWidth());
		
		fieldNames.add("Original bounding height");
		rowData.add(n.getBounds().getHeight());

		fieldNames.add("Nucleus CoM");
		rowData.add(n.getCentreOfMass().toString());

		fieldNames.add("Nucleus position");
		rowData.add(n.getPosition()[0]+"-"+n.getPosition()[1]);



		NucleusType type = NucleusType.getNucleusType(n);

		if(type!=null){
						
			for(BorderTagObject tag : n.getBorderTags().keySet()){
				fieldNames.add(tag);
				if(n.hasBorderTag(tag)){

					BorderPoint p = n.getBorderPoint(tag);
					
					int index = n.getOffsetBorderIndex(BorderTagObject.REFERENCE_POINT, n.getBorderIndex(tag));
					
					rowData.add(p.toString()+" at profile index "+index);
				} else {
					rowData.add("N/A");
				}
			}
		} 
		addNuclearSignalsToTable(fieldNames, rowData, n, d);

		model.addColumn("", fieldNames.toArray(new Object[0])); 
		model.addColumn("Info", rowData.toArray(new Object[0]));

			
		return model;	
	}
	
	/**
	 * Add the nuclear statistic information to a cell table
	 * @param fieldNames
	 * @param rowData
	 * @param n
	 */
	private void addNuclearStatisticsToTable(List<Object> fieldNames,  List<Object> rowData, Nucleus n){
		
		DecimalFormat df = new DecimalFormat("#0.00"); 
		
		for(NucleusStatistic stat : NucleusStatistic.values()){

			if( ! stat.equals(NucleusStatistic.VARIABILITY)){

				fieldNames.add(stat.label(GlobalOptions.getInstance().getScale()  )  );

				double value = n.getStatistic(stat, GlobalOptions.getInstance().getScale()  );
					rowData.add(df.format(value) );
			}

		}
		
	}
	
	/**
	 * Add the nuclear signal information to a cell table
	 * @param fieldNames
	 * @param rowData
	 * @param n the nucleus
	 * @param d the source dataset for the nucleus
	 */
	private void addNuclearSignalsToTable(List<Object> fieldNames,  List<Object> rowData, Nucleus n, AnalysisDataset d){
		
		int j=0;

		for(UUID signalGroup : d.getCollection().getSignalGroupIDs()){
			

			if( ! n.getSignalCollection().hasSignal(signalGroup)){
				j++;
				continue;
			}
			
			SignalGroup g = d.getCollection().getSignalGroup(signalGroup);

			fieldNames.add("");
			rowData.add("");

			SignalTableCell tableCell = new SignalTableCell(signalGroup, g.getGroupName());

			Color colour = g.hasColour()
					     ? g.getGroupColour()
						 : ColourSelecter.getColor(j++);

			tableCell.setColor(colour);

			fieldNames.add("Signal group");
			rowData.add(tableCell);		

			fieldNames.add("Source image");
			rowData.add(n.getSignalCollection().getSourceFile(signalGroup));

			fieldNames.add("Source channel");
			rowData.add(g.getChannel());

			fieldNames.add("Number of signals");
			rowData.add(n.getSignalCollection().numberOfSignals(signalGroup));

			for(NuclearSignal s : n.getSignalCollection().getSignals(signalGroup)){
				addSignalStatisticsToTable(fieldNames, rowData, s );
			}			

		}
		
	}
	
	/**
	 * Add the nuclear signal statistics to a cell table
	 * @param fieldNames
	 * @param rowData
	 * @param s
	 */
	private void addSignalStatisticsToTable(List<Object> fieldNames,  List<Object> rowData, NuclearSignal s){
		
		DecimalFormat df = new DecimalFormat("#0.00"); 
		
		for(SignalStatistic stat : SignalStatistic.values()){

			fieldNames.add(    stat.label(   GlobalOptions.getInstance().getScale() )  );

			double value = s.getStatistic(stat, GlobalOptions.getInstance().getScale() );

//			if(stat.isDimensionless()){
				rowData.add(df.format(value) );
//			} else {
//				double micron = s.getStatistic(stat, MeasurementScale.MICRONS);
//				rowData.add(df.format(pixel) +" ("+ df.format(micron)+ " "+ stat.units(MeasurementScale.MICRONS)+")");
//			}
		}

		fieldNames.add("Signal CoM");
		rowData.add(s.getCentreOfMass().toString());
		
	}
	
	/**
	 * Create an XY dataset for the offset xy positions of the start positions of a segment 
	 * @param options the chart options
	 * @return a chart
	 */
	public XYDataset createPositionFeatureDataset(ChartOptions options) throws Exception {

		XYDataset ds = null;
		
		if(options.isSingleDataset()){
			finest("Creating single dataset position dataset");
			
			ds = createSinglePositionFeatureDataset(options);

		}
		
		if(options.isMultipleDatasets()){
			
			finest("Creating multiple dataset position dataset");
			
			if(NucleusBorderSegment.segmentCountsMatch(options.getDatasets())){
			
				ds = createMultiPositionFeatureDataset(options);
			} else {
				fine("Unable to create multiple chart: segment counts do not match");
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
	private XYDataset createSinglePositionFeatureDataset(ChartOptions options) throws Exception{
		
		DefaultXYDataset ds = new DefaultXYDataset();
		
		finest("Fetching segment position list");
		
		List<XYPoint> offsetPoints = createAbsolutePositionFeatureList(options.firstDataset(), options.getSegID());

		double[] xPoints = new double[offsetPoints.size()];
		double[] yPoints = new double[offsetPoints.size()];

		for(int i=0; i<offsetPoints.size(); i++){

			xPoints[i] = offsetPoints.get(i).getX();
			yPoints[i] = offsetPoints.get(i).getY();

		}

		double[][] data = { xPoints, yPoints };

		
		ds.addSeries("Segment_"+options.getSegID()+"_"+options.firstDataset().getName(), data);
		finest("Created segment position dataset for segment "+options.getSegID());
		return ds;
	}
	
	/**
	 * Create an XYDataset of segment start positions for multiple datasets
	 * @param options
	 * @return
	 * @throws Exception
	 */
	private XYDataset createMultiPositionFeatureDataset(ChartOptions options) throws Exception{
		
		DefaultXYDataset ds = new DefaultXYDataset();

		for(AnalysisDataset dataset :options.getDatasets()){
			
			/*
			 * We need to convert the seg position into a seg id
			 */
			UUID segID = dataset.getCollection()
					.getProfileCollection(ProfileType.ANGLE)
					.getSegmentedProfile(BorderTagObject.REFERENCE_POINT)
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
	public List<XYPoint> createAbsolutePositionFeatureList(AnalysisDataset dataset, UUID segmentID) throws Exception{
		
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
		finest("Fetching segment position for each nucleus");
		for(Nucleus nucleus : dataset.getCollection().getNuclei()){

//			nucleus.updateVerticallyRotatedNucleus(); // TODO: forcing an update here because new analyses don't have a proper vertical yet
			Nucleus verticalNucleus = nucleus.getVerticallyRotatedNucleus();
			finest("Fetched vertical nucleus");

			// Get the segment start position XY coordinates

			if( ! verticalNucleus.getProfile(ProfileType.ANGLE)
					.hasSegment(segmentID)){
				fine("Segment "+segmentID.toString()+" not found in vertical nucleus for "+nucleus.getNameAndNumber());
				continue;

			}
			NucleusBorderSegment segment = verticalNucleus.getProfile(ProfileType.ANGLE)
					.getSegment(segmentID);
			finest("Fetched segment "+segmentID.toString());

			int start = segment.getStartIndex();
			finest("Getting start point at index "+start);
			XYPoint point = verticalNucleus.getBorderPoint(start);
			result.add(point);	
		}	
		finest("Fetched segment position for each nucleus");
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
	public List<XYPoint> createRelativePositionFeatureList(AnalysisDataset dataset, UUID segmentID) throws Exception{
		
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
		NucleusBorderSegment segment = consensus.getProfile(ProfileType.ANGLE)
											.getSegment(segmentID);
		
		XYPoint centrePoint = consensus.getBorderPoint(segment.getStartIndex());
		
		/*
		 * The list of XYPoints are the absolute positions in cartesian space.
		 * This should be corrected to offsets from the geometric centre of the cluster
		 */
				
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
