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
package com.bmskinner.nuclear_morphology.analysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.Taggable;
import com.bmskinner.nuclear_morphology.components.generic.IProfileCollection;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableComponentException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.generic.UnsegmentedProfileException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.Stats;

/**
 * Checks the state of a dataset to report on abnormalities in, for example,
 * segmentation patterns in parent versus child datasets
 * 
 * @author bms41
 * @since 1.13.6
 *
 */
public class DatasetValidator implements Loggable {

	public static final List<String> errorList = new ArrayList<>();
	public static final Set<ICell> errorCells  = new HashSet<>();

	public DatasetValidator() {

	}

	public List<String> getErrors() {
		return errorList;
	}

	public Set<ICell> getErrorCells(){
		return errorCells;
	}

	/**
	 * Run validation on a single collection. This will test the consistency
	 * of all cells in the collection, but has no child testing.
	 * @param collection
	 * @return
	 */
	public boolean validate(final @NonNull ICellCollection collection) {
		errorList.clear();
		errorCells.clear();
		
		int errors = 0;
		
		if (!checkAllNucleiHaveProfiles(collection)) {
			errorList.add("Error in nucleus profiling");
			errors++;
		}
		
		if (!checkSegmentsAreConsistentInAllCells(collection)) {
			errorList.add("Error in segmentation between cells");
			errors++;
		}
		
		if (!checkNucleiHaveRPOnASegmentBoundary(collection)) {
			errorList.add("Error in RP placement between cells");
			errors++;
		}

		if (errors == 0) {
			errorList.add("Collection OK");
			return true;
		}
		errorList.add(String.format("collection failed validation: %s out of %s cells have errors", errorCells.size(), collection.getCells().size()));
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
		
		int errors = 0;

		if (!checkAllNucleiHaveProfiles(d)) {
			errorList.add("Error in nucleus profiling");
			errors++;
		}
		
		if (!checkChildDatasetsHaveProfileCollections(d)) {
			errorList.add("Error in child dataset profiling");
			errors++;
		}
		
		
		if (!checkSegmentsAreConsistentInProfileCollections(d)) {
			errorList.add("Error in segmentation between datasets");
			errors++;
		}
		
		if (!checkChildDatasetsHaveBorderTagsPresentInRoot(d)) {
			errorList.add("Error in segmentation between datasets");
			errors++;
		}

		if (!checkSegmentsAreConsistentInAllCells(d)) {
			errorList.add("Error in segmentation between cells");
			errors++;
		}
		
		if (!checkNucleiHaveRPOnASegmentBoundary(d)) {
			errorList.add("Error in RP placement between cells");
			errors++;
		}

		if (errors == 0) {
			errorList.add("Dataset OK");
			return true;
		}
		errorList.add(String.format("Dataset failed validation: %s out of %s cells have errors", errorCells.size(), d.getCollection().getCells().size()));
		return false;

	}
	
	private boolean checkAllNucleiHaveProfiles(@NonNull IAnalysisDataset d) {
		return checkAllNucleiHaveProfiles(d.getCollection());
	}
	
	private boolean checkAllNucleiHaveProfiles(@NonNull ICellCollection collection) {
		boolean isOk = true;

		for (ProfileType type : ProfileType.values()) {
			for(ICell c : collection) {
				for (Nucleus n : c.getNuclei()) {
					try {
						n.getProfile(type);
					} catch (UnavailableProfileTypeException e) {
						errorList.add(String.format("Nucleus %s does not have %s profile", n.getNameAndNumber(), type));
						errorCells.add(c);
						isOk = false;
					}
				}
			}
		}
		return isOk;	
	}
	
	private boolean checkChildDatasetsHaveProfileCollections(@NonNull IAnalysisDataset d) {
		List<IAnalysisDataset> children = d.getAllChildDatasets();
		boolean isOk = true;
		
		IProfileCollection pc = d.getCollection().getProfileCollection();
		for (ProfileType type : ProfileType.values()) {
			try {
				pc.getProfile(type, Tag.REFERENCE_POINT, Stats.MEDIAN);
			} catch (UnavailableProfileTypeException | UnavailableBorderTagException | ProfileException e) {
				errorList.add(String.format("Root dataset %s does not have %s", d.getName(), type));
				isOk = false;
			}
			
			try {
				pc.getSegmentedProfile(type, Tag.REFERENCE_POINT, Stats.MEDIAN);
			} catch (UnavailableProfileTypeException | UnavailableBorderTagException | ProfileException | UnsegmentedProfileException e) {
				errorList.add(String.format("Root dataset %s does not have segmented %s", d.getName(), type));
				isOk = false;
			}
			

			
			for (IAnalysisDataset child : children) {
				IProfileCollection childPc = child.getCollection().getProfileCollection();
				try {
					childPc.getProfile(type, Tag.REFERENCE_POINT, Stats.MEDIAN);
				} catch (UnavailableProfileTypeException | UnavailableBorderTagException | ProfileException e) {
					errorList.add(String.format("Child dataset %s does not have %s", child.getName(), type));
					isOk = false;
				}
				
				try {
					childPc.getSegmentedProfile(type, Tag.REFERENCE_POINT, Stats.MEDIAN);
				} catch (UnavailableProfileTypeException | UnavailableBorderTagException | ProfileException | UnsegmentedProfileException e) {
					errorList.add(String.format("Child dataset %s does not have segmented %s", child.getName(), type));
					isOk = false;
				}
			}
			
		}
		return isOk;	
	}

