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

import gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;
//import ij.IJ;
import ij.process.FloatPolygon;
import stats.DipTester;
import stats.KruskalTester;
import stats.NucleusStatistic;
import stats.SegmentStatistic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import logging.Loggable;

import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import charting.options.ChartOptions;
import utility.Constants;
import weka.estimators.KernelEstimator;
import analysis.AnalysisDataset;
import analysis.BooleanAligner;
import analysis.nucleus.NucleusMeshBuilder;
import analysis.nucleus.NucleusMeshBuilder.NucleusMesh;
import analysis.nucleus.NucleusMeshBuilder.NucleusMeshEdge;
import components.AbstractCellularComponent;
import components.Cell;
import components.CellCollection;
import components.CellularComponent;
import components.generic.BorderTag;
import components.generic.Equation;
import components.generic.MeasurementScale;
import components.generic.Profile;
import components.generic.ProfileCollection;
import components.generic.ProfileType;
import components.generic.SegmentedProfile;
import components.generic.XYPoint;
import components.nuclear.NuclearSignal;
import components.nuclear.BorderPoint;
import components.nuclear.NucleusBorderSegment;
import components.nuclei.ConsensusNucleus;
import components.nuclei.Nucleus;
import components.nuclei.sperm.RodentSpermNucleus;

public class NucleusDatasetCreator implements Loggable {
	
	/**
	 * Add individual segments from a profile to a dataset. Offset them to the given length
	 * @param segments the list of segments to add
	 * @param profile the profile against which to add them
	 * @param ds the dataset the segments are to be added to
	 * @param length the profile length
	 * @param offset an offset to the x position. Used to align plots to the right
	 * @param binSize the size of the ProfileAggregate bins, to adjust the offset of the median
	 * @return the updated dataset
	 */
	private static XYDataset addSegmentsFromProfile(List<NucleusBorderSegment> segments, Profile profile, DefaultXYDataset ds, int length, double offset) throws Exception {
		
		Profile xpoints = profile.getPositions(length);
		xpoints = xpoints.add(offset);
		for(NucleusBorderSegment seg : segments){

			if(seg.wraps()){ // case when array wraps. We need to plot the two ends as separate series
				
				if(seg.getEndIndex()==0){
					// no need to make two sections
					Profile subProfile = profile.getSubregion(seg.getStartIndex(), profile.size()-1);
					Profile subPoints  = xpoints.getSubregion(seg.getStartIndex(), profile.size()-1);
					
					double[][] data = { subPoints.asArray(), subProfile.asArray() };
					
					// check if the series key is taken
					String seriesName = checkSeriesName(ds, seg.getName());
					
					ds.addSeries(seriesName, data);
					
				} else {
							
					int lowerIndex = Math.min(seg.getEndIndex(), seg.getStartIndex());
					int upperIndex = Math.max(seg.getEndIndex(), seg.getStartIndex());

					// beginning of array
					Profile subProfileA = profile.getSubregion(0, lowerIndex);
					Profile subPointsA  = xpoints.getSubregion(0, lowerIndex);

					double[][] dataA = { subPointsA.asArray(), subProfileA.asArray() };
					ds.addSeries(seg.getName()+"_A", dataA);

					// end of array
					Profile subProfileB = profile.getSubregion(upperIndex, profile.size()-1);
					Profile subPointsB  = xpoints.getSubregion(upperIndex, profile.size()-1);

					double[][] dataB = { subPointsB.asArray(), subProfileB.asArray() };
					ds.addSeries(seg.getName()+"_B", dataB);
				}

				continue; // move on to the next segment
					
				
			} 
			Profile subProfile = profile.getSubregion(seg);
			Profile subPoints  = xpoints.getSubregion(seg);

			double[][] data = { subPoints.asArray(), subProfile.asArray() };
			
			// check if the series key is taken
			String seriesName = checkSeriesName(ds, seg.getName());
			
			ds.addSeries(seriesName, data);
		}
		return ds;
	}
	
	/**
	 * Check if the string for the series key is aleady used. If so, append _1 and check again
	 * @param ds the dataset of series
	 * @param name the name to check
	 * @return a valid name
	 */
	private static String checkSeriesName(XYDataset ds, String name){
		String result = name;
		boolean ok = true;
		for(int i=0;i<ds.getSeriesCount();i++){
			if(ds.getSeriesKey(i).equals(name)){
				ok=false; // do not allow the same name to be added twice
			}
		}
		if(!ok){
			result = checkSeriesName(ds, name+"_1");
		} 
		return result;

	}


    /**
     * Create a line chart dataset for comparing segment lengths. Each normalised profile will be drawn in full, 
     * plus the given segment within each profile. 
     * @param list the datasets to draw
     * @param segName the segment to add in each dataset
     * @return an XYDataset to plot
     */
    public static DefaultXYDataset createMultiProfileSegmentDataset(List<AnalysisDataset> list, String segName) throws Exception{
        
        DefaultXYDataset ds = new DefaultXYDataset();
        for (int i=0; i < list.size(); i++) {

            CellCollection collection = list.get(i).getCollection();
            
            Profile profile = collection.getProfileCollection(ProfileType.REGULAR).getProfile(BorderTag.REFERENCE_POINT, Constants.MEDIAN);
            Profile xpoints = profile.getPositions(100);
            double[][] data = { xpoints.asArray(), profile.asArray() };
            ds.addSeries("Profile_"+i, data);
            
            List<NucleusBorderSegment> segments = collection.getProfileCollection(ProfileType.REGULAR).getSegments(BorderTag.REFERENCE_POINT);
            List<NucleusBorderSegment> segmentsToAdd = new ArrayList<NucleusBorderSegment>(0);
            
            // add only the segment of interest
            for(NucleusBorderSegment seg : segments){
//                if(seg.getName().equals(segName)){
                    segmentsToAdd.add(seg);
//                }
            }
            if(!segmentsToAdd.isEmpty()){
                addSegmentsFromProfile(segmentsToAdd, profile, ds, 100, 0);
            }
            

        }
        return ds;
    }

	
	/**
	 * For offsetting a raw profile to the right. Find the maximum length of median profile in the dataset.
	 * @param list the datasets to check
	 * @return the maximum length
	 */
	public static double getMaximumMedianProfileLength(List<AnalysisDataset> list){
		double length = 100;
		for(AnalysisDataset dataset : list){
			length = dataset.getCollection().getMedianArrayLength()>length ? dataset.getCollection().getMedianArrayLength() : length;
		}
		return length;
	}
	
