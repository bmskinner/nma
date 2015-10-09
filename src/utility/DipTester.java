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
	
	
	public static BooleanProfile testCollection(CellCollection collection, BorderTag tag, double significance){
		
		BooleanProfile resultProfile = null;
		boolean[] modes = null;
		try {
			
//			IJ.log("Testing at p<"+significance);
			String pointType = collection.getPoint(tag);
			int offset = collection.getProfileCollection().getOffset(pointType);
			
			// ensure the postions are starting from the right place
			List<Double> keys = collection.getProfileCollection().getAggregate().getXKeyset();
//			Double[] keyArray = keys.toArray(new Double[0]);
			
			modes = new boolean[keys.size()];
			
			for(int i=0; i<keys.size(); i++ ){
				
				double position = keys.get(i);
				try{ 
					double[] values = collection.getProfileCollection().getAggregate().getValuesAtPosition(position);
					Arrays.sort(values);

					double[] result = DistributionTest.diptest_presorted(values);
					
//					IJ.log(position+"    "+result[0]+"    "+result[1]);

					if(result[1]<significance){
						modes[i] = true;
					} else {
						modes[i] = false;
					}
				} catch(Exception e){
					modes[i] = false;
					IJ.log("Cannot get values for position "+position);
				}
			}
			
			resultProfile = new BooleanProfile(modes);
			resultProfile = resultProfile.offset(offset);
			
			
		} catch (Exception e) {
			IJ.log("Error in dip test: "+e.getMessage());
			for(StackTraceElement e1 : e.getStackTrace()){
				IJ.log(e1.toString());
			}
		}
		return resultProfile;
		
	}
	
	public static double[] dipTest(double[] values){
				
		double[] result = DistributionTest.diptest(values);
		return result;
	}

}
