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
package com.bmskinner.nma.components.datasets;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.Taggable;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.profiles.IProfileCollection;
import com.bmskinner.nma.components.profiles.IProfileSegment;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.profiles.ISegmentedProfile;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;
import com.bmskinner.nma.components.profiles.MissingProfileException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.stats.Stats;

/**
 * Checks the state of a dataset to report on abnormalities in, for example,
 * segmentation patterns in parent versus child datasets
 * 
 * @author bms41
 * @since 1.13.6
 *
 */
public class DatasetValidator {

	private static final String CONSENSUS_NUCLEUS_LACKS_LANDMARK = "Consensus nucleus does not have required landmark";

	private static final Logger LOGGER = Logger.getLogger(DatasetValidator.class.getName());

	public static final List<String> errorList = new ArrayList<>();
	public static final List<String> summaryList = new ArrayList<>();
	public static final Set<ICell> errorCells = new HashSet<>();

	public DatasetValidator() {

	}

	/**
	 * Get the overall summary of errors in the dataset
	 * 
	 * @return
	 */
	public List<String> getSummary() {
		return summaryList;
	}

	/**
	 * Get a detailed list of every error in the dataset
	 * 
	 * @return
	 */
	public List<String> getErrors() {
		return errorList;
	}

	/**
	 * Get the cells in the dataset with errors
	 * 
	 * @return
	 */
	public Set<ICell> getErrorCells() {
		return errorCells;
	}

	/**
	 * Run validation on a single collection. This will test the consistency of all
	 * cells in the collection, but has no child testing.
	 * 
	 * @param collection
	 * @return
	 */
	public boolean validate(final @NonNull ICellCollection collection) {

		errorList.clear();
		errorCells.clear();
		summaryList.clear();

		int errors = 0;

		int rpErrors = checkAllNucleiHaveRP(collection);
		if (rpErrors != 0) {
			summaryList.add(String.format("Error in RP assignment for %s nuclei", rpErrors));
			errors++;
		}

		int profileErrors = checkAllNucleiHaveProfiles(collection);
		if (profileErrors != 0) {
			summaryList
					.add(String.format("Error in nucleus profiling for %s nuclei", profileErrors));
			errors++;
		}

		int segErrors = checkSegmentsAreConsistentInAllCells(collection);
		if (segErrors != 0) {
			summaryList.add(
					String.format("Error in segmentation between cells: %s errors", segErrors));
			errors++;
		}

		int rpBoundaryErrors = checkNucleiHaveRPOnASegmentBoundary(collection);
		if (rpBoundaryErrors != 0) {
			summaryList.add(
					String.format("Error in RP/segment placement in %s cells", rpBoundaryErrors));
			errors++;
		}

		if (errors == 0) {
			summaryList.add("Collection OK");
			return true;
		}

		summaryList.add(String.format(
				"collection failed validation: %s out of %s cells have errors", errorCells.size(),
				collection.size()));
		return false;
	}

	/**
	 * Run validation on the given dataset
	 * 
	 * @param d
	 */
	public boolean validate(final @NonNull IAnalysisDataset d) {

		errorList.clear();
		errorCells.clear();
		summaryList.clear();

		int errors = 0;

		int rpErrors = checkAllNucleiHaveRP(d.getCollection());
		if (rpErrors != 0) {
			summaryList.add(String.format("Error in RP assignment for %s nuclei", rpErrors));
			errors++;
		}

		int profileErrors = checkAllNucleiHaveProfiles(d);
		if (profileErrors != 0) {
			summaryList
					.add(String.format("Error in nucleus profiling for %s nuclei", profileErrors));
			errors++;
		}

		int pcErrors = checkChildDatasetsHaveProfileCollections(d);
		if (pcErrors != 0) {
			summaryList.add(
					String.format("Error in child dataset profiling for %s profiles", pcErrors));
			errors++;
		}

		int segMatch = checkSegmentsAreConsistentInProfileCollections(d);
		if (segMatch != 0) {
			summaryList.add("There are " + segMatch + " errors in segmentation between datasets");
			errors++;
		}

		int childErrors = checkChildDatasetsHaveBorderTagsPresentInRoot(d);
		if (childErrors != 0) {
			summaryList.add(
					"There are " + childErrors + " errors in segmentation between child datasets");
			errors++;
		}

		int segErrors = checkSegmentsAreConsistentInAllCells(d);
		if (segErrors != 0) {
			summaryList.add(
					String.format("Error in segmentation between cells: %s errors", segErrors));
			errors++;
		}

		int rpBoundaryErrors = checkNucleiHaveRPOnASegmentBoundary(d);
		if (rpBoundaryErrors != 0) {
			summaryList.add(
					String.format("Error in RP/segment placement in %s cells", rpBoundaryErrors));
			errors++;
		}

		if (errors == 0) {
			summaryList.add("Dataset OK");
			return true;
		}
		summaryList.add(String.format("Dataset failed validation: %s out of %s cells have errors",
				errorCells.size(),
				d.getCollection().getCells().size()));
		return false;
	}

