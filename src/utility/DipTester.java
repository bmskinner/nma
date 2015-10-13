package utility;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import utility.Constants.BorderTag;
import ij.IJ;
import components.CellCollection;
import components.generic.BooleanProfile;
import components.generic.Profile;
import jdistlib.disttest.DistributionTest;

/**
 * The purpose is to test the difference at a particular point of a
 * median profile in a collection; for each nucleus in the collection,
 * what is the difference to the median at that point? Is the list of
 * differences bimodal?
 */
public class DipTester {
	
	/**
	 * Test the given collection for non-unimodality at each point in the profile,
	 * using Hartigan's Dip Test. Returns a profile with the dip test p-values
	 * at each point
	 * @param collection the collection of nuclei
	 * @param tag the border tag to offset from
	 * @return a boolean profile of results
	 */
	public static Profile testCollectionGetPValues(CellCollection collection, BorderTag tag){
		Profile resultProfile = null;
		
		double[] pvals = null;
		try {
			String pointType = collection.getPoint(tag);
			int offset = collection.getProfileCollection().getOffset(pointType);
			
			// ensure the postions are starting from the right place
			List<Double> keys = collection.getProfileCollection().getAggregate().getXKeyset();

			
			pvals = new double[keys.size()];
			
			for(int i=0; i<keys.size(); i++ ){
				
				double position = keys.get(i);
				try{ 
					double[] values = collection.getProfileCollection().getAggregate().getValuesAtPosition(position);
					Arrays.sort(values);

					double[] result = DistributionTest.diptest_presorted(values);
					
					pvals[i] = result[1];

				} catch(Exception e){
					IJ.log("Cannot get values for position "+position);
				}
			}
			
			resultProfile = new Profile(pvals);
			resultProfile = resultProfile.offset(offset);
			
			
		} catch (Exception e) {
			IJ.log("Error in dip test: "+e.getMessage());
			for(StackTraceElement e1 : e.getStackTrace()){
				IJ.log(e1.toString());
			}
		}
		return resultProfile;
	}
	
	/**
	 * Get the p-value for a Dip Test at the given x position in the angle profile
	 * @param collection
	 * @param xPosition
	 * @return
	 * @throws Exception
	 */
	public static double getPValueForPositon(CellCollection collection, double xPosition) throws Exception {
		
		double[] values = collection.getProfileCollection().getAggregate().getValuesAtPosition(xPosition);
		Arrays.sort(values);
		double[] result = DistributionTest.diptest_presorted(values);
		return result[1];
	}
	
	
	/**
	 * Test the given collection for non-unimodality at each point in the profile,
	 * using Hartigan's Dip Test. Returns a boolean profile with the points at which 
	 * the dip test p-value is less than the given significance level
	 * @param collection the collection of nuclei
	 * @param tag the border tag to offset from
	 * @param significance the p-value threshold
	 * @return a boolean profile of results
	 */
	public static BooleanProfile testCollectionGetIsNotUniModal(CellCollection collection, BorderTag tag, double significance){
		
		BooleanProfile resultProfile = null;
		boolean[] modes = null;
		try {
			
			Profile pvals = testCollectionGetPValues(collection, tag);
			modes = new boolean[pvals.size()];
			
			for(int i=0; i<pvals.size(); i++ ){
				
				if(pvals.get(i)<significance){
					modes[i] = true;
				} else {
					modes[i] = false;
				}
				
			}
			resultProfile = new BooleanProfile(modes);
		} catch (Exception e) {
			IJ.log("Error in dip test: "+e.getMessage());
			for(StackTraceElement e1 : e.getStackTrace()){
				IJ.log(e1.toString());
			}
		}
		return resultProfile;
	}
////			IJ.log("Testing at p<"+significance);
//			String pointType = collection.getPoint(tag);
//			int offset = collection.getProfileCollection().getOffset(pointType);
//			
//			// ensure the postions are starting from the right place
//			List<Double> keys = collection.getProfileCollection().getAggregate().getXKeyset();
////			Double[] keyArray = keys.toArray(new Double[0]);
//			
//			modes = new boolean[keys.size()];
//			
//			for(int i=0; i<keys.size(); i++ ){
//				
//				double position = keys.get(i);
//				try{ 
//					double[] values = collection.getProfileCollection().getAggregate().getValuesAtPosition(position);
//					Arrays.sort(values);
//
//					double[] result = DistributionTest.diptest_presorted(values);
//					
////					IJ.log(position+"    "+result[0]+"    "+result[1]);
//
//					if(result[1]<significance){
//						modes[i] = true;
//					} else {
//						modes[i] = false;
//					}
//				} catch(Exception e){
//					modes[i] = false;
//					IJ.log("Cannot get values for position "+position);
//				}
//			}
//			
//			resultProfile = new BooleanProfile(modes);
//			resultProfile = resultProfile.offset(offset);
			
			
//		} catch (Exception e) {
//			IJ.log("Error in dip test: "+e.getMessage());
//			for(StackTraceElement e1 : e.getStackTrace()){
//				IJ.log(e1.toString());
//			}
//		}
//		return resultProfile;
//		
//	}

}
