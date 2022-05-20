package com.bmskinner.nma.analysis.profiles;

import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.AnalysisMethodException;
import com.bmskinner.nma.analysis.DefaultAnalysisResult;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nma.components.MissingLandmarkException;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.datasets.DatasetValidator;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.profiles.Landmark;
import com.bmskinner.nma.components.profiles.MissingProfileException;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.stats.Stats;

/**
 * Update landmarks in a dataset
 * 
 * @author bs19022
 *
 */
public class UpdateLandmarkMethod extends SingleDatasetAnalysisMethod {

	private static final Logger LOGGER = Logger.getLogger(UpdateLandmarkMethod.class.getName());

	private DatasetValidator dv = new DatasetValidator();

	private final Landmark lm;
	private final int newIndex;

	/**
	 * Create a landmark update
	 * 
	 * @param dataset  the dataset to update
	 * @param lm       the landmark to update
	 * @param newIndex the new index in the median profile
	 */
	public UpdateLandmarkMethod(IAnalysisDataset dataset, @NonNull Landmark lm, int newIndex) {
		super(dataset);
		this.lm = lm;
		this.newIndex = newIndex;
	}

	@Override
	public IAnalysisResult call() throws Exception {
		run();

		if (!dv.validate(dataset))
			throw new AnalysisMethodException(
					"Unable to validate dataset after moving landmark '" + lm + "': "
							+ dv.getSummary() + "\n"
							+ dv.getErrors());
		return new DefaultAnalysisResult(dataset);
	}

	private void run() {
		Landmark rp = dataset.getCollection().getRuleSetCollection()
				.getLandmark(OrientationMark.REFERENCE).get();

		if (dataset.getCollection().isVirtual() && rp.equals(lm)) {
			LOGGER.warning("Cannot update core border tag for a child dataset");
			return;
		}

		LOGGER.fine(
				"Requested " + lm + " set to index " + newIndex + " in '"
						+ dataset.getName() + "'");

		try {
			// Try updating to an existing tag index. If this
			// succeeds, do nothing else
			if (couldUpdateTagToExistingTagIndex(lm, newIndex))
				return;

			// Otherwise, find the best fit for each child dataset
			double prop = dataset.getCollection().getProfileCollection()
					.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.MEDIAN)
					.getFractionOfIndex(newIndex);

			dataset.getCollection().getProfileManager().updateLandmark(lm, newIndex);
			fireProgressEvent();

			for (IAnalysisDataset child : dataset.getAllChildDatasets()) {

				// Update each child median profile to the same proportional index
				int childIndex = child.getCollection().getProfileCollection()
						.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.MEDIAN)
						.getIndexOfFraction(prop);

				child.getCollection().getProfileManager().updateLandmark(lm, childIndex);
				fireProgressEvent();
			}

		} catch (IndexOutOfBoundsException | ProfileException | MissingLandmarkException
				| MissingProfileException e) {
			LOGGER.warning("Unable to update border tag index");
			LOGGER.log(Loggable.STACK, "Profiling error", e);
		} catch (Exception e) {
			LOGGER.warning("Unexpected error");
			LOGGER.log(Loggable.STACK, "Unexpected error", e);
		}

	}

	/**
	 * If a landmark is to be updated to an index with an existing tag, don't
	 * perform alignments; just set the landmark to the same index directly. The
	 * user is expecting the landmark to lie at the same index in every nucleus.
	 * 
	 * @param landmark the tag to update
	 * @param index    the new index for the landmark
	 * @return
	 * @throws MissingLandmarkException
	 * @throws IndexOutOfBoundsException
	 * @throws MissingProfileException
	 * @throws ProfileException
	 * @throws ComponentCreationException
	 */
	private synchronized boolean couldUpdateTagToExistingTagIndex(Landmark tag, int index)
			throws MissingLandmarkException, MissingProfileException, ProfileException,
			IndexOutOfBoundsException,
			ComponentCreationException {
		List<OrientationMark> tags = dataset.getCollection().getProfileCollection()
				.getOrientationMarks();
		for (OrientationMark existingTag : tags) {
			if (existingTag.equals(tag))
				continue;
			int existingTagIndex = dataset.getCollection().getProfileCollection()
					.getLandmarkIndex(existingTag);
			if (index == existingTagIndex) {
				dataset.getCollection().getProfileManager().updateLandmark(tag, existingTagIndex);
				for (IAnalysisDataset child : dataset.getAllChildDatasets()) {
					child.getCollection().getProfileManager().updateLandmark(tag, existingTagIndex);
				}
				return true;
			}
		}
		return false;
	}

}
