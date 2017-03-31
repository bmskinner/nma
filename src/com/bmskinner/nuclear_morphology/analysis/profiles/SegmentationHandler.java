/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package com.bmskinner.nuclear_morphology.analysis.profiles;

import java.util.UUID;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ProfileableCellularComponent.IndexOutOfBoundsException;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.generic.UnsegmentedProfileException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.Quartile;

/**
 * This coordinates updates to segmentation between datasets
 * and their children. When a UI request is made to update segmentation,
 * this handler is reponsible for keeping all child datasets in sync
 * @author bms41
 * @since 1.13.3
 *
 */
public class SegmentationHandler implements Loggable {
	
	private IAnalysisDataset dataset;
	
	public SegmentationHandler(IAnalysisDataset d){
		dataset = d;
	}
	
	/**
	 * Unmerge segments with the given ID in this collection
	 * and its children, as long as the collection is real.
	 * @param segID the segment ID to be unmerged
	 */
	public void mergeSegments(UUID segID1, UUID segID2){
		
		if(dataset.getCollection().isVirtual()){
			return;
		}

		// Give the new merged segment a new ID
		UUID newID = java.util.UUID.randomUUID();
		
		try {

			ISegmentedProfile medianProfile = dataset.getCollection()
					.getProfileCollection()
					.getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Quartile.MEDIAN);

			IBorderSegment seg1 = medianProfile.getSegment(segID1);
			IBorderSegment seg2 = medianProfile.getSegment(segID2);
			
			boolean ok = dataset.getCollection()
					.getProfileManager().testSegmentsMergeable(seg1, seg2);
			
			if(ok){
				
				dataset.getCollection()
					.getProfileManager()
					.mergeSegments(seg1, seg2, newID);
				
				for(IAnalysisDataset child : dataset.getAllChildDatasets()){
					
					ISegmentedProfile childProfile = child.getCollection()
							.getProfileCollection()
							.getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Quartile.MEDIAN);

					IBorderSegment childSeg1 = childProfile.getSegment(segID1);
					IBorderSegment childSeg2 = childProfile.getSegment(segID2);
					
					
					child.getCollection()
						.getProfileManager()
						.mergeSegments(childSeg1, childSeg2, newID);
				}
			} else {
				warn("Segments are not mergable");
			}
			
		} catch(ProfileException | UnavailableBorderTagException 
				| UnavailableProfileTypeException | UnsegmentedProfileException e){
			warn("Welp, shit done got fucked");
			error("Error merging segments", e);

		}
	}
	
	/**
	 * Unmerge segments with the given ID in this collection
	 * and its children, as long as the collection is real.
	 * @param segID the segment ID to be unmerged
	 */
	public void unmergeSegments(UUID segID){
		
		if(dataset.getCollection().isVirtual()){
			return;
		}

		try {

			ISegmentedProfile medianProfile = dataset.getCollection()
					.getProfileCollection()
					.getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Quartile.MEDIAN);

			IBorderSegment seg = medianProfile.getSegment(segID);
			
			if( ! seg.hasMergeSources()){
				return;
			}

			dataset.getCollection()
				.getProfileManager()
				.unmergeSegments(seg);


			for(IAnalysisDataset child : dataset.getAllChildDatasets()){

				ISegmentedProfile childProfile = child.getCollection()
						.getProfileCollection()
						.getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Quartile.MEDIAN);

				IBorderSegment childSeg = childProfile.getSegment(segID);


				child.getCollection()
				.getProfileManager()
				.unmergeSegments(childSeg);
			}
		} catch(ProfileException | UnavailableBorderTagException 
				| UnavailableProfileTypeException | UnsegmentedProfileException e){
			warn("Welp, shit done got fucked");
			error("Error unmerging segments", e);

		}
	}


	/**
	 * Split the segment with the given ID in this collection
	 * and its children, as long as the collection is real.
	 * @param segID the segment ID to be split
	 */
	public void splitSegment(UUID segID){

		if(dataset.getCollection().isVirtual()){
			return;
		}

		try {

			ISegmentedProfile medianProfile = dataset.getCollection()
					.getProfileCollection()
					.getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Quartile.MEDIAN);

			IBorderSegment seg = medianProfile.getSegment(segID);

			UUID newID1 = java.util.UUID.randomUUID();
			UUID newID2 = java.util.UUID.randomUUID();


			boolean ok = dataset
					.getCollection()
					.getProfileManager()
					.splitSegment(seg, newID1, newID2);

			if(ok){

				for(IAnalysisDataset child : dataset.getAllChildDatasets()){
					
					child.getCollection()
					.getProfileManager()
					.splitSegment(seg, newID1, newID2);
				}
			}

		} catch(ProfileException | UnavailableBorderTagException 
				| UnavailableProfileTypeException | UnsegmentedProfileException e){
			warn("Welp, shit done got fucked");
			error("Error unmerging segments", e);

		}
	}

	/**
	 * Update the start index of the given segment to the given index in the 
	 * median profile, and update individual nuclei to match.
	 * @param id
	 * @param index
	 * @throws Exception
	 */
	public void updateSegmentStartIndexAction(UUID id, int index) {
				
		try {

			double prop = dataset.getCollection()
				.getProfileCollection().getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Quartile.MEDIAN)
				.getFractionOfIndex(index);
		

		// Update the median profile
			dataset
			.getCollection()
			.getProfileManager()
			.updateMedianProfileSegmentIndex(true, id, index); // DraggablePanel always uses seg start index
		
		
		for(IAnalysisDataset child : dataset.getAllChildDatasets()){
			
			// Update each child median profile to the same proportional index 
			
			int childIndex = child.getCollection()
					.getProfileCollection().getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Quartile.MEDIAN)
					.getIndexOfFraction(prop);
			
			child.getCollection()
				.getProfileManager()
				.updateMedianProfileSegmentIndex(true, id, childIndex);
		}

		// Lock all the segments except the one to change
		dataset
			.getCollection()
			.getProfileManager()
			.setLockOnAllNucleusSegmentsExcept(id, true);

		} catch(ProfileException | UnavailableBorderTagException 
				| UnavailableProfileTypeException | UnsegmentedProfileException e){
			warn("Welp, shit done got fucked");
			error("Error unmerging segments", e);

		}

		
		
						


		
	}
	
	
	/**
	 * Update the border tag in the median profile to the given index, 
	 * and update individual nuclei to match.
	 * @param tag
	 * @param newTagIndex
	 */
	public void setBorderTag(Tag tag, int index){

		if(tag==null){
			throw new IllegalArgumentException("Tag is null");
		}
		
		if(dataset.getCollection().isVirtual()){
			return;
		}


		try {
			
			double prop = dataset.getCollection()
					.getProfileCollection().getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Quartile.MEDIAN)
					.getFractionOfIndex(index);
			
			
			dataset.getCollection()
				.getProfileManager()
				.updateBorderTag(tag, index);
			
			
			for(IAnalysisDataset child : dataset.getAllChildDatasets()){
				
				// Update each child median profile to the same proportional index 
				
				int childIndex = child.getCollection()
						.getProfileCollection().getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Quartile.MEDIAN)
						.getIndexOfFraction(prop);
				
				child.getCollection()
					.getProfileManager()
					.updateBorderTag(tag, childIndex);
			}
			
			
		} catch (IndexOutOfBoundsException | ProfileException | UnavailableBorderTagException | UnavailableProfileTypeException e) {
			warn("Unable to update border tag index");
			stack("Profiling error", e);
			return;
		}
	}
	
}

