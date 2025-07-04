package com.bmskinner.nma.components.datasets;

import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.profiles.IProfileSegment;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.profiles.ISegmentedProfile;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;
import com.bmskinner.nma.components.profiles.MissingProfileException;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.stats.Stats;

/**
 * This class checks a dataset for errors using the DatasetValidator, and
 * attempts to apply simple fixes to repair problems. It is not guaranteed to
 * fix all problems.
 * 
 * @author ben
 * @since 1.16.0
 *
 */
public class DatasetRepairer {

	private static final Logger LOGGER = Logger.getLogger(DatasetRepairer.class.getName());

	public DatasetRepairer() {
		// No state to be set here
	}

	/**
	 * Check for issues in the dataset, and repair any possible.Has no effect if no
	 * issues are found.
	 * 
	 * @param d the dataset to repair.
	 */
	public void repair(IAnalysisDataset d) {
		DatasetValidator dv = new DatasetValidator();

		// No action if the dataset is ok
		if (dv.validate(d))
			return;

		Set<ICell> brokenCells = dv.getErrorCells();

		try {

			UUID seg0Id = d.getCollection().getProfileCollection()
					.getSegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.MEDIAN)
					.getSegmentContaining(0).getID();

			for (ICell c : brokenCells) {
				for (Nucleus n : c.getNuclei())
					repairNucleusRPNotAtSegmentBoundary(n, seg0Id);
			}

		} catch (MissingDataException | SegmentUpdateException e) {
			// allow isOk to fall through
			LOGGER.fine("No border tag present");
		}

		if (dv.validate(d)) {
			LOGGER.info("Dataset repaired: " + d.getName());
		} else {
			LOGGER.info("Could not repair " + d.getName());
		}

	}

	/**
	 * Repair nuclei in which the RP is not at the expected segment boundary. Has no
	 * effect if the nucleus does not have this issue. The existing segmentation
	 * pattern is preserved, and the RP is moved to the given segment start index.
	 * 
	 * @param n                      the nucleus to repair.
	 * @param expectedRPSegmentStart the segment id which the RP should lie on the
	 *                               start index of
	 * @throws ProfileException
	 */
	private void repairNucleusRPNotAtSegmentBoundary(Nucleus n, UUID expectedRPSegmentStart) {
		boolean wasLocked = n.isLocked();
		if (wasLocked)
			n.setLocked(false);
		try {
			int rpIndex = n.getBorderIndex(OrientationMark.REFERENCE);
			ISegmentedProfile profile = n.getProfile(ProfileType.ANGLE);
			IProfileSegment s = profile.getSegment(expectedRPSegmentStart);
			int segStart = s.getStartIndex();

			if (s.getStartIndex() != rpIndex) {
				// We can't just set RP since setting RP will update segments by the same amount
				// Need to copy the profile and segments as is, then reload them after the RP
				// has been changed
				n.setLandmark(OrientationMark.REFERENCE, segStart);
			}
			LOGGER.finest(n.getNameAndNumber() + ": Seg start index now "
					+ n.getProfile(ProfileType.ANGLE).getSegment(expectedRPSegmentStart)
							.getStartIndex());

		} catch (MissingLandmarkException e) {
			LOGGER.fine("No RP tag present in " + n.getNameAndNumber());
		} catch (MissingProfileException e) {
			LOGGER.fine("No angle profile present in " + n.getNameAndNumber());
		} catch (MissingDataException e) {
			LOGGER.fine(
					"No segment with id " + expectedRPSegmentStart + " in " + n.getNameAndNumber());
		} catch (IndexOutOfBoundsException e) {
			LOGGER.fine("No suitable index in " + n.getNameAndNumber());
		} catch (SegmentUpdateException e) {
			LOGGER.fine("Unable to update segment bounds in " + n.getNameAndNumber());
		}
		n.setLocked(wasLocked);
	}
}
