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
package com.bmskinner.nuclear_morphology.visualisation.datasets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.XYDataset;

import com.bmskinner.nuclear_morphology.components.MissingComponentException;
import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.UnavailableBorderPointException;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.cells.Nucleus;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.measure.DoubleEquation;
import com.bmskinner.nuclear_morphology.components.measure.LineEquation;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.measure.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.mesh.Mesh;
import com.bmskinner.nuclear_morphology.components.mesh.MeshEdge;
import com.bmskinner.nuclear_morphology.components.mesh.MeshVertex;
import com.bmskinner.nuclear_morphology.components.profiles.BooleanProfile;
import com.bmskinner.nuclear_morphology.components.profiles.DefaultProfile;
import com.bmskinner.nuclear_morphology.components.profiles.IProfile;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileSegment;
import com.bmskinner.nuclear_morphology.components.profiles.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.MissingProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.signals.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.signals.ISignalGroup;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.Stats;
import com.bmskinner.nuclear_morphology.visualisation.options.ChartOptions;
import com.bmskinner.nuclear_morphology.visualisation.options.DefaultChartOptions;

import ij.process.FloatPolygon;
import weka.estimators.KernelEstimator;

public class NucleusDatasetCreator extends AbstractDatasetCreator<ChartOptions> {

	private static final Logger LOGGER = Logger.getLogger(NucleusDatasetCreator.class.getName());

	private static final String UNABLE_TO_GET_BORDER_POINT_ERROR = "Unable to get border point";
	private static final String UNABLE_TO_GET_MEDIAN_PROFILE_ERROR = "Unable to get median profile";

	public NucleusDatasetCreator(@NonNull ChartOptions options) {
		super(options);
	}

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
	 * Get a boxplot dataset for the given statistic for each collection
	 * 
	 * @return
	 * @throws ChartDatasetCreationException
	 */
	public BoxAndWhiskerCategoryDataset createBoxplotDataset() throws ChartDatasetCreationException {
		List<IAnalysisDataset> datasets = options.getDatasets();
		Measurement stat = options.getMeasurement();
		MeasurementScale scale = options.getScale();
		ExportableBoxAndWhiskerCategoryDataset ds = new ExportableBoxAndWhiskerCategoryDataset();

		for (int i = 0; i < datasets.size(); i++) {
			ICellCollection c = datasets.get(i).getCollection();

			double[] stats = c.getRawValues(stat, CellularComponent.NUCLEUS, scale);
			List<Double> list = Arrays.stream(stats).boxed().collect(Collectors.toList());
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

		Measurement stat = options.getMeasurement();

		if (stat.equals(Measurement.LENGTH)) {
			return createSegmentLengthDataset(options.getDatasets(), options.getSegPosition(), options.getScale());
		}

		if (stat.equals(Measurement.DISPLACEMENT)) {
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

			IProfileSegment medianSeg;
			try {
				medianSeg = collection.getProfileCollection()
						.getSegmentedProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT, Stats.MEDIAN)
						.getSegments().get(options.getSegPosition());
				//                        .getSegmentAt(segPosition);
			} catch (MissingLandmarkException | ProfileException | MissingProfileException e) {
				LOGGER.log(Loggable.STACK, "Error getting profile from tag", e);
				throw new ChartDatasetCreationException(UNABLE_TO_GET_MEDIAN_PROFILE_ERROR, e);
			}

			List<Double> list = new ArrayList<>(0);

			for (Nucleus n : collection.getNuclei()) {
				double length = 0;

				try {

					IProfileSegment seg = n.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT)
							.getSegment(medianSeg.getID());

					if (seg != null) {
						int indexLength = seg.length();
						double proportionPerimeter = (double) indexLength / (double) seg.getProfileLength();
						length = n.getMeasurement(Measurement.PERIMETER, scale) * proportionPerimeter;

					}

				} catch (ProfileException | MissingComponentException e) {
					LOGGER.warning("Cannot get segment length for " + n.getNameAndNumber());
					LOGGER.log(Loggable.STACK, "Error getting profile", e);

				}

				list.add(length);
			}

			dataset.add(list, IProfileSegment.SEGMENT_PREFIX + segPosition + "_" + i,
					IProfileSegment.SEGMENT_PREFIX + segPosition);
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

			IProfileSegment medianSeg;
			try {
				medianSeg = collection.getProfileCollection()
						.getSegmentedProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT, Stats.MEDIAN)
						.getSegments().get(options.getSegPosition());
				//                        .getSegmentAt(segPosition);
			} catch (MissingLandmarkException | ProfileException | MissingProfileException e) {
				LOGGER.log(Loggable.STACK, "Error getting profile from tag", e);
				throw new ChartDatasetCreationException(UNABLE_TO_GET_MEDIAN_PROFILE_ERROR, e);
			}

