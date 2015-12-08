package stats;

import ij.IJ;

import java.util.List;

import jdistlib.disttest.DistributionTest;
import analysis.AnalysisDataset;
import components.CellCollection;
import components.generic.BorderTag;
import components.generic.Profile;
import components.generic.ProfileCollectionType;

public class KruskalTester {
	
	public static Profile testCollectionGetPValues(AnalysisDataset one, AnalysisDataset two, BorderTag tag, ProfileCollectionType type){
		
		Profile resultProfile = null;
		int sampleNumber = 300;
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
}
