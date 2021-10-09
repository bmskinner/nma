package com.bmskinner.nuclear_morphology.analysis;

import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.MissingComponentException;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileSegment;
import com.bmskinner.nuclear_morphology.components.profiles.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.profiles.MissingProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.UnsegmentedProfileException;
import com.bmskinner.nuclear_morphology.stats.Stats;

/**
 * This class checks a dataset for errors using the DatasetValidator,
 * and attempts to apply simple fixes to repair problems. It is not guaranteed
 * to fix all problems.
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
	 * Check for issues in the dataset, and repair any
	 * possible.Has no effect if no issues are found.
	 * @param d the dataset to repair.
	 */
	public void repair(IAnalysisDataset d) {
		DatasetValidator dv = new DatasetValidator();

		// No action if the dataset is ok
		if(dv.validate(d))
			return;

		Set<ICell> brokenCells = dv.getErrorCells();

		try {

			UUID seg0Id = d.getCollection()
					.getProfileCollection()
					.getSegmentedProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT, Stats.MEDIAN)
					.getSegmentContaining(0)
					.getID();

			for(ICell c : brokenCells) {
				for(Nucleus n : c.getNuclei())
					repairNucleusRPNotAtSegmentBoundary(n, seg0Id);
			}

		} catch (MissingLandmarkException | MissingProfileException | ProfileException e) {
			// allow isOk to fall through
			LOGGER.fine("No border tag present");
		}
		
		if(dv.validate(d)) {
			LOGGER.info("Dataset repaired: "+d.getName());
		} else{
			LOGGER.info("Could not repair "+d.getName());
		}


	}
	
	/**
	 * Repair nuclei in which the RP is not at the expected segment boundary. Has
	 * no effect if the nucleus does not have this issue. The existing segmentation pattern
	 * is preserved, and the RP is moved to the given segment start index.
	 * @param n the nucleus to repair.
	 * @param expectedRPSegmentStart the segment id which the RP should lie on the start index of
	 */
	private void repairNucleusRPNotAtSegmentBoundary(Nucleus n, UUID expectedRPSegmentStart) {		
		boolean wasLocked = n.isLocked();
		if(wasLocked)
			n.setLocked(false);
		try {
			int rpIndex = n.getBorderIndex(Landmark.REFERENCE_POINT);
			ISegmentedProfile profile = n.getProfile(ProfileType.ANGLE);
			IProfileSegment s  = profile.getSegment(expectedRPSegmentStart);
			int segStart = s.getStartIndex();

			if(s.getStartIndex()!=rpIndex) {
				// We can't just set RP since setting RP will update segments by the same amount
				// Need to copy the profile and segments as is, then reload them after the RP has been changed
				LOGGER.finest("RP at "+rpIndex+"; expected at "+segStart);
				n.setBorderTag(Landmark.REFERENCE_POINT, segStart);
				LOGGER.finest(n.getNameAndNumber()+": updated RP to index "+segStart);
				n.setProfile(ProfileType.ANGLE, profile);
			}
			LOGGER.finest(n.getNameAndNumber()+": Seg start index now "+ n.getProfile(ProfileType.ANGLE).getSegment(expectedRPSegmentStart).getStartIndex());
			
		} catch (MissingLandmarkException e) {
			LOGGER.fine("No RP tag present in "+n.getNameAndNumber());
		} catch (MissingProfileException e) {
			LOGGER.fine("No angle profile present in "+n.getNameAndNumber());
		} catch (MissingComponentException e) {
			LOGGER.fine("No segment with id "+expectedRPSegmentStart+" in "+n.getNameAndNumber());
		}	
		n.setLocked(wasLocked);
	}
}
