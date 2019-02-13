/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.charting.datasets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.HistogramDataset;
//import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import com.bmskinner.ViolinPlots.ExportableBoxAndWhiskerCategoryDataset;
import com.bmskinner.nuclear_morphology.analysis.mesh.Mesh;
import com.bmskinner.nuclear_morphology.analysis.mesh.MeshEdge;
import com.bmskinner.nuclear_morphology.analysis.mesh.MeshVertex;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.DefaultChartOptions;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.BooleanProfile;
import com.bmskinner.nuclear_morphology.components.generic.DoubleEquation;
import com.bmskinner.nuclear_morphology.components.generic.FloatProfile;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.LineEquation;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderPointException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableComponentException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.generic.UnsegmentedProfileException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.Lobe;
import com.bmskinner.nuclear_morphology.components.nuclei.LobedNucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.stats.DipTester;
import com.bmskinner.nuclear_morphology.stats.Stats;

import ij.process.FloatPolygon;
import weka.estimators.KernelEstimator;

public class NucleusDatasetCreator extends AbstractDatasetCreator<ChartOptions> {

    private static final double DEFAULT_PROFILE_LENGTH = 100;

    public NucleusDatasetCreator(ChartOptions options) {
        super(options);
    }
    
    private static IProfile createXPositions(IProfile profile, int newLength){
        float[] result = new float[profile.size()];
        for (int i = 0; i < profile.size(); i++) {
            result[i] = (float) (profile.getFractionOfIndex(i) * newLength);
        }
        return new FloatProfile(result);
    }

//    /**
//     * Add individual segments from a profile to a dataset. Offset them to the
//     * given length
//     * 
//     * @param segments the list of segments to add
//     * @param profile the profile against which to add them
//     * @param ds the dataset the segments are to be added to
//     * @param length the profile length
//     * @param offset an offset to the x position. Used to align plots to the right
//     * @param binSize the size of the ProfileAggregate bins, to adjust the offset of
//     *            the median
//     * @return the updated dataset
//     * @throws ProfileException
//     */
//    private XYDataset addSegmentsFromProfile(List<IBorderSegment> segments, IProfile profile, FloatXYDataset ds,
//            int length, double offset) throws ProfileException {
//
//        IProfile xpoints = createXPositions(profile, length);
//        xpoints = xpoints.add(offset);
//        for (IBorderSegment seg : segments) {
//
//            if (seg.wraps()) { // case when array wraps. We need to plot the two
//                               // ends as separate series
//
//                if (seg.getEndIndex() == 0) {
//                    // no need to make two sections
//                    IProfile subProfile = profile.getSubregion(seg.getStartIndex(), profile.size() - 1);
//                    IProfile subPoints = xpoints.getSubregion(seg.getStartIndex(), profile.size() - 1);
//
//                    float[][] data = { subPoints.toFloatArray(), subProfile.toFloatArray() };
//
//                    // check if the series key is taken
//                    String seriesName = checkSeriesName(ds, seg.getName());
//
//                    ds.addSeries(seriesName, data);
//
//                } else {
//
//                    int lowerIndex = Math.min(seg.getEndIndex(), seg.getStartIndex());
//                    int upperIndex = Math.max(seg.getEndIndex(), seg.getStartIndex());
//
//                    // beginning of array
//                    IProfile subProfileA = profile.getSubregion(0, lowerIndex);
//                    IProfile subPointsA = xpoints.getSubregion(0, lowerIndex);
//
//                    float[][] dataA = { subPointsA.toFloatArray(), subProfileA.toFloatArray() };
//                    ds.addSeries(seg.getName() + "_A", dataA);
//
//                    // end of array
//                    IProfile subProfileB = profile.getSubregion(upperIndex, profile.size() - 1);
//                    IProfile subPointsB = xpoints.getSubregion(upperIndex, profile.size() - 1);
//
//                    float[][] dataB = { subPointsB.toFloatArray(), subProfileB.toFloatArray() };
//                    ds.addSeries(seg.getName() + "_B", dataB);
//                }
//
//                continue; // move on to the next segment
//
//            }
//            IProfile subProfile = profile.getSubregion(seg);
//            IProfile subPoints = xpoints.getSubregion(seg);
//
//            float[][] data = { subPoints.toFloatArray(), subProfile.toFloatArray() };
//
//            // check if the series key is taken
//            String seriesName = checkSeriesName(ds, seg.getName());
//
//            ds.addSeries(seriesName, data);
//        }
//        return ds;
//    }

    /**
     * Create a dataset containing only the given bounds, starting at 0,0. 
     * @param w
     * @param h
     * @return
     */
    public FloatXYDataset createAnnotationRectangleDataset(int w, int h) {
        FloatXYDataset ds = new FloatXYDataset();

        float[] xpoints = { 0, 0, w, w };
        float[] ypoints = { 0, h, 0, h };

        float[][] data = { xpoints, ypoints };
        ds.addSeries("Bounds", data, 0);
        return ds;
    }

