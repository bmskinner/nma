package stats;

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
		
		double[] pvals = null;
		try {
//			int offsetOne = one.getCollection().getProfileCollection(type).getOffset(tag);
//			int offsetTwo = two.getCollection().getProfileCollection(type).getOffset(tag);
			
			// ensure the postions are starting from the right place
//			List<Double> keysOne = one.getCollection().getProfileCollection(type).getAggregate().getXKeyset();
//			List<Double> keysTwo = two.getCollection().getProfileCollection(type).getAggregate().getXKeyset();

			
			pvals = new double[200];
			
			for(int i=0; i<200; i++ ){
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
					for(int j=valuesOne.length; j<valuesTwo.length; j++){
						valuesCombined[j] = valuesTwo[j-valuesOne.length];
						groupsCombined[j] = 2;
					}
					
					double[] pval = DistributionTest.kruskal_wallis_test(valuesCombined, groupsCombined);
					pvals[i] = pval[1];
//
				} catch(Exception e){
//					IJ.log("Cannot get values for position "+position);
					pvals[i] = 1;
				}
			}
//			
			resultProfile = new Profile(pvals);
//			resultProfile = resultProfile.offset(offset);
			
			
		} catch (Exception e) {
			pvals = new double[100];
			for(int i=0; i<200; i++){
				pvals[i] = 1;
			}
			resultProfile = new Profile(pvals);
		}
		return resultProfile;
	}
}