	public boolean validate(@NonNull ICell c) {
		errorList.clear();
		errorCells.clear();
		summaryList.clear();
		Nucleus n = c.getPrimaryNucleus();

		if (!c.getPrimaryNucleus().hasLandmark(OrientationMark.REFERENCE)) {
			errorList.add(String.format("Nucleus %s does not have RP",
					c.getPrimaryNucleus().getNameAndNumber()));
			errorCells.add(c);
		}

		int rpIsOk = 0;

		try {
			int rpIndex = n.getBorderIndex(OrientationMark.REFERENCE);

			// A profile starting from RP will have RP at index zero.
			// One segment should start at index 0
			ISegmentedProfile profile = n.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);
			LOGGER.finer("Testing RP " + rpIndex + " on profile " + profile.toString());
			for (IProfileSegment s : profile.getSegments()) {
				if (s.getStartIndex() == 0)
					rpIsOk++;
			}

			if (rpIsOk == 0) {
				errorList.add(String.format(
						"Nucleus %s does not have RP at a segment boundary: RP at %s, profile %s",
						n.getNameAndNumber(), rpIndex, profile.toString()));
				errorCells.add(c);
			}

		} catch (MissingLandmarkException e) {
			errorList
					.add(String.format("Nucleus %s does not have an RP set", n.getNameAndNumber()));
			errorCells.add(c);
		} catch (MissingProfileException e) {
			errorList.add(String.format("Nucleus %s does not have an angle profile",
					n.getNameAndNumber()));
			errorCells.add(c);
		} catch (MissingDataException e) {
			errorList.add(String.format("Nucleus %s is missing data",
					n.getNameAndNumber()));
			errorCells.add(c);
		} catch (SegmentUpdateException e) {
			errorList.add(String.format("Nucleus %s had an error finding segments: %s",
					n.getNameAndNumber(),
					e.getMessage()));
			errorCells.add(c);
		}