    /**
     * Create a line chart dataset for comparing segment lengths. Each
     * normalised profile will be drawn in full, plus the given segment within
     * each profile.
     * 
     * @param list
     *            the datasets to draw
     * @param segName
     *            the segment to add in each dataset
     * @return an XYDataset to plot
     */
//    public FloatXYDataset createMultiProfileSegmentDataset(List<IAnalysisDataset> list, String segName)
//            throws ChartDatasetCreationException {
//
//        FloatXYDataset ds = new FloatXYDataset();
//        for (int i = 0; i < list.size(); i++) {
//
//            ICellCollection collection = list.get(i).getCollection();
//
//            try {
//                IProfile profile = collection.getProfileCollection().getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT,
//                        Stats.MEDIAN);
//
//                IProfile xpoints = createXPositions(profile, 100);
//                float[][] data = { xpoints.toFloatArray(), profile.toFloatArray() };
//                ds.addSeries("Profile_" + i, data);
//
//                List<IBorderSegment> segments = collection.getProfileCollection().getSegments(Tag.REFERENCE_POINT);
//                List<IBorderSegment> segmentsToAdd = new ArrayList<IBorderSegment>(0);
//
//                // add only the segment of interest
//                for (IBorderSegment seg : segments) {
//                    segmentsToAdd.add(seg);
//                }
//
//                if (!segmentsToAdd.isEmpty()) {
//                    addSegmentsFromProfile(segmentsToAdd, profile, ds, 100, 0);
//                }
//
//            } catch (UnavailableBorderTagException | ProfileException | UnavailableProfileTypeException e) {
//                warn("Unable to add median profile from " + collection.getName());
//                fine("Error getting median profile", e);
//            }
//
//        }
//        return ds;
//    }

    /**
     * For offsetting a raw profile to the right. Find the maximum length of
     * median profile in the dataset.
     * 
     * @param list
     *            the datasets to check
     * @return the maximum length
     */
    private double getMaximumMedianProfileLength(final List<IAnalysisDataset> list) {
        double length = 100;
        for (IAnalysisDataset dataset : list) {
            length = dataset.getCollection().getMedianArrayLength() > length
                    ? dataset.getCollection().getMedianArrayLength() : length;
        }
        return length;
    }

    /**
     * Get the maximum nucleus length in a collection
     * 
     * @param list
     * @return
     */
    private double getMaximumNucleusProfileLength(ICellCollection collection) {
        double length = 100;

        for (Nucleus n : collection.getNuclei()) {
            length = n.getBorderLength() > length ? n.getBorderLength() : length;
        }

        return length;
    }
    

    /**
     * Get a boxplot dataset for the given statistic for each collection
     * 
     * @return
     * @throws ChartDatasetCreationException
     */
    public BoxAndWhiskerCategoryDataset createBoxplotDataset() throws ChartDatasetCreationException {
        List<IAnalysisDataset> datasets = options.getDatasets();
        PlottableStatistic stat = options.getStat();
        MeasurementScale scale = options.getScale();
        ExportableBoxAndWhiskerCategoryDataset ds = new ExportableBoxAndWhiskerCategoryDataset();

        for (int i = 0; i < datasets.size(); i++) {
            ICellCollection c = datasets.get(i).getCollection();

            List<Double> list = new ArrayList<Double>();
            double[] stats = c.getRawValues(stat, CellularComponent.NUCLEUS, scale);

            for (double d : stats) {
                list.add(new Double(d));
            }
            ds.add(list, c.getName() + "_" + i, stat.toString());
        }

        return ds;
    }

    /**
     * Create a box and whisker dataset for the desired segment statistic
     * 
     * @return
     * @throws ChartDatasetCreationException
     */
    public BoxAndWhiskerCategoryDataset createSegmentStatDataset() throws ChartDatasetCreationException {

        PlottableStatistic stat = options.getStat();

        if (stat.equals(PlottableStatistic.LENGTH)) {
            return createSegmentLengthDataset(options.getDatasets(), options.getSegPosition(), options.getScale());
        }

        if (stat.equals(PlottableStatistic.DISPLACEMENT)) {
            return createSegmentDisplacementDataset(options.getDatasets(), options.getSegPosition());
        }

        return null;

    }

    /**
     * Get the lengths of the given segment in the collections
     * 
     * @return
     * @throws ChartDatasetCreationException
     */
    public BoxAndWhiskerCategoryDataset createSegmentLengthDataset(List<IAnalysisDataset> collections, int segPosition,
            MeasurementScale scale) throws ChartDatasetCreationException {

        ExportableBoxAndWhiskerCategoryDataset dataset = new ExportableBoxAndWhiskerCategoryDataset();

        for (int i = 0; i < collections.size(); i++) {

            ICellCollection collection = collections.get(i).getCollection();

            IBorderSegment medianSeg;
            try {
                medianSeg = collection.getProfileCollection()
                        .getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN)
                        .getSegmentAt(segPosition);
            } catch (UnavailableBorderTagException | ProfileException | UnavailableProfileTypeException
                    | UnsegmentedProfileException e) {
                fine("Error getting profile from tag", e);
                throw new ChartDatasetCreationException("Unable to get median profile", e);
            }

            List<Double> list = new ArrayList<Double>(0);

            for (Nucleus n : collection.getNuclei()) {
                double length = 0;

                try {

                    IBorderSegment seg = n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT)
                            .getSegment(medianSeg.getID());

                    if (seg != null) {
                        int indexLength = seg.length();
                        double proportionPerimeter = (double) indexLength / (double) seg.getProfileLength();
                        length = n.getStatistic(PlottableStatistic.PERIMETER, scale) * proportionPerimeter;

                    }

                } catch (ProfileException | UnavailableComponentException e) {
                    warn("Cannot get segment length for " + n.getNameAndNumber());
                    fine("Error getting profile", e);

                }

                list.add(length);
            }

