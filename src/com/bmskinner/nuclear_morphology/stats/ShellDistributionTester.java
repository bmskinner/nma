/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.stats;

import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult.Aggregation;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult.Normalisation;

/**
 * Compare shell distributions between shell results
 * @author ben
 * @since 1.14.0
 *
 */
public class ShellDistributionTester {
	
	private final IShellResult observed;
	private final IShellResult expected;
	
	/**
	 * A tuple to return chi square test results 
	 * @author ben
	 * @since 1.14.0
	 *
	 */
	public class ChiSquareResult{
		private final double chi, pval;
		public ChiSquareResult(double chi, double pval) {
			this.chi = chi;
			this.pval = pval;
		}
		
		public double getChi() {
			return chi;
		}
		
		public double getPValue() {
			return pval;
		}
	}
	
	/**
	 * Construct with two shell results to compare
	 * @param observed
	 * @param expected
	 */
	public ShellDistributionTester(@NonNull final IShellResult observed, @NonNull final IShellResult expected) {
		this.observed = observed;
		this.expected = expected;
	}
	
	/**
	 * Test the shell results for the given parameters
	 * @param agg
	 * @param norm
	 * @return
	 */
	public ChiSquareResult test(@NonNull Aggregation agg, @NonNull Normalisation norm) {
		double chi = getChiSquareValue(agg, norm);
		double p   = getPValue(agg, norm);
		if(Double.isNaN(p))
			p=1;
		return new ChiSquareResult(chi, p);
	}

	private double getChiSquareValue(@NonNull Aggregation agg, @NonNull Normalisation norm) {
		long[] obs = observed.getAggregateCounts(agg, norm);
		double[] other = expected.getProportions(agg, norm);
		double[] exp = getExpected(agg, norm, other, observed.getNumberOfSignals(agg));

		for(double d : exp){
			if(d<=0) // we can't do a chi square test if one of the values is zero
				return 1;
		}

		ChiSquareTest test = new ChiSquareTest();
		return test.chiSquare(exp, obs);
	}


	private double getPValue(@NonNull Aggregation agg, @NonNull Normalisation norm) {
		long[] obs = observed.getAggregateCounts(agg, norm);
		double[] other = expected.getProportions(agg, norm);	
		double[] exp   = getExpected(agg, norm, other, observed.getNumberOfSignals(agg));

		for(double d : exp){
			if(d<=0) // we can't do a chi square test if one of the values is zero
				return 1;
		}

		ChiSquareTest test = new ChiSquareTest();
		return test.chiSquareTest(exp, obs);
	}
        

    /**
     * Get the expected values for the chi-sqare test    
     * @param agg the aggregation
     * @param norm the normalisation
     * @param other the other shell proportions
     * @param nObjects the number of objects to be tested 
     * @return
     */
    private double[] getExpected(@NonNull Aggregation agg, @NonNull Normalisation norm, double[] other, int nObjects) {
        double[] exp = new double[other.length];
        for (int i=0; i<other.length; i++) {
        	exp[i] = other[i] * nObjects;
        }
        return exp;
    }

}