	/**
	 * Get the maximum nucleus length in a collection
	 * @param list
	 * @return
	 */
	public static double getMaximumNucleusProfileLength(CellCollection collection){
		double length = 100;

		for(Nucleus n : collection.getNuclei()){
			length = n.getBorderLength() > length ? n.getBorderLength() : length;
		}

		return length;
	}
	
	
	/**
	 * Create raw profiles for each given AnalysisDataset. Offset them to left or right, and add the given segment 
	 * @param list the datasets
	 * @param segName the segment to display
	 * @param rightAlign alignment to left or right
	 * @return a dataset to plot
	 * @throws Exception 
	 */
	public static DefaultXYDataset createRawMultiProfileSegmentDataset(List<AnalysisDataset> list, String segName, ProfileAlignment alignment) throws Exception{
		DefaultXYDataset ds = new DefaultXYDataset();
		
		double length = getMaximumMedianProfileLength(list);

		for (int i=0; i < list.size(); i++) {
			CellCollection collection = list.get(i).getCollection();

			Profile profile = collection.getProfileCollection(ProfileType.REGULAR).getProfile(BorderTag.REFERENCE_POINT, Constants.MEDIAN);
			Profile xpoints = profile.getPositions((int) collection.getMedianArrayLength());
			
			double offset = 0;
			if(alignment.equals(ProfileAlignment.RIGHT)){
				double differenceToMaxLength = length - collection.getMedianArrayLength();
				offset = differenceToMaxLength;
				xpoints = xpoints.add(differenceToMaxLength);
			}
			double[][] data = { xpoints.asArray(), profile.asArray() };
			ds.addSeries("Profile_"+i, data);
			
			List<NucleusBorderSegment> segments = collection.getProfileCollection(ProfileType.REGULAR).getSegments(BorderTag.REFERENCE_POINT);
			List<NucleusBorderSegment> segmentsToAdd = new ArrayList<NucleusBorderSegment>(0);
			
			// add only the segment of interest
			for(NucleusBorderSegment seg : segments){
//				if(seg.getName().equals(segName)){
					segmentsToAdd.add(seg);
//				}
			}
			if(!segmentsToAdd.isEmpty()){
				addSegmentsFromProfile(segmentsToAdd, profile, ds, (int) collection.getMedianArrayLength(), offset);
			}
		}
		return ds;
	}
	
	
	
	/**
	 * Create a charting dataset for the median profile of an AnalysisDataset.
	 * This is only the median line, with no segments. To get a segmented profile,
	 * use createSegmentedMedianProfileDataset()
	 * @param dataset
	 * @param normalised
	 * @param alignment
	 * @param point
	 * @return
	 * @see createSegmentedMedianProfileDataset()
	 * @throws Exception 
	 */
	public static XYDataset createNonsegmentedMedianProfileDataset(AnalysisDataset dataset, boolean normalised, ProfileAlignment alignment, BorderTag point) throws Exception{
		CellCollection collection = dataset.getCollection();
		DefaultXYDataset ds = new DefaultXYDataset();
				
		int maxLength = (int) getMaximumNucleusProfileLength(collection);
		int medianProfileLength = (int) collection.getMedianArrayLength();
		double offset = 0;
				
		Profile profile = collection.getProfileCollection(ProfileType.REGULAR).getProfile(point, 50);
		Profile xpoints = null;
		if(normalised){
			xpoints = profile.getPositions(100);
		} else {
			xpoints = profile.getPositions( medianProfileLength );
			
			if(alignment.equals(ProfileAlignment.RIGHT)){
				double differenceToMaxLength = maxLength - collection.getMedianArrayLength();
				offset = differenceToMaxLength;
				xpoints = xpoints.add(differenceToMaxLength);
			}
		}

		// rendering order will be first on top
		double[][] data = { xpoints.asArray(), profile.asArray() };
		ds.addSeries("Profile_"+dataset.getName(), data);

		return ds;
	}
	
	
	/**
	 * Make a dataset from the given collection, with each segment profile as a separate series
	 * @param dataset 
	 * @param normalised normalise profile length to 100, or show raw
	 * @return a dataset
	 * @throws Exception 
	 */
	public static XYDataset createSegmentedMedianProfileDataset(AnalysisDataset dataset, boolean normalised, ProfileAlignment alignment, BorderTag point) throws Exception{
		
		CellCollection collection = dataset.getCollection();
		DefaultXYDataset ds = new DefaultXYDataset();
		

		// Find the longest nucleus profile in the collection (for alignment)
		int maxLength = (int) getMaximumNucleusProfileLength(collection);
		int medianProfileLength = (int) collection.getMedianArrayLength();
		double offset = 0;
				
		Profile profile = collection.getProfileCollection(ProfileType.REGULAR).getProfile(point, Constants.MEDIAN);
		Profile xpoints = null;
		if(normalised){
			xpoints = profile.getPositions(100);
		} else {
			xpoints = profile.getPositions( medianProfileLength );
			
			if(alignment.equals(ProfileAlignment.RIGHT)){
				double differenceToMaxLength = maxLength - collection.getMedianArrayLength();
				offset = differenceToMaxLength;
				xpoints = xpoints.add(differenceToMaxLength);
			}
		}

		// rendering order will be first on top
		
		// add the segments
		List<NucleusBorderSegment> segments = collection.getProfileCollection(ProfileType.REGULAR)
				.getSegmentedProfile(point)
				.getOrderedSegments();

		if(normalised){
			addSegmentsFromProfile(segments, profile, ds, 100, 0);
		} else {
			addSegmentsFromProfile(segments, profile, ds, (int) collection.getMedianArrayLength(), offset);
		}

		return ds;
	}
	
	/**
	 * Create a segmented profile dataset
	 * @param options
	 * @return
	 * @throws Exception
	 */
	public static XYDataset createSegmentedProfileDataset(ChartOptions options) throws Exception{
//		IJ.log(options.toString());
		return createSegmentedProfileDataset(options.firstDataset().getCollection(),
				options.isNormalised(),
				options.getAlignment(),
				options.getTag(),
				options.getType());
	}
	