	/**
	 * Check that all the tags assigned in the root profile collection are present
	 * in all nuclei, and in all child collections
	 * @param d
	 * @return
	 */
	private boolean checkChildDatasetsHaveBorderTagsPresentInRoot(@NonNull IAnalysisDataset d) {
		List<IAnalysisDataset> children = d.getAllChildDatasets();
		boolean isOk = true;

		List<Tag> rootTags = d.getCollection().getProfileCollection().getBorderTags();
		for(ICell c : d.getCollection()) {
			for(Nucleus n : c.getNuclei()) {
				for(Tag t : rootTags) {
					if(!n.hasBorderTag(t)) {
						isOk = false;
						errorList.add(String.format("Nucleus %s does not have root collection tag", n.getNameAndNumber(), t));
						errorCells.add(c);
					}
				}
			}
		}
		
		if(d.getCollection().hasConsensus()) {
			for(Tag t : rootTags) {
				if(!d.getCollection().getConsensus().hasBorderTag(t)) {
					isOk = false;
					errorList.add(String.format("Consensus nucleus does not have root collection tag", t));
				}
			}
		}
			

		for (IAnalysisDataset child : children) {
			for(Tag t : rootTags) {
				if(!child.getCollection().getProfileCollection().getBorderTags().contains(t)) {
					isOk = false;
					errorList.add(String.format("Child dataset %s does not have root collection tag", child.getName(), t));
				}
			}
			
			if(child.getCollection().hasConsensus()) {
				for(Tag t : rootTags) {
					if(!child.getCollection().getConsensus().hasBorderTag(t)) {
						isOk = false;
						errorList.add(String.format("Child dataset %s consensus nucleus does not have root collection tag", child.getName(), t));
					}
				}
			}
		}

		return isOk;	
	}
	
	
	private boolean checkNucleiHaveRPOnASegmentBoundary(@NonNull IAnalysisDataset d) {
		return checkNucleiHaveRPOnASegmentBoundary(d.getCollection());
	}
	
	/**
	 * Check if the RP is at a segment boundary in all cells
	 * @param d
	 * @return
	 */
	private boolean checkNucleiHaveRPOnASegmentBoundary(@NonNull ICellCollection collection) {
		boolean allOk = true;
		for(ICell c : collection) {
			for(Nucleus n : c.getNuclei()) {
				boolean isOk = false;
				
				try {
					int rpIndex = n.getBorderIndex(Tag.REFERENCE_POINT);
					ISegmentedProfile profile = n.getProfile(ProfileType.ANGLE);
					for(IBorderSegment s : profile.getSegments()){
						if(s.getStartIndex()==rpIndex)
							isOk = true;
					}
				} catch (UnavailableBorderTagException | UnavailableProfileTypeException e) {
					// allow isOk to fall through
					fine("No border tag present");
				}

				if(!isOk) {
					errorList.add(String.format("Nucleus %s does not have RP at a segment boundary", n.getNameAndNumber()));
					errorCells.add(c);
					allOk = false;
				}
			}
		}
		return allOk;
	}

		