            dataset.add(list, IBorderSegment.SEGMENT_PREFIX + segPosition + "_" + i,
                    IBorderSegment.SEGMENT_PREFIX + segPosition);
        }
        return dataset;
    }

    /**
     * Get the displacements of the given segment in the collections
     * 
     * @param collections
     * @param segName
     * @return
     * @throws Exception
     */
    public BoxAndWhiskerCategoryDataset createSegmentDisplacementDataset(List<IAnalysisDataset> collections,
            int segPosition) throws ChartDatasetCreationException {

        ExportableBoxAndWhiskerCategoryDataset dataset = new ExportableBoxAndWhiskerCategoryDataset();

        for (int i = 0; i < collections.size(); i++) {

            ICellCollection collection = collections.get(i).getCollection();

            IBorderSegment medianSeg;
            try {
                medianSeg = collection.getProfileCollection()
                        .getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN)
                        .getSegmentAt(segPosition);
            } catch (UnavailableBorderTagException | ProfileException | UnavailableProfileTypeException
                    | UnsegmentedProfileException e) {
                fine("Error getting profile from tag", e);
                throw new ChartDatasetCreationException("Unable to get median profile", e);
            }

            List<Double> list = new ArrayList<Double>(0);

            for (Nucleus n : collection.getNuclei()) {

                try {
                    ISegmentedProfile profile = n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
                    IBorderSegment seg = profile.getSegment(medianSeg.getID());

                    double displacement = profile.getDisplacement(seg);
                    list.add(displacement);

                } catch (ProfileException | UnavailableComponentException e) {
                    warn("Cannot get segment displacement for " + n.getNameAndNumber());
                    fine("Error getting profile", e);
                }
            }

            dataset.add(list, IBorderSegment.SEGMENT_PREFIX + segPosition + "_" + i,
                    IBorderSegment.SEGMENT_PREFIX + segPosition);
        }
        return dataset;
    }

    /**
     * Get the variability of each segment in terms of length difference to the
     * median profile segment
     * 
     * @param datasets
     * @return
     * @throws Exception
     */
    public BoxAndWhiskerCategoryDataset createSegmentVariabillityDataset(@NonNull List<IAnalysisDataset> datasets)
            throws ChartDatasetCreationException {

        if (datasets == null || datasets.isEmpty()) {
            return null;
        }
        ExportableBoxAndWhiskerCategoryDataset dataset = new ExportableBoxAndWhiskerCategoryDataset();

        for (int i = 0; i < datasets.size(); i++) {

            ICellCollection collection = datasets.get(i).getCollection();

            List<IBorderSegment> segments;
            try {
                segments = collection.getProfileCollection()
                        .getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN)
                        .getOrderedSegments();

                for (IBorderSegment medianSeg : segments) {

                    int medianSegmentLength = medianSeg.length();

                    List<Integer> list = new ArrayList<Integer>(0);

                    for (Nucleus n : collection.getNuclei()) {
                        IBorderSegment seg = n.getProfile(ProfileType.ANGLE).getSegment(medianSeg.getName());

                        int differenceToMedian = 0;
                        // if seg is null, catch before we throw an error
                        if (seg != null) {
                            differenceToMedian = medianSegmentLength - seg.length();
                        }

                        list.add(differenceToMedian);
                    }

                    dataset.add(list, medianSeg.getName(), collection.getName());
                }

            } catch (ProfileException | UnsegmentedProfileException | UnavailableComponentException e) {
                fine("Error getting profile from tag", e);
                throw new ChartDatasetCreationException("Unable to get median profile", e);
            }
        }
        return dataset;
    }

    /**
     * Get the outline of the consensus nucleus. No segmentation, no IQR
     * 
     * @param dataset
     * @return
     */
    public XYDataset createBareNucleusOutline(@NonNull CellularComponent n) throws ChartDatasetCreationException {
        ComponentOutlineDataset ds = new ComponentOutlineDataset();

        double[] xpoints = new double[n.getBorderLength() + 1];
        double[] ypoints = new double[n.getBorderLength() + 1];

        try {
            for (int i = 0; i < n.getBorderLength(); i++) {
                IBorderPoint p = n.getBorderPoint(i);
                xpoints[i] = p.getX();
                ypoints[i] = p.getY();
            }
            // complete the line
            xpoints[n.getBorderLength()] = xpoints[0];
            ypoints[n.getBorderLength()] = ypoints[0];
        } catch (UnavailableBorderPointException e) {
            throw new ChartDatasetCreationException("Unable to get border point", e);
        } // get the border points in the segment

        double[][] data = { xpoints, ypoints };
        ds.addSeries("Outline", data);
        ds.setComponent(0, n);
        return ds;
    }

    /**
     * Get the outline of the consensus nucleus. No segmentation, no IQR
     * 
     * @param dataset
     * @return
     */
    public XYDataset createBareNucleusOutline(@NonNull IAnalysisDataset dataset) throws ChartDatasetCreationException {
        return createBareNucleusOutline(dataset.getCollection().getConsensus());
    }
    
    /**
     * Get the outline of the consensus nucleus with the OP drawn
     * 
     * @param dataset
     * @return
     */
    public XYDataset createAnnotatedNucleusOutline() throws ChartDatasetCreationException {
    	Nucleus consensus = options.firstDataset().getCollection().getConsensus();
    	ComponentOutlineDataset ds = (ComponentOutlineDataset) createBareNucleusOutline(consensus);
    	
    	try {
    		IBorderPoint p = consensus.getBorderPoint(Tag.ORIENTATION_POINT);
			double[] xpoints = new double[1];
	        double[] ypoints = new double[1];
	        
	        xpoints[0] = p.getX();
	        ypoints[0] = p.getY();
	        
	        double[][] data = { xpoints, ypoints };
	        
	        ds.addSeries(Tag.ORIENTATION_POINT, data);
			
		} catch (UnavailableBorderTagException e) {
			throw new ChartDatasetCreationException("Unable to get OP", e);
		}
    	return ds;
    	
    }

    /**
     * Get the scale of the nucleus; the lowest absolute x or y limit
     * 
     * @param n
     * @return
     */
    private double getScaleForIQRRange(@NonNull Nucleus n) {
        // get the maximum values from nuclear diameters
        // get the limits for the plot
        double min = Math.min(n.getMinX(), n.getMinY());
        double max = Math.max(n.getMaxX(), n.getMaxY());
        double scale = Math.min(Math.abs(min), Math.abs(max));
        return scale;
    }
    
    /**
     * Create an outline of the consensus nucleus, and apply segments as
     * separate series
     * 
     * @param collection
     * @return
     * @throws Exception
     */
    public XYDataset createSegmentedNucleusOutline(@NonNull ICellCollection collection) throws ChartDatasetCreationException {
        FloatXYDataset ds = new FloatXYDataset();

        // get the consensus nucleus for the population
        Nucleus n = collection.getConsensus();

        // make the IQR
        IProfile q25;
        IProfile q75;
        try {
            q25 = collection.getProfileCollection().getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.LOWER_QUARTILE);
            q75 = collection.getProfileCollection().getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.UPPER_QUARTILE);
        } catch (UnavailableBorderTagException | ProfileException | UnavailableProfileTypeException e) {
            stack("Error getting upper or lower quartile profile from tag", e);
            throw new ChartDatasetCreationException("Unable to get quartile profile", e);
        }

        // get the limits for the plot
        double scale = getScaleForIQRRange(n);

        // find the range of the iqr, and scale the values in the iqr profile to
        // 1/10 of the total range of the plot
        // The scaled IQR is a profile beginning from the orientation point
        IProfile iqrRange    = q75.subtract(q25);
        IProfile scaledRange = iqrRange.divide(iqrRange.getMax()); // iqr as fraction of total variability
        scaledRange = scaledRange.multiply(scale / 10); // set to 10% min radius of the chart

        try {
        	ISegmentedProfile angleProfile = n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);

        	// At this point, the angle profile and the iqr profile should be in sync
        	// The following set of checks confirms this.
        	if (angleProfile.hasSegments()) { // only draw if there are segments

        		// go through each segment
        		for (IBorderSegment seg : angleProfile.getOrderedSegments()) {

        			addSegmentIQRToConsensus(seg, ds, n, scaledRange, Tag.REFERENCE_POINT);

        			// draw the segment
        			float[] xpoints = new float[seg.length()];
        			float[] ypoints = new float[seg.length()];

        			Iterator<Integer> it = seg.iterator();
        			int i = 0;
        			while(it.hasNext()) {
        				int index = it.next();
        				IBorderPoint p = n.getBorderPoint(index);
        				xpoints[i] = (float) p.getX();
        				ypoints[i++] = (float) p.getY();
        			}
        			float[][] data = { xpoints, ypoints };
        			ds.addSeries(seg.getName(), data, 0);
        		}
        	}

        	if(ds.getSeriesCount()<angleProfile.getSegmentCount())
        		throw new ChartDatasetCreationException("Cannot make segmented nucleus outline: too few series in chart dataset");
        } catch (ProfileException | UnavailableBorderTagException | UnavailableProfileTypeException | UnavailableBorderPointException e) {
        	stack("Error getting nucleus angle profile from " + Tag.REFERENCE_POINT, e);
        	throw new ChartDatasetCreationException("Cannot make segmented nucleus outline", e);
        }
        
        // Add the TV, BV as a series only if the options are in debug mode
        if(GlobalOptions.getInstance().getBoolean(GlobalOptions.IS_DEBUG_INTERFACE_KEY)) {
        	try {
        		IPoint tv = n.getBorderPoint(Tag.TOP_VERTICAL);
        		IPoint bv = n.getBorderPoint(Tag.BOTTOM_VERTICAL);

        		float[] xpoints = { (float) tv.getX(), (float) bv.getX() };
        		float[] ypoints = { (float) tv.getY(), (float) bv.getY() };

        		float[][] data = { xpoints, ypoints };
        		ds.addSeries(TAG_PREFIX, data, ds.getSeriesCount());
        	} catch (UnavailableBorderTagException e) {
        		stack("Error getting border tags", e);
        	}
        }
        
        return ds;
    }

    /**
     * Add the IQR for a segment to the given dataset
     * 
     * @param segment the segment to add
     * @param ds the dataset to add it to
     * @param n the consensus nucleus
     * @param scaledRange the IQR scale profile
     */
    private void addSegmentIQRToConsensus(@NonNull IBorderSegment segment, @NonNull FloatXYDataset ds, @NonNull Nucleus n, @NonNull IProfile scaledRange,
            @NonNull Tag pointType) throws ChartDatasetCreationException {

        // what we need to do is match the profile positions to the borderpoints
        // Add lines to show the IQR of the angle profile at each point

        // arrays to hold the positions for the IQR lines
        int arrayLength = segment.length() + 1;

        float[] innerIQRX = new float[arrayLength];
        float[] innerIQRY = new float[arrayLength];
        float[] outerIQRX = new float[arrayLength];
        float[] outerIQRY = new float[arrayLength];

        // Go through each position in the segment.
        // The zero index of the segmented profile is the pointType selected
        // previously in createSegmentedNucleusOutline()
        // Hence a segment start index of zero is at the pointType

        for (int i = 0; i <= segment.length(); i++) {
        	try {
        		// get the index of this point of the segment in the nucleus border list.
        		// The nucleus border list has an arbitrary zero location, and the
        		// pointType index is given within this
        		// We need to add the index of the pointType to the values within the segment
        		int segmentIndex = segment.getStartIndex() + i;
        		int index = CellularComponent.wrapIndex(segmentIndex + n.getBorderIndex(pointType), n.getBorderLength());

        		// get the border point at this index
        		IBorderPoint p = n.getBorderPoint(index);

                // Find points three indexes ahead and behind to make a triangle
                // from
                int prevIndex = n.wrapIndex(index - 3);
                int nextIndex = n.wrapIndex(index + 3);

                // decide the angle at which to place the iqr points
                // make a line between points 3 ahead and behind.
                // get the orthogonal line, running through the XYPoint
                LineEquation eq = new DoubleEquation(n.getBorderPoint(prevIndex), n.getBorderPoint(nextIndex));
                // move the line to the index point, and find the orthogonal line
                LineEquation perp = eq.translate(p).getPerpendicular(p);

                // Select the index from the scaledRange corresponding to the position in the segment
                // The scaledRange is aligned to the segment already
                IPoint aPoint = perp.getPointOnLine(p,
                        (0 - scaledRange.get(CellularComponent.wrapIndex(segmentIndex, scaledRange.size()))));
                IPoint bPoint = perp.getPointOnLine(p,
                        scaledRange.get(CellularComponent.wrapIndex(segmentIndex, scaledRange.size())));

                // determine which of the points is inside the nucleus and which
                // is outside

                FloatPolygon nucleusRoi = n.toPolygon();
                IPoint innerPoint = nucleusRoi.contains((float) aPoint.getX(), (float) aPoint.getY()) ? aPoint : bPoint;
                IPoint outerPoint = nucleusRoi.contains((float) bPoint.getX(), (float) bPoint.getY()) ? aPoint : bPoint;

                // assign the points
                innerIQRX[i] = (float) innerPoint.getX();
                innerIQRY[i] = (float) innerPoint.getY();
                outerIQRX[i] = (float) outerPoint.getX();
                outerIQRY[i] = (float) outerPoint.getY();

            } catch (UnavailableBorderPointException e) {
                throw new ChartDatasetCreationException("Unable to get border point", e);
            } catch(IllegalArgumentException e){
                throw new ChartDatasetCreationException("Problem with line equation", e);
            } catch (UnavailableBorderTagException e) {
            	throw new ChartDatasetCreationException("Border tag is not present: "+pointType, e);
			}

        }

        float[][] inner = { innerIQRX, innerIQRY };
        ds.addSeries(QUARTILE_SERIES_PREFIX+"25_" + segment.getName(), inner, 0);
        float[][] outer = { outerIQRX, outerIQRY };
        ds.addSeries(QUARTILE_SERIES_PREFIX+"75_" + segment.getName(), outer, 0);
    }

    /**
     * Get the position of a segment in the nucleus angle profile
     * 
     * @param n
     * @param seg
     * @return
     */
