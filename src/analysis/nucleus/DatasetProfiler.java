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

import analysis.AnalysisDataset;
import analysis.AnalysisWorker;
import components.CellCollection;
import components.generic.BorderTag;
import components.generic.ProfileCollection;
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
	 * Create the regular profile collection to hold angles from nuclear
	 * profiles
	 * @return
	 * @throws Exception
	 */
	private ProfileCollection createProfileCollection() throws Exception{
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
			fine("Creating initial profile aggregate: "+type);
			ProfileCollection pc = collection.getProfileCollection(type);
			pc.createProfileAggregate(collection, type);
		}
		
		// Use the angle profiles to identify features
		ProfileCollection pc = collection.getProfileCollection(ProfileType.REGULAR);	
		return pc;
	}
	
	/**
	 * Perform a tail finding in the current median profile, and offset
	 * the nucleus profiles for best fit. Create the profile aggregate fresh.
	 * The idea here is to refine the median to the best fit across all nuclei. 
	 * @param pointType
	 * @param finder
	 * @return
	 * @throws Exception
	 */
	private double rebuildProfileAggregate(BorderTag pointType, ProfileFeatureFinder finder) throws Exception{
		// rebuild the aggregate - needed if the orientation point index has changed in any nuclei
		CellCollection collection = getDataset().getCollection();
		
		for(ProfileType type : ProfileType.values()){
			if(type.equals(ProfileType.FRANKEN)){
				continue;
			}
			fine("Rebuilding profile aggregate: "+type);
			collection.getProfileCollection(type)
					.createProfileAggregate(collection, type);
		}

		// carry out the orientation point detection in the median again
		finder.findTailIndexInMedianCurve();

		// apply offsets to each nucleus in the collection
		ProfileOffsetter offsetter = new ProfileOffsetter(collection);
		offsetter.calculateOffsets(); 



		// get the difference between aligned profiles and the median
		double score = compareProfilesToMedian(pointType);
		fine("Reticulating splines: score: "+(int)score);
		return score;
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
			
			CellCollection collection = getDataset().getCollection();

			ProfileCollection angleCollection = createProfileCollection();		
			

			// use the median profile of this aggregate to find the orientation point ("tail")
			ProfileFeatureFinder finder = new ProfileFeatureFinder(collection);
			finder.findTailIndexInMedianCurve();


			// carry out iterative offsetting to refine the orientation point estimate
			double score = compareProfilesToMedian(pointType);
			double prevScore = score*2;
			while(score < prevScore){
				
				prevScore = score;
				
				score = rebuildProfileAggregate(pointType, finder);
			}
			
			/*
			 * Update the distance profiles using the offsets from the
			 * angle profile
			 * 
			 */
			
			for(ProfileType type : ProfileType.values()){
				if(type.equals(ProfileType.FRANKEN)){
					continue;
				}
				fine("Adding offsets for profile "+type);
				ProfileCollection pc = collection.getProfileCollection(type);
				for(BorderTag tag : angleCollection.getOffsetKeys()){
					fine("Added offset for "+tag);
					pc.addOffset(tag, angleCollection.getOffset(tag));
				}
			}
						

			fine("Finished profiling collection; median generated");

		} catch(Exception e){
			error("Error in morphology profiling", e);
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
		 
}