	/**
	 * Test if all child collections have the same segmentation pattern applied
	 * as the parent collection
	 * 
	 * @param d the root dataset to check
	 * @return
	 */
	private boolean checkSegmentsAreConsistentInProfileCollections(@NonNull IAnalysisDataset d) {

		List<UUID> idList = d.getCollection().getProfileCollection().getSegmentIDs();

		List<IAnalysisDataset> children = d.getAllChildDatasets();

		for (IAnalysisDataset child : children) {

			List<UUID> childList = child.getCollection().getProfileCollection().getSegmentIDs();

			if(idList.size()!=childList.size()) {
				errorList.add(String.format("Root dataset %s segments; child dataset has %s", idList.size(), childList.size()));
				return false;
			}

			// check all parent segments are in child
			for (UUID id : idList) {
				if (!childList.contains(id)) {
					errorList.add("Segment " + id + " not found in child " + child.getName());
					return false;
				}
			}

			// Check all child segments are in parent
			for (UUID id : childList) {
				if (!idList.contains(id)) {
					errorList.add(child.getName() + " segment " + id + " not found in parent");
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Check if all cells in the dataset have the same segmentation pattern, 
	 * including the consensus nucleus.
	 * 
	 * @param d the dataset collection to test
	 * @return
	 */
	private boolean checkSegmentsAreConsistentInAllCells(@NonNull IAnalysisDataset d) {
		return checkSegmentsAreConsistentInAllCells(d.getCollection());
	}
	
	/**
	 * Check if all cells in the collection have the same segmentation pattern, 
	 * including the consensus nucleus
	 * 
	 * @param collection the cell collection to test
	 * @return
	 */
	private boolean checkSegmentsAreConsistentInAllCells(@NonNull ICellCollection collection) {
		
		ISegmentedProfile medianProfile;
		try {
			medianProfile = collection.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE,
					Tag.REFERENCE_POINT, Stats.MEDIAN);
		} catch (UnavailableBorderTagException | UnavailableProfileTypeException | ProfileException
				| UnsegmentedProfileException e) {
			errorList.add("Unable to fetch median profile for collection");
			return false;
		}

		
		List<UUID> idList = collection.getProfileCollection().getSegmentIDs();

		int errorCount = 0;

		for(ICell c : collection.getCells()){
			int cellErrors = 0;
			for (Nucleus n :c.getNuclei()) {
				cellErrors += checkSegmentation(n, idList, medianProfile);
			}
			
			if(cellErrors>0)
				errorCells.add(c);
			errorCount+=cellErrors;
		}

		if(collection.hasConsensus()) {
			int consensusErrors = checkSegmentation(collection.getConsensus(), idList, medianProfile);
			if(consensusErrors>0)
				errorList.add("Segmentation error in consensus");
			errorCount += consensusErrors;
		}
		
		if(errorCount>0) {
			errorList.add(String.format("Segments are not consistent in all cells"));
		}

		return errorCount==0;
	}
	
	/**
	 * Check a nucleus segmentation matches the expected list of segments,
	 * and that segmentation patterns are internally consistent
	 * @param n the object to check
	 * @param expectedSegments the expected segment ids
	 * @return the number of errors found
	 */
	private int checkSegmentation(Taggable n, List<UUID> expectedSegments, ISegmentedProfile medianProfile) {
		
		int errorCount = 0;
		boolean hasSegments = expectedSegments.size()>0;
		ISegmentedProfile p;
		try {
			p = n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
			if(p.hasSegments()!=hasSegments) {
				errorList.add(String.format("Profile collection segments is %s; nucleus is %s", hasSegments, p.hasSegments()));
				errorCount++;
			}

			List<UUID> childList = p.getSegmentIDs();

			if(expectedSegments.size()!=childList.size()) {
				errorList.add(String.format("Profile collection has %s segments; nucleus has %s", expectedSegments.size(), childList.size()));
				errorCount++;
			}

			// Check all nucleus segments are in root dataset
			for (UUID id : childList) {
				if (!expectedSegments.contains(id) && !id.equals(n.getID())) {
					errorList.add(String.format("Nucleus %s has segment %s not found in parent", n.getID(), id));
					errorCount++;
				}
			}

			// Check all root dataset segments are in nucleus

			for (UUID id : expectedSegments) {
				if (!childList.contains(id)) {
					errorList.add(String.format("Profile collection segment %s not found in object %s", id, n.getID()));
					errorCount++;
				}
			}



			// Check each profile index in only covered once by a segment
			for (UUID id1 : expectedSegments) {
				IBorderSegment s1 = p.getSegment(id1);
				for (UUID id2 : expectedSegments) {
					if(id1==id2)
						continue;
					IBorderSegment s2 = p.getSegment(id2);
					if(s1.overlapsBeyondEndpoints(s2)){
						errorList.add(String.format("%s overlaps %s in object %s", s1.getDetail(), s2.getDetail(), n.getID()));
						errorCount++;
					}

				}
			}
			

				
			for(UUID id : medianProfile.getSegmentIDs()){
				IBorderSegment medianSeg = medianProfile.getSegment(id);
				IBorderSegment objectSeg = p.getSegment(id);
				if(medianSeg.hasMergeSources()!=objectSeg.hasMergeSources())
					errorCount++;
				for(IBorderSegment mge : medianSeg.getMergeSources()) {
					if(!objectSeg.hasMergeSource(mge.getID())) {
						errorList.add(String.format("Object segment %s does not have expected median merge source in object %s", mge.getName(), n.getID()));
						errorCount++;
					}
				}
				for(IBorderSegment obj : objectSeg.getMergeSources()) {
					if(!medianSeg.hasMergeSource(obj.getID())) {
						errorList.add(String.format("Median segment %s does not have object merge source in object %s", medianSeg.getName(), n.getID()));
						errorCount++;
					}
						
				}
			}



		} catch (ProfileException | UnavailableComponentException e) {
			errorList.add(String.format("Error getting segments for object %s: %s", n.getID(), e.getMessage()));
			errorCount++;
		}
		return errorCount;
	}

}