	/**
	 * Make a dataset from the given collection, with each segment profile as a separate series
	 * @param collection the NucleusCollection
	 * @param normalised normalise profile length to 100, or show raw
	 * @return a dataset
	 * @throws Exception 
	 */
	private static XYDataset createSegmentedProfileDataset(CellCollection collection, boolean normalised, ProfileAlignment alignment, BorderTag point, ProfileType type) throws Exception{
		
//		IJ.log("Creating segmented profile dataset");
		DefaultXYDataset ds = new DefaultXYDataset();
				
		int maxLength = (int) getMaximumNucleusProfileLength(collection);
		int medianProfileLength = (int) collection.getMedianArrayLength();
		double offset = 0;
				
//		IJ.log("Getting median profile dataset");
		Profile profile = collection.getProfileCollection(type).getProfile(point, Constants.MEDIAN);
		Profile xpoints = null;
		if(normalised){
			xpoints = profile.getPositions(100);
		} else {
			xpoints = profile.getPositions( medianProfileLength );
			
			if(alignment.equals(ProfileAlignment.RIGHT)){
				double differenceToMaxLength = maxLength - collection.getMedianArrayLength();
				offset = differenceToMaxLength;
				xpoints = xpoints.add(differenceToMaxLength);
			}
		}

		// rendering order will be first on top
		
		// add the segments
		List<NucleusBorderSegment> segments = collection.getProfileCollection(type)
				.getSegmentedProfile(point)
				.getOrderedSegments();
		
//		IJ.log("Adding segments from median angle profile");
		if(normalised){
			addSegmentsFromProfile(segments, profile, ds, 100, 0);
		} else {
			addSegmentsFromProfile(segments, profile, ds, (int) collection.getMedianArrayLength(), offset);
		}

		// make the IQR
		Profile profile25 = collection.getProfileCollection(type).getProfile(point, Constants.LOWER_QUARTILE);
		Profile profile75 = collection.getProfileCollection(type).getProfile(point, Constants.UPPER_QUARTILE);
		double[][] data25 = { xpoints.asArray(), profile25.asArray() };
		ds.addSeries("Q25", data25);
		double[][] data75 = { xpoints.asArray(), profile75.asArray() };
		ds.addSeries("Q75", data75);

		// add the individual nuclei
		for(Nucleus n : collection.getNuclei()){
			Profile angles  = null;
			Profile x		= null;
			if(normalised){
				
				int length = xpoints.size();
				angles = n.getProfile(type, point).interpolate(length);
				x = xpoints;
			} else {
				angles = n.getProfile(type, point);
				x = angles.getPositions(n.getBorderLength());
				if(alignment.equals(ProfileAlignment.RIGHT)){
					double differenceToMaxLength = maxLength - n.getBorderLength();
					x = x.add(differenceToMaxLength);
				}
			}
			double[][] ndata = { x.asArray(), angles.asArray() };

			ds.addSeries("Nucleus_"+n.getSourceFileName()+"-"+n.getNucleusNumber(), ndata);
			
		}
		return ds;
	}
	
		
	public static DefaultXYDataset createMultiProfileDataset(ChartOptions options) throws Exception{
		return createMultiProfileDataset(options.getDatasets(),
				options.isNormalised(),
				options.getAlignment(),
				options.getTag(),
				options.getType());
	}
		
	
	/**
	 * Create a dataset for multiple AnalysisDatsets
	 * @param list the datasets
	 * @param normalised is the length normalised to 100 
	 * @param rightAlign is a non-normalised dataset hung to the right
	 * @return
	 * @throws Exception 
	 */
	private static DefaultXYDataset createMultiProfileDataset(List<AnalysisDataset> list, boolean normalised, ProfileAlignment alignment, BorderTag borderTag, ProfileType type) throws Exception{
		DefaultXYDataset ds = new DefaultXYDataset();
		
		double length = getMaximumMedianProfileLength(list);

		for(int i=0; i<list.size(); i++){ //AnalysisDataset dataset : list){
			AnalysisDataset dataset = list.get(i);
			CellCollection collection = dataset.getCollection();
			Profile profile = collection.getProfileCollection(type).getProfile(borderTag, 50);
			Profile xpoints = null;
			
			if(normalised){	
				xpoints = profile.getPositions(100);
			} else {
				xpoints = profile.getPositions((int) collection.getMedianArrayLength());
			}

			if(alignment.equals(ProfileAlignment.RIGHT)){
				double differenceToMaxLength = length - dataset.getCollection().getMedianArrayLength();
				xpoints = xpoints.add(differenceToMaxLength);
			}
			
			
			double[][] data = { xpoints.asArray(), profile.asArray() };
			ds.addSeries("Profile_"+i, data);
		}
		return ds;
	}
	
	/**
	 * Create a franken profile dataset for multiple AnalysisDatsets
	 * @param list the datasets
	 * @param normalised is the length normalised to 100 
	 * @param rightAlign is a non-normalised dataset hung to the right
	 * @return
	 * @throws Exception 
	 */
	public static DefaultXYDataset createMultiProfileFrankenDataset(List<AnalysisDataset> list, boolean normalised, ProfileAlignment alignment, BorderTag borderTag) throws Exception{
		DefaultXYDataset ds = new DefaultXYDataset();

		int i=0;
		for(AnalysisDataset dataset : list){
			CellCollection collection = dataset.getCollection();
			Profile profile = collection.getProfileCollection(ProfileType.FRANKEN).getProfile(borderTag, 50);
			Profile xpoints = profile.getPositions(100);

			double[][] data = { xpoints.asArray(), profile.asArray() };
			ds.addSeries("Profile_"+i, data);
			i++;
		}
		return ds;
	}
	
	/**
	 * Create an IQR series from a profilecollection
	 * @param pc the ProfileCollection
	 * @param point the reference or orientation point 
	 * @param series the index of the series
	 * @param length the maximum length of the dataset
	 * @param medianLength the median length of the collection profile
	 * @param normalised should the data be normalised to 100
	 * @param rightAlign should the data be aligned to the right
	 * @return a new series
	 * @throws Exception 
	 */
	private static XYSeriesCollection addMultiProfileIQRSeries(ProfileCollection pc, BorderTag point, int series, double length, double medianLength, boolean normalised, ProfileAlignment alignment) throws Exception{
		Profile profile = pc.getProfile(point, 50);
		
		Profile xpoints = null;
		if(normalised){
			xpoints = profile.getPositions(100);
			
		} else {
			xpoints = profile.getPositions( (int) medianLength );
		}
		
		if(alignment.equals(ProfileAlignment.RIGHT)){
			double differenceToMaxLength = length - medianLength;
			xpoints = xpoints.add(differenceToMaxLength);
		}

		// rendering order will be first on top

		// make the IQR
		Profile profile25 = pc.getProfile(point, 25);
		Profile profile75 = pc.getProfile(point, 75);
		
		XYSeries series25 = new XYSeries("Q25_"+series);
		for(int j=0; j<profile25.size();j++){
			series25.add(xpoints.get(j), profile25.get(j));
		}
		
		XYSeries series75 = new XYSeries("Q75_"+series);
		for(int j=0; j<profile75.size();j++){
			series75.add(xpoints.get(j), profile75.get(j));
		}
		
		XYSeriesCollection xsc = new XYSeriesCollection();
	    xsc.addSeries(series25);
	    xsc.addSeries(series75);
	    return xsc;
	}
	
	
	public static List<XYSeriesCollection> createMultiProfileIQRDataset(ChartOptions options) throws Exception{
		return createMultiProfileIQRDataset(options.getDatasets(),
				options.isNormalised(),
				options.getAlignment(),
				options.getTag(),
				options.getType());
	}

	
	/**
	 * Get the IQR for a set of profiles as a dataset
	 * @param list the datasets
	 * @param normalised should the data be normalised or raw length
	 * @param rightAlign should raw data be aligned to the right edge of the plot
	 * @return a dataset
	 * @throws Exception 
	 */
	private static List<XYSeriesCollection> createMultiProfileIQRDataset(List<AnalysisDataset> list, boolean normalised, ProfileAlignment alignment, BorderTag borderTag, ProfileType type) throws Exception{

		List<XYSeriesCollection> result = new ArrayList<XYSeriesCollection>(0);
		
		double length = getMaximumMedianProfileLength(list);

		for(int i=0; i<list.size(); i++){ //AnalysisDataset dataset : list){
			AnalysisDataset dataset = list.get(i);
			CellCollection collection = dataset.getCollection();
			
			XYSeriesCollection xsc = addMultiProfileIQRSeries(collection.getProfileCollection(type), 
										borderTag,
										i,
										length,
										collection.getMedianArrayLength(),
										normalised,
										alignment);
		    result.add(xsc);
		}
		return result;
	}
	
	
	/**
	 * Get the IQR for the frankenmedian in a list of datasets
	 * @param list
	 * @return
	 * @throws Exception 
	 */
	public static List<XYSeriesCollection> createMultiProfileIQRFrankenDataset(List<AnalysisDataset> list, boolean normalised, ProfileAlignment alignment,  BorderTag borderTag) throws Exception{
		List<XYSeriesCollection> result = new ArrayList<XYSeriesCollection>(0);

		int i=0;
		for(AnalysisDataset dataset : list){
			
			CellCollection collection = dataset.getCollection();
			
			XYSeriesCollection xsc = addMultiProfileIQRSeries(collection.getProfileCollection(ProfileType.FRANKEN), 
					borderTag,
					i,
					100,
					100,
					true,
					alignment);
		    result.add(xsc);
		    i++;
		}
		return result;
	}
	
