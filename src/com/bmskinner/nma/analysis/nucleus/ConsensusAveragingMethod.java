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
package com.bmskinner.nma.analysis.nucleus;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.DefaultAnalysisResult;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nma.components.ComponentBuilderFactory;
import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.Consensus;
import com.bmskinner.nma.components.cells.DefaultConsensusNucleus;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.generic.FloatPoint;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.components.options.MissingOptionException;
import com.bmskinner.nma.components.profiles.IProfile;
import com.bmskinner.nma.components.profiles.IProfileSegment;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.profiles.ISegmentedProfile;
import com.bmskinner.nma.components.profiles.Landmark;
import com.bmskinner.nma.components.profiles.MissingProfileException;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.profiles.UnprofilableObjectException;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.gui.events.UIController;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.stats.Stats;

/**
 * This method refolds the consensus nucleus based on averaging the positions of
 * equally spaced points around the perimeter of each vertical nucleus in the
 * dataset.
 * 
 * @author ben
 * @since 1.13.5
 *
 */
public class ConsensusAveragingMethod extends SingleDatasetAnalysisMethod {

	private static final Logger LOGGER = Logger.getLogger(ConsensusAveragingMethod.class.getName());

	private static final String EMPTY_FILE = "Empty";

	/** This length was chosen to avoid issues copying segments */
	private static final double PROFILE_LENGTH = 1000d;

	public ConsensusAveragingMethod(@NonNull final IAnalysisDataset dataset) {
		super(dataset);
	}

	@Override
	public IAnalysisResult call() throws Exception {
		run();
		return new DefaultAnalysisResult(dataset);
	}

	private void run() throws MissingDataException, UnprofilableObjectException,
			ComponentCreationException,
			ProfileException, MissingOptionException, SegmentUpdateException {
		LOGGER.finer("Running consensus averaging on " + dataset.getName());

		List<IPoint> border = calculatePointAverage();
		Consensus refoldNucleus = makeConsensus(border);

		dataset.getCollection().setConsensus(refoldNucleus);

		UIController.getInstance().fireConsensusNucleusChanged(dataset);
	}

	/**
	 * Set the landmarks from the profile collection to the nucleus.
	 * 
	 * @param n
	 * @throws ProfileException
	 * @throws SegmentUpdateException
	 * @throws MissingDataException
	 */
	private void setLandmarks(Nucleus n)
			throws ProfileException, MissingDataException, SegmentUpdateException {
		// Add all landmarks from the profile collection

		// Landmarks were originally found via rulesets when the nucleus was created in
		// the builder. We need to replace this with something more reflective of the
		// collection.

		// Set the RP, so we can offset everything from there
		Landmark rp = dataset.getCollection().getProfileCollection()
				.getLandmark(OrientationMark.REFERENCE);
		n.setLandmark(rp, 0);

		IProfile rpMedian = dataset.getCollection().getProfileCollection().getProfile(
				ProfileType.ANGLE, rp, Stats.MEDIAN);
		int rpIndex = n.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE)
				.findBestFitOffset(rpMedian);