//    public int getSegmentPosition(@NonNull Nucleus n, @NonNull IBorderSegment seg) {
//        int result = 0;
//        if (seg.getStartIndex() == n.getBorderIndex(Tag.REFERENCE_POINT)) {
//            return result;
//        }
//		result++;
//		result += getSegmentPosition(n, seg.prevSegment());
//        return result;
//    }

    /**
     * Create a dataset with lines from each of the BorderTags within the
     * nucleus to the centre of mass of the nucleus
     * 
     * @param cell
     * @return
     * @throws Exception
     */
    public XYDataset createNucleusIndexTags(@NonNull Nucleus nucleus) throws ChartDatasetCreationException {

        FloatXYDataset ds = new FloatXYDataset();
        try {
            for (Tag tag : nucleus.getBorderTags().keySet()) {
                IBorderPoint tagPoint;

                int tagIndex = nucleus.getBorderIndex(tag);
                tagPoint = nucleus.getOriginalBorderPoint(tagIndex);

                float[] xpoints = { (float) (tagPoint.getX() - 0.5), (float) (nucleus.getOriginalCentreOfMass().getX() - 0.5) };
                float[] ypoints = { (float) (tagPoint.getY() - 0.5), (float) (nucleus.getOriginalCentreOfMass().getY() - 0.5) };
                float[][] data = { xpoints, ypoints };
                ds.addSeries("Tag_" + tag, data, 0);
            }
        } catch (UnavailableBorderPointException | UnavailableBorderTagException e) {
            throw new ChartDatasetCreationException("Unable to get border point", e);
        }

        return ds;
    }

    /**
     * Create a dataset with lines from each of the BorderTags within the
     * nucleus to the centre of mass of the nucleus
     * 
     * @param cell
     * @return
     * @throws Exception
     */
    public ComponentOutlineDataset createNucleusLobeDataset(@NonNull LobedNucleus nucleus) throws ChartDatasetCreationException {

        ComponentOutlineDataset<CellularComponent> ds = new ComponentOutlineDataset<CellularComponent>();

        int i = 0;
        Iterator<Lobe> lobes = nucleus.getLobes().iterator();
        while (lobes.hasNext()) {
            Lobe l = lobes.next();
            String seriesKey = CellularComponent.NUCLEAR_LOBE + "_" + i;
            finest("Adding lobe to dataset: " + seriesKey);
            OutlineDatasetCreator dc = new OutlineDatasetCreator(options, l);
            dc.addOutline(ds, seriesKey, false);
            i++;
        }
        return ds;
    }

    /**
     * Create a dataset for the signal groups in the cell. Each signalGroup is a
     * new dataset, and each signal in that group is a series
     * 
     * @param cell
     *            the cell to get signals from
     * @return a dataset for charting
     */
    public List<ComponentOutlineDataset> createSignalOutlines(@NonNull ICell cell, @NonNull IAnalysisDataset dataset)
            throws ChartDatasetCreationException {

        List<ComponentOutlineDataset> result = new ArrayList<>();
        List<IAnalysisDataset> datasets = new ArrayList<>();
        datasets.add(dataset);

        if (cell == null) {
            finest("Input cell is null, returning blank signal outline dataset list");
            return result;
        }

        if (dataset == null) {
            finest("Input dataset is null, returning blank signal outline dataset list");
            return result;
        }

        Nucleus nucleus = cell.getNucleus();

        finest("Attempting to create signal outlines for " + nucleus.getNameAndNumber());

        for (UUID signalGroup : nucleus.getSignalCollection().getSignalGroupIds()) {

            if (!nucleus.getSignalCollection().hasSignal(signalGroup)) {
                continue;
            }

            Optional<ISignalGroup> group = dataset.getCollection().getSignalGroup(signalGroup);

			if(!group.isPresent())
			    continue;

			if (group.get().isVisible()) {

			    ComponentOutlineDataset<CellularComponent> groupDataset = new ComponentOutlineDataset<CellularComponent>();
			    int signalNumber = 0;

			    for (INuclearSignal signal : nucleus.getSignalCollection().getSignals(signalGroup)) {

			        String seriesKey = CellularComponent.NUCLEAR_SIGNAL + "_" + signalGroup + "_signal_"
			                + signalNumber;
			        finest("Adding signal to dataset: " + seriesKey);
			        OutlineDatasetCreator dc = new OutlineDatasetCreator(new DefaultChartOptions(datasets), signal);
			        try {
			            dc.addOutline(groupDataset, seriesKey, false);

			        } catch (ChartDatasetCreationException e) {
			            error("Unable to add signal " + seriesKey + " to dataset", e);
			        }
			        signalNumber++;
			    }
			    result.add(groupDataset);

			} else {
			    finest("Not adding " + group + ": not set as visible");
			}

        }

        finest("Made signal outlines for " + result.size() + " signal groups");
        return result;
    }

    /**
     * Given a list of analysis datasets, get the outlines of the consensus
     * nuclei they contain

     * @return a chartable dataset
     */
    public XYDataset createMultiNucleusOutline() throws ChartDatasetCreationException {

        ComponentOutlineDataset ds = new ComponentOutlineDataset();

        List<IAnalysisDataset> list = options.getDatasets();
        MeasurementScale scale = options.getScale();

        int i = 0;
        for (IAnalysisDataset dataset : list) {
            ICellCollection collection = dataset.getCollection();

            String seriesKey = CellularComponent.NUCLEUS + "_" + i + "_" + collection.getName();

            if (collection.hasConsensus()) {
                Nucleus n = collection.getConsensus();

                double[] xpoints = new double[n.getBorderLength()];
                double[] ypoints = new double[n.getBorderLength()];

                int j = 0;

                for (IBorderPoint p : n.getBorderList()) {

                    double x = p.getX();
                    double y = p.getY();

                    if (scale.equals(MeasurementScale.MICRONS)) {
                        x = PlottableStatistic.micronLength(x, n.getScale());
                        y = PlottableStatistic.micronLength(y, n.getScale());
                    }

                    xpoints[j] = x;
                    ypoints[j] = y;
                    j++;
                }

                double[][] data = { xpoints, ypoints };
                ds.addSeries(seriesKey, data);
                ds.setComponent(seriesKey, n);
            } else {
                double[][] data = { { 0 }, { 0 } }; // make an empty series if
                                                    // no consensus
                ds.addSeries(seriesKey, data);
            }
            i++;

        }

        return ds;
    }

    /**
     * Create a charting dataset for the angles within the AnalysisDataset at
     * the given normalised position. This dataset has the probability density
     * function from angles 0-360 at 0.1 degree intervals.
     * 
     * @param xposition
     * @param dataset
     * @return
     * @throws Exception
     */
    public XYDataset createModalityProbabililtyDataset(double xposition, IAnalysisDataset dataset, ProfileType type)
            throws ChartDatasetCreationException {

        FloatXYDataset ds = new FloatXYDataset();

        ICellCollection collection = dataset.getCollection();
        KernelEstimator est = createProfileProbabililtyKernel(xposition, dataset, type);

        // List<Double> xValues = new ArrayList<Double>();
        // List<Double> yValues = new ArrayList<Double>();

        float[] xvalues = new float[3600];
        float[] yvalues = new float[3600];

        float step = 0.1f;

        // for(double i=0; i<=360; i+=0.1){
        for (int i = 0; i < xvalues.length; i++) {

            float position = i * step;
            xvalues[i] = position;
            yvalues[i] = (float) est.getProbability(position);

            // xValues.add(i);
            // yValues.add(est.getProbability(i));
        }
        float[][] data = { xvalues, yvalues };
        // double[][] data = { Utils.getdoubleFromDouble(xValues.toArray(new
        // Double[0])),
        // Utils.getdoubleFromDouble(yValues.toArray(new Double[0])) };

        ds.addSeries(collection.getName(), data, 0);

        return ds;
    }

    /**
     * Generate a chart dataset showing the p-values along each profile position
     * for all datasets
     * 
     * @param options
     *            the charting options
     * @return
     * @throws ChartDatasetCreationException
     */
    public XYDataset createModalityProfileDataset() throws ChartDatasetCreationException {

        // log("Creating modality p-value dataset");
        FloatXYDataset ds = new FloatXYDataset();

        for (IAnalysisDataset dataset : options.getDatasets()) {

            ICellCollection collection = dataset.getCollection();

            IProfile pvalues = new DipTester(collection).testCollectionGetPValues(options.getTag(), options.getType());

            float[] yvalues = pvalues.toFloatArray();
            float[] xvalues = createXPositions(pvalues, 100).toFloatArray();

            float[][] data = { xvalues, yvalues };
            ds.addSeries(collection.getName(), data, 0);
        }

        return ds;
    }

    /**
     * Create a charting dataset for the angles within the AnalysisDataset at
     * the given normalised position. This dataset has the individual angle
     * values for each nucleus profile
     * 
     * @param xposition
     * @param dataset
     * @return
     * @throws ChartDatasetCreationException
     */
    public XYDataset createModalityValuesDataset(double xposition, IAnalysisDataset dataset, ProfileType type) throws ChartDatasetCreationException {

        FloatXYDataset ds = new FloatXYDataset();

        ICellCollection collection = dataset.getCollection();

        double[] dValues;
        try {
            dValues = collection.getProfileCollection().getValuesAtPosition(type, xposition);
        } catch (UnavailableProfileTypeException e) {
            throw new ChartDatasetCreationException("Cannot get profile values", e);
        }
        float[] values = new float[dValues.length];
        for(int i=0; i<dValues.length; i++){
            values[i] = (float) dValues[i];
        }

        
        float[] xvalues = new float[values.length];
        for (int i = 0; i < values.length; i++) {
            xvalues[i] = 0;
        }

        float[][] data = { values, xvalues };
        ds.addSeries(collection.getName(), data, 0);
        return ds;
    }

    /**
     * Create a probability kernel estimator for the profile angle values in the
     * dataset
     * 
     * @param xposition
     *            the profile position
     * @param dataset
     * @return
     * @throws Exception
     */
    public KernelEstimator createProfileProbabililtyKernel(double xposition, IAnalysisDataset dataset, ProfileType type)
            throws ChartDatasetCreationException {
        ICellCollection collection = dataset.getCollection();
        KernelEstimator est = new KernelEstimator(0.001);
        double[] values;
        try {
            values = collection.getProfileCollection().getValuesAtPosition(type, xposition);
        } catch (UnavailableProfileTypeException e) {
            throw new ChartDatasetCreationException("Cannot get profile values at position " + xposition, e);
        }
        // add the values to a kernel estimator
        // give each value equal weighting
        for (double d : values) {
            est.addValue(d, 1);
        }
        return est;
    }

    /**
     * Create a probability kernel estimator for an array of values using
     * default precision of the KernelEstimator (0.001)
     * 
     * @param values
     *            the array of values
     * @return
     * @throws Exception
     */
    public KernelEstimator createProbabililtyKernel(double[] values) throws ChartDatasetCreationException {

        return createProbabililtyKernel(values, 0.001);
    }

    /**
     * Create a probability kernel estimator for an array of values
     * 
     * @param values
     *            the array of values
     * @param binWidth
     *            the precision of the KernelEstimator
     * @return
     * @throws Exception
     */
    public KernelEstimator createProbabililtyKernel(double[] values, double binWidth)
            throws ChartDatasetCreationException {
        KernelEstimator est = new KernelEstimator(binWidth);

        // add the values to a kernel estimator
        // give each value equal weighting
        for (double d : values) {
            est.addValue(d, 1);
        }
        return est;
    }

    /**
     * Create a probability kernel estimator for an array of values
     * 
     * @param values
     *            the array of values
     * @param binWidth
     *            the precision of the KernelEstimator
     * @return
     */
    public KernelEstimator createProbabililtyKernel(List<Number> values, double binWidth) {
        KernelEstimator est = new KernelEstimator(binWidth);
        // add the values to a kernel estimator
        // give each value equal weighting

        for (Number d : values) {
            est.addValue(d.doubleValue(), 1);
        }
        return est;
    }

    /**
     * Create a dataset suitable for making a QQ plot
     * 
     * @param values
     *            the array of values to use
     * @return a dataset for charting
     */
    public XYDataset createQQDataset(float[] values) throws ChartDatasetCreationException {
        FloatXYDataset ds = new FloatXYDataset();

        Arrays.sort(values);
        // double[] percentiles = new double[values.length];
        float[] zscores = new float[values.length];

        for (int i = 0; i < values.length; i++) {

            int rank = i + 1;
            float percentile = (float) (rank - 0.5) / values.length;
            zscores[i] = (float) DipTester.getInvNormProbabililty(percentile);

        }
        float[][] data = { values, zscores };
        ds.addSeries("Q-Q", data, 0);
        return ds;

    }
    
    /**
     * Create an XYDataset with the edges in a NucleusMesh comparison. Also
     * stores the result edge length ratios.
     * 
     * @param mesh
     * @return
     * @throws Exception
     */
    public NucleusMeshXYDataset createNucleusMeshVertexDataset(Mesh<Nucleus> mesh) throws ChartDatasetCreationException {
        NucleusMeshXYDataset ds = new NucleusMeshXYDataset();

        for (MeshVertex v : mesh.getPeripheralVertices()) {

            double[] yvalues = { v.getPosition().getY() };
            double[] xvalues = { v.getPosition().getX() };
            double[][] data = { xvalues, yvalues };
            ds.addSeries(v.toString(), data);
            ds.setRatio(v.toString(), 1);
        }
        
        for (MeshVertex v : mesh.getInternalVertices()) {
            double[] yvalues = { v.getPosition().getY() };
            double[] xvalues = { v.getPosition().getX() };
            double[][] data = { xvalues, yvalues };
            ds.addSeries(v.toString(), data);
            ds.setRatio(v.toString(), -1);
        }
        return ds;
    }

    /**
     * Create an XYDataset with the edges in a NucleusMesh comparison. Also
     * stores the result edge length ratios.
     * 
     * @param mesh
     * @return
     * @throws Exception
     */
    public NucleusMeshXYDataset createNucleusMeshEdgeDataset(Mesh<Nucleus> mesh) throws ChartDatasetCreationException {
        NucleusMeshXYDataset ds = new NucleusMeshXYDataset();


        for (MeshEdge edge : mesh.getEdges()) {

            double[] yvalues = { edge.getV1().getPosition().getY(), edge.getV2().getPosition().getY() };

            double[] xvalues = { edge.getV1().getPosition().getX(), edge.getV2().getPosition().getX() };

            double[][] data = { xvalues, yvalues };
            ds.addSeries(edge.toString(), data);
            ds.setRatio(edge.toString(), edge.getLog2Ratio());
        }
        return ds;
    }

    /**
     * Create an XYDataset with the midpoints of edges in a NucleusMesh
     * comparison.
     * 
     * @param mesh
     * @return
     * @throws Exception
     */
    public NucleusMeshXYDataset createNucleusMeshMidpointDataset(Mesh<Nucleus> mesh) throws Exception {
        NucleusMeshXYDataset ds = new NucleusMeshXYDataset();

        for (MeshEdge edge : mesh.getEdges()) {

            double[] yvalues = { edge.getMidpoint().getY(), };

            double[] xvalues = { edge.getMidpoint().getX(), };

            double[][] data = { xvalues, yvalues };
            ds.addSeries(edge.toString(), data);
            ds.setRatio(edge.toString(), edge.getLog2Ratio());
        }
        return ds;
    }

    public HistogramDataset createNucleusMeshHistogramDataset(Mesh<Nucleus> mesh) throws ChartDatasetCreationException {
        HistogramDataset ds = new HistogramDataset();

        int bins = 100;

        double max = mesh.getEdges().parallelStream()
                .max((e1, e2) -> Double.compare(e1.getLog2Ratio(), e2.getLog2Ratio())).get().getLog2Ratio();

        double min = mesh.getEdges().parallelStream()
                .min((e1, e2) -> Double.compare(e1.getLog2Ratio(), e2.getLog2Ratio())).get().getLog2Ratio();

        double[] values = mesh.getEdges().parallelStream().mapToDouble(MeshEdge::getLog2Ratio).toArray();

        ds.addSeries("mesh result", values, bins, min, max);

        return ds;
    }

    public XYDataset createBooleanProfileDataset(IProfile p, BooleanProfile limits)
            throws ChartDatasetCreationException {
        FloatXYDataset result = new FloatXYDataset();
        
        float[] xTrueData = new float[limits.countTrue()];
        float[] yTrueData = new float[limits.countTrue()];
        float[] xFalseData = new float[limits.countFalse()];
        float[] yFalseData = new float[limits.countFalse()];

        // Split true and false values from limits to separate arrays 
        for (int i = 0, t=0, f=0; i < p.size(); i++) {

            boolean b = limits.get(i);
            double value = p.get(i);
            if (b) {
                xTrueData[t] = i;
                yTrueData[t++] = (float) value;
            } else {
                xFalseData[f] = i;
                yFalseData[f++] = (float) value;
            }

        }
        float[][] trueData = { xTrueData, yTrueData };
        float[][] falseData = { xFalseData, yFalseData };

        result.addSeries("True", trueData, 0);

        result.addSeries("False", falseData, 0);
        return result;
    }

}
