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

import gui.components.MeasurementUnitSettingsPanel.MeasurementScale;
import gui.components.ProfileAlignmentOptionsPanel.ProfileAlignment;
import ij.IJ;
import ij.process.FloatPolygon;

import java.util.ArrayList;
import java.util.List;

import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import utility.Constants.BorderTag;
import utility.Equation;
import utility.Utils;
import weka.estimators.KernelEstimator;
import analysis.AnalysisDataset;
import components.Cell;
import components.CellCollection;
import components.CellCollection.NucleusStatistic;
import components.CellCollection.ProfileCollectionType;
import components.generic.Profile;
import components.generic.ProfileCollection;
import components.generic.SegmentedProfile;
import components.generic.XYPoint;
import components.nuclear.NuclearSignal;
import components.nuclear.NucleusBorderPoint;
import components.nuclear.NucleusBorderSegment;
import components.nuclei.ConsensusNucleus;
import components.nuclei.Nucleus;

public class NucleusDatasetCreator {
	
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
	private static XYDataset addSegmentsFromProfile(List<NucleusBorderSegment> segments, Profile profile, DefaultXYDataset ds, int length, double offset){
		
		Profile xpoints = profile.getPositions(length);
		xpoints = xpoints.add(offset);
		for(NucleusBorderSegment seg : segments){

			if(seg.wraps()){ // case when array wraps. We need to plot the two ends as separate series
				
				if(seg.getStartIndex()<profile.size()){
										

					// beginning of array
					Profile subProfileA = profile.getSubregion(0, seg.getEndIndex());
					Profile subPointsA  = xpoints.getSubregion(0, seg.getEndIndex());

					double[][] dataA = { subPointsA.asArray(), subProfileA.asArray() };
					ds.addSeries(seg.getName()+"_A", dataA);

					// end of array
					Profile subProfileB = profile.getSubregion(seg.getStartIndex(), profile.size()-1);
					Profile subPointsB  = xpoints.getSubregion(seg.getStartIndex(), profile.size()-1);
					
					double[][] dataB = { subPointsB.asArray(), subProfileB.asArray() };
					ds.addSeries(seg.getName()+"_B", dataB);
					
					continue; // move on to the next segment
					
				} else { // there is an error in the segment assignment; skip and warn
					IJ.log("Profile skipping issue: "+seg.getName()+" : "+seg.getStartIndex()+" - "+seg.getEndIndex()+" in total of "+profile.size());
				}
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
            
            Profile profile = collection.getProfileCollection(ProfileCollectionType.REGULAR).getProfile(BorderTag.ORIENTATION_POINT, 50);
            Profile xpoints = profile.getPositions(100);
            double[][] data = { xpoints.asArray(), profile.asArray() };
            ds.addSeries("Profile_"+i, data);
            
            List<NucleusBorderSegment> segments = collection.getProfileCollection(ProfileCollectionType.REGULAR).getSegments(BorderTag.ORIENTATION_POINT);
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
			length = n.getLength() > length ? n.getLength() : length;
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

			Profile profile = collection.getProfileCollection(ProfileCollectionType.REGULAR).getProfile(BorderTag.ORIENTATION_POINT, 50);
			Profile xpoints = profile.getPositions((int) collection.getMedianArrayLength());
			
			double offset = 0;
			if(alignment.equals(ProfileAlignment.RIGHT)){
				double differenceToMaxLength = length - collection.getMedianArrayLength();
				offset = differenceToMaxLength;
				xpoints = xpoints.add(differenceToMaxLength);
			}
			double[][] data = { xpoints.asArray(), profile.asArray() };
			ds.addSeries("Profile_"+i, data);
			
			List<NucleusBorderSegment> segments = collection.getProfileCollection(ProfileCollectionType.REGULAR).getSegments(BorderTag.ORIENTATION_POINT);
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
	 * Make a dataset from the given collection, with each segment profile as a separate series
	 * @param dataset 
	 * @param normalised normalise profile length to 100, or show raw
	 * @return a dataset
	 * @throws Exception 
	 */
	public static XYDataset createSegmentedMedianProfileDataset(AnalysisDataset dataset, boolean normalised, ProfileAlignment alignment, BorderTag point) throws Exception{
		
		CellCollection collection = dataset.getCollection();
		DefaultXYDataset ds = new DefaultXYDataset();
		
//		String point = collection.getOrientationPoint();
		
		int maxLength = (int) getMaximumNucleusProfileLength(collection);
		int medianProfileLength = (int) collection.getMedianArrayLength();
		double offset = 0;
				
		Profile profile = collection.getProfileCollection(ProfileCollectionType.REGULAR).getProfile(point, 50);
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
		List<NucleusBorderSegment> segments = collection.getProfileCollection(ProfileCollectionType.REGULAR).getSegments(point);
		if(normalised){
			addSegmentsFromProfile(segments, profile, ds, 100, 0);
		} else {
			addSegmentsFromProfile(segments, profile, ds, (int) collection.getMedianArrayLength(), offset);
		}

		return ds;
	}
	
	/**
	 * Make a dataset from the given collection, with each segment profile as a separate series
	 * @param collection the NucleusCollection
	 * @param normalised normalise profile length to 100, or show raw
	 * @return a dataset
	 * @throws Exception 
	 */
	public static XYDataset createSegmentedProfileDataset(CellCollection collection, boolean normalised, ProfileAlignment alignment, BorderTag point) throws Exception{
		DefaultXYDataset ds = new DefaultXYDataset();
		
//		String point = collection.getOrientationPoint();
		
		int maxLength = (int) getMaximumNucleusProfileLength(collection);
		int medianProfileLength = (int) collection.getMedianArrayLength();
		double offset = 0;
				
		Profile profile = collection.getProfileCollection(ProfileCollectionType.REGULAR).getProfile(point, 50);
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
		List<NucleusBorderSegment> segments = collection.getProfileCollection(ProfileCollectionType.REGULAR).getSegments(point);
		if(normalised){
			addSegmentsFromProfile(segments, profile, ds, 100, 0);
		} else {
			addSegmentsFromProfile(segments, profile, ds, (int) collection.getMedianArrayLength(), offset);
		}

		// make the IQR
		Profile profile25 = collection.getProfileCollection(ProfileCollectionType.REGULAR).getProfile(point, 25);
		Profile profile75 = collection.getProfileCollection(ProfileCollectionType.REGULAR).getProfile(point, 75);
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
				angles = n.getAngleProfile(point).interpolate(length);
				x = xpoints;
			} else {
				angles = n.getAngleProfile(point);
				x = angles.getPositions(n.getLength());
				if(alignment.equals(ProfileAlignment.RIGHT)){
					double differenceToMaxLength = maxLength - n.getLength();
					x = x.add(differenceToMaxLength);
				}
			}
			double[][] ndata = { x.asArray(), angles.asArray() };
			try{
			ds.addSeries("Nucleus_"+n.getImageName()+"-"+n.getNucleusNumber(), ndata);
			} catch(Exception e){
				IJ.log("Error forming data series:"+e.getMessage());
				IJ.log("Angles:"+angles.size());
				IJ.log("xpoints:"+xpoints.size());
				IJ.log("x:"+x.size());
				
				for(StackTraceElement e1 : e.getStackTrace()){
					IJ.log(e1.toString());
				}
			}
		}
		return ds;
	}
	
		
	
	/**
	 * Create a dataset for multiple AnalysisDatsets
	 * @param list the datasets
	 * @param normalised is the length normalised to 100 
	 * @param rightAlign is a non-normalised dataset hung to the right
	 * @return
	 * @throws Exception 
	 */
	public static DefaultXYDataset createMultiProfileDataset(List<AnalysisDataset> list, boolean normalised, ProfileAlignment alignment, BorderTag borderTag) throws Exception{
		DefaultXYDataset ds = new DefaultXYDataset();
		
		double length = getMaximumMedianProfileLength(list);

		for(int i=0; i<list.size(); i++){ //AnalysisDataset dataset : list){
			AnalysisDataset dataset = list.get(i);
			CellCollection collection = dataset.getCollection();
			Profile profile = collection.getProfileCollection(ProfileCollectionType.REGULAR).getProfile(borderTag, 50);
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
	
	
	public static DefaultXYDataset createMultiProfileFrankenDataset(List<AnalysisDataset> list, boolean normalised, ProfileAlignment alignment, BorderTag borderTag) throws Exception{
		DefaultXYDataset ds = new DefaultXYDataset();

		int i=0;
		for(AnalysisDataset dataset : list){
			CellCollection collection = dataset.getCollection();
			Profile profile = collection.getProfileCollection(ProfileCollectionType.FRANKEN).getProfile(borderTag, 50);
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
	
	
	/**
	 * Get the IQR for a set of profiles as a dataset
	 * @param list the datasets
	 * @param normalised should the data be normalised or raw length
	 * @param rightAlign should raw data be aligned to the right edge of the plot
	 * @return a dataset
	 * @throws Exception 
	 */
	public static List<XYSeriesCollection> createMultiProfileIQRDataset(List<AnalysisDataset> list, boolean normalised, ProfileAlignment alignment, BorderTag borderTag) throws Exception{

		List<XYSeriesCollection> result = new ArrayList<XYSeriesCollection>(0);
		
		double length = getMaximumMedianProfileLength(list);

		for(int i=0; i<list.size(); i++){ //AnalysisDataset dataset : list){
			AnalysisDataset dataset = list.get(i);
			CellCollection collection = dataset.getCollection();
			
			XYSeriesCollection xsc = addMultiProfileIQRSeries(collection.getProfileCollection(ProfileCollectionType.REGULAR), 
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
			
			XYSeriesCollection xsc = addMultiProfileIQRSeries(collection.getProfileCollection(ProfileCollectionType.FRANKEN), 
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
	
	public static XYDataset createIQRVariabilityDataset(List<AnalysisDataset> list, BorderTag borderTag, ProfileCollectionType type) throws Exception{

		
		if(list.size()==1){
			CellCollection collection = list.get(0).getCollection();

			Profile profile = collection.getProfileCollection(type).getIQRProfile(borderTag);
			
			
			List<NucleusBorderSegment> segments = collection.getProfileCollection(type).getSegments(borderTag);
			XYDataset ds = addSegmentsFromProfile(segments, profile, new DefaultXYDataset(), 100, 0);	
			return ds;
		} else {
			int i = 0;
			DefaultXYDataset ds = new DefaultXYDataset();
			for(AnalysisDataset dataset : list){
				CellCollection collection = dataset.getCollection();

				Profile profile = collection.getProfileCollection(type).getIQRProfile(borderTag);
				Profile xpoints = profile.getPositions(100);
				double[][] data = { xpoints.asArray(), profile.asArray() };
				ds.addSeries("Profile_"+i+"_"+collection.getName(), data);
				i++;
			}
			return ds;
		}
		
	}
		
	public static XYDataset createFrankenSegmentDataset(CellCollection collection, boolean normalised, ProfileAlignment alignment, BorderTag point) throws Exception{
		DefaultXYDataset ds = new DefaultXYDataset();
		
//		String pointType = collection.getOrientationPoint();
		Profile profile = collection.getProfileCollection(ProfileCollectionType.FRANKEN).getProfile(point, 50);
		Profile xpoints = profile.getPositions(100);
		
		// rendering order will be first on top
		
		// add the segments (these are the same as in the regular profile collection)
		List<NucleusBorderSegment> segments = collection.getProfileCollection(ProfileCollectionType.REGULAR).getSegments(point);
		addSegmentsFromProfile(segments, profile, ds, 100, 0);

		// make the IQR
		Profile profile25 = collection.getProfileCollection(ProfileCollectionType.FRANKEN).getProfile(point, 25);
		Profile profile75 = collection.getProfileCollection(ProfileCollectionType.FRANKEN).getProfile(point, 75);
		double[][] data25 = { xpoints.asArray(), profile25.asArray() };
		ds.addSeries("Q25", data25);
		double[][] data75 = { xpoints.asArray(), profile75.asArray() };
		ds.addSeries("Q75", data75);

		// add the individual nuclei
		int profileCount = 0;
		for(Profile angles : collection.getProfileCollection(ProfileCollectionType.FRANKEN).getNucleusProfiles(point)){

			double[][] ndata = { xpoints.asArray(), angles.asArray() };
			ds.addSeries("Nucleus_"+profileCount, ndata);
			profileCount++;
		}
		return ds;
	}
	
	/**
	 * Create a segmented dataset for an individual nucleus.
	 * @param nucleus the nucleus to draw
	 * @return
	 * @throws Exception 
	 */
	public static XYDataset createSegmentedProfileDataset(Nucleus nucleus) throws Exception{
		DefaultXYDataset ds = new DefaultXYDataset();
		
		SegmentedProfile profile = nucleus.getAngleProfile(BorderTag.REFERENCE_POINT);
		Profile xpoints = profile.getPositions(nucleus.getLength());
		
		// rendering order will be first on top
		
		// add the segments
		List<NucleusBorderSegment> segments = profile.getSegments();
		addSegmentsFromProfile(segments, profile, ds, nucleus.getLength(), 0);
		
		double[][] ndata = { xpoints.asArray(), profile.asArray() };
		ds.addSeries("Nucleus_"+nucleus.getImageName()+"-"+nucleus.getNucleusNumber(), ndata);
		
		return ds;
	}

	
	/**
	 * Get a boxplot dataset for the given statistic for each collection
	 * @param collections
	 * @param stat
	 * @param scale
	 * @return
	 * @throws Exception
	 */
	public static BoxAndWhiskerCategoryDataset createBoxplotDataset(List<AnalysisDataset> collections, NucleusStatistic stat, MeasurementScale scale) throws Exception{
        
		DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();

		for (int i=0; i < collections.size(); i++) {
			CellCollection c = collections.get(i).getCollection();

			List<Double> list = new ArrayList<Double>();
			double[] stats = c.getNuclearStatistics(stat, scale);

			for (double d : stats) {
				list.add(new Double(d));
			}
			dataset.add(list, c.getType()+"_"+i, stat.toString());
		}

		return dataset;
	}
		
	/**
	 * Get the lengths of the given segment in the collections
	 * @param collections
	 * @param segName
	 * @return
	 * @throws Exception 
	 */
	public static BoxAndWhiskerCategoryDataset createSegmentLengthDataset(List<AnalysisDataset> collections, String segName, MeasurementScale scale) throws Exception {

		DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();

		for (int i=0; i < collections.size(); i++) {

			CellCollection collection = collections.get(i).getCollection();


			List<Double> list = new ArrayList<Double>(0);

			for(Nucleus n : collection.getNuclei()){
				NucleusBorderSegment seg = n.getAngleProfile().getSegment(segName);
				
				int indexLength = seg.length();
				double proportionPerimeter = (double) indexLength / (double) seg.getTotalLength();
				double length = n.getStatistic(NucleusStatistic.PERIMETER, scale) * proportionPerimeter;
				list.add(length);
			}

			dataset.add(list, segName+"_"+i, segName);
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
		DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();

		for (int i=0; i < datasets.size(); i++) {

			CellCollection collection = datasets.get(i).getCollection();

			List<NucleusBorderSegment> segments = collection.getProfileCollection(ProfileCollectionType.REGULAR).getSegments(BorderTag.ORIENTATION_POINT);
			
			for(NucleusBorderSegment medianSeg : segments){
				
				int medianSegmentLength = medianSeg.length();
				
				List<Integer> list = new ArrayList<Integer>(0);
				
				for(Nucleus n : collection.getNuclei()){
					NucleusBorderSegment seg = n.getAngleProfile().getSegment(medianSeg.getName());
					
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
		
		double[] xpoints = new double[n.getLength()+1];
		double[] ypoints = new double[n.getLength()+1];
		
		for(int i=0; i<n.getLength();i++){
			NucleusBorderPoint p = n.getBorderPoint(i);
			xpoints[i] = p.getX();
			ypoints[i] = p.getY();
		}
		// complete the line
		xpoints[n.getLength()] = xpoints[0];
		ypoints[n.getLength()] = ypoints[0];
		
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
		
		BorderTag pointType = BorderTag.ORIENTATION_POINT;
		
		// get the quartile profiles, beginning from the orientation point
		Profile q25 = collection.getProfileCollection(ProfileCollectionType.REGULAR).getProfile(pointType, 25).interpolate(n.getLength());
		Profile q75 = collection.getProfileCollection(ProfileCollectionType.REGULAR).getProfile(pointType, 75).interpolate(n.getLength());
		
		// get the limits  for the plot  	
		double scale = getScaleForIQRRange(n);
		
		// find the range of the iqr, and scale the values in the iqr profile to 1/10 of the total range of the plot
		//The scaled IQR is a profile beginning from the orientation point
		Profile iqrRange = q75.subtract(q25);
		Profile scaledRange = iqrRange.divide(iqrRange.getMax()); // iqr as fraction of total variability
		scaledRange = scaledRange.multiply(scale/10); // set to 10% min radius of the chart

		
		// Get the angle profile, starting from the tail point
		SegmentedProfile angleProfile = n.getAngleProfile(pointType);
		
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
			for(NucleusBorderSegment seg :  angleProfile.getSegments()){
								
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
					int borderIndex = Utils.wrapIndex(seg.getStartIndex()+j+pointIndex, n.getLength());
					
					NucleusBorderPoint p = n.getBorderPoint(borderIndex); // get the border points in the segment
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
			int index = Utils.wrapIndex(segmentIndex + n.getBorderIndex(pointType), n.getLength());
			
			// get the border point at this index
			NucleusBorderPoint p = n.getBorderPoint(index); // get the border points in the segment
			
//			IJ.log("Selecting border index: "+index+" from "+segment.getName()+" index "+segmentIndex);

			// Find points three indexes ahead and behind to make a triangle from
			int prevIndex = Utils.wrapIndex(index-3, n.getLength());
			int nextIndex = Utils.wrapIndex(index+3, n.getLength());


			
			// decide the angle at which to place the iqr points
			// make a line between points 3 ahead and behind. 
			// get the orthogonal line, running through the XYPoint
			Equation eq = new Equation(n.getPoint( prevIndex  ), n.getPoint( nextIndex  ));
			// move the line to the index point, and find the orthogonal line
			Equation perp = eq.translate(p).getPerpendicular(p);
			
			// Select the index from the scaledRange corresponding to the position in the segment
			// The scaledRange is aligned to the segment already
			XYPoint aPoint = perp.getPointOnLine(p, (0-scaledRange.get(Utils.wrapIndex(segmentIndex, n.getLength() )   )    )    );
			XYPoint bPoint = perp.getPointOnLine(p, scaledRange.get(Utils.wrapIndex(segmentIndex, n.getLength() )));

			// determine which of the points is inside the nucleus and which is outside
			
			FloatPolygon nucleusRoi = Utils.createPolygon(n);
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
		DefaultXYDataset ds = new DefaultXYDataset();

		Nucleus nucleus = cell.getNucleus();

		if(segmented){

			/*
			 * With the ability to merge segments, we cannot be sure that an iterator
			 * based on numbers will work
			 */
			List<NucleusBorderSegment> segmentList = nucleus.getAngleProfile().getSegments();
			if(!segmentList.isEmpty()){ // only draw if there are segments
				
				for(NucleusBorderSegment seg  : segmentList){

					double[] xpoints = new double[seg.length()+1];
					double[] ypoints = new double[seg.length()+1];
					for(int j=0; j<=seg.length();j++){
						int k = Utils.wrapIndex(seg.getStartIndex()+j, nucleus.getLength());
						NucleusBorderPoint p = nucleus.getBorderPoint(k); // get the border points in the segment
						xpoints[j] = p.getX();
						ypoints[j] = p.getY();
					}

					double[][] data = { xpoints, ypoints };
					ds.addSeries(seg.getName(), data);
				}
			}

		} else {
			double[] xpoints = new double[nucleus.getOriginalBorderList().size()];
			double[] ypoints = new double[nucleus.getOriginalBorderList().size()];

			int i =0;
			for(XYPoint p : nucleus.getOriginalBorderList()){
				xpoints[i] = p.getX();
				ypoints[i] = p.getY();
				i++;
			}

			double[][] data = { xpoints, ypoints };
			ds.addSeries("Nucleus Border", data);

		}		
		return ds;
	}
	
	public static XYDataset createNucleusIndexTags(Cell cell) throws Exception {

		DefaultXYDataset ds = new DefaultXYDataset();

		Nucleus nucleus = cell.getNucleus();// draw the index points on the nucleus border
		for(BorderTag tag : nucleus.getBorderTags().keySet()){
			NucleusBorderPoint tagPoint = nucleus.getPoint(tag);
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
			
			DefaultXYDataset groupDataset = new DefaultXYDataset();
			int signalNumber = 0;

			for(NuclearSignal signal : nucleus.getSignals(signalGroup)){
				
				
				
				if(dataset.isSignalGroupVisible(signalGroup)){ // only add the groups that are set to visible

					double[] xpoints = new double[signal.getBorder().size()];
					double[] ypoints = new double[signal.getBorder().size()];

					int i =0;
					for(XYPoint p : signal.getBorder()){
						xpoints[i] = p.getX() - nucleus.getPosition()[Nucleus.X_BASE];
						ypoints[i] = p.getY() - nucleus.getPosition()[Nucleus.Y_BASE];
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

				double[] xpoints = new double[n.getLength()];
				double[] ypoints = new double[n.getLength()];

				int j =0;

				for(NucleusBorderPoint p : n.getBorderList()){
					xpoints[j] = p.getX();
					ypoints[j] = p.getY();
					j++;
				}
				double[][] data = { xpoints, ypoints };
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
	public static XYDataset createModalityProbabililtyDataset(Double xposition, AnalysisDataset dataset, ProfileCollectionType type) throws Exception {

		DefaultXYDataset ds = new DefaultXYDataset();


		CellCollection collection = dataset.getCollection();
		KernelEstimator est = createProfileProbabililtyKernel(xposition, dataset, type);
		
		List<Double> xValues = new ArrayList<Double>();
		List<Double> yValues = new ArrayList<Double>();

		for(double i=0; i<=360; i+=0.1){
			xValues.add(i);
			yValues.add(est.getProbability(i));
		}

		double[][] data = { Utils.getdoubleFromDouble(xValues.toArray(new Double[0])),  
				Utils.getdoubleFromDouble(yValues.toArray(new Double[0])) };
		
		
		ds.addSeries(collection.getName(), data);
			

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
	public static XYDataset createModalityValuesDataset(Double xposition, AnalysisDataset dataset, ProfileCollectionType type) throws Exception {

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
	public static KernelEstimator createProfileProbabililtyKernel(Double xposition, AnalysisDataset dataset, ProfileCollectionType type) throws Exception {
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
	public static KernelEstimator createProbabililtyKernel(double[] values) throws Exception {
		KernelEstimator est = new KernelEstimator(0.001);
		// add the values to a kernel estimator
		// give each value equal weighting
		for(double d : values){
			est.addValue(d, 1);
		}
		return est;
	}
	
}
