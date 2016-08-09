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
package stats;

import java.util.logging.Level;

import charting.options.ChartOptions;
import jdistlib.disttest.DistributionTest;
import analysis.AnalysisDataset;
import analysis.profiles.DatasetSegmenter;
import analysis.profiles.ProfileManager;
import analysis.profiles.SegmentFitter;
import analysis.profiles.DatasetSegmenter.MorphologyAnalysisMode;
import components.generic.BorderTag;
import components.generic.BorderTagObject;
import components.generic.Profile;
import components.generic.ProfileCollection;
import components.generic.ProfileType;
import components.generic.SegmentedProfile;
import components.nuclei.Nucleus;

public class KruskalTester {
	
	public static Profile testCollectionGetPValues(AnalysisDataset one, AnalysisDataset two, BorderTagObject tag, ProfileType type){
		
		Profile resultProfile = null;
		int sampleNumber = 200;
		double[] pvals = null;
		try {
			
			
			pvals = new double[sampleNumber];
			
			for(int i=0; i<sampleNumber; i++ ){
//				
				double position = ( (double) i / 2d);
				try{ 
					double[] valuesOne = one.getCollection().getProfileCollection(type).getAggregate().getValuesAtPosition(position);
					double[] valuesTwo = two.getCollection().getProfileCollection(type).getAggregate().getValuesAtPosition(position);
					
					
					double pval = calculateKruskalPValue(valuesOne, valuesTwo);
					
					// Perform Bonferroni correction with hard upper limit of p=1
					pvals[i] = pval * sampleNumber > 1 ? 1 : pval * sampleNumber;
					//
				} catch(Exception e){

					pvals[i] = 1;
				}
			}
//			
			resultProfile = new Profile(pvals);

			
			
		} catch (Exception e) {
			pvals = new double[sampleNumber];
			for(int i=0; i<sampleNumber; i++){
				pvals[i] = 1;
			}
			resultProfile = new Profile(pvals);
		}
		return resultProfile;
	}
	
	/**
	 * Run a Kruskal-Wallis test at each position along the normalised angle profiles
	 * for the two given datasets, starting at the given tag.
	 * 
	 * This compares profile one to frankenprofile for two, using the segment pattern in one
	 * @param one
	 * @param two
	 * @param tag
	 * @return
	 */
	public static Profile testCollectionGetFrankenPValues(ChartOptions options){
		
		AnalysisDataset one = options.getDatasets().get(0);
		AnalysisDataset two = options.getDatasets().get(1);
		
		Profile resultProfile = null;
		int sampleNumber = 200;
		double[] pvals = null;
		try {
			
			/*
			 * Make a copy of dataset two, so the nuclei segments are not overwritten
			 */
			AnalysisDataset copyOfTwo = two.duplicate();
			
			/*
			 * Ensure that the profile collections have the same lengths in each collection
			 */
			one.getCollection().getProfileManager().copyCollectionOffsets(copyOfTwo.getCollection());
			
			/*
			 * This is taken from MorphologyAnalysis.
			 * 
			 * Create a new ProfileCollection based on the segments from dataset one
			 */
			ProfileCollection pc = one.getCollection().getProfileCollection(ProfileType.ANGLE);
//			List<NucleusBorderSegment> segments = pc.getSegments(tag);
//			ProfileCollection frankenCollection = new ProfileCollection();
			
			
			/*
			 * Create a segmenter just to access the segmnet fitter. Do not execute the analysis function 
			 * in the segmenter
			 */
			SegmentedProfile medianProfile = pc.getSegmentedProfile(options.getTag());
//			DatasetSegmenter segmenter = new DatasetSegmenter(one, MorphologyAnalysisMode.NEW, options.getLogger());
			SegmentFitter fitter = new SegmentFitter(medianProfile);
			
			for(Nucleus n : copyOfTwo.getCollection().getNuclei()){ 
				fitter.fit(n, pc);

				// recombine the segments at the lengths of the median profile segments
				Profile recombinedProfile = fitter.recombine(n, BorderTagObject.REFERENCE_POINT);
				n.setProfile(ProfileType.FRANKEN, new SegmentedProfile(recombinedProfile));
			}
			
			ProfileCollection frankenCollection = copyOfTwo.getCollection().getProfileCollection(ProfileType.FRANKEN);
			frankenCollection.createProfileAggregate(copyOfTwo.getCollection(), ProfileType.FRANKEN, (int) one.getCollection().getMedianArrayLength());
						
			/*
			 * This returns to the Kruskal test above, but using the franken profiles 
			 */
			
			resultProfile = testCollectionGetPValues(one, copyOfTwo, options.getTag(), ProfileType.FRANKEN);		
			
		} catch (Exception e) {
			options.log(Level.SEVERE, "Error in franken profiling", e);
			
			pvals = new double[sampleNumber];
			for(int i=0; i<sampleNumber; i++){
				pvals[i] = 1;
			}
			resultProfile = new Profile(pvals);
		}
		return resultProfile;
	}

	/**
	 * Calculate the Kruskal-Wallis pvalues between two groups
	 * @param valuesOne
	 * @param valuesTwo
	 * @return
	 */
	private static double calculateKruskalPValue(double[] valuesOne, double[] valuesTwo){

		// The values must be combined to a group array
		double[] valuesCombined = new double[valuesOne.length+valuesTwo.length];
		int[] groupsCombined = new int[valuesOne.length+valuesTwo.length];

		for(int j=0; j<valuesOne.length; j++){
			valuesCombined[j] = valuesOne[j];
			groupsCombined[j] = 1;
		}
		for(int j=valuesOne.length; j<valuesCombined.length; j++){
			valuesCombined[j] = valuesTwo[j-valuesOne.length];
			groupsCombined[j] = 2;
		}

		double[] pval = DistributionTest.kruskal_wallis_test(valuesCombined, groupsCombined);

		return pval[1];
	
	}
}
