/*******************************************************************************
 *  	Copyright (C) 2015, 2016 Ben Skinner
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
 *     GNU General Public License for more details. Gluten-free. May contain 
 *     traces of LDL asbestos. Avoid children using heavy machinery while under the
 *     influence of alcohol.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package analysis.profiles;

import utility.Constants;
import analysis.AnalysisDataset;
import analysis.AnalysisWorker;
import components.CellCollection;
import components.generic.BorderTag;
import components.generic.Profile;
import components.generic.ProfileType;
import components.generic.BorderTag.BorderTagType;

/**
 * This class contains the methods for detecting the reference and orientation points in a median
 * profile, creating the median profile, and adjusting individual nuclei border points to fit the median.
 * @author ben
 *
 */
public class DatasetProfiler extends AnalysisWorker {
	
	private static final BorderTag DEFAULT_BORDER_TAG = BorderTag.REFERENCE_POINT;
	
	public static final int RECALCULATE_MEDIAN = 0;

	public DatasetProfiler(AnalysisDataset dataset){
		super(dataset);
	}
	 
	@Override
    protected Boolean doInBackground() throws Exception {
    	
    	boolean result = true;
		try{


				fine("Profiling dataset");

				// profile the collection from head/tip, then apply to tail
				runProfiler(DEFAULT_BORDER_TAG);

				fine("Datset profiling complete");
			
			
		} catch(Exception e){
			
			error("Error in dataset profiling", e);
			return false;
		} 

		return result;
	}
			
	/**
	 * Calculaate the median profile of the colleciton, and generate the
	 * best fit offsets of each nucleus to match
	 * @param collection
	 * @param pointType
	 */
	private void runProfiler(BorderTag pointType){
		
		try{
			fine("Profiling collection");
			
			/*
			 * The individual nuclei within the collection have had RP
			 * determined from their internal profiles. 
			 * (This is part of Nucleus constructor)
			 * 
			 * Build the median based on the RP indexes.
			 * 
			 * 
			 * If moving RP index in a nucleus improves the 
			 * median, move it.
			 * 
			 * Continue until the best-fit of RP has been obtained.
			 * 
			 * Find the OP and other BorderTags in the median
			 * 
			 * Apply to nuclei using offsets 
			 * 
			 * 
			 */
			
			CellCollection collection = getDataset().getCollection();

			
			// Build the ProfileCollections for each ProfileType
			collection.getProfileManager().createProfileCollections();	
			finest("Created profile collections");
					
			
			// Create a median from the current reference points in the nuclei
			Profile median = collection.getProfileCollection(ProfileType.ANGLE)
					.getProfile(BorderTag.REFERENCE_POINT, Constants.MEDIAN);
			finest("Fetched median from initial RP");
			
			// RP index *should be* zero in the median profile at this point
			// Check this before updating nuclei
			ProfileIndexFinder finder = new ProfileIndexFinder();
			int rpIndex = finder.identifyIndex(collection, BorderTag.REFERENCE_POINT);
			fine("RP in default median is located at index "+rpIndex);
			
			// Update the nucleus profiles to best fit the median
			collection.getProfileManager()
				.offsetNucleusProfiles(BorderTag.REFERENCE_POINT, ProfileType.ANGLE, median);
			
//			fine("Current median profile:");
//			fine(collection.getProfileCollection(ProfileType.ANGLE).getProfile(DEFAULT_BORDER_TAG, 50).toString());
			// Now each nucleus should be at the best fit to the median profile from the RP
			// Rebuilding the median may cause the RP index to change in the median
			
			int coercionCounter = 0;
			while(rpIndex != 0){
				
				// Rebuild the median and offset the nuclei until the RP settles at zero
				fine("Coercing RP to zero, round "+coercionCounter);
				coercionCounter++;
				rpIndex = coerceRPToZero(collection);
				
				if(coercionCounter>50){
					warn("Unable to cleanly assign RP");
					break;
				}
			}
			
			
			fine("Identified best RP in nuclei and constructed median profiles");
			
			fine("Current state of profile collection:"+collection.getProfileCollection(ProfileType.ANGLE).tagString());
			
			
			fine("Identifying OP and other BorderTags");
			
	
			// Identify the border tags in the median profile

			for(BorderTag tag : BorderTag.values() ){
				
				// Don't identify the RP again, it could cause off-by-one errors
				// We do need to assign the RP in other ProfileTypes though
				if(tag.equals(BorderTag.REFERENCE_POINT)){
					
					fine("Checking location of RP in profile");
					int index = finder.identifyIndex(collection, tag);
					fine("RP is found at index "+index);
					continue; 
				}
				
					
				int index = finder.identifyIndex(collection, tag);

				if( index > -2){ // Ruleset was applied
					
					if( index > -1){
						
						// Add the index to the median profiles
						collection.getProfileManager()
							.updateProfileCollectionOffsets(tag, index);

						fine(tag+" in median is located at index "+index);

						// Create a median from the current reference points in the nuclei
						Profile tagMedian = collection.getProfileCollection(ProfileType.ANGLE)
								.getProfile(tag, Constants.MEDIAN);

						collection.getProfileManager()
							.offsetNucleusProfiles(tag, ProfileType.ANGLE, tagMedian);
						fine("Assigned offset in nucleus profiles for "+tag);

					} else {
						
						warn("Unable to detect "+tag+" using default ruleset");
						
						if(tag.type().equals(BorderTagType.CORE)){
							warn("Falling back on reference point");
							index = 0;
						}
						
					}
					fine("Current state of profile collection:"+collection.getProfileCollection(ProfileType.ANGLE).tagString());
									
				} else {
					fine("No ruleset for "+tag+"; skipping");
				}
			}
			
			
						

			fine("Finished profiling collection");

		} catch(Exception e){
			error("Error in dataset profiling", e);
		}
	}
	
	private int coerceRPToZero(CellCollection collection){
		
		ProfileIndexFinder finder = new ProfileIndexFinder();
		
		// check the RP index in the median
		int rpIndex = finder.identifyIndex(collection, BorderTag.REFERENCE_POINT);
		fine("RP in median is located at index "+rpIndex);

		// If RP is not at zero, update
		if(rpIndex!=0){
			
			fine("RP in median is not yet at zero");
			
			// Update the offsets in the profile collection to the new RP
			collection.getProfileManager().updateRP(rpIndex);
			
					
			
			// Find the effects of the offsets on the RP
			// It should be found at zero
			finer("Checking RP index again");
			rpIndex = finder.identifyIndex(collection, BorderTag.REFERENCE_POINT);
			fine("RP in median is now located at index "+rpIndex);
//			fine("Current median profile:");
//			fine(collection.getProfileCollection(ProfileType.ANGLE).getProfile(DEFAULT_BORDER_TAG, Constants.MEDIAN).toString());
			
			fine("Current state of profile collection:");
			fine(collection.getProfileCollection(ProfileType.ANGLE).tagString());
			
		}

		return rpIndex;
	}
	

	
	/**
	 * Get the total differences to the median for all the nuclei in
	 * the collection
	 * @param pointType the BorderTag to compare to
	 * @return
	 */
	private double compareProfilesToMedian(BorderTag pointType) throws Exception {
		double[] scores = getDataset().getCollection().getDifferencesToMedianFromPoint(pointType);
		double result = 0;
		for(double s : scores){
			result += s;
		}
		return result;
	}
			 
}
