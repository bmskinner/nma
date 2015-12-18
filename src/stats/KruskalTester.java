package stats;

import ij.IJ;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import jdistlib.disttest.DistributionTest;
import analysis.AnalysisDataset;
import analysis.nucleus.SegmentFitter;
import components.CellCollection;
import components.generic.BorderTag;
import components.generic.Profile;
import components.generic.ProfileCollection;
import components.generic.ProfileCollectionType;
import components.generic.SegmentedProfile;
import components.nuclear.NucleusBorderSegment;
import components.nuclei.Nucleus;

public class KruskalTester {
	
	public static Profile testCollectionGetPValues(AnalysisDataset one, AnalysisDataset two, BorderTag tag, ProfileCollectionType type){
		
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
					pvals[i] = pval[1] * sampleNumber > 1 ? 1 : pval[1] * sampleNumber;
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
	
public static Profile testCollectionGetFrankenPValues(AnalysisDataset one, AnalysisDataset two, BorderTag tag, ProfileCollectionType type){
		
		Profile resultProfile = null;
		int sampleNumber = 200;
		double[] pvals = null;
		try {
			
			/*
			 * This is taken from MorphologyAnalysis
			 */
			ProfileCollection pc = one.getCollection().getProfileCollection(ProfileCollectionType.REGULAR);
			List<NucleusBorderSegment> segments = pc.getSegments(pointType);
			ProfileCollection frankenCollection = new ProfileCollection();
			for(BorderTag key : pc.getOffsetKeys()){
				int offset = pc.getOffset(key);
				frankenCollection.addOffset(key, pc.getOffset(key));
			}
			frankenCollection.addSegments(tag, segments);

			SegmentedProfile medianProfile = pc.getSegmentedProfile(tag);
			SegmentFitter fitter = new SegmentFitter(medianProfile, fileLogger);
			List<Profile> frankenProfiles = new ArrayList<Profile>(0);

			int count = 1;
			for(Nucleus n : two.getCollection().getNuclei()){ 
				fitter.fit(n, pc);

				// recombine the segments at the lengths of the median profile segments
				Profile recombinedProfile = fitter.recombine(n, BorderTag.REFERENCE_POINT);
				frankenProfiles.add(recombinedProfile);
			}

			// add all the nucleus frankenprofiles to the frankencollection
			frankenCollection.addNucleusProfiles(frankenProfiles);

			// update the profile aggregate
			frankenCollection.createProfileAggregateFromInternalProfiles((int)pc.getAggregate().length());
			
			
			pvals = new double[sampleNumber];
			
			for(int i=0; i<sampleNumber; i++ ){
//				
				double position = ( (double) i / 2d);
				try{ 
					double[] valuesOne = one.getCollection().getProfileCollection(type).getAggregate().getValuesAtPosition(position);
					double[] valuesTwo = frankenCollection.getAggregate().getValuesAtPosition(position);
					
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
					pvals[i] = pval[1] * sampleNumber > 1 ? 1 : pval[1] * sampleNumber;
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
}
