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

import java.util.logging.Level;
import java.util.logging.Logger;

import analysis.AnalysisDataset;
import analysis.AnalysisWorker;
import components.AbstractCellularComponent;
import components.CellCollection;
import components.generic.BooleanProfile;
import components.generic.BorderTag;
import components.generic.Profile;
import components.generic.ProfileCollection;
import components.generic.ProfileType;
import components.generic.XYPoint;
import components.nuclei.Nucleus;
import components.nuclei.sperm.PigSpermNucleus;
import components.nuclei.sperm.RodentSpermNucleus;
import utility.Constants;
import utility.Utils;

/**
 * This class contains the methods for detecting the reference and orientation points in a median
 * profile, creating the median profile, and adjusting individual nuclei border points to fit the median.
 * @author ben
 *
 */
public class DatasetProfiler extends AnalysisWorker {

	public DatasetProfiler(AnalysisDataset dataset){
		super(dataset);
	}
	 
	@Override
    protected Boolean doInBackground() throws Exception {
    	
    	boolean result = true;
		try{


				log(Level.FINE, "Profiling dataset");

//				this.setProgressTotal(3);

				BorderTag pointType = BorderTag.REFERENCE_POINT;

				// profile the collection from head/tip, then apply to tail
				runProfiler(pointType);

				log(Level.FINE, "Datset profiling complete");
			
			
		} catch(Exception e){
			
			logError("Error in dataset profiling", e);
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
			log(Level.FINE,  "Profiling collection");
			
			CellCollection collection = getDataset().getCollection();
			
			
			/*
			 * Build a first set of profile aggregates
			 * Default is to make profile aggregate from reference point
			 */
			for(ProfileType type : ProfileType.values()){
				if(type.equals(ProfileType.FRANKEN)){
					continue;
				}
				log(Level.FINE, "Creating initial profile aggregate: "+type);
				ProfileCollection pc = collection.getProfileCollection(type);
				pc.createProfileAggregate(collection, type);
			}
			
			// Use the angle profiles to identify features
			ProfileCollection angleCollection = collection.getProfileCollection(ProfileType.REGULAR);			
			
//			log(Level.INFO, "Angle aggregate first:");
//			log(Level.INFO, angleCollection.getAggregate().toString());
//			publish(1);

			// use the median profile of this aggregate to find the orientation point ("tail")
			ProfileFeatureFinder finder = new ProfileFeatureFinder(collection);
			finder.findTailIndexInMedianCurve();
//			publish(2);

			// carry out iterative offsetting to refine the orientation point estimate
			double score = compareProfilesToMedian(collection, pointType);
			double prevScore = score*2;
			while(score < prevScore){

				// rebuild the aggregate - needed if the orientation point index has changed in any nuclei
//				angleCollection.createProfileAggregate(collection, ProfileType.REGULAR);
				
				for(ProfileType type : ProfileType.values()){
					if(type.equals(ProfileType.FRANKEN)){
						continue;
					}
					log(Level.FINE, "Rebuilding profile aggregate: "+type);
					collection.getProfileCollection(type)
							.createProfileAggregate(collection, type);
				}

				// carry out the orientation point detection in the median again
				finder.findTailIndexInMedianCurve();

				// apply offsets to each nucleus in the collection
				ProfileOffsetter offsetter = new ProfileOffsetter(collection);
				offsetter.calculateOffsets(); 

				prevScore = score;

				// get the difference between aligned profiles and the median
				score = compareProfilesToMedian(collection, pointType);
				log(Level.FINE, "Reticulating splines: score: "+(int)score);
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
				log(Level.FINE, "Adding offsets for profile "+type);
				ProfileCollection pc = collection.getProfileCollection(type);
				for(BorderTag tag : angleCollection.getOffsetKeys()){
					log(Level.FINE, "Added offset for "+tag);
					pc.addOffset(tag, angleCollection.getOffset(tag));
				}
			}
						
//			publish(3);
			log(Level.FINE,  "Finished profiling collection; median generated");

		} catch(Exception e){
			logError("Error in morphology profiling", e);
		}
	}
	
	/**
	 * Get the total differences to the median for all the nuclei in
	 * the collection
	 * @param collection
	 * @param pointType
	 * @return
	 */
	private static double compareProfilesToMedian(CellCollection collection, BorderTag pointType) throws Exception {
		double[] scores = collection.getDifferencesToMedianFromPoint(pointType);
		double result = 0;
		for(double s : scores){
			result += s;
		}
		return result;
	}
		 
}
