package com.bmskinner.nma.analysis.nucleus;

import java.io.File;
import java.util.List;
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
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.generic.FloatPoint;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.components.options.MissingOptionException;
import com.bmskinner.nma.components.profiles.IProfile;
import com.bmskinner.nma.components.profiles.IProfileSegment;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.profiles.ISegmentedProfile;
import com.bmskinner.nma.components.profiles.Landmark;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.profiles.UnprofilableObjectException;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.gui.events.UIController;
import com.bmskinner.nma.stats.Stats;

/**
 * Choose the nucleus most similar to a dataset median profile as the consensus
 * 
 * @author Ben Skinner
 *
 */
public class ConsensusSimilarityMethod extends SingleDatasetAnalysisMethod {

	private static final Logger LOGGER = Logger.getLogger(ConsensusAveragingMethod.class.getName());

	private static final String EMPTY_FILE = "Empty";

	/** This length was chosen to avoid issues copying segments */
	private static final double PROFILE_LENGTH = 1000d;

	public ConsensusSimilarityMethod(@NonNull final IAnalysisDataset dataset) {
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
		LOGGER.finer("Running consensus similarity on " + dataset.getName());

		Nucleus template = dataset.getCollection()
				.getNucleusMostSimilarToMedian(OrientationMark.REFERENCE).duplicate();
		template.moveCentreOfMass(new FloatPoint(0, 0));

		// Build a consensus nucleus from the template
		// Create a nucleus with the same rulesets as the dataset
		IAnalysisOptions op = dataset.getAnalysisOptions().orElseThrow(MissingOptionException::new);

		Nucleus n = ComponentBuilderFactory
				.createNucleusBuilderFactory(op.getRuleSetCollection(),
						op.getProfileWindowProportion(), template.getScale())
				.newBuilder()
				.fromPoints(template.getBorderList())
				.withFile(new File(EMPTY_FILE))
				.withCoM(template.getCentreOfMass())
				.build();

		// Add landmarks and segments from the profile collection
		setLandmarks(n);
		setSegments(n);

		Consensus cons = new DefaultConsensusNucleus(n);

		// Calculate any other stats that need the vertical alignment
		cons.getOrientedNucleus();

		dataset.getCollection().setConsensus(cons);
		fireProgressEvent();

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
	 * @throws ProfileException
	 * @throws SegmentUpdateException
	 * @throws MissingDataException
	 */
	private void setSegments(Nucleus n)
			throws ProfileException,
			SegmentUpdateException, MissingDataException {
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

}