		for (Landmark l : dataset.getCollection().getProfileCollection().getLandmarks()) {
			if (rp.equals(l))
				continue;

			IProfile median = dataset.getCollection().getProfileCollection().getProfile(
					ProfileType.ANGLE, l, Stats.MEDIAN);

			int newIndex = n.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE)
					.findBestFitOffset(median);
			n.setLandmark(l, n.wrapIndex(newIndex + rpIndex));
		}

	}

	/**
	 * Set the segments from the profile collection to the nucleus.
	 * 
	 * @param n
	 * @throws SegmentUpdateException
	 * @throws MissingDataException
	 */
	private void setSegments(Nucleus n)
			throws SegmentUpdateException, MissingDataException {
		// Add segments to the new nucleus profile
		if (dataset.getCollection().getProfileCollection().hasSegments()) {
			ISegmentedProfile profile = n.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);

			List<IProfileSegment> segs = dataset.getCollection().getProfileCollection()
					.getSegments(OrientationMark.REFERENCE);

			List<IProfileSegment> newSegs = IProfileSegment.scaleSegments(segs, profile.size());
			IProfileSegment.linkSegments(newSegs);

			profile.setSegments(newSegs);
			n.setSegments(profile.getSegments());
		}
	}

	/**
	 * Create the consensus nucleus
	 * 
	 * @param n
	 * @throws ProfileException
	 * @throws MissingProfileException
	 * @throws SegmentUpdateException
	 * @throws MissingDataException
	 */
	private Consensus makeConsensus(List<IPoint> list)
			throws UnprofilableObjectException, ComponentCreationException,
			ProfileException, SegmentUpdateException, MissingDataException {

		// Decide on the best scale for the consensus,
		// and scale the points back into pixel coordinates
		double scale = choosePixelToMicronScale();

		for (IPoint p : list)
			p.set(p.multiply(scale));

		// Create a nucleus with the same rulesets as the dataset
		IAnalysisOptions op = dataset.getAnalysisOptions().orElseThrow(MissingOptionException::new);

		Nucleus n = ComponentBuilderFactory
				.createNucleusBuilderFactory(op.getRuleSetCollection(),
						op.getProfileWindowProportion(), scale)
				.newBuilder()
				.fromPoints(list)
				.withFile(new File(EMPTY_FILE))
				.withCoM(new FloatPoint(0, 0))
				.build();

		// Add landmarks and segments from the profile collection
		setLandmarks(n);
		setSegments(n);

		// Build a consensus nucleus from the template points
		Consensus cons = new DefaultConsensusNucleus(n);

		// Calculate any other stats that need the vertical alignment
		cons.getOrientedNucleus();
		return cons;
	}

	/**
	 * The pixel to micron scale used for the consensus depends on the cells in the
	 * dataset. If all the cells come from the same microscope, then we use that
	 * scale. If they come from different scopes, then we need to pick one.
	 * 
	 * @return
	 */
	private double choosePixelToMicronScale() {

		// Easiest option - the scale is consistent across the dataset, and is in the
		// options
		Optional<IAnalysisOptions> analysisOptions = dataset.getAnalysisOptions();
		if (analysisOptions.isPresent()) {
			Optional<HashOptions> nucleusOptions = analysisOptions.get()
					.getNucleusDetectionOptions();
			if (nucleusOptions.isPresent()) {
				if (nucleusOptions.get().hasDouble(HashOptions.SCALE))
					return nucleusOptions.get().getDouble(HashOptions.SCALE);
			} else {
				LOGGER.fine(
						"No nucleus detection options present, unable to find pixel scale for consensus");
			}
		} else {
			LOGGER.fine("No analysis options present, unable to find pixel scale for consensus");
		}

		// The scale is not set at the dataset level. Choose the scale of the
		// first nucleus in the dataset.
		return dataset.getCollection().stream().map(ICell::getPrimaryNucleus).findFirst().get()
				.getScale();

	}

	/**
	 * Calculate the average position of points evenly spaced around the nuclear
	 * perimeters. The points are returned in micron coordinates in case nuclei in
	 * the dataset have cells at different scales.
	 * 
	 * @return
	 */
	private List<IPoint> calculatePointAverage() {

		final Map<Double, List<IPoint>> perimeterPoints = new HashMap<>();

//		final List<IPoint>[] arr = new ArrayList[1000];
//		Arrays.setAll(arr, e -> new ArrayList<IPoint>());

		IPoint zeroCoM = IPoint.atOrigin();

		try {
			// Make a list of points at equivalent positions in each nucleus
			for (Nucleus n : dataset.getCollection().getNuclei()) {

				Nucleus v = n.getOrientedNucleus();
				IPoint oldCoM = v.getCentreOfMass().duplicate();
				v.moveCentreOfMass(zeroCoM);
				IProfile p = v.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);

				for (int i = 0; i < PROFILE_LENGTH; i++) {

					double fractionOfPerimeter = i / PROFILE_LENGTH;

					List<IPoint> list = perimeterPoints.computeIfAbsent(fractionOfPerimeter,
							k -> new ArrayList<>());

					int indexInProfile = p.getIndexOfFraction(fractionOfPerimeter);
					int borderIndex = v.getIndexRelativeTo(OrientationMark.REFERENCE,
							indexInProfile);
					IPoint point = v.getBorderPoint(borderIndex);

					// Scale the point to microns
					IPoint micronPoint = point.divide(n.getScale());
					list.add(micronPoint);

//					arr[i].add(micronPoint);
				}
				// Put the oriented nucleus back where it belongs
				v.moveCentreOfMass(oldCoM);
			}
		} catch (Exception e1) {
			LOGGER.log(Loggable.STACK, "Error calculating perimeter points in nuclei", e1);
		}

		// Avoid errors in border calculation due to identical points by
		// checking each average point in the list is different to the
		// previous. Needed since we have a large profile length.
		List<IPoint> averagedPoints = new ArrayList<>();
		for (int i = 0; i < PROFILE_LENGTH; i++) {
			double d = i / PROFILE_LENGTH;
			List<IPoint> list = perimeterPoints.get(d);
			IPoint avg = calculateMedianPoint(list);

			if (averagedPoints.isEmpty()
					|| !averagedPoints.get(averagedPoints.size() - 1).equals(avg)) {
				averagedPoints.add(avg);
			}
			fireProgressEvent();
		}
		return averagedPoints;
	}

	/**
	 * Find the point with the median x and y coordinate of the given points
	 * 
	 * @param list
	 * @return
	 */
	private IPoint calculateMedianPoint(List<IPoint> list) {
		double[] xpoints = new double[list.size()];
		double[] ypoints = new double[list.size()];

		for (int i = 0; i < list.size(); i++) {
			IPoint p = list.get(i);

			xpoints[i] = p.getX();
			ypoints[i] = p.getY();
		}

		double xMed = Stats.quartile(xpoints, Stats.MEDIAN);
		double yMed = Stats.quartile(ypoints, Stats.MEDIAN);

		return new FloatPoint(xMed, yMed);
	}
}
