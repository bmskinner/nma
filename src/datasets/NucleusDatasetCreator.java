package datasets;

import ij.IJ;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import no.analysis.AnalysisDataset;
import no.analysis.CurveRefolder;
import no.collections.CellCollection;
import no.components.AnalysisOptions;
import no.components.AnalysisOptions.CannyOptions;
import no.components.AnalysisOptions.NuclearSignalOptions;
import no.components.NuclearSignal;
import no.components.NucleusBorderPoint;
import no.components.NucleusBorderSegment;
import no.components.Profile;
import no.components.SegmentedProfile;
import no.components.ShellResult;
import no.components.XYPoint;
import no.nuclei.Nucleus;

import org.apache.commons.math3.stat.inference.MannWhitneyUTest;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import cell.Cell;
import components.Flagellum;
import utility.Constants;
import utility.Equation;
import utility.Utils;

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
    public static DefaultXYDataset createMultiProfileSegmentDataset(List<AnalysisDataset> list, String segName){
        
        DefaultXYDataset ds = new DefaultXYDataset();
        for (int i=0; i < list.size(); i++) {

            CellCollection collection = list.get(i).getCollection();
            
            Profile profile = collection.getProfileCollection().getProfile(collection.getOrientationPoint());
            Profile xpoints = profile.getPositions(100);
            double[][] data = { xpoints.asArray(), profile.asArray() };
            ds.addSeries("Profile_"+i, data);
            
            List<NucleusBorderSegment> segments = collection.getProfileCollection().getSegments(collection.getOrientationPoint());
            List<NucleusBorderSegment> segmentsToAdd = new ArrayList<NucleusBorderSegment>(0);
            
            // add only the segment of interest
            for(NucleusBorderSegment seg : segments){
                if(seg.getName().equals(segName)){
                    segmentsToAdd.add(seg);
                }
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
	 */
	public static DefaultXYDataset createRawMultiProfileSegmentDataset(List<AnalysisDataset> list, String segName, boolean rightAlign){
		DefaultXYDataset ds = new DefaultXYDataset();
		
		double length = getMaximumMedianProfileLength(list);

		for (int i=0; i < list.size(); i++) {
			CellCollection collection = list.get(i).getCollection();

			Profile profile = collection.getProfileCollection().getProfile(collection.getOrientationPoint());
			Profile xpoints = profile.getPositions((int) collection.getMedianArrayLength());
			
			double offset = 0;
			if(rightAlign){
				double differenceToMaxLength = length - collection.getMedianArrayLength();
				offset = differenceToMaxLength;
				xpoints = xpoints.add(differenceToMaxLength);
			}
			double[][] data = { xpoints.asArray(), profile.asArray() };
			ds.addSeries("Profile_"+i, data);
			
			List<NucleusBorderSegment> segments = collection.getProfileCollection().getSegments(collection.getOrientationPoint());
			List<NucleusBorderSegment> segmentsToAdd = new ArrayList<NucleusBorderSegment>(0);
			
			// add only the segment of interest
			for(NucleusBorderSegment seg : segments){
				if(seg.getName().equals(segName)){
					segmentsToAdd.add(seg);
				}
			}
			if(!segmentsToAdd.isEmpty()){
				addSegmentsFromProfile(segmentsToAdd, profile, ds, (int) collection.getMedianArrayLength(), offset);
			}
		}
		return ds;
	}
	
	/**
	 * Make a dataset from the given collection, with each segment profile as a separate series
	 * @param collection the NucleusCollection
	 * @param normalised normalise profile length to 100, or show raw
	 * @return a dataset
	 */
	public static XYDataset createSegmentedProfileDataset(CellCollection collection, boolean normalised, boolean rightAlign){
		DefaultXYDataset ds = new DefaultXYDataset();
		
		int maxLength = (int) getMaximumNucleusProfileLength(collection);
		double offset = 0;
				
		Profile profile = collection.getProfileCollection().getProfile(collection.getOrientationPoint());
		Profile xpoints = null;
		if(normalised){
			xpoints = profile.getPositions(100);
		} else {
			xpoints = profile.getPositions( (int) collection.getMedianArrayLength());
			
			if(rightAlign){
				double differenceToMaxLength = maxLength - collection.getMedianArrayLength();
				offset = differenceToMaxLength;
				xpoints = xpoints.add(differenceToMaxLength);
			}
		}

		// rendering order will be first on top
		
		// add the segments
		List<NucleusBorderSegment> segments = collection.getProfileCollection().getSegments(collection.getOrientationPoint());
		if(normalised){
			addSegmentsFromProfile(segments, profile, ds, 100, 0);
		} else {
			addSegmentsFromProfile(segments, profile, ds, (int) collection.getMedianArrayLength(), offset);
		}

		// make the IQR
		Profile profile25 = collection.getProfileCollection().getProfile(collection.getOrientationPoint()+"25");
		Profile profile75 = collection.getProfileCollection().getProfile(collection.getOrientationPoint()+"75");
		double[][] data25 = { xpoints.asArray(), profile25.asArray() };
		ds.addSeries("Q25", data25);
		double[][] data75 = { xpoints.asArray(), profile75.asArray() };
		ds.addSeries("Q75", data75);

		// add the individual nuclei
		for(Nucleus n : collection.getNuclei()){
			Profile angles  = null;
			Profile x		= null;
			if(normalised){
				angles = n.getAngleProfile(collection.getOrientationPoint()).interpolate((int)collection.getMedianArrayLength());
				x = xpoints;
			} else {
				angles = n.getAngleProfile(collection.getOrientationPoint());
				x = angles.getPositions(n.getLength());
				if(rightAlign){
					double differenceToMaxLength = maxLength - n.getLength();
					x = x.add(differenceToMaxLength);
				}
			}
			double[][] ndata = { x.asArray(), angles.asArray() };
			ds.addSeries("Nucleus_"+n.getImageName()+"-"+n.getNucleusNumber(), ndata);
		}
		return ds;
	}
	
		
	
	/**
	 * Create a dataset for multiple AnalysisDatsets
	 * @param list the datasets
	 * @param normalised is the length normalised to 100 
	 * @param rightAlign is a non-normalised dataset hung to the right
	 * @return
	 */
	public static DefaultXYDataset createMultiProfileDataset(List<AnalysisDataset> list, boolean normalised, boolean rightAlign){
		DefaultXYDataset ds = new DefaultXYDataset();
		
		double length = getMaximumMedianProfileLength(list);

		for(int i=0; i<list.size(); i++){ //AnalysisDataset dataset : list){
			AnalysisDataset dataset = list.get(i);
			CellCollection collection = dataset.getCollection();
			Profile profile = collection.getProfileCollection().getProfile(collection.getOrientationPoint());
			Profile xpoints = null;
			
			if(normalised){	
				xpoints = profile.getPositions(100);
			} else {
				xpoints = profile.getPositions((int) collection.getMedianArrayLength());
			}

			if(rightAlign){
				double differenceToMaxLength = length - dataset.getCollection().getMedianArrayLength();
				xpoints = xpoints.add(differenceToMaxLength);
			}
			
			
			double[][] data = { xpoints.asArray(), profile.asArray() };
			ds.addSeries("Profile_"+i, data);
		}
		return ds;
	}
	
	
	public static DefaultXYDataset createMultiProfileFrankenDataset(List<AnalysisDataset> list){
		DefaultXYDataset ds = new DefaultXYDataset();

		int i=0;
		for(AnalysisDataset dataset : list){
			CellCollection collection = dataset.getCollection();
			Profile profile = collection.getFrankenCollection().getProfile(collection.getOrientationPoint());
			Profile xpoints = profile.getPositions(100);
//			xpoints = xpoints.add(0.5);
			double[][] data = { xpoints.asArray(), profile.asArray() };
			ds.addSeries("Profile_"+i, data);
			i++;
		}
		return ds;
	}
	
	
	/**
	 * Get the IQR for a set of profiles as a dataset
	 * @param list the datasets
	 * @param normalised should the data be normalised or raw length
	 * @param rightAlign should raw data be aligned to the right edge of the plot
	 * @return a dataset
	 */
	public static List<XYSeriesCollection> createMultiProfileIQRDataset(List<AnalysisDataset> list, boolean normalised, boolean rightAlign){

		List<XYSeriesCollection> result = new ArrayList<XYSeriesCollection>(0);
		
		double length = getMaximumMedianProfileLength(list);

		for(int i=0; i<list.size(); i++){ //AnalysisDataset dataset : list){
			AnalysisDataset dataset = list.get(i);
			CellCollection collection = dataset.getCollection();
			Profile profile = collection.getProfileCollection().getProfile(collection.getOrientationPoint());
			
			Profile xpoints = null;
			if(normalised){
				xpoints = profile.getPositions(100);
				
			} else {
				xpoints = profile.getPositions( (int) collection.getMedianArrayLength());
			}
			
			if(rightAlign){
				double differenceToMaxLength = length - dataset.getCollection().getMedianArrayLength();
				xpoints = xpoints.add(differenceToMaxLength);
			}

			// rendering order will be first on top

			// make the IQR
			Profile profile25 = collection.getProfileCollection().getProfile(collection.getOrientationPoint()+"25");
			Profile profile75 = collection.getProfileCollection().getProfile(collection.getOrientationPoint()+"75");
			
			XYSeries series25 = new XYSeries("Q25_"+i);
			for(int j=0; j<profile25.size();j++){
				series25.add(xpoints.get(j), profile25.get(j));
			}
			
			XYSeries series75 = new XYSeries("Q75_"+i);
			for(int j=0; j<profile75.size();j++){
				series75.add(xpoints.get(j), profile75.get(j));
			}
			
			XYSeriesCollection xsc = new XYSeriesCollection();
		    xsc.addSeries(series25);
		    xsc.addSeries(series75);
		    result.add(xsc);
		}
		return result;
	}
	
	
	public static List<XYSeriesCollection> createMultiProfileIQRFrankenDataset(List<AnalysisDataset> list){
		List<XYSeriesCollection> result = new ArrayList<XYSeriesCollection>(0);

		int i=0;
		for(AnalysisDataset dataset : list){
			CellCollection collection = dataset.getCollection();
			Profile profile = collection.getFrankenCollection().getProfile(collection.getOrientationPoint());
			Profile xpoints = profile.getPositions(100);

			// rendering order will be first on top

			// make the IQR
			Profile profile25 = collection.getFrankenCollection().getProfile(collection.getOrientationPoint()+"25");
			Profile profile75 = collection.getFrankenCollection().getProfile(collection.getOrientationPoint()+"75");
			
			XYSeries series25 = new XYSeries("Q25_"+i);
			for(int j=0; j<profile25.size();j++){
				series25.add(xpoints.get(j), profile25.get(j));
			}
			
			XYSeries series75 = new XYSeries("Q75_"+i);
			for(int j=0; j<profile75.size();j++){
				series75.add(xpoints.get(j), profile75.get(j));
			}
			
			XYSeriesCollection xsc = new XYSeriesCollection();
		    xsc.addSeries(series25);
		    xsc.addSeries(series75);
		    result.add(xsc);
		    i++;
		}
		return result;
	}
	
	public static XYDataset createIQRVariabilityDataset(List<AnalysisDataset> list){

		if(list.size()==1){
			CellCollection collection = list.get(0).getCollection();
			Profile profile = collection.getProfileCollection().getIQRProfile(collection.getOrientationPoint());
			
			
			List<NucleusBorderSegment> segments = collection.getProfileCollection().getSegments(collection.getOrientationPoint());
			XYDataset ds = addSegmentsFromProfile(segments, profile, new DefaultXYDataset(), 100, 0);	
			return ds;
		} else {
			int i = 0;
			DefaultXYDataset ds = new DefaultXYDataset();
			for(AnalysisDataset dataset : list){
				CellCollection collection = dataset.getCollection();
				Profile profile = collection.getProfileCollection().getIQRProfile(collection.getOrientationPoint());
				Profile xpoints = profile.getPositions(100);
				double[][] data = { xpoints.asArray(), profile.asArray() };
				ds.addSeries("Profile_"+i+"_"+collection.getName(), data);
				i++;
			}
			return ds;
		}
		
	}
		
	public static XYDataset createFrankenSegmentDataset(CellCollection collection){
		DefaultXYDataset ds = new DefaultXYDataset();
		Profile profile = collection.getProfileCollection().getProfile(collection.getOrientationPoint());
		Profile xpoints = profile.getPositions(100);
		
		// rendering order will be first on top
		
		// add the segments
		List<NucleusBorderSegment> segments = collection.getProfileCollection().getSegments(collection.getOrientationPoint());
		addSegmentsFromProfile(segments, profile, ds, 100, 0);

		// make the IQR
		Profile profile25 = collection.getProfileCollection().getProfile(collection.getOrientationPoint()+"25");
		Profile profile75 = collection.getProfileCollection().getProfile(collection.getOrientationPoint()+"75");
		double[][] data25 = { xpoints.asArray(), profile25.asArray() };
		ds.addSeries("Q25", data25);
		double[][] data75 = { xpoints.asArray(), profile75.asArray() };
		ds.addSeries("Q75", data75);

		// add the individual nuclei
		for(Nucleus n : collection.getNuclei()){
			Profile angles = n.getAngleProfile(collection.getOrientationPoint()).interpolate(profile.size());
			double[][] ndata = { xpoints.asArray(), angles.asArray() };
			ds.addSeries("Nucleus_"+n.getImageName()+"-"+n.getNucleusNumber(), ndata);
		}
		return ds;
	}
	
	/**
	 * Create a segmented dataset for an individual nucleus.
	 * @param nucleus the nucleus to draw
	 * @return
	 */
	public static XYDataset createSegmentedProfileDataset(Nucleus nucleus){
		DefaultXYDataset ds = new DefaultXYDataset();
		
		SegmentedProfile profile = nucleus.getAngleProfile(Constants.Nucleus.RODENT_SPERM.referencePoint());
		Profile xpoints = profile.getPositions(nucleus.getLength());
		
		// rendering order will be first on top
		
		// add the segments
		List<NucleusBorderSegment> segments = profile.getSegments();
		addSegmentsFromProfile(segments, profile, ds, nucleus.getLength(), 0);
		
		double[][] ndata = { xpoints.asArray(), profile.asArray() };
		ds.addSeries("Nucleus_"+nucleus.getImageName()+"-"+nucleus.getNucleusNumber(), ndata);
		
		return ds;
	}

	
	
	
	
	public static BoxAndWhiskerCategoryDataset createAreaBoxplotDataset(List<AnalysisDataset> collections) {
                
		DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();

		for (int i=0; i < collections.size(); i++) {
			CellCollection c = collections.get(i).getCollection();

			List<Double> list = new ArrayList<Double>();

			for (double d : c.getAreas()) {
				list.add(new Double(d));
			}
			dataset.add(list, c.getType()+"_"+i, "Area");
		}

		return dataset;
	}
	
	public static BoxAndWhiskerCategoryDataset createPerimBoxplotDataset(List<AnalysisDataset> collections) {

		DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();

		for (int i=0; i < collections.size(); i++) {
			CellCollection c = collections.get(i).getCollection();

			List<Double> list = new ArrayList<Double>();

			
			for (double d : c.getPerimeters()) {
				list.add(new Double(d));
			}
			dataset.add(list, c.getType()+"_"+i, "Perimeter");
		}

		return dataset;
	}
	
	public static BoxAndWhiskerCategoryDataset createMinFeretBoxplotDataset(List<AnalysisDataset> collections) {

		DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();

		for (int i=0; i < collections.size(); i++) {
			CellCollection c = collections.get(i).getCollection();

			List<Double> list = new ArrayList<Double>();

			for (double d : c.getMinFerets()) {
				list.add(new Double(d));
			}
			dataset.add(list, c.getType()+"_"+i, "Min feret");
		}

		return dataset;
	}
	
	public static BoxAndWhiskerCategoryDataset createMaxFeretBoxplotDataset(List<AnalysisDataset> collections) {

		DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();

		for (int i=0; i < collections.size(); i++) {
			CellCollection c = collections.get(i).getCollection();

			List<Double> list = new ArrayList<Double>();

			for (double d : c.getFerets()) {
				list.add(new Double(d));
			}
			dataset.add(list, c.getType()+"_"+i, "Max feret");
		}

		return dataset;
	}

	public static BoxAndWhiskerCategoryDataset createDifferenceBoxplotDataset(List<AnalysisDataset> collections) {

		DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();

		for (int i=0; i < collections.size(); i++) {
			CellCollection c = collections.get(i).getCollection();

			List<Double> list = new ArrayList<Double>();

			for (double d : c.getNormalisedDifferencesToMedianFromPoint(c.getOrientationPoint())) {
				list.add(new Double(d));
			}
			dataset.add(list, c.getType()+"_"+i, "Perimeter-normalised difference to median");
		}

		return dataset;
	}
	
	/**
	 * Get the lengths of the given segment in the collections
	 * @param collections
	 * @param segName
	 * @return
	 */
	public static BoxAndWhiskerCategoryDataset createSegmentLengthDataset(List<AnalysisDataset> collections, String segName) {

		DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();

		for (int i=0; i < collections.size(); i++) {

			CellCollection collection = collections.get(i).getCollection();


			List<Integer> list = new ArrayList<Integer>(0);

			for(Nucleus n : collection.getNuclei()){
				NucleusBorderSegment seg = n.getAngleProfile().getSegment(segName);
				list.add(seg.length());
			}

			dataset.add(list, segName+"_"+i, "Segment length: "+segName);
		}
		return dataset;
	}
	
	/**
	 * Get the variability of each segment in terms of length difference to the median
	 * profile segment
	 * @param datasets
	 * @return
	 */
	public static BoxAndWhiskerCategoryDataset createSegmentVariabillityDataset(List<AnalysisDataset> datasets) {

		DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();

		for (int i=0; i < datasets.size(); i++) {

			CellCollection collection = datasets.get(i).getCollection();

			List<NucleusBorderSegment> segments = collection.getProfileCollection().getSegments(collection.getOrientationPoint());
			
			for(NucleusBorderSegment medianSeg : segments){
				
				int medianSegmentLength = medianSeg.length();
				
				List<Integer> list = new ArrayList<Integer>(0);
				
				for(Nucleus n : collection.getNuclei()){
					NucleusBorderSegment seg = n.getAngleProfile().getSegment(medianSeg.getName());
					
					int differenceToMedian = medianSegmentLength - seg.length();
					list.add(differenceToMedian);
				}
				
				dataset.add(list, medianSeg.getName(), medianSeg.getName());
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
	 * Create an outline of the consensus nucleus, and apply segment colours
	 * @param collection
	 * @return
	 */
	public static XYDataset createNucleusOutline(CellCollection collection){
		DefaultXYDataset ds = new DefaultXYDataset();
		Nucleus n = collection.getConsensusNucleus();
		Profile q25 = collection.getProfileCollection().getProfile(collection.getOrientationPoint()+"25").interpolate(n.getLength());
		Profile q75 = collection.getProfileCollection().getProfile(collection.getOrientationPoint()+"75").interpolate(n.getLength());
		

		// Add lines to show the IQR of the angle profile at each point
		double[] innerIQRX = new double[n.getLength()+1];
		double[] innerIQRY = new double[n.getLength()+1];
		double[] outerIQRX = new double[n.getLength()+1];
		double[] outerIQRY = new double[n.getLength()+1];

		// find the maximum difference between IQRs
		double maxIQR = 0;

		for(int i=0; i<n.getLength(); i++){
			if(q75.get(i) - q25.get(i)>maxIQR){
				maxIQR = q75.get(i) - q25.get(i);
			}
		}

		// get the maximum values from nuclear diameters
		// get the limits  for the plot  	
		double min = Math.min(n.getMinX(), n.getMinY());
		double max = Math.max(n.getMaxX(), n.getMaxY());
		double scale = Math.min(Math.abs(min), Math.abs(max));
		
		Profile iqrRange = q75.subtract(q25);
		Profile scaledRange = iqrRange.divide(iqrRange.getMax()); // iqr as fraction of total variability
		scaledRange = scaledRange.multiply(scale/10); // set to 10% min radius


		// rendering order will be first on top
		
		// add the segments
		List<NucleusBorderSegment> segmentList = n.getAngleProfile().getSegments();
		if(!segmentList.isEmpty()){ // only draw if there are segments
			for(int i=0;i<segmentList.size();i++){

				NucleusBorderSegment seg = n.getAngleProfile().getSegment("Seg_"+i);

				double[] xpoints = new double[seg.length()+1];
				double[] ypoints = new double[seg.length()+1];
				for(int j=0; j<=seg.length();j++){
					int k = Utils.wrapIndex(seg.getStartIndex()+j, n.getLength());
					NucleusBorderPoint p = n.getBorderPoint(k); // get the border points in the segment
					xpoints[j] = p.getX();
					ypoints[j] = p.getY();
				}

				double[][] data = { xpoints, ypoints };
				ds.addSeries("Seg_"+i, data);
			}
		}

		// add the IQR	
		// what we need to do is match the profile positions to the borderpoints
		
		int tailIndex = n.getBorderIndex(collection.getOrientationPoint());

		for(int i=0; i<n.getLength(); i++){

			int index = Utils.wrapIndex(i + tailIndex, n.getLength()); // start from the orientation point

			int prevIndex = Utils.wrapIndex(index-3, n.getLength());
			int nextIndex = Utils.wrapIndex(index+3, n.getLength());

			XYPoint p = n.getBorderPoint( index  );

			
			// decide the angle at which to place the iqr points
			// make a line between points 3 ahead and behind. 
			// get the orthogonal line, running through the XYPoint
			Equation eq = new Equation(n.getPoint( prevIndex  ), n.getPoint( nextIndex  ));
			// move the line to the index point, and find the orthogonal line
			Equation perp = eq.translate(p).getPerpendicular(p);
			
			XYPoint aPoint = perp.getPointOnLine(p, (0-scaledRange.get(i)));
			XYPoint bPoint = perp.getPointOnLine(p, scaledRange.get(i));

			XYPoint innerPoint = Utils.createPolygon(n).contains(  (float) aPoint.getX(), (float) aPoint.getY() ) ? aPoint : bPoint;
			XYPoint outerPoint = Utils.createPolygon(n).contains(  (float) bPoint.getX(), (float) bPoint.getY() ) ? aPoint : bPoint;

			innerIQRX[i] = innerPoint.getX();
			innerIQRY[i] = innerPoint.getY();
			outerIQRX[i] = outerPoint.getX();
			outerIQRY[i] = outerPoint.getY();

		}
		innerIQRX[n.getLength()] = innerIQRX[0];
		innerIQRY[n.getLength()] = innerIQRY[0];
		outerIQRX[n.getLength()] = outerIQRX[0];
		outerIQRY[n.getLength()] = outerIQRY[0];
		
		double[][] inner = { innerIQRX, innerIQRY };
		ds.addSeries("Q25", inner);
		double[][] outer = { outerIQRX, outerIQRY };
		ds.addSeries("Q75", outer);
		return ds;
	}
	
	/**
	 * Get the outline for a specific nucleus in a dataset. Sets the position
	 * to the original coordinates in the image 
	 * @param cell
	 * @return
	 */
	public static XYDataset createNucleusOutline(Cell cell){
		DefaultXYDataset ds = new DefaultXYDataset();
		
		Nucleus nucleus = cell.getNucleus();
		
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
		return ds;
	}
	
	/**
	 * Get the segmented outline for a specific nucleus in a dataset. Sets the position
	 * to the original coordinates in the image 
	 * @param cell the cell to draw
	 * @return
	 */
	public static XYDataset createSegmentedNucleusOutline(Cell cell){
		DefaultXYDataset ds = new DefaultXYDataset();
		
		Nucleus nucleus = cell.getNucleus();
		
		// add the segments
		List<NucleusBorderSegment> segmentList = nucleus.getAngleProfile().getSegments();
		if(!segmentList.isEmpty()){ // only draw if there are segments
			for(int i=0;i<segmentList.size();i++){

				NucleusBorderSegment seg = nucleus.getAngleProfile().getSegment("Seg_"+i);
				
				double[] xpoints = new double[seg.length()+1];
				double[] ypoints = new double[seg.length()+1];
				for(int j=0; j<=seg.length();j++){
					int k = Utils.wrapIndex(seg.getStartIndex()+j, nucleus.getLength());
					NucleusBorderPoint p = nucleus.getBorderPoint(k); // get the border points in the segment
					xpoints[j] = p.getX();
					ypoints[j] = p.getY();
				}

				double[][] data = { xpoints, ypoints };
				ds.addSeries("Seg_"+i, data);
			}
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
						xpoints[i] = p.getX();
						ypoints[i] = p.getY();
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
	
	

	public static XYPoint getXYCoordinatesForSignal(NuclearSignal n, Nucleus outline){
		double angle = n.getAngle();

		double fractionalDistance = n.getFractionalDistanceFromCoM();

		// determine the total distance to the border at this angle
		double distanceToBorder = CurveRefolder.getDistanceFromAngle(angle, outline);

		// convert to fractional distance to signal
		double signalDistance = distanceToBorder * fractionalDistance;

		// adjust X and Y because we are now counting angles from the vertical axis
		double signalX = Utils.getXComponentOfAngle(signalDistance, angle-90);
		double signalY = Utils.getYComponentOfAngle(signalDistance, angle-90);
		return new XYPoint(signalX, signalY);
	}
	
	public static XYDataset createSignalCoMDataset(AnalysisDataset dataset){
		DefaultXYDataset ds = new DefaultXYDataset();
		CellCollection collection = dataset.getCollection();

		if(collection.hasSignals()){

			for(int group : collection.getSignalGroups()){

				if(dataset.isSignalGroupVisible(group)){

					double[] xpoints = new double[collection.getSignals(group).size()];
					double[] ypoints = new double[collection.getSignals(group).size()];

					int signalCount = 0;
					for(NuclearSignal n : collection.getSignals(group)){

						XYPoint p = getXYCoordinatesForSignal(n, collection.getConsensusNucleus());

						//					IJ.log("A: "+angle+"  D: "+signalDistance+"  X: "+signalX+"  Y: "+signalY);
						xpoints[signalCount] = p.getX();
						ypoints[signalCount] = p.getY();
						signalCount++;

					}
					double[][] data = { xpoints, ypoints };
					ds.addSeries("Group_"+group, data);
				}
			}
		}
		return ds;
	}
	
	public static List<Shape> createSignalRadiusDataset(AnalysisDataset dataset, int signalGroup){

		CellCollection collection = dataset.getCollection();
		List<Shape> result = new ArrayList<Shape>(0);
		
		if(dataset.isSignalGroupVisible(signalGroup)){
			if(collection.hasSignals(signalGroup)){

				for(NuclearSignal n : collection.getSignals(signalGroup)){
					XYPoint p = getXYCoordinatesForSignal(n, collection.getConsensusNucleus());

					// ellipses are drawn starting from x y at upper left. Provide an offset from the centre
					double offset = n.getRadius(); 

					result.add(new Ellipse2D.Double(p.getX()-offset, p.getY()-offset, n.getRadius()*2, n.getRadius()*2));
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
	 */
	public static TableModel createSignalStatsTable(List<AnalysisDataset> list){

		DefaultTableModel model = new DefaultTableModel();
		
		List<Object> fieldNames = new ArrayList<Object>(0);
				
		// find the collection with the most channels
		// this defines  the number of rows

		if(list==null){
			model.addColumn("No data loaded");
			
		} else {
			
			int maxChannels = 0;
			for(AnalysisDataset dataset : list){
				CellCollection collection = dataset.getCollection();
				maxChannels = Math.max(collection.getHighestSignalGroup(), maxChannels);
			}
			if(maxChannels>0){
				// create the row names
				fieldNames.add("Number of signal groups");
				
				for(int i=0;i<maxChannels;i++){
					fieldNames.add("");
					fieldNames.add("Signal group");
					fieldNames.add("Group name");
					fieldNames.add("Channel");
					fieldNames.add("Source");
					fieldNames.add("Signals");
					fieldNames.add("Signals per nucleus");
					fieldNames.add("Median area");
					fieldNames.add("Median angle");
					fieldNames.add("Median feret");
					fieldNames.add("Median distance from CoM");
				}
				
				int numberOfRowsPerSignalGroup = fieldNames.size()/(maxChannels+1);
				model.addColumn("", fieldNames.toArray(new Object[0])); // separate row block for each channel
				
	//			IJ.log("Added headers");
					
				// format the numbers and make into a tablemodel
				DecimalFormat df = new DecimalFormat("#0.00"); 
	
				// make a new column for each collection
				for(AnalysisDataset dataset : list){
					CellCollection collection = dataset.getCollection();
					
	//				IJ.log("Adding collection");
					List<Object> rowData = new ArrayList<Object>(0);
					rowData.add(collection.getSignalGroups().size());
	
					for(int signalGroup : collection.getSignalGroups()){
						if(collection.getSignalCount(signalGroup)>0){
							rowData.add("");
							rowData.add(signalGroup);
							rowData.add(collection.getSignalGroupName(signalGroup));
							rowData.add(collection.getSignalChannel(signalGroup));
							rowData.add(collection.getSignalSourceFolder(signalGroup));
							rowData.add(collection.getSignalCount(signalGroup));
							double signalPerNucleus = (double) collection.getSignalCount(signalGroup)/  (double) collection.getCellsWithNuclearSignals(signalGroup, true).size();
							rowData.add(df.format(signalPerNucleus));
							rowData.add(df.format(collection.getMedianSignalArea(signalGroup)));
							rowData.add(df.format(collection.getMedianSignalAngle(signalGroup)));
							rowData.add(df.format(collection.getMedianSignalFeret(signalGroup)));
							rowData.add(df.format(collection.getMedianSignalDistance(signalGroup)));
						} else {
							
							for(int i = 0; i<numberOfRowsPerSignalGroup;i++){
								rowData.add("");
							}
						}
					}
					model.addColumn(collection.getName(), rowData.toArray(new Object[0])); // separate row block for each channel
				}
			} else {
				model.addColumn("No data loaded");
			}
		}
//		IJ.log("Created model");
		return model;	
	}
	
	/**
	 * Create a table of signal stats for the given list of datasets. This table
	 * covers analysis parameters for the signals
	 * @param list the AnalysisDatasets to include
	 * @return a table model
	 */
	public static TableModel createSignalDetectionParametersTable(List<AnalysisDataset> list){

		DefaultTableModel model = new DefaultTableModel();
		
		List<Object> fieldNames = new ArrayList<Object>(0);
				
		// find the collection with the most channels
		// this defines  the number of rows

		if(list==null){
			model.addColumn("No data loaded");
			
		} else {
			
			int maxChannels = 0;
			for(AnalysisDataset dataset : list){
				CellCollection collection = dataset.getCollection();
				maxChannels = Math.max(collection.getHighestSignalGroup(), maxChannels);
			}
			if(maxChannels>0){
			
				// create the row names
				fieldNames.add("Number of signal groups");
				
				for(int i=0;i<maxChannels;i++){
					fieldNames.add("");
					fieldNames.add("Signal group");
					fieldNames.add("Group name");
					fieldNames.add("Channel");
					fieldNames.add("Source");
					fieldNames.add("Threshold");
					fieldNames.add("Min size");
					fieldNames.add("Max fraction");
					fieldNames.add("Min circ");
					fieldNames.add("Max circ");
					fieldNames.add("Reverse threshold");
				}
				
				int numberOfRowsPerSignalGroup = fieldNames.size()/ (maxChannels+1);
				model.addColumn("", fieldNames.toArray(new Object[0])); // separate row block for each channel
				
	//			IJ.log("Added headers");
					
				// format the numbers and make into a tablemodel
				DecimalFormat df = new DecimalFormat("#0.00"); 
	
				// make a new column for each collection
				for(AnalysisDataset dataset : list){
					CellCollection collection = dataset.getCollection();
					
	//				IJ.log("Adding collection");
					List<Object> rowData = new ArrayList<Object>(0);
					rowData.add(collection.getSignalGroups().size());
	
					for(int signalGroup : collection.getSignalGroups()){
						
						NuclearSignalOptions ns = dataset.getAnalysisOptions()
														.getNuclearSignalOptions(collection.getSignalGroupName(signalGroup));
						
						// TODO separate options for red and green channels from start
						if(ns==null){
							ns = dataset.getAnalysisOptions()
									.getNuclearSignalOptions("default");
						}
						
//						if(collection.getSignalCount(signalGroup)>0){
							rowData.add("");
							rowData.add(signalGroup);
							rowData.add(collection.getSignalGroupName(signalGroup));
							rowData.add(collection.getSignalChannel(signalGroup));
							rowData.add(collection.getSignalSourceFolder(signalGroup));
							rowData.add(  ns.isReverseThreshold() ? "Variable" : ns.getSignalThreshold());
							rowData.add(ns.getMinSize());
							rowData.add(df.format(ns.getMaxFraction()));
							rowData.add(df.format(ns.getMinCirc()));
							rowData.add(df.format(ns.getMaxCirc()));
							rowData.add(ns.isReverseThreshold());
							
//						} else {
//							
//							for(int i = 0; i<numberOfRowsPerSignalGroup;i++){
//								rowData.add("");
//							}
//						}
					}
					model.addColumn(collection.getName(), rowData.toArray(new Object[0])); // separate row block for each channel
				}
			} else {
				model.addColumn("No data loaded");
			}
		}
//		IJ.log("Created model");
		return model;	
	}
	
	public static HistogramDataset createSignalAngleHistogramDataset(List<AnalysisDataset> list){
		HistogramDataset ds = new HistogramDataset();
		for(AnalysisDataset dataset : list){
			CellCollection collection = dataset.getCollection();
			
			for(int signalGroup : collection.getSignalGroups()){
				
				if(dataset.isSignalGroupVisible(signalGroup)){

					if(collection.hasSignals(signalGroup)){

						List<Double> angles = new ArrayList<Double>(0);

						for(Nucleus n : collection.getNuclei()){
							angles.addAll(n.getSignalCollection().getAngles(signalGroup));
						}
						double[] values = Utils.getdoubleFromDouble(angles.toArray(new Double[0]));
						ds.addSeries("Group_"+signalGroup+"_"+collection.getName(), values, 12);
					}
				}
			}
			
		}
		return ds;
	}
	
	public static HistogramDataset createSignalDistanceHistogramDataset(List<AnalysisDataset> list){
		HistogramDataset ds = new HistogramDataset();
		for(AnalysisDataset dataset : list){
			CellCollection collection = dataset.getCollection();

			for(int signalGroup : collection.getSignalGroups()){
				
				if(dataset.isSignalGroupVisible(signalGroup)){

					if(collection.hasSignals(signalGroup)){

						List<Double> angles = new ArrayList<Double>(0);

						for(Nucleus n : collection.getNuclei()){
							angles.addAll(n.getSignalCollection().getDistances(signalGroup));
						}
						double[] values = Utils.getdoubleFromDouble(angles.toArray(new Double[0]));
						ds.addSeries("Group_"+signalGroup+"_"+collection.getName(), values, 12);
					}
				}
			}

		}
		return ds;
	}
	
	/**
	 * Create a boxplot dataset for signal areas
	 * @param dataset the AnalysisDataset to get signal info from
	 * @return a boxplot dataset
	 */
	public static BoxAndWhiskerCategoryDataset createSignalAreaBoxplotDataset(AnalysisDataset dataset) {

		DefaultBoxAndWhiskerCategoryDataset result = new DefaultBoxAndWhiskerCategoryDataset();

		CellCollection c = dataset.getCollection();
		
		for(int signalGroup : c.getSignalGroups()){
			
			List<Double> list = new ArrayList<Double>();
			for(NuclearSignal s : c.getSignals(signalGroup)){
				
				list.add(s.getArea());
			}
			result.add(list, "Group_"+signalGroup, "Area");
		}
		return result;
	}
	
	public static CategoryDataset createShellBarChartDataset(List<AnalysisDataset> list){
		DefaultStatisticalCategoryDataset ds = new DefaultStatisticalCategoryDataset();
		for(AnalysisDataset dataset : list){
			
			CellCollection collection = dataset.getCollection();

			for(int signalGroup : collection.getSignalGroups()){
				
				if(collection.hasSignals(signalGroup)){
					ShellResult r = dataset.getShellResult(signalGroup);

					for(int shell = 0; shell<r.getNumberOfShells();shell++){
						Double d = r.getMeans().get(shell);
						Double std = r.getStandardErrors().get(shell);
						ds.add(d*100, std.doubleValue()*100, "Group_"+signalGroup+"_"+collection.getName(), String.valueOf(shell)); 
						// we need the string value for shell otherwise we get error
						// "the method addValue(Number, Comparable, Comparable) is ambiguous for the type DefaultCategoryDataset"
						// ditto the doublevalue for std

					}
				}
			}
		}
		return ds;
	}
}
