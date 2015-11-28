/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
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

import ij.IJ;

import java.util.Arrays;
import java.util.List;

import jdistlib.InvNormal;
import jdistlib.disttest.DistributionTest;
import jdistlib.disttest.NormalityTest;
import components.CellCollection;
import components.generic.BooleanProfile;
import components.generic.BorderTag;
import components.generic.Profile;
import components.generic.ProfileCollectionType;

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
	public static Profile testCollectionGetPValues(CellCollection collection, BorderTag tag, ProfileCollectionType type){
		Profile resultProfile = null;
		
		double[] pvals = null;
		try {
			int offset = collection.getProfileCollection(type).getOffset(tag);
			
			// ensure the postions are starting from the right place
			List<Double> keys = collection.getProfileCollection(type).getAggregate().getXKeyset();

			
			pvals = new double[keys.size()];
			
			for(int i=0; i<keys.size(); i++ ){
				
				double position = keys.get(i);
				try{ 
					double[] values = collection.getProfileCollection(type).getAggregate().getValuesAtPosition(position);

					double pval =  getDipTestPValue(values);
					pvals[i] = pval;

				} catch(Exception e){
//					IJ.log("Cannot get values for position "+position);
					pvals[i] = 1;
				}
			}
			
			resultProfile = new Profile(pvals);
			resultProfile = resultProfile.offset(offset);
			
			
		} catch (Exception e) {
			pvals = new double[100];
			for(int i=0; i<100; i++){
				pvals[i] = 1;
			}
			resultProfile = new Profile(pvals);
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
	public static double getPValueForPositon(CellCollection collection, double xPosition, ProfileCollectionType type) throws Exception {
		
		double[] values = collection.getProfileCollection(type).getAggregate().getValuesAtPosition(xPosition);
		return getDipTestPValue(values);
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
	public static BooleanProfile testCollectionIsUniModal(CellCollection collection, BorderTag tag, double significance, ProfileCollectionType type){
		
		BooleanProfile resultProfile = null;
		boolean[] modes = null;
		try {
			
			Profile pvals = testCollectionGetPValues(collection, tag, type);
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

	/**
	 * Given an array of values, perform a dip test and return the p-value.
	 * If the array size is <10, returns 1.
	 * @param values
	 * @return
	 */
	public static double getDipTestPValue(double[] values){
		
		if(values.length<10){
			return 1;
		} else {
			double[] result = DistributionTest.diptest(values);
			return result[1];
		}
	}
	
	/**
	 * Given an array of values, perform a dip test and return the test statistic
	 * @param values
	 * @return
	 */
	public static double getDipTestTestStatistic(double[] values){
		if(values.length<10){
			return 1;
		} else {
			double[] result = DistributionTest.diptest(values);
			return result[0];
		}
		
	}
	
	public static double getShapiroWilkStatistic(double[] values){
		return NormalityTest.shapiro_wilk_statistic(values);
	}
	
	public static double getShapiroWilkPValue(double[] values){
		return NormalityTest.shapiro_wilk_pvalue(NormalityTest.shapiro_wilk_statistic(values), values.length) ;
	}
 	
	public static double getInvNormProbabililty(double p){
		InvNormal dist = new InvNormal(0, 1);
		return dist.cumulative(p);
	}

}
