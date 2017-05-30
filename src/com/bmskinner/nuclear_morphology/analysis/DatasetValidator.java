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

package com.bmskinner.nuclear_morphology.analysis;


import java.util.List;
import java.util.UUID;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Checks the state of a dataset to report on abnormalities in,
 * for example, segmentation patterns in parent versus child datasets
 * @author bms41
 * @since 1.13.6
 *
 */
public class DatasetValidator implements Loggable {
	
	public DatasetValidator(){
		
	}
	
	public void validate(final IAnalysisDataset d){
		
		if( ! d.isRoot() ){
			log("Dataset not checked - not root");
			return;
		}
		
		int errors = 0;
		
		if(!checkSegmentsAreConsistentInProfileCollections(d)){
			warn("Error in segmentation between datasets");
			errors++;
		}
		
		if( ! checkSegmentsAreConsistentInAllCells(d)){
			warn("Error in segmentation between cells");
			errors++;
		}
		
		if(errors==0){
			log("Dataset OK");
		} else {
			log("Dataset failed validation");
		}
		
	}
	
	/**
	 * Test if all child collections have the same segmentation pattern applied
	 * as the parent collection
	 * @param d the root dataset to check
	 * @return
	 */
	private boolean checkSegmentsAreConsistentInProfileCollections(IAnalysisDataset d){
		
		List<UUID> idList = d.getCollection().getProfileCollection().getSegmentIDs();
	
		
		List<IAnalysisDataset> children = d.getAllChildDatasets();
		
		for(IAnalysisDataset child : children){
			
			List<UUID> childList = child.getCollection().getProfileCollection().getSegmentIDs();
			
			// check all parent segments are in child
			for(UUID id : idList){
				if( ! childList.contains(id) ){
					warn("Segment "+id+" not found in child "+child.getName());
					return false;
				}
			}
			
			// Check all child segments are in parent
			for(UUID id : childList){
				if( ! idList.contains(id) ){
					warn(child.getName()+" segment "+id+" not found in parent");
					return false;
				}
			}
			
		}
		
		return true;		
	}
	
	/**
	 * Check if all cells in the dataset have the same segmentation pattern
	 * @param d
	 * @return
	 */
	private boolean checkSegmentsAreConsistentInAllCells(IAnalysisDataset d){
		
		List<UUID> idList = d.getCollection().getProfileCollection().getSegmentIDs();
	
		
		for(Nucleus n : d.getCollection().getNuclei()){
			
			ISegmentedProfile p;
			try {
				p = n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
				List<UUID> childList = p.getSegmentIDs();
				
				// Check all nucleus segments are in root dataset
				for(UUID id : childList){
					if( ! idList.contains(id) ){
						warn("Nucleus "+n.getNameAndNumber()+" has segment "+id+" not found in parent");
						return false;
					}
				}
				
				// Check all root dataset segments are in nucleus
				for(UUID id : idList){
					if( ! childList.contains(id) ){
						warn("Segment "+id+" not found in child "+n.getNameAndNumber());
						return false;
					}
				}
			} catch (UnavailableBorderTagException | UnavailableProfileTypeException | ProfileException e) {
				warn("Error getting segments");
				stack(e);
				return false;
			}
			
			
		}
		
		return true;		
	}

}