		return errorCells.isEmpty();
	}

	private int checkAllNucleiHaveProfiles(@NonNull IAnalysisDataset d) {
		return checkAllNucleiHaveProfiles(d.getCollection());
	}

	private int checkAllNucleiHaveRP(@NonNull ICellCollection collection) {
		int withErrors = 0;

		for (ICell c : collection) {
			for (Nucleus n : c.getNuclei()) {
				if (!n.hasLandmark(OrientationMark.REFERENCE)) {
					errorList.add(
							String.format("Nucleus %s does not have RP", n.getNameAndNumber()));
					errorCells.add(c);
					withErrors++;
				}
			}
		}
		return withErrors;
	}

	private int checkAllNucleiHaveProfiles(@NonNull ICellCollection collection) {
		int withErrors = 0;

		for (ProfileType type : ProfileType.values()) {
			for (ICell c : collection) {
				for (Nucleus n : c.getNuclei()) {
					try {
						n.getProfile(type);
					} catch (MissingDataException | SegmentUpdateException e) {
						errorList.add(String.format("Nucleus %s does not have %s profile",
								n.getNameAndNumber(), type));
						errorCells.add(c);
						withErrors++;
					}
				}
			}
		}
		return withErrors;
	}

	private int checkChildDatasetsHaveProfileCollections(@NonNull IAnalysisDataset d) {
		List<IAnalysisDataset> children = d.getAllChildDatasets();
		int withErrors = 0;

		IProfileCollection pc = d.getCollection().getProfileCollection();
		for (ProfileType type : ProfileType.values()) {
			try {
				pc.getProfile(type, OrientationMark.REFERENCE, Stats.MEDIAN);
			} catch (MissingDataException | SegmentUpdateException e) {
				summaryList
						.add(String.format("Root dataset %s does not have %s", d.getName(), type));
				withErrors++;
			}

			try {
				pc.getSegmentedProfile(type, OrientationMark.REFERENCE, Stats.MEDIAN);
			} catch (MissingDataException | SegmentUpdateException e) {
				summaryList.add(String.format("Root dataset %s does not have segmented %s",
						d.getName(), type));
				withErrors++;
			}

			for (IAnalysisDataset child : children) {
				IProfileCollection childPc = child.getCollection().getProfileCollection();
				try {
					childPc.getProfile(type, OrientationMark.REFERENCE, Stats.MEDIAN);
				} catch (MissingDataException | SegmentUpdateException e) {
					summaryList.add(String.format("Child dataset %s does not have %s",
							child.getName(), type));
					withErrors++;
				}

				try {
					childPc.getSegmentedProfile(type, OrientationMark.REFERENCE, Stats.MEDIAN);
				} catch (MissingDataException | SegmentUpdateException e) {
					summaryList
							.add(String.format("Child dataset %s does not have segmented %s",
									child.getName(), type));
					withErrors++;
				}
			}

		}
		return withErrors;
	}

	/**
	 * Check that all the tags assigned in the root profile collection are present
	 * in all nuclei, and in all child collections
	 * 
	 * @param d
	 * @return
	 */
	private int checkChildDatasetsHaveBorderTagsPresentInRoot(@NonNull IAnalysisDataset d) {
		List<IAnalysisDataset> children = d.getAllChildDatasets();
		int withErrors = 0;

		List<OrientationMark> rootTags = d.getCollection().getProfileCollection()
				.getOrientationMarks();
		for (ICell c : d.getCollection()) {
			for (Nucleus n : c.getNuclei()) {
				for (OrientationMark t : rootTags) {
					if (!n.hasLandmark(t)) {
						withErrors++;
						errorList.add(
								String.format("Nucleus %s does not have root collection tag %s",
										n.getNameAndNumber(), t));
						errorCells.add(c);
					}
				}
			}
		}

		if (d.getCollection().hasConsensus()) {

			try {
				for (OrientationMark t : rootTags) {
					if (!d.getCollection().getConsensus().hasLandmark(t)) {
						withErrors++;
						errorList.add(String.format(
								"Consensus nucleus does not have root collection tag %s", t));
					}
				}
			} catch (MissingLandmarkException | ComponentCreationException e) {
				errorList.add(CONSENSUS_NUCLEUS_LACKS_LANDMARK);
			}
		}

		for (IAnalysisDataset child : children) {
			for (OrientationMark t : rootTags) {
				if (!child.getCollection().getProfileCollection().getOrientationMarks()
						.contains(t)) {
					withErrors++;
					errorList.add(
							String.format("Child dataset %s does not have root collection tag %s",
									child.getName(), t));
				}
			}

			if (child.getCollection().hasConsensus()) {
				try {
					for (OrientationMark t : rootTags) {
						if (!child.getCollection().getConsensus().hasLandmark(t)) {
							withErrors++;
							errorList.add(String.format(
									"Child dataset %s consensus nucleus does not have root collection tag %s",
									child.getName(), t));
						}
					}
				} catch (MissingLandmarkException | ComponentCreationException e) {
					errorList.add(CONSENSUS_NUCLEUS_LACKS_LANDMARK);
				}
			}
		}

		return withErrors;
	}

	private int checkNucleiHaveRPOnASegmentBoundary(@NonNull IAnalysisDataset d) {
		return checkNucleiHaveRPOnASegmentBoundary(d.getCollection());
	}

	/**
	 * Check if the RP is at a segment boundary in all cells. Does not check which
	 * segment boundary the RP is at
	 * 
	 * @param d
	 * @return
	 */
	private int checkNucleiHaveRPOnASegmentBoundary(@NonNull ICellCollection collection) {
		int allErrors = 0;
		for (ICell c : collection) {
			for (Nucleus n : c.getNuclei()) {
				int rpIsOk = 0;

				try {
					int rpIndex = n.getBorderIndex(OrientationMark.REFERENCE);

					// A profile starting from RP will have RP at index zero.
					// One segment should start at index 0
					ISegmentedProfile profile = n.getProfile(ProfileType.ANGLE,
							OrientationMark.REFERENCE);
					LOGGER.finer("Testing RP " + rpIndex + " on profile " + profile.toString());
					for (IProfileSegment s : profile.getSegments()) {
						if (s.getStartIndex() == 0)
							rpIsOk++;
					}

					if (rpIsOk == 0) {
						errorList.add(
								String.format(
										"Nucleus %s does not have RP at a segment boundary: RP at %s, profile %s",
										n.getNameAndNumber(), rpIndex, profile.toString()));
						errorCells.add(c);
						allErrors++;
					}

				} catch (MissingLandmarkException e) {
					errorList.add(String.format("Nucleus %s does not have an RP set",
							n.getNameAndNumber()));
					errorCells.add(c);
					allErrors++;
				} catch (MissingProfileException e) {
					errorList.add(String.format("Nucleus %s does not have an angle profile",
							n.getNameAndNumber()));
					errorCells.add(c);
					allErrors++;
				} catch (MissingDataException e) {
					errorList.add(String.format("Nucleus %s is missing data",
							n.getNameAndNumber()));
					errorCells.add(c);
					allErrors++;

				} catch (SegmentUpdateException e) {
					errorList.add(String.format("Nucleus %s had an error finding segments: %s",
							n.getNameAndNumber(),
							e.getMessage()));
					errorCells.add(c);
					allErrors++;
				}
			}
		}
		return allErrors;
	}

	/**
	 * Test if all child collections have the same segmentation pattern applied as
	 * the parent collection
	 * 
	 * @param d the root dataset to check
	 * @return
	 */
	private int checkSegmentsAreConsistentInProfileCollections(@NonNull IAnalysisDataset d) {

		int numErrors = 0;
		List<UUID> idList = d.getCollection().getProfileCollection().getSegmentIDs();

		List<IAnalysisDataset> children = d.getAllChildDatasets();

		for (IAnalysisDataset child : children) {

			List<UUID> childList = child.getCollection().getProfileCollection().getSegmentIDs();

			if (idList.size() != childList.size()) {
				summaryList.add(String.format("Root dataset %s segments; child dataset has %s",
						idList.size(),
						childList.size()));
				numErrors++;
			}

			// check all parent segments are in child
			for (UUID id : idList) {
				if (!childList.contains(id)) {
					errorList.add("Segment " + id + " not found in child " + child.getName());
					numErrors++;
				}
			}

			// Check all child segments are in parent
			for (UUID id : childList) {
				if (!idList.contains(id)) {
					errorList.add(child.getName() + " segment " + id + " not found in parent");
					numErrors++;
				}
			}
		}

		return numErrors;
	}

	/**
	 * Check if all cells in the dataset have the same segmentation pattern,
	 * including the consensus nucleus.
	 * 
	 * @param d the dataset collection to test
	 * @return
	 */
	private int checkSegmentsAreConsistentInAllCells(@NonNull IAnalysisDataset d) {
		return checkSegmentsAreConsistentInAllCells(d.getCollection());
	}

	/**
	 * Check if all cells in the collection have the same segmentation pattern,
	 * including the consensus nucleus
	 * 
	 * @param collection the cell collection to test
	 * @return
	 */
	private int checkSegmentsAreConsistentInAllCells(@NonNull ICellCollection collection) {

		ISegmentedProfile medianProfile;
		try {
			medianProfile = collection.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE,
					OrientationMark.REFERENCE, Stats.MEDIAN);
		} catch (MissingDataException | SegmentUpdateException e) {
			errorList.add("Unable to fetch median profile for collection");
			return 1;
		}

		List<UUID> idList = collection.getProfileCollection().getSegmentIDs();

		int errorCount = 0;

		for (ICell c : collection.getCells()) {
			int cellErrors = 0;
			for (Nucleus n : c.getNuclei()) {
				cellErrors += checkSegmentation(n, idList, medianProfile);
			}

			if (cellErrors > 0)
				errorCells.add(c);
			errorCount += cellErrors;
		}

		if (collection.hasConsensus()) {
			try {
				int consensusErrors = checkSegmentation(collection.getConsensus(), idList,
						medianProfile);
				if (consensusErrors > 0)
					errorList.add("Segmentation error in consensus");
				errorCount += consensusErrors;
			} catch (MissingLandmarkException | ComponentCreationException e) {
				errorList.add(CONSENSUS_NUCLEUS_LACKS_LANDMARK);
			}
		}

		if (errorCount > 0) {
			errorList.add(String.format("Segments are not consistent in all cells"));
		}

		return errorCount;
	}

	/**
	 * Check a nucleus segmentation matches the expected list of segments, and that
	 * segmentation patterns are internally consistent
	 * 
	 * @param n                the object to check
	 * @param expectedSegments the expected segment ids
	 * @return the number of errors found
	 */
	private int checkSegmentation(Taggable n, List<UUID> expectedSegments,
			ISegmentedProfile medianProfile) {

		int errorCount = 0;
		boolean hasSegments = expectedSegments.size() > 0;
		ISegmentedProfile p;
		try {
			p = n.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);
			if (p.hasSegments() != hasSegments) {
				errorList.add(String.format("Profile collection segments is %s; nucleus is %s",
						hasSegments,
						p.hasSegments()));
				errorCount++;
			}

			List<UUID> childList = p.getSegmentIDs();

			if (expectedSegments.size() != childList.size()) {
				errorList.add(String.format("Profile collection has %s segments; nucleus has %s",
						expectedSegments.size(), childList.size()));
				errorCount++;
			}

			// Check all nucleus segments are in root dataset
			for (UUID id : childList) {
				if (!expectedSegments.contains(id) && !id.equals(n.getId())) {
					errorList.add(String.format("Nucleus %s has segment %s not found in parent",
							n.getId(), id));
					errorCount++;
				}
			}

			// Check all root dataset segments are in nucleus

			for (UUID id : expectedSegments) {
				if (!childList.contains(id)) {
					errorList.add(String.format(
							"Profile collection segment %s not found in object %s", id, n.getId()));
					errorCount++;
				}
			}

			// Check each profile index in only covered once by a segment
			for (UUID id1 : expectedSegments) {
				IProfileSegment s1 = p.getSegment(id1);
				for (UUID id2 : expectedSegments) {
					if (id1 == id2)
						continue;
					IProfileSegment s2 = p.getSegment(id2);
					if (s1.overlapsBeyondEndpoints(s2)) {
						errorList.add(String.format("%s overlaps %s in object %s", s1.getDetail(),
								s2.getDetail(),
								n.getId()));
						errorCount++;
					}

				}
			}
			// Check all the merge sources from the median profile are present
			for (UUID id : medianProfile.getSegmentIDs()) {
				IProfileSegment medianSeg = medianProfile.getSegment(id);
				IProfileSegment objectSeg = p.getSegment(id);
				if (medianSeg.hasMergeSources() != objectSeg.hasMergeSources())
					errorCount++;
				for (IProfileSegment mge : medianSeg.getMergeSources()) {
					if (!objectSeg.hasMergeSource(mge.getID())) {
						errorList.add(String.format(
								"Object segment %s does not have expected median merge source in object %s",
								mge.getName(), n.getId()));
						errorCount++;
					}
				}
				for (IProfileSegment obj : objectSeg.getMergeSources()) {
					if (!medianSeg.hasMergeSource(obj.getID())) {
						errorList.add(String.format(
								"Median segment %s does not have merge source %s from nucleus %s",
								medianSeg.getName(), obj.getID(), n.getId()));
						errorCount++;
					}
				}
			}

		} catch (MissingDataException | SegmentUpdateException e) {
			errorList.add(String.format("Error getting segments for object %s: %s", n.getId(),
					e.getMessage()));
			errorCount++;
		}
		return errorCount;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Validator:\nSummary:\n");

		for (String s : this.getSummary()) {
			sb.append(s + "\n");
		}
		sb.append("Errors:\n");
		for (String s : this.getErrors()) {
			sb.append(s + "\n");
		}
		return sb.toString();
	}

}