	/**
	 * Create a chart of the variability in the interquartile ranges across the angle profiles of the given datasets
	 * @param list
	 * @param borderTag
	 * @param data.type
	 * @return
	 * @throws Exception
	 */
	public static XYDataset createIQRVariabilityDataset(ChartOptions options) throws Exception{

		
		if(options.isSingleDataset()){
			CellCollection collection = options.firstDataset().getCollection();

			Profile profile = collection.getProfileCollection(options.getType()).getIQRProfile(options.getTag());

			List<NucleusBorderSegment> segments = collection.getProfileCollection(options.getType())
					.getSegmentedProfile(options.getTag())
					.getOrderedSegments();
			
			XYDataset ds = addSegmentsFromProfile(segments, profile, new DefaultXYDataset(), 100, 0);	
			return ds;
		} else {
			int i = 0;
			DefaultXYDataset ds = new DefaultXYDataset();
			for(AnalysisDataset dataset : options.getDatasets()){
				CellCollection collection = dataset.getCollection();

				Profile profile = collection.getProfileCollection(options.getType()).getIQRProfile(options.getTag());
				Profile xpoints = profile.getPositions(100);
				double[][] data = { xpoints.asArray(), profile.asArray() };
				ds.addSeries("Profile_"+i+"_"+collection.getName(), data);
				i++;
			}
			return ds;
		}
		
	}
	
	public static XYDataset createFrankenSegmentDataset(ChartOptions options) throws Exception{
		return createFrankenSegmentDataset(options.firstDataset().getCollection(),
				options.isNormalised(),
				options.getAlignment(),
				options.getTag());
	}
		
		
	/**
	 * Create a dataset containing series for each segment within the FrankenCollection
	 * @param collection the cell collection to draw from
	 * @param normalised should the chart be normalised to 100
	 * @param alignment hang the chart from left or right
	 * @param point the zero index of the chart
	 * @return
	 * @throws Exception
	 */
	private static XYDataset createFrankenSegmentDataset(CellCollection collection, boolean normalised, ProfileAlignment alignment, BorderTag point) throws Exception{
		DefaultXYDataset ds = new DefaultXYDataset();
		
//		String pointType = collection.getOrientationPoint();
		Profile profile = collection.getProfileCollection(ProfileType.FRANKEN).getProfile(point, Constants.MEDIAN);
		Profile xpoints = profile.getPositions(100);
		
		// rendering order will be first on top
		
		// add the segments (these are the same as in the regular profile collection)
//		List<NucleusBorderSegment> segments = collection.getProfileCollection(ProfileCollectionType.REGULAR).getSegments(point);
		List<NucleusBorderSegment> segments = collection.getProfileCollection(ProfileType.REGULAR)
				.getSegmentedProfile(point)
				.getOrderedSegments();
		addSegmentsFromProfile(segments, profile, ds, 100, 0);

		// make the IQR
		Profile profile25 = collection.getProfileCollection(ProfileType.FRANKEN).getProfile(point, Constants.LOWER_QUARTILE);
		Profile profile75 = collection.getProfileCollection(ProfileType.FRANKEN).getProfile(point, Constants.UPPER_QUARTILE);
		double[][] data25 = { xpoints.asArray(), profile25.asArray() };
		ds.addSeries("Q25", data25);
		double[][] data75 = { xpoints.asArray(), profile75.asArray() };
		ds.addSeries("Q75", data75);

		// add the individual nuclei
		int profileCount = 0;
		
		for(Nucleus n : collection.getNuclei()){
//		for(Profile angles : collection.getProfileCollection(ProfileType.FRANKEN).getNucleusProfiles(point)){

			Profile angles = n.getProfile(ProfileType.FRANKEN); // do not offset, the offsets for a nucleus do not match a frankenprofile
			double[] xArray = xpoints.asArray();
			double[] yArray = angles.asArray();
			
			if(xArray.length!=yArray.length){ // ensure length mismatches don't crash the system

				yArray = new double[xArray.length];
				for(int i=0; i<xArray.length; i++){
					yArray[i] = 0;
				}
				
			} 
			double[][] ndata = new double[][] { xArray, yArray };
			
			
			ds.addSeries("Nucleus_"+profileCount, ndata);
			profileCount++;
		}
		return ds;
	}
	
