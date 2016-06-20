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

import java.util.List;

import utility.Constants;
import analysis.AnalysisDataset;
import analysis.AnalysisWorker;
import components.CellCollection;
import components.generic.BorderTag;
import components.generic.Profile;
import components.generic.ProfileType;

/**
 * This class contains the methods for detecting the reference and orientation points in a median
 * profile, creating the median profile, and adjusting individual nuclei border points to fit the median.
 * @author ben
 *
 */
public class DatasetProfiler extends AnalysisWorker {
	
	private static final BorderTag DEFAULT_BORDER_TAG = BorderTag.REFERENCE_POINT;

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
			
			// Set the RP in the ProfileCollection to index zero
			// Each nucleus currently has a best-guess RP and nothing else
			collection.getProfileCollection(ProfileType.REGULAR)
				.addOffset(BorderTag.REFERENCE_POINT, 0);
			
			// Create a median from the current reference points in the nuclei
			Profile median = collection.getProfileCollection(ProfileType.REGULAR)
					.getProfile(BorderTag.REFERENCE_POINT, Constants.MEDIAN);
			
			// RP index *should be* zero in the median profile at this point
			// Check this before updating nuclei
			ProfileIndexFinder finder = new ProfileIndexFinder();
			int rpIndex = finder.identifyIndex(collection, BorderTag.REFERENCE_POINT);
			fine("RP in default median is located at index "+rpIndex);
			
			// Update the nucleus profiles to best fit the median
			collection.getProfileManager()
				.offsetNucleusProfiles(BorderTag.REFERENCE_POINT, ProfileType.REGULAR, median);
			
			// Now each nucleus should be at the best fit to the median profile from the RP
			// Rebuilding the median may cause the RP index to change in the median
			
			while(rpIndex != 0){
				
				// Rebuild the median and offset the nuclei until the RP settles at zero
				rpIndex = coerceRPToZero(collection);
			}
			
//			{
//				// check the RP in the median is still at zero
//				rpIndex = finder.identifyIndex(collection, BorderTag.REFERENCE_POINT);
//				fine("RP in new median is located at index "+rpIndex);
//
//				// If RP is not at zero, update
//				if(rpIndex!=0){
//					fine("RP in median is not yet at zero");
//					collection.getProfileManager()
//						.updateProfileCollectionOffsets(BorderTag.REFERENCE_POINT, rpIndex);
//					finer("Changed RP index in median to "+rpIndex);
//					
//					// Get the median again, using the new RP
//					median = collection.getProfileCollection(ProfileType.REGULAR)
//							.getProfile(BorderTag.REFERENCE_POINT, Constants.MEDIAN);
//					
//					// Update the nuclei
//					collection.getProfileManager()
//						.offsetNucleusProfiles(BorderTag.REFERENCE_POINT, ProfileType.REGULAR, median);
//					
//					rpIndex = finder.identifyIndex(collection, BorderTag.REFERENCE_POINT);
//					fine("RP in median is now located at index "+rpIndex);
//				}
//			
//			}
			

			// carry out iterative offsetting to refine the RP
			double score = compareProfilesToMedian(pointType);
			double prevScore = score+1;
			while(score < prevScore){
				
				prevScore = score;
				
				median = collection.getProfileCollection(ProfileType.REGULAR)
						.getProfile(BorderTag.REFERENCE_POINT, Constants.MEDIAN);
				
				// Update the nucleus profiles to the median
				collection.getProfileManager()
					.offsetNucleusProfiles(BorderTag.REFERENCE_POINT, ProfileType.REGULAR, median);
				
				// Build the ProfileCollections for each ProfileType
				collection.getProfileManager().createProfileCollections();	
				
//				score = rebuildProfileAggregate(pointType, finder);
				score = compareProfilesToMedian(BorderTag.REFERENCE_POINT);
				fine("Reticulating splines: score: "+(int)score);
			}
			

			fine("Identified best RP in nuclei and constructed median profiles");
			
			fine("Identifying OP and other BorderTags");
			
	
			// Identify the border tags in the median profile

			for(BorderTag tag : BorderTag.values() ){
				
				// Don't identify the RP again, it could cause off-by-one errors
				// We do need to assign the RP in other ProfileTypes though
				if(tag.equals(BorderTag.REFERENCE_POINT)){
					
					fine("Checking location of RP in profile");
					int index = finder.identifyIndex(collection, tag);
					
					fine("RP is found at index "+index);

					if(index!=0){
						collection.getProfileManager()
						.updateProfileCollectionOffsets(tag, 0);
						fine("Forcing RP to index zero");
					}
					continue; 
				}
				
					
				int index = finder.identifyIndex(collection, tag);

				if( index > -2){ // Ruleset was applied
					
					if( index == -1){
						warn("Unable to detect "+tag+" using default ruleset");
						warn("Falling back on reference point");
						index = 0;
						
					}
										
					// Add the index to the median profiles
					collection.getProfileManager()
						.updateProfileCollectionOffsets(tag, index);

					fine(tag+" in median is located at index "+index);

					// Create a median from the current reference points in the nuclei
					Profile tagMedian = collection.getProfileCollection(ProfileType.REGULAR)
							.getProfile(tag, Constants.MEDIAN);

					collection.getProfileManager()
						.offsetNucleusProfiles(tag, ProfileType.REGULAR, tagMedian);
					fine("Assigned offset in nucleus profiles for "+tag);
					

				} else {
					fine("No ruleset for "+tag+" or index not found; skipping");
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
			collection.getProfileManager()
				.updateProfileCollectionOffsets(BorderTag.REFERENCE_POINT, rpIndex);
			finer("Changed RP index in median to "+rpIndex);

			// Get the median again, using the new RP
			Profile median = collection.getProfileCollection(ProfileType.REGULAR)
					.getProfile(BorderTag.REFERENCE_POINT, Constants.MEDIAN);

			finer("Rebuilt the profile collection");
			
			// Update the nuclei
			collection.getProfileManager()
				.offsetNucleusProfiles(BorderTag.REFERENCE_POINT, ProfileType.REGULAR, median);
			finer("Offset the nuclei");
			finer("Checking RP index again");

			rpIndex = finder.identifyIndex(collection, BorderTag.REFERENCE_POINT);
			fine("RP in median is now located at index "+rpIndex);
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
