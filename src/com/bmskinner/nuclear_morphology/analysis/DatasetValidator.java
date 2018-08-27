/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


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
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableComponentException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.logging.Loggable;

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
	 * Run validation on the given dataset
	 * 
	 * @param d
	 */
	public boolean validate(final IAnalysisDataset d) {

		errorList.clear();
		errorCells.clear();

		if (!d.isRoot()) {
			errorList.add(d.getName() + ": Dataset not checked - not root");
			return false;
		}

		int errors = 0;

		if (!checkSegmentsAreConsistentInProfileCollections(d)) {
			errorList.add("Error in segmentation between datasets");
			errors++;
		}

		if (!checkSegmentsAreConsistentInAllCells(d)) {
			errorList.add("Error in segmentation between cells");
			errors++;
		}

		if (errors == 0) {
			errorList.add("Dataset OK");
			return true;
		}
		errorList.add("Dataset failed validation");
		return false;

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
	 * including the consensus nucleus
	 * 
	 * @param d
	 * @return
	 */
	private boolean checkSegmentsAreConsistentInAllCells(@NonNull IAnalysisDataset d) {

		List<UUID> idList = d.getCollection().getProfileCollection().getSegmentIDs();

		boolean hasSegments = d.getCollection().getProfileCollection().hasSegments();
		int errorCount = 0;

		for(ICell c : d.getCollection().getCells()){
			int cellErrors = 0;
			for (Nucleus n :c.getNuclei()) {

				ISegmentedProfile p;
				try {
					p = n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
					if(p.hasSegments()!=hasSegments) {
						errorList.add(String.format("Profile collection segments is %s; nucleus is %s", hasSegments, p.hasSegments()));
						errorCount++;
					}

					List<UUID> childList = p.getSegmentIDs();

					if(idList.size()!=childList.size()) {
						errorList.add(String.format("Profile collection has %s segments; nucleus has %s", idList.size(), childList.size()));
						errorCount++;
					}

					// Check all nucleus segments are in root dataset
					for (UUID id : childList) {
						if (!idList.contains(id) && !id.equals(n.getID())) {
							errorList.add(String.format("Nucleus %s has segment %s not found in parent", n.getID(), id));
							cellErrors++;
						}
					}


					// Check all root dataset segments are in nucleus

					for (UUID id : idList) {
						if (!childList.contains(id)) {
							errorList.add(String.format("Profile collection segment %s not found in nucleus %s", id, n.getNameAndNumber()));
							cellErrors++;
						}



						// Check each profile index in only covered once by a segment
						for (UUID id1 : idList) {
							IBorderSegment s1 = p.getSegment(id1);
							for (UUID id2 : idList) {
								if(id1==id2)
									continue;
								IBorderSegment s2 = p.getSegment(id2);
								if(s1.overlapsBeyondEndpoints(s2)){
									errorList.add(String.format("Segment %s overlaps segment %s in %s", s1.getDetail(), s2.getDetail(), n.getNameAndNumber()));
									cellErrors++;
								}

							}
						}
					}

				} catch (ProfileException | UnavailableComponentException e) {
					errorList.add(String.format("Error getting segments for nucleus %s: %s", n.getNameAndNumber(), e.getMessage()));
					stack(e);
					e.printStackTrace();
					cellErrors++;
				}

			}
			if(cellErrors>0)
				errorCells.add(c);
			errorCount+=cellErrors;
		}

		if(d.getCollection().hasConsensus()){
			try {
				ISegmentedProfile p = d.getCollection().getConsensus().getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);

				if(idList.size()!=p.getSegmentCount()) {
					errorList.add("Consensus does not have the same number of segments as the root dataset");
					errorCount++;
				}

				for (UUID id : idList) {
					if (!p.hasSegment(id)) {
						errorList.add("Segment " + id + " not found in consensus");
						errorCount++;
					}
				}

				for(UUID id : p.getSegmentIDs()){
					if (!idList.contains(id)) {
						errorList.add("Segment " + id + " in consensus not found in root");
						errorCount++;
					}
				}


			} catch (UnavailableBorderTagException | UnavailableProfileTypeException | ProfileException e) {
				errorList.add(String.format("Error getting segments for consensus nucleus: %s", e.getMessage()));
				e.printStackTrace();
				errorCount++;
			}
		}

		return errorCount==0;
	}

}
