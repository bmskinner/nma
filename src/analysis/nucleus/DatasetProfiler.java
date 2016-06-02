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
package analysis.nucleus;

import java.util.List;

import utility.Constants;
import analysis.AnalysisDataset;
import analysis.AnalysisWorker;
import analysis.profiles.ProfileIndexFinder;
import analysis.profiles.RuleSet;
import components.CellCollection;
import components.generic.BorderTag;
import components.generic.Profile;
import components.generic.ProfileCollection;
import components.generic.ProfileType;
import components.nuclei.Nucleus;

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
	 * Create the regular profile collection to hold angles from nuclear
	 * profiles
	 * @return
	 * @throws Exception
	 */
	private void createProfileCollections() {
		CellCollection collection = getDataset().getCollection();

		/*
		 * Build a first set of profile aggregates
		 * Default is to make profile aggregate from reference point
		 * Do not build an aggregate for the non-existent frankenprofile
		 */
		for(ProfileType type : ProfileType.values()){
			
			if(type.equals(ProfileType.FRANKEN)){
				continue;
			}
			
			fine("Creating profile aggregate: "+type);
			ProfileCollection pc = collection.getProfileCollection(type);
			pc.createProfileAggregate(collection, type);
		}
	}
	
//	/**
//	 * Perform a tail finding in the current median profile, and offset
//	 * the nucleus profiles for best fit. Create the profile aggregate fresh.
//	 * The idea here is to refine the median to the best fit across all nuclei. 
//	 * @param pointType
//	 * @param finder
//	 * @return
//	 * @throws Exception
//	 */
////	private double rebuildProfileAggregate(BorderTag pointType, ProfileFeatureFinder finder) throws Exception{
////		// rebuild the aggregate - needed if the orientation point index has changed in any nuclei
////		CellCollection collection = getDataset().getCollection();
////		
////		for(ProfileType type : ProfileType.values()){
////			if(type.equals(ProfileType.FRANKEN)){
////				continue;
////			}
////			fine("Rebuilding profile aggregate: "+type);
////			collection.getProfileCollection(type)
////					.createProfileAggregate(collection, type);
////		}
////
////		// carry out the orientation point detection in the median again
////		finder.findTailIndexInMedianCurve();
////
////		// apply offsets to each nucleus in the collection
////		ProfileOffsetter offsetter = new ProfileOffsetter(collection);
////		offsetter.calculateOffsets(); 
////
////
////
////		// get the difference between aligned profiles and the median
////		double score = compareProfilesToMedian(pointType);
////		fine("Reticulating splines: score: "+(int)score);
////		return score;
////	}
	
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
			createProfileCollections();	
			
			// Set the RP in the ProfileCollection to index zero
			collection.getProfileCollection(ProfileType.REGULAR).addOffset(BorderTag.REFERENCE_POINT, 0);
			
			// Create a median from the current reference points in the nuclei
			Profile median = collection.getProfileCollection(ProfileType.REGULAR)
					.getProfile(BorderTag.REFERENCE_POINT, Constants.MEDIAN);
			
			// RP index is zero in the median profile at this point
			
			// Update the nucleus profiles to the median
			offsetNucleusProfiles(BorderTag.REFERENCE_POINT, ProfileType.REGULAR, median);
			
			// Rebuild the median
			
			{
				// This section is not really necessary
				// check the RP in the median is still at zero
				ProfileIndexFinder finder = new ProfileIndexFinder();
				List<RuleSet> rpSet = collection.getRuleSetCollection().getRuleSets(BorderTag.REFERENCE_POINT);
				int rpIndex = finder.identifyIndex(median, rpSet);
				fine("RP in median is located at index "+rpIndex);
			
			}
			
			
//			// use the median profile of this aggregate to find the orientation point ("tail")
//			ProfileFeatureFinder finder = new ProfileFeatureFinder(collection);
//			finder.findTailIndexInMedianCurve();


			// carry out iterative offsetting to refine the RP
			double score = compareProfilesToMedian(pointType);
			double prevScore = score+1;
			while(score < prevScore){
				
				prevScore = score;
				
				median = collection.getProfileCollection(ProfileType.REGULAR)
						.getProfile(BorderTag.REFERENCE_POINT, Constants.MEDIAN);
				
				// Update the nucleus profiles to the median
				offsetNucleusProfiles(BorderTag.REFERENCE_POINT, ProfileType.REGULAR, median);
				
				// Build the ProfileCollections for each ProfileType
				createProfileCollections();	
				
//				score = rebuildProfileAggregate(pointType, finder);
				score = compareProfilesToMedian(BorderTag.REFERENCE_POINT);
				fine("Reticulating splines: score: "+(int)score);
			}
			
			fine("Identified best RP in nuclei and constructed median profiles");
			
			fine("Identifying OP and other BorderTags");
			
	
			// Identify the border tags in the median profile

			ProfileIndexFinder finder = new ProfileIndexFinder();
			for(BorderTag tag : BorderTag.values() ){
				List<RuleSet> ruleSets = collection.getRuleSetCollection().getRuleSets(tag);
				
				if(ruleSets.size()>0){ // Rules might not all be set
					
					int index = finder.identifyIndex(median, ruleSets);
					// Add the index to the median profiles
					updateProfileCollectionOffsets(tag, index);

					fine(tag+" in median is located at index "+index);
					
					// Create a median from the current reference points in the nuclei
					Profile tagMedian = collection.getProfileCollection(ProfileType.REGULAR)
							.getProfile(tag, Constants.MEDIAN);
					
					offsetNucleusProfiles(tag, ProfileType.REGULAR, tagMedian);
					fine("Assigned offset in nucleus profiles for "+tag);
					
				} else {
					fine("No ruleset for "+tag+"; skipping");
				}
			}
			
			
						

			fine("Finished profiling collection");

		} catch(Exception e){
			error("Error in dataset profiling", e);
		}
	}
	
	/**
	 * Add the given offset to each of the profile types in the ProfileCollection
	 * except for the frankencollection
	 * @param tag
	 * @param index
	 */
	private void updateProfileCollectionOffsets(BorderTag tag, int index){
		
		for(ProfileType type : ProfileType.values()){
			if(type.equals(ProfileType.FRANKEN)){
				continue;
			}
			
			getDataset().getCollection()
				.getProfileCollection(type)
				.addOffset(tag, index);

		}
		
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
	
	
	/**
	 * Update the given tag in each nucleus of the collection to the index with a best fit
	 * of the profile to the given median profile
	 * @param tag
	 * @param type
	 * @param median
	 */
	private void offsetNucleusProfiles(BorderTag tag, ProfileType type, Profile median){
		
		CellCollection collection = this.getDataset().getCollection();
		
		for(Nucleus n : collection.getNuclei()){

			// returns the positive offset index of this profile which best matches the median profile
			
			int newIndex = n.getProfile(type).getSlidingWindowOffset(median);
			n.setBorderTag(tag, newIndex);			
		}
		
	}
		 
}
