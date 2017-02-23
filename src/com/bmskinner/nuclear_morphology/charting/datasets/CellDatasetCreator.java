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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;

public class CellDatasetCreator extends AbstractDatasetCreator<ChartOptions> {
		
	public CellDatasetCreator(final ChartOptions options){
		super(options);
	}
	
	
	/**
	 * Create an XY dataset for the offset xy positions of the start positions of a segment 
	 * @param options the chart options
	 * @return a chart
	 */
	public XYDataset createPositionFeatureDataset() throws ChartDatasetCreationException {

		XYDataset ds = null;
		
		if(options.isSingleDataset()){
			finest("Creating single dataset position dataset");
			
			ds = createSinglePositionFeatureDataset();

		}
		
		if(options.isMultipleDatasets()){
			
			finest("Creating multiple dataset position dataset");
			
			if(IBorderSegment.segmentCountsMatch(options.getDatasets())){
			
				ds = createMultiPositionFeatureDataset();
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
	private XYDataset createSinglePositionFeatureDataset() throws ChartDatasetCreationException{
		
		DefaultXYDataset ds = new DefaultXYDataset();
		
		finest("Fetching segment position list");
		
		List<IPoint> offsetPoints = createAbsolutePositionFeatureList(options.firstDataset(), options.getSegID());

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
	private XYDataset createMultiPositionFeatureDataset() throws ChartDatasetCreationException{
		
		DefaultXYDataset ds = new DefaultXYDataset();

		for(IAnalysisDataset dataset :options.getDatasets()){
			
			/*
			 * We need to convert the seg position into a seg id
			 */
			try {
				UUID segID = dataset.getCollection()
						.getProfileCollection()
						.getSegmentAt(Tag.REFERENCE_POINT,  options.getSegPosition()   )
						.getID();
				
				List<IPoint> offsetPoints = createAbsolutePositionFeatureList(dataset, segID);

				double[] xPoints = new double[offsetPoints.size()];
				double[] yPoints = new double[offsetPoints.size()];

				for(int i=0; i<offsetPoints.size(); i++){

					xPoints[i] = offsetPoints.get(i).getX();
					yPoints[i] = offsetPoints.get(i).getY();

				}

				double[][] data = { xPoints, yPoints };

				ds.addSeries("Segment_"+segID+"_"+dataset.getName(), data);
				
			} catch (UnavailableBorderTagException | ProfileException e) {
				warn("Missing segment from "+dataset.getName());
			}
			
			
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
	public List<IPoint> createAbsolutePositionFeatureList(IAnalysisDataset dataset, UUID segmentID) throws ChartDatasetCreationException{
		
		if(dataset==null){
			throw new IllegalArgumentException("Dataset is null");
		}
		
		if(segmentID==null){
			throw new IllegalArgumentException("Segment id is null");
		}
		
		List<IPoint> result = new ArrayList<IPoint>();
		
		/*
		 * Fetch the cells from the dataset, and rotate the nuclei appropriately
		 */
		finest("Fetching segment position for each nucleus");
		for(Nucleus nucleus : dataset.getCollection().getNuclei()){

//			nucleus.updateVerticallyRotatedNucleus(); // TODO: forcing an update here because new analyses don't have a proper vertical yet
			Nucleus verticalNucleus = nucleus.getVerticallyRotatedNucleus();
			finest("Fetched vertical nucleus");

			// Get the segment start position XY coordinates
			
			try {

				if( ! verticalNucleus.getProfile(ProfileType.ANGLE)
						.hasSegment(segmentID)){
					fine("Segment "+segmentID.toString()+" not found in vertical nucleus for "+nucleus.getNameAndNumber());
					continue;

				}
				IBorderSegment segment = verticalNucleus.getProfile(ProfileType.ANGLE)
						.getSegment(segmentID);
				finest("Fetched segment "+segmentID.toString());

				int start = segment.getStartIndex();
				finest("Getting start point at index "+start);
				IPoint point = verticalNucleus.getBorderPoint(start);
				result.add(point);	
			} catch(UnavailableProfileTypeException e){
				warn("Cannot get angle profile for nucleus");
				
			}
			
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
	 * @throws ChartDatasetCreationException if data is not available 
	 */
	public List<IPoint> createRelativePositionFeatureList(IAnalysisDataset dataset, UUID segmentID)
			throws ChartDatasetCreationException{
		
		List<IPoint> result = createAbsolutePositionFeatureList( dataset, segmentID);
		
		/*
		 * Don't worry about changing things if there is not consensus nucleus
		 */
		if( ! dataset.getCollection().hasConsensus()){
			return result;		
		} 

		
		/*
		 * Find the start point of the segment in the consensus nucleus
		 */
		Nucleus consensus = dataset.getCollection()
				.getConsensus()
				.getVerticallyRotatedNucleus();
		
		// Get the segment start position XY coordinates
		IBorderSegment segment;
		try {
			segment = consensus.getProfile(ProfileType.ANGLE)
												.getSegment(segmentID);
			
			IPoint centrePoint = consensus.getBorderPoint(segment.getStartIndex());
			
			/*
			 * The list of XYPoints are the absolute positions in cartesian space.
			 * This should be corrected to offsets from the geometric centre of the cluster
			 */
					
			/*
			 * Update the result positions to be offsets to the centre 
			 */
			
			for(IPoint p : result){

				double offsetX = p.getX() - centrePoint.getX();
				double offsetY = p.getY() - centrePoint.getY();
				
//				p.setX(offsetX);
//				p.setY(offsetY);

			}
			
			
		} catch(UnavailableProfileTypeException e){
			warn("Cannot get angle profile for nucleus");	
		}

		return result;
	}

}