			List<Double> list = new ArrayList<>(0);

			for (Nucleus n : collection.getNuclei()) {

				try {
					ISegmentedProfile profile = n.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT);
					IProfileSegment seg = profile.getSegment(medianSeg.getID());

					double displacement = profile.getDisplacement(seg);
					list.add(displacement);

				} catch (ProfileException | MissingComponentException e) {
					LOGGER.warning("Cannot get segment displacement for " + n.getNameAndNumber());
					LOGGER.log(Loggable.STACK, "Error getting profile", e);
				}
			}

			dataset.add(list, IProfileSegment.SEGMENT_PREFIX + segPosition + "_" + i,
					IProfileSegment.SEGMENT_PREFIX + segPosition);
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

			List<IProfileSegment> segments;
			try {
				segments = collection.getProfileCollection()
						.getSegmentedProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT, Stats.MEDIAN)
						.getOrderedSegments();

				for (IProfileSegment medianSeg : segments) {

					int medianSegmentLength = medianSeg.length();

					List<Integer> list = new ArrayList<>(0);

					for (Nucleus n : collection.getNuclei()) {
						IProfileSegment seg = n.getProfile(ProfileType.ANGLE).getSegment(medianSeg.getName());

						int differenceToMedian = 0;
						// if seg is null, catch before we throw an error
						if (seg != null) {
							differenceToMedian = medianSegmentLength - seg.length();
						}

						list.add(differenceToMedian);
					}

					dataset.add(list, medianSeg.getName(), collection.getName());
				}

			} catch (ProfileException | MissingComponentException e) {
				LOGGER.log(Loggable.STACK, "Error getting profile from tag", e);
				throw new ChartDatasetCreationException(UNABLE_TO_GET_MEDIAN_PROFILE_ERROR, e);
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
				IPoint p = n.getBorderPoint(i);
				xpoints[i] = p.getX();
				ypoints[i] = p.getY();
			}
			// complete the line
			xpoints[n.getBorderLength()] = xpoints[0];
			ypoints[n.getBorderLength()] = ypoints[0];
		} catch (UnavailableBorderPointException e) {
			throw new ChartDatasetCreationException(UNABLE_TO_GET_BORDER_POINT_ERROR, e);
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
		try {
			return createBareNucleusOutline(dataset.getCollection().getConsensus());
		} catch (MissingLandmarkException | ComponentCreationException e) {
			throw new ChartDatasetCreationException(e);
		}
	}

	/**
	 * Get the scale of the nucleus; the lowest absolute x or y limit
	 * 
	 * @param n
	 * @return
	 */
	private double getScaleForIQRRange(@NonNull Nucleus n) {
		double min = Math.min(n.getMinX(), n.getMinY());
		double max = Math.max(n.getMaxX(), n.getMaxY());
		return Math.min(Math.abs(min), Math.abs(max));
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
		Nucleus n;

		// make the IQR
		IProfile q25;
		IProfile q75;
		try {
			n = collection.getConsensus();
			q25 = collection.getProfileCollection().getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT, Stats.LOWER_QUARTILE);
			q75 = collection.getProfileCollection().getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT, Stats.UPPER_QUARTILE);
		} catch (MissingLandmarkException | ProfileException | MissingProfileException | ComponentCreationException e) {
			LOGGER.log(Loggable.STACK, "Error getting upper or lower quartile profile or consensus", e);
			throw new ChartDatasetCreationException("Unable to get quartile profile or consensus", e);
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
			ISegmentedProfile angleProfile = n.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT);

			// At this point, the angle profile and the iqr profile should be in sync
			// The following set of checks confirms this.
			if (angleProfile.hasSegments()) { // only draw if there are segments

				// go through each segment
				for (IProfileSegment seg : angleProfile.getOrderedSegments()) {

					addSegmentIQRToConsensus(seg, ds, n, scaledRange, Landmark.REFERENCE_POINT);

					// draw the segment
					float[] xpoints = new float[seg.length()];
					float[] ypoints = new float[seg.length()];

					Iterator<Integer> it = seg.iterator();
					int i = 0;
					while(it.hasNext()) {
						int index = it.next();
						IPoint p = n.getBorderPoint(index);
						xpoints[i] = (float) p.getX();
						ypoints[i++] = (float) p.getY();
					}
					float[][] data = { xpoints, ypoints };
					ds.addSeries(seg.getName(), data, 0);
				}
			}

			if(ds.getSeriesCount()<angleProfile.getSegmentCount())
				throw new ChartDatasetCreationException("Cannot make segmented nucleus outline: too few series in chart dataset");
		} catch (ProfileException | MissingLandmarkException | MissingProfileException | UnavailableBorderPointException e) {
			LOGGER.log(Loggable.STACK, "Error getting nucleus angle profile from " + Landmark.REFERENCE_POINT, e);
			throw new ChartDatasetCreationException("Cannot make segmented nucleus outline", e);
		}

		// Add the TV, BV as a series only if the options are in debug mode
		if(GlobalOptions.getInstance().getBoolean(GlobalOptions.IS_DEBUG_INTERFACE_KEY)) {
			try {
				IPoint tv = n.getBorderPoint(Landmark.TOP_VERTICAL);
				IPoint bv = n.getBorderPoint(Landmark.BOTTOM_VERTICAL);

				float[] xpoints = { (float) tv.getX(), (float) bv.getX() };
				float[] ypoints = { (float) tv.getY(), (float) bv.getY() };

				float[][] data = { xpoints, ypoints };
				ds.addSeries(TAG_PREFIX, data, ds.getSeriesCount());
			} catch (MissingLandmarkException e) {
				LOGGER.log(Loggable.STACK, "Error getting border tags", e);
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
	private void addSegmentIQRToConsensus(@NonNull IProfileSegment segment, @NonNull FloatXYDataset ds, @NonNull Nucleus n, @NonNull IProfile scaledRange,
			@NonNull Landmark pointType) throws ChartDatasetCreationException {

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
				IPoint p = n.getBorderPoint(index);

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
				throw new ChartDatasetCreationException(UNABLE_TO_GET_BORDER_POINT_ERROR, e);
			} catch(IllegalArgumentException e){
				throw new ChartDatasetCreationException("Problem with line equation", e);
			} catch (MissingLandmarkException e) {
				throw new ChartDatasetCreationException("Border tag is not present: "+pointType, e);
			}

		}

		float[][] inner = { innerIQRX, innerIQRY };
		ds.addSeries(QUARTILE_SERIES_PREFIX+"25_" + segment.getName(), inner, 0);
		float[][] outer = { outerIQRX, outerIQRY };
		ds.addSeries(QUARTILE_SERIES_PREFIX+"75_" + segment.getName(), outer, 0);
	}

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
			for (Landmark tag : nucleus.getLandmarks().keySet()) {
				IPoint tagPoint;

				int tagIndex = nucleus.getBorderIndex(tag);
				tagPoint = nucleus.getOriginalBorderPoint(tagIndex);

				float[] xpoints = { (float) (tagPoint.getX() - 0.5), (float) (nucleus.getOriginalCentreOfMass().getX() - 0.5) };
				float[] ypoints = { (float) (tagPoint.getY() - 0.5), (float) (nucleus.getOriginalCentreOfMass().getY() - 0.5) };
				float[][] data = { xpoints, ypoints };
				ds.addSeries("Tag_" + tag, data, 0);
			}
		} catch (UnavailableBorderPointException | MissingLandmarkException e) {
			throw new ChartDatasetCreationException(UNABLE_TO_GET_BORDER_POINT_ERROR, e);
		}

		return ds;
	}

	/**
	 * Create a dataset for the signal groups in the cell. Each signalGroup is a
	 * new dataset, and each signal in that group is a series
	 * 
	 * @param cell the cell to get signals from
	 * @param dataset the dataset the cell belongs to
	 * @return a dataset for charting
	 * 
	 */
	public List<ComponentOutlineDataset<CellularComponent>> createSignalOutlines(@NonNull ICell cell, @NonNull IAnalysisDataset dataset)
			throws ChartDatasetCreationException {

		List<ComponentOutlineDataset<CellularComponent>> result = new ArrayList<>();
		List<IAnalysisDataset> datasets = new ArrayList<>();
		datasets.add(dataset);

		Nucleus nucleus = cell.getPrimaryNucleus();

		LOGGER.finest( "Attempting to create signal outlines for " + nucleus.getNameAndNumber());

		for (UUID signalGroup : nucleus.getSignalCollection().getSignalGroupIds()) {

			if (!nucleus.getSignalCollection().hasSignal(signalGroup)) {
				continue;
			}

			Optional<ISignalGroup> group = dataset.getCollection().getSignalGroup(signalGroup);

			if(!group.isPresent())
				continue;

			if (group.get().isVisible()) {

				ComponentOutlineDataset<CellularComponent> groupDataset = new ComponentOutlineDataset<>();
				int signalNumber = 0;

				for (INuclearSignal signal : nucleus.getSignalCollection().getSignals(signalGroup)) {

					String seriesKey = CellularComponent.NUCLEAR_SIGNAL + "_" + signalGroup + "_signal_"
							+ signalNumber;
					LOGGER.finest( "Adding signal to dataset: " + seriesKey);
					OutlineDatasetCreator dc = new OutlineDatasetCreator(new DefaultChartOptions(datasets), signal);
					try {
						dc.addOutline(groupDataset, seriesKey, false);

					} catch (ChartDatasetCreationException e) {
						LOGGER.log(Loggable.STACK, "Unable to add signal " + seriesKey + " to dataset", e);
					}
					signalNumber++;
				}
				result.add(groupDataset);

			} else {
				LOGGER.finest( "Not adding " + group + ": not set as visible");
			}
		}
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
				try {
					Nucleus n = collection.getConsensus();
					double consensusScale = n.getScale();
					double[] xpoints = new double[n.getBorderLength()];
					double[] ypoints = new double[n.getBorderLength()];

					int j = 0;

					for (IPoint p : n.getBorderList()) {

						double x = p.getX();
						double y = p.getY();

						if (scale.equals(MeasurementScale.MICRONS)) {

							x = Measurement.lengthToMicrons(x, consensusScale);
							y = Measurement.lengthToMicrons(y, consensusScale);
						}

						xpoints[j] = x;
						ypoints[j] = y;
						j++;
					}

					double[][] data = { xpoints, ypoints };
					ds.addSeries(seriesKey, data);
					ds.setComponent(seriesKey, n);

				} catch(MissingLandmarkException | ComponentCreationException e) {
					throw new ChartDatasetCreationException("Cannot orient consensus", e);
				}
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
//	public XYDataset createModalityProbabililtyDataset(double xposition, IAnalysisDataset dataset, ProfileType type)
//			throws ChartDatasetCreationException {
//
//		FloatXYDataset ds = new FloatXYDataset();
//
//		ICellCollection collection = dataset.getCollection();
//		KernelEstimator est = createProfileProbabililtyKernel(xposition, dataset, type);
//
//		float[] xvalues = new float[3600];
//		float[] yvalues = new float[3600];
//
//		float step = 0.1f;
//		for (int i = 0; i < xvalues.length; i++) {
//
//			float position = i * step;
//			xvalues[i] = position;
//			yvalues[i] = (float) est.getProbability(position);
//		}
//		float[][] data = { xvalues, yvalues };
//
//		ds.addSeries(collection.getName(), data, 0);
//
//		return ds;
//	}

	/**
	 * Generate a chart dataset showing the p-values along each profile position
	 * for all datasets

	 * @return
	 * @throws ChartDatasetCreationException
	 */
//	public XYDataset createModalityProfileDataset() throws ChartDatasetCreationException {
//
//		FloatXYDataset ds = new FloatXYDataset();
//
//		for (IAnalysisDataset dataset : options.getDatasets()) {
//
//			ICellCollection collection = dataset.getCollection();
//
//			IProfile pvalues = new DipTester(collection).testCollectionGetPValues(options.getTag(), options.getType());
//
//			float[] yvalues = pvalues.toFloatArray();
//			float[] xvalues = createXPositions(pvalues, 100).toFloatArray();
//
//			float[][] data = { xvalues, yvalues };
//			ds.addSeries(collection.getName(), data, 0);
//		}
//
//		return ds;
//	}

	private static IProfile createXPositions(IProfile profile, int newLength){
		float[] result = new float[profile.size()];
		for (int i = 0; i < profile.size(); i++) 
			result[i] = (float) (profile.getFractionOfIndex(i) * newLength);
		return new DefaultProfile(result);
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
//	public KernelEstimator createProfileProbabililtyKernel(double xposition, IAnalysisDataset dataset, ProfileType type)
//			throws ChartDatasetCreationException {
//		ICellCollection collection = dataset.getCollection();
//		KernelEstimator est = new KernelEstimator(0.001);
//		double[] values;
//		try {
//			values = collection.getProfileCollection().getValuesAtPosition(type, xposition);
//		} catch (MissingProfileException e) {
//			throw new ChartDatasetCreationException("Cannot get profile values at position " + xposition, e);
//		}
//		// add the values to a kernel estimator
//		// give each value equal weighting
//		for (double d : values) {
//			est.addValue(d, 1);
//		}
//		return est;
//	}

	/**
	 * Create a probability kernel estimator for an array of values using
	 * default precision of the KernelEstimator (0.001)
	 * 
	 * @param values the array of values
	 * @return
	 */
	public KernelEstimator createProbabililtyKernel(double[] values) {
		return createProbabililtyKernel(values, 0.001);
	}

	/**
	 * Create a probability kernel estimator for an array of values
	 * 
	 * @param values the array of values
	 * @param binWidth the precision of the KernelEstimator
	 * @return
	 */
	public KernelEstimator createProbabililtyKernel(double[] values, double binWidth) {
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
	 * @param values the array of values
	 * @param binWidth the precision of the KernelEstimator
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
	 * Create an XYDataset with the edges in a NucleusMesh comparison. Also
	 * stores the result edge length ratios.
	 * 
	 * @param mesh
	 * @return
	 * @throws Exception
	 */
	public NucleusMeshXYDataset createNucleusMeshVertexDataset(Mesh mesh) throws ChartDatasetCreationException {
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
	public NucleusMeshXYDataset createNucleusMeshEdgeDataset(Mesh mesh) throws ChartDatasetCreationException {
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
	public NucleusMeshXYDataset createNucleusMeshMidpointDataset(Mesh mesh) throws ChartDatasetCreationException {
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

	public HistogramDataset createNucleusMeshHistogramDataset(Mesh mesh) throws ChartDatasetCreationException {
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