	/**
	 * Create a segmented dataset for an individual nucleus. Segments are added for
	 * all types except frankenprofiles, since the frankenprofile boundaries will not match
	 * @param nucleus the nucleus to draw
	 * @param type the profile type to draw.
	 * @return
	 * @throws Exception 
	 */
	public static XYDataset createSegmentedProfileDataset(Nucleus nucleus, ProfileType type) throws Exception{
		DefaultXYDataset ds = new DefaultXYDataset();
		
		SegmentedProfile profile;
		Profile xpoints;
		
		if(type.equals(ProfileType.FRANKEN)){
			profile = nucleus.getProfile(type);
			xpoints = profile.getPositions(profile.size());
		} else {
			profile = nucleus.getProfile(type, BorderTag.REFERENCE_POINT);
			xpoints = profile.getPositions(nucleus.getBorderLength());
			
			// add the segments
			List<NucleusBorderSegment> segments = nucleus.getProfile(ProfileType.REGULAR, BorderTag.REFERENCE_POINT).getOrderedSegments();
			addSegmentsFromProfile(segments, profile, ds, nucleus.getBorderLength(), 0);
		}

		double[][] ndata = { xpoints.asArray(), profile.asArray() };
		ds.addSeries("Nucleus_"+nucleus.getSourceFileName()+"-"+nucleus.getNucleusNumber(), ndata);
		
		return ds;
	}

	
	/**
	 * Get a boxplot dataset for the given statistic for each collection
	 * @param options the charting options
	 * @return
	 * @throws Exception
	 */
	public static BoxAndWhiskerCategoryDataset createBoxplotDataset(ChartOptions options) throws Exception{
		List<AnalysisDataset> datasets = options.getDatasets();
		NucleusStatistic stat = (NucleusStatistic) options.getStat();
		MeasurementScale scale = options.getScale();
		OutlierFreeBoxAndWhiskerCategoryDataset ds = new OutlierFreeBoxAndWhiskerCategoryDataset();

		for (int i=0; i < datasets.size(); i++) {
			CellCollection c = datasets.get(i).getCollection();

			List<Double> list = new ArrayList<Double>();
			double[] stats = c.getNuclearStatistics(stat, scale);

			for (double d : stats) {
				list.add(new Double(d));
			}
			ds.add(list, c.getName()+"_"+i, stat.toString());
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
	 * @throws Exception
	 */
	public static BoxAndWhiskerCategoryDataset createSegmentStatDataset(ChartOptions options) throws Exception {
		
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
	
	/**
	 * Get the lengths of the given segment in the collections
	 * @param collections
	 * @param segName
	 * @return
	 * @throws Exception 
	 */
	public static BoxAndWhiskerCategoryDataset createSegmentLengthDataset(List<AnalysisDataset> collections, int segPosition, MeasurementScale scale) throws Exception {

		OutlierFreeBoxAndWhiskerCategoryDataset dataset = new OutlierFreeBoxAndWhiskerCategoryDataset();

		for (int i=0; i < collections.size(); i++) {

			CellCollection collection = collections.get(i).getCollection();
			
			NucleusBorderSegment medianSeg = collection
					.getProfileCollection(ProfileType.REGULAR)
					.getSegmentedProfile(BorderTag.REFERENCE_POINT)
					.getSegmentAt(segPosition);


			List<Double> list = new ArrayList<Double>(0);

			for(Nucleus n : collection.getNuclei()){
				
				NucleusBorderSegment seg = n.getProfile(ProfileType.REGULAR, BorderTag.REFERENCE_POINT)
						.getSegment(medianSeg.getID());			

				
				double length = 0;
				if(seg!=null){
					int indexLength = seg.length();
					double proportionPerimeter = (double) indexLength / (double) seg.getTotalLength();
					length = n.getStatistic(NucleusStatistic.PERIMETER, scale) * proportionPerimeter;
					
				}
				list.add(length);
			}

			dataset.add(list, Constants.SEGMENT_PREFIX+segPosition+"_"+i, Constants.SEGMENT_PREFIX+segPosition);
		}
		return dataset;
	}
	
	/**
	 * Get the displacements of the given segment in the collections
	 * @param collections
	 * @param segName
	 * @return
	 * @throws Exception 
	 */
	public static BoxAndWhiskerCategoryDataset createSegmentDisplacementDataset(List<AnalysisDataset> collections, int segPosition) throws Exception {

		OutlierFreeBoxAndWhiskerCategoryDataset dataset = new OutlierFreeBoxAndWhiskerCategoryDataset();

		for (int i=0; i < collections.size(); i++) {

			CellCollection collection = collections.get(i).getCollection();
			
			NucleusBorderSegment medianSeg = collection
					.getProfileCollection(ProfileType.REGULAR)
					.getSegmentedProfile(BorderTag.REFERENCE_POINT)
					.getSegmentAt(segPosition);


			List<Double> list = new ArrayList<Double>(0);

			for(Nucleus n : collection.getNuclei()){
				SegmentedProfile profile = n.getProfile(ProfileType.REGULAR, BorderTag.REFERENCE_POINT);
				
				NucleusBorderSegment seg = profile.getSegment(medianSeg.getID());
				
				double displacement = profile.getDisplacement(seg);
				list.add(displacement);
			}

			dataset.add(list, Constants.SEGMENT_PREFIX+segPosition+"_"+i, Constants.SEGMENT_PREFIX+segPosition);
		}
		return dataset;
	}
	
	/**
	 * Get the variability of each segment in terms of length difference to the median
	 * profile segment
	 * @param datasets
	 * @return
	 * @throws Exception 
	 */
	public static BoxAndWhiskerCategoryDataset createSegmentVariabillityDataset(List<AnalysisDataset> datasets) throws Exception {

		if(datasets==null || datasets.isEmpty()){
			return null;
		}
		OutlierFreeBoxAndWhiskerCategoryDataset dataset = new OutlierFreeBoxAndWhiskerCategoryDataset();

		for (int i=0; i < datasets.size(); i++) {

			CellCollection collection = datasets.get(i).getCollection();

			List<NucleusBorderSegment> segments = collection.getProfileCollection(ProfileType.REGULAR).getSegmentedProfile(BorderTag.REFERENCE_POINT).getOrderedSegments();
//			List<NucleusBorderSegment> segments = collection.getProfileCollection(ProfileCollectionType.REGULAR).getSegments(BorderTag.ORIENTATION_POINT);
			
			for(NucleusBorderSegment medianSeg : segments){
				
				int medianSegmentLength = medianSeg.length();
				
				List<Integer> list = new ArrayList<Integer>(0);
				
				for(Nucleus n : collection.getNuclei()){
					NucleusBorderSegment seg = n.getProfile(ProfileType.REGULAR).getSegment(medianSeg.getName());
					
					int differenceToMedian = 0;
					// if seg is null, catch before we throw an error
					if(seg!=null){
						differenceToMedian = medianSegmentLength - seg.length();
					}

					list.add(differenceToMedian);
				}
				
				dataset.add(list, medianSeg.getName(), collection.getName());
			}
		}
		return dataset;
	}
	
	/**
	 * Get the outline of the consensus nucleus. No segmentation, no IQR
	 * @param dataset
	 * @return
	 */
	public static XYDataset createBareNucleusOutline(AnalysisDataset dataset){
		DefaultXYDataset ds = new DefaultXYDataset();
		Nucleus n = dataset.getCollection().getConsensusNucleus();
		
		double[] xpoints = new double[n.getBorderLength()+1];
		double[] ypoints = new double[n.getBorderLength()+1];
		
		for(int i=0; i<n.getBorderLength();i++){
			BorderPoint p = n.getBorderPoint(i);
			xpoints[i] = p.getX();
			ypoints[i] = p.getY();
		}
		// complete the line
		xpoints[n.getBorderLength()] = xpoints[0];
		ypoints[n.getBorderLength()] = ypoints[0];
		
		double[][] data = { xpoints, ypoints };
		ds.addSeries("Outline", data);
		return ds;
	}
	
	
	/**
	 * Get the scale of the nucleus; the lowest absolute x or y limit
	 * @param n
	 * @return
	 */
	private static double getScaleForIQRRange(Nucleus n){
		// get the maximum values from nuclear diameters
		// get the limits  for the plot  	
		double min = Math.min(n.getMinX(), n.getMinY());
		double max = Math.max(n.getMaxX(), n.getMaxY());
		double scale = Math.min(Math.abs(min), Math.abs(max));
		return scale;
	}
	
	/**
	 * Create an outline of the consensus nucleus, and apply segments as separate series
	 * @param collection
	 * @return
	 * @throws Exception 
	 */
	public static XYDataset createSegmentedNucleusOutline(CellCollection collection) throws Exception {
		DefaultXYDataset ds = new DefaultXYDataset();
		
		// get the consensus nucleus for the population
		ConsensusNucleus n = collection.getConsensusNucleus();
		
		BorderTag pointType = BorderTag.REFERENCE_POINT;
		
		// get the quartile profiles, beginning from the orientation point
		Profile q25 = collection.getProfileCollection(ProfileType.REGULAR).getProfile(pointType, Constants.LOWER_QUARTILE).interpolate(n.getBorderLength());
		Profile q75 = collection.getProfileCollection(ProfileType.REGULAR).getProfile(pointType, Constants.UPPER_QUARTILE).interpolate(n.getBorderLength());
		
		// get the limits  for the plot  	
		double scale = getScaleForIQRRange(n);
		
		// find the range of the iqr, and scale the values in the iqr profile to 1/10 of the total range of the plot
		//The scaled IQR is a profile beginning from the orientation point
		Profile iqrRange = q75.subtract(q25);
		Profile scaledRange = iqrRange.divide(iqrRange.getMax()); // iqr as fraction of total variability
		scaledRange = scaledRange.multiply(scale/10); // set to 10% min radius of the chart

		
		// Get the angle profile, starting from the tail point
		SegmentedProfile angleProfile = n.getProfile(ProfileType.REGULAR, pointType);
		
		// At this point, the angle profile and the iqr profile should be in sync
		// The following set of checks confirms this.
		int pointIndex = n.getBorderIndex(pointType);
//		n.dumpInfo(Nucleus.ALL_POINTS);
		
//		IJ.log("Nucleus tail index: "+n.getBorderIndex(pointType));
//		IJ.log("");
//		IJ.log("IQR range");
//		iqrRange.print();
//		IJ.log("");
//		IJ.log("Angle profile");
//		angleProfile.print();
		
		if(angleProfile.hasSegments()){ // only draw if there are segments
			
			// go through each segment
			for(NucleusBorderSegment seg :  angleProfile.getOrderedSegments()){
								
				// check the indexes that the segment covers
//				IJ.log(seg.toString());
				
				// add the segment, taking the indexes from the segment, and drawing the values 
				// in the scaled IQR profile at these positions
				
				// The segment start and end indexes should be in correspondence with the offsets
				// That is, the zero index in a segment start / end is the pointType
				
				addSegmentIQRToConsensus(seg, ds, n, scaledRange, pointType);

				// draw the segment itself
				double[] xpoints = new double[seg.length()+1];
				double[] ypoints = new double[seg.length()+1];
				
				// go through each index in the segment.
				for(int j=0; j<=seg.length();j++){
					
					// get the corresponding border index. The segments are zeroed at the tail point
					// so the correct border point needs to be offset
					int borderIndex = AbstractCellularComponent.wrapIndex(seg.getStartIndex()+j+pointIndex, n.getBorderLength());
					
					BorderPoint p = n.getBorderPoint(borderIndex); // get the border points in the segment
					xpoints[j] = p.getX();
					ypoints[j] = p.getY();
				}

				double[][] data = { xpoints, ypoints };
				ds.addSeries(seg.getName(), data);
			}
		}

		
		return ds;
	}
	
	/**
	 * Add the IQR for a segment to the given dataset
	 * @param segment the segment to add
	 * @param ds the dataset to add it to
	 * @param n the consensus nucleus
	 * @param scaledRange the IQR scale profile
	 */
	private static void addSegmentIQRToConsensus(NucleusBorderSegment segment, DefaultXYDataset ds, Nucleus n, Profile scaledRange, BorderTag pointType){

		// what we need to do is match the profile positions to the borderpoints
		// Add lines to show the IQR of the angle profile at each point
		
		// arrays to hold the positions for the IQR lines
		int arrayLength = segment.length()+1;
		
		double[] innerIQRX = new double[arrayLength];
		double[] innerIQRY = new double[arrayLength];
		double[] outerIQRX = new double[arrayLength];
		double[] outerIQRY = new double[arrayLength];
		
		// Go through each position in the segment.
		// The zero index of the segmented profile is the pointType selected previously in createSegmentedNucleusOutline()
		// Hence a segment start index of zero is at the pointType
		
		for(int i=0; i<=segment.length(); i++){
			
			// get the index of this point of the segment in the nucleus border list.
			// The nucleus border list has an arbitrary zero location, and the 
			// pointType index is given within this
			// We need to add the index of the pointType to the values within the segment
			int segmentIndex = segment.getStartIndex() + i;
			int index = AbstractCellularComponent.wrapIndex(segmentIndex + n.getBorderIndex(pointType), n.getBorderLength());
			
			// get the border point at this index
			BorderPoint p = n.getBorderPoint(index); // get the border points in the segment
			
//			IJ.log("Selecting border index: "+index+" from "+segment.getName()+" index "+segmentIndex);

			// Find points three indexes ahead and behind to make a triangle from
			int prevIndex = AbstractCellularComponent.wrapIndex(index-3, n.getBorderLength());
			int nextIndex = AbstractCellularComponent.wrapIndex(index+3, n.getBorderLength());


			
			// decide the angle at which to place the iqr points
			// make a line between points 3 ahead and behind. 
			// get the orthogonal line, running through the XYPoint
			Equation eq = new Equation(n.getBorderPoint( prevIndex  ), n.getBorderPoint( nextIndex  ));
			// move the line to the index point, and find the orthogonal line
			Equation perp = eq.translate(p).getPerpendicular(p);
			
			// Select the index from the scaledRange corresponding to the position in the segment
			// The scaledRange is aligned to the segment already
			XYPoint aPoint = perp.getPointOnLine(p, (0-scaledRange.get(AbstractCellularComponent.wrapIndex(segmentIndex, n.getBorderLength() )   )    )    );
			XYPoint bPoint = perp.getPointOnLine(p, scaledRange.get(AbstractCellularComponent.wrapIndex(segmentIndex, n.getBorderLength() )));

			// determine which of the points is inside the nucleus and which is outside
			
			FloatPolygon nucleusRoi = n.createPolygon();
			XYPoint innerPoint = nucleusRoi.contains(  (float) aPoint.getX(), (float) aPoint.getY() ) ? aPoint : bPoint;
			XYPoint outerPoint = nucleusRoi.contains(  (float) bPoint.getX(), (float) bPoint.getY() ) ? aPoint : bPoint;

			
			// assign the points
			innerIQRX[i] = innerPoint.getX();
			innerIQRY[i] = innerPoint.getY();
			outerIQRX[i] = outerPoint.getX();
			outerIQRY[i] = outerPoint.getY();

		}
		
		double[][] inner = { innerIQRX, innerIQRY };
		ds.addSeries("Q25_"+segment.getName(), inner);
		double[][] outer = { outerIQRX, outerIQRY };
		ds.addSeries("Q75_"+segment.getName(), outer);
	}
	
	/**
	 * Get the outline for a specific nucleus in a dataset. Sets the position
	 * to the original coordinates in the image 
	 * @param cell
	 * @param segmented add the segments?
	 * @return
	 * @throws Exception 
	 */
	public static XYDataset createNucleusOutline(Cell cell, boolean segmented) throws Exception{
		return createNucleusOutline(cell.getNucleus(), segmented);
	}
	
	/**
	 * Get the outline for a specific nucleus in a dataset. Sets the position
	 * to the original coordinates in the image 
	 * @param cell
	 * @param segmented add the segments?
	 * @return
	 * @throws Exception 
	 */
	public static XYDataset createNucleusOutline(Nucleus nucleus, boolean segmented) throws Exception{
		DefaultXYDataset ds = new DefaultXYDataset();

		if(segmented){

			/*
			 * With the ability to merge segments, we cannot be sure that an iterator
			 * based on numbers will work
			 */
			List<NucleusBorderSegment> segmentList = nucleus.getProfile(ProfileType.REGULAR).getSegments();
//			SegmentedProfile profile = nucleus.getAngleProfile(BorderTag.REFERENCE_POINT);

			
			if(!segmentList.isEmpty()){ // only draw if there are segments
				
				for(NucleusBorderSegment seg  : segmentList){

					double[] xpoints = new double[seg.length()+1];
					double[] ypoints = new double[seg.length()+1];
					
					/* We are adding the border points directly in the order they are found in the nucleus
					 * This means the profile cannot be offset for segmentation.
					 * The choice of segment name must be made in another manner
					 * 
					 * Identify Seg_0 by finding the segment with the reference point index 
					 * at the start
					 */
					
					int segmentPosition = getSegmentPosition(nucleus, seg);
					
					
					
					for(int j=0; j<=seg.length();j++){
						int k = AbstractCellularComponent.wrapIndex(seg.getStartIndex()+j, nucleus.getBorderLength());
						BorderPoint p = nucleus.getBorderPoint(k); // get the border points in the segment
//						nucleus.getB
						xpoints[j] = p.getX();
						ypoints[j] = p.getY();
					}

					double[][] data = { xpoints, ypoints };
//					ds.addSeries(seg.getName(), data);
					ds.addSeries("Seg_"+segmentPosition, data);
				}
			}

		} else {
			double[] xpoints = new double[nucleus.getOriginalBorderList().size()];
			double[] ypoints = new double[nucleus.getOriginalBorderList().size()];

			int i =0;
			for(XYPoint p : nucleus.getBorderList()){
				xpoints[i] = p.getX();
				ypoints[i] = p.getY();
				i++;
			}

			double[][] data = { xpoints, ypoints };
			ds.addSeries(nucleus.getNameAndNumber(), data);

		}		
		return ds;
	}
	
	/**
	 * Create a dataset with the hook and hump rois for a rodent sperm nucleus. If the
	 * given cell does not contain a rodent sperm nucleus, the returned dataset is empty
	 * @param cell
	 * @return
	 * @throws Exception
	 */
	public static XYDataset createNucleusHookHumpOutline(Cell cell) throws Exception{
		
		DefaultXYDataset ds = new DefaultXYDataset();
		
		if(cell.getNucleus().getClass()==RodentSpermNucleus.class){
			
			RodentSpermNucleus nucleus = (RodentSpermNucleus) cell.getNucleus();
//			double[] position = nucleus.getPosition();
			
			double[] xpoints = new double[nucleus.getHookRoi().size()];
			double[] ypoints = new double[nucleus.getHookRoi().size()];

			int i =0;
			for(XYPoint p : nucleus.getHookRoi()){
				xpoints[i] = p.getX();
				ypoints[i] = p.getY();
				i++;
			}

			double[][] data = { xpoints, ypoints };
			ds.addSeries("Hook", data);
			
//			double[] xpoints2 = new double[nucleus.getHumpRoi().size()];
//			double[] ypoints2 = new double[nucleus.getHumpRoi().size()];
//
//			i =0;
//			for(XYPoint p : nucleus.getHumpRoi()){
//				xpoints2[i] = p.getX();
//				ypoints2[i] = p.getY();
//				i++;
//			}
//
//			double[][] data2 = { xpoints2, ypoints2 };
//			ds.addSeries("Hump", data2);
			
		} 
		return ds;
	}
	
	/**
	 * Get the position of a segment in the nucleus angle profile
	 * @param n
	 * @param seg
	 * @return
	 */
	public static int getSegmentPosition(Nucleus n, NucleusBorderSegment seg){
		
		int result = 0;
		if(seg.getStartIndex()==n.getBorderIndex(BorderTag.REFERENCE_POINT)){
			return result;
		} else {
			result++;
			result += getSegmentPosition(n, seg.prevSegment());
		}
		return result;
	}
	
	/**
	 * Create a dataset with lines from each of the BorderTags within the nucleus
	 * to the centre of mass of the nucleus
	 * @param cell
	 * @return
	 * @throws Exception
	 */
	public static XYDataset createNucleusIndexTags(Cell cell) throws Exception {

		DefaultXYDataset ds = new DefaultXYDataset();

		Nucleus nucleus = cell.getNucleus();// draw the index points on the nucleus border
		for(BorderTag tag : nucleus.getBorderTags().keySet()){
			BorderPoint tagPoint = nucleus.getBorderPoint(tag);
			double[] xpoints = { tagPoint.getX(), nucleus.getCentreOfMass().getX() };
			double[] ypoints = { tagPoint.getY(), nucleus.getCentreOfMass().getY() };
			double[][] data = { xpoints, ypoints };
			ds.addSeries("Tag_"+tag, data);
		}

		return ds;
	}
		
	/**
	 * Create a dataset for the signal groups in the cell. Each signalGroup
	 * is a new series
	 * @param cell the cell to get signals from
	 * @return a dataset for charting
	 */
	public static List<DefaultXYDataset> createSignalOutlines(Cell cell, AnalysisDataset dataset){
		
		List<DefaultXYDataset> result = new ArrayList<DefaultXYDataset>(0);
		
		Nucleus nucleus = cell.getNucleus();
		
		for(int signalGroup : nucleus.getSignalGroups()){
			
			if(dataset.isSignalGroupVisible(signalGroup)){ // only add the groups that are set to visible

				DefaultXYDataset groupDataset = new DefaultXYDataset();
				int signalNumber = 0;

				for(NuclearSignal signal : nucleus.getSignals(signalGroup)){

					double[] xpoints = new double[signal.getBorderLength()];
					double[] ypoints = new double[signal.getBorderLength()];

					int i =0;
					for(XYPoint p : signal.getBorderList()){
						xpoints[i] = p.getX() - nucleus.getPosition()[CellularComponent.X_BASE];
						ypoints[i] = p.getY() - nucleus.getPosition()[CellularComponent.Y_BASE];
						i++;
					}
					double[][] data = { xpoints, ypoints };
					groupDataset.addSeries("Group_"+signalGroup+"_signal_"+signalNumber, data);

					result.add(groupDataset);
				}
				signalNumber++;
			}
			
		}
		return result;
	}

	/**
	 * Given a list of analysis datasets, get the outlines of the consensus
	 * nuclei they contain
	 * @param list the analysis datasets
	 * @return a chartable dataset
	 */
	public static XYDataset createMultiNucleusOutline(List<AnalysisDataset> list){

		DefaultXYDataset ds = new DefaultXYDataset();

		int i=0;
		for(AnalysisDataset dataset : list){
			CellCollection collection = dataset.getCollection();
			if(collection.hasConsensusNucleus()){
				Nucleus n = collection.getConsensusNucleus();

				double[] xpoints = new double[n.getBorderLength()];
				double[] ypoints = new double[n.getBorderLength()];

				int j =0;

				for(BorderPoint p : n.getBorderList()){
					xpoints[j] = p.getX();
					ypoints[j] = p.getY();
					j++;
				}
				double[][] data = { xpoints, ypoints };
				ds.addSeries("Nucleus_"+i+"_"+collection.getName(), data);
			} else {
				double[][] data = { {0}, {0} }; // make an empty series if no consensus
				ds.addSeries("Nucleus_"+i+"_"+collection.getName(), data);
			}
			i++;

		}
		return ds;
	}
	
	/**
	 * Create a charting dataset for the angles within the AnalysisDataset at the given normalised position.
	 * This dataset has the probability density function from angles 0-360 at 0.1 degree intervals.
	 * @param xposition
	 * @param dataset
	 * @return
	 * @throws Exception
	 */
	public static XYDataset createModalityProbabililtyDataset(double xposition, AnalysisDataset dataset, ProfileType type) throws Exception {

		DefaultXYDataset ds = new DefaultXYDataset();


		CellCollection collection = dataset.getCollection();
		KernelEstimator est = createProfileProbabililtyKernel(xposition, dataset, type);
		
//		List<Double> xValues = new ArrayList<Double>();
//		List<Double> yValues = new ArrayList<Double>();
		
		double[] xvalues = new double[3600];
		double[] yvalues = new double[3600];
		
		double step = 0.1;

//		for(double i=0; i<=360; i+=0.1){
		for(int i=0; i<xvalues.length; i++){
			
			double position = (double) i * step;
			xvalues[i] = position;
			yvalues[i] = est.getProbability(position);
			
//			xValues.add(i);
//			yValues.add(est.getProbability(i));
		}
		double[][] data = { xvalues, yvalues };
//		double[][] data = { Utils.getdoubleFromDouble(xValues.toArray(new Double[0])),  
//				Utils.getdoubleFromDouble(yValues.toArray(new Double[0])) };
		
		
		ds.addSeries(collection.getName(), data);
			

		return ds;
	}
	
	/**
	 * Generate a chart dataset showing the p-values along each profile position
	 * for all datasets
	 * @param options the charting options
	 * @return
	 * @throws Exception
	 */
	public static XYDataset createModalityProfileDataset(ChartOptions options) throws Exception {

		DefaultXYDataset ds = new DefaultXYDataset();
	
		for(AnalysisDataset dataset : options.getDatasets()){
			
			CellCollection collection = dataset.getCollection();
			
			Profile pvalues = DipTester.testCollectionGetPValues(collection, options.getTag(), options.getType());
			
			double[] yvalues = pvalues.asArray();
			double[] xvalues = pvalues.getPositions(100).asArray();
			
			double[][] data = { xvalues, yvalues };
			ds.addSeries(collection.getName(), data);
		}

		return ds;
	}
	
	/**
	 * Create a charting dataset for the angles within the AnalysisDataset at the given normalised position.
	 * This dataset has the individual angle values for each nucleus profile 
	 * @param xposition
	 * @param dataset
	 * @return
	 * @throws Exception
	 */
	public static XYDataset createModalityValuesDataset(double xposition, AnalysisDataset dataset, ProfileType type) throws Exception {

		DefaultXYDataset ds = new DefaultXYDataset();
		
		CellCollection collection = dataset.getCollection();

		double[] values = collection.getProfileCollection(type).getAggregate().getValuesAtPosition(xposition);
		double[] xvalues = new double[values.length];
		for(int i=0; i<values.length; i++){
			xvalues[i] = 0;
		}
		
		double[][] data = { values, xvalues };
		ds.addSeries(collection.getName(), data);
		return ds;
	}
	
	/**
	 * Create a probability kernel estimator for the profile angle values in the dataset
	 * @param xposition the profile position
	 * @param dataset
	 * @return
	 * @throws Exception
	 */
	public static KernelEstimator createProfileProbabililtyKernel(double xposition, AnalysisDataset dataset, ProfileType type) throws Exception {
		CellCollection collection = dataset.getCollection();
		KernelEstimator est = new KernelEstimator(0.001);
		double[] values = collection.getProfileCollection(type).getAggregate().getValuesAtPosition(xposition);
		// add the values to a kernel estimator
		// give each value equal weighting
		for(double d : values){
			est.addValue(d, 1);
		}
		return est;
	}
	
	/**
	 * Create a probability kernel estimator for the profile angle values in the dataset
	 * @param xposition the profile position
	 * @param dataset
	 * @return
	 * @throws Exception
	 */
	public static KernelEstimator createProbabililtyKernel(double[] values, double binWidth) throws Exception {
		KernelEstimator est = new KernelEstimator(binWidth);
		// add the values to a kernel estimator
		// give each value equal weighting
		for(double d : values){
			est.addValue(d, 1);
		}
		return est;
	}
	
	
	/**
	 * Create a dataset suitable for making a QQ plot
	 * @param values the array of values to use
	 * @return a dataset for charting
	 */
	public static XYDataset createQQDataset(double[] values){
		DefaultXYDataset ds = new DefaultXYDataset();
		
		Arrays.sort(values);
//		double[] percentiles = new double[values.length];
		double[] zscores = new double[values.length];
		
		for(int i=0; i<values.length; i++){
			
			int rank = i+1;
			double percentile = (double) (rank-0.5)/ (double) values.length;
			zscores[i] = DipTester.getInvNormProbabililty(percentile);
			
		}
		double[][] data = { values, zscores };
		ds.addSeries("Q-Q", data);
		return ds;
		
	}
	
	/**
	 * Create a Kruskal-Wallis comparison along the angle profiles for two analysis datasets
	 * @param options
	 * @return
	 * @throws Exception
	 */
	public static XYDataset createKruskalProfileDataset(ChartOptions options) throws Exception {

		DefaultXYDataset ds = new DefaultXYDataset();
		
		AnalysisDataset setOne = options.getDatasets().get(0);
		AnalysisDataset setTwo = options.getDatasets().get(1);
	
		Profile pvalues = KruskalTester.testCollectionGetPValues(setOne, setTwo, options.getTag(), options.getType());
//		Profile pvalues = KruskalTester.testCollectionGetFrankenPValues(setOne, setTwo, options.getTag(), options.getLogger());
		
		double[] yvalues = pvalues.asArray();
		double[] xvalues = pvalues.getPositions(100).asArray();
		
		double[][] data = { xvalues, yvalues };
		ds.addSeries(setOne.getCollection().getName(), data);

		return ds;
	}
	
	/**
	 * Create a Kruskal-Wallis comparison along the angle profiles for two analysis datasets
	 * @param options
	 * @return
	 * @throws Exception
	 */
	public static XYDataset createFrankenKruskalProfileDataset(ChartOptions options) throws Exception {

		DefaultXYDataset ds = new DefaultXYDataset();
		Profile pvalues = KruskalTester.testCollectionGetFrankenPValues(options);
		
		double[] yvalues = pvalues.asArray();
		double[] xvalues = pvalues.getPositions(100).asArray();
		
		double[][] data = { xvalues, yvalues };
		ds.addSeries(options.firstDataset().getCollection().getName(), data);

		return ds;
	}
	
	/**
	 * Create an XYDataset with the edges in a NucleusMesh comparison. Also stores the result
	 * edge length ratios.
	 * @param mesh
	 * @return
	 * @throws Exception
	 */
	public static NucleusMeshXYDataset createNucleusMeshDataset(NucleusMesh mesh) throws Exception {
		NucleusMeshXYDataset ds = new NucleusMeshXYDataset();
		
		for(NucleusMeshEdge edge : mesh.getEdges()){
			
			double[] yvalues = {
					edge.getV1().getPosition().getY(),
					edge.getV2().getPosition().getY()
			};
			
			
			double[] xvalues = {
					edge.getV1().getPosition().getX(),
					edge.getV2().getPosition().getX()
			};

			double[][] data = { xvalues, yvalues };
			ds.addSeries(edge.toString(), data);
			ds.setRatio(edge.toString(), edge.getLog2Ratio());
		}
		return ds;
	}
	
	/**
	 * Create an XYDataset with the midpoints of edges in a NucleusMesh comparison. 
	 * @param mesh
	 * @return
	 * @throws Exception
	 */
	public static NucleusMeshXYDataset createNucleusMeshMidpointDataset(NucleusMesh mesh) throws Exception {
		NucleusMeshXYDataset ds = new NucleusMeshXYDataset();
		
		for(NucleusMeshEdge edge : mesh.getEdges()){
			
			double[] yvalues = {
				edge.getMidpoint().getY(),
			};
			
			
			double[] xvalues = {
				edge.getMidpoint().getX(),
			};

			double[][] data = { xvalues, yvalues };
			ds.addSeries(edge.toString(), data);
			ds.setRatio(edge.toString(), edge.getLog2Ratio());
		}
		return ds;
	}
	
	
	public static HistogramDataset createNucleusMeshHistogramDataset(NucleusMesh mesh)throws Exception {
		HistogramDataset ds = new HistogramDataset();
		
		int bins = 100;
		
		double max = mesh.getEdges().parallelStream()
			.max( (e1, e2) -> Double.compare(e1.getLog2Ratio(), e2.getLog2Ratio()  ))
			.get()
			.getLog2Ratio();
		
		double min = mesh.getEdges().parallelStream()
				.min( (e1, e2) -> Double.compare(e1.getLog2Ratio(), e2.getLog2Ratio()  ))
				.get()
				.getLog2Ratio();
		
		double[] values = mesh.getEdges().parallelStream()
				.mapToDouble(NucleusMeshEdge::getLog2Ratio)
				.toArray();

		ds.addSeries("mesh result", values, bins, min, max );
		
		return ds;
	}
	
	

	
}
