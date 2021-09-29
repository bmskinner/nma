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

import java.util.logging.Logger;
import java.util.stream.IntStream;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
import com.bmskinner.nuclear_morphology.components.profiles.BooleanProfile;
import com.bmskinner.nuclear_morphology.components.profiles.FloatProfile;
import com.bmskinner.nuclear_morphology.components.profiles.IProfile;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import jdistlib.InvNormal;
import jdistlib.disttest.DistributionTest;
import jdistlib.disttest.NormalityTest;

/**
 * The purpose is to test the difference at a particular point of a median
 * profile in a collection; for each nucleus in the collection, what is the
 * difference to the median at that point? Is the list of differences bimodal?
 */
public class DipTester implements SignificanceTest {
	
	private static final Logger LOGGER = Logger.getLogger(DipTester.class.getName());

    private ICellCollection collection;

    public DipTester(ICellCollection collection) {
        this.collection = collection;
    }

    /**
     * Test the given collection for non-unimodality at each point in the
     * profile, using Hartigan's Dip Test. Returns a profile with the dip test
     * p-values at each point
     * 
     * @param collection
     *            the collection of nuclei
     * @param tag
     *            the border tag to offset from
     * @return a profile of results
     */
    public IProfile testCollectionGetPValues(Landmark tag, ProfileType type) {
        IProfile resultProfile = null;

        float[] pvals = null;
        try {
            int offset = collection.getProfileCollection().getIndex(tag);

            // ensure the postions are starting from the right place
            int length = collection.getProfileCollection().length();

            double[] keys = IntStream.range(0, length).mapToDouble(i-> (double)i/(double)length).toArray();

            pvals = new float[keys.length];

            for (int i = 0; i < keys.length; i++) {
                double position = keys[i];
                double[] values = collection.getProfileCollection().getValuesAtPosition(type, position);
                pvals[i] = (float) getDipTestPValue(values);
            }

            resultProfile = new FloatProfile(pvals);
            resultProfile = resultProfile.offset(offset);
        } catch (ProfileException | UnavailableBorderTagException
                | UnavailableProfileTypeException e) {
            LOGGER.log(Loggable.STACK, "Error converting values or offsetting profile", e);
            resultProfile = createErrorPValueProfile();

        }
        return resultProfile;
    }

    private IProfile createErrorPValueProfile() {
        float[] pvals = new float[100];
        for (int i = 0; i < 100; i++) {
            pvals[i] = 1;
        }
        return new FloatProfile(pvals);
    }

    /**
     * Get the p-value for a Dip Test at the given x position in the angle
     * profile
     * 
     * @param collection
     * @param xPosition
     *            the position between zero and one along the profile
     * @return
     * @throws UnavailableProfileTypeException
     * @throws Exception
     */
    public double getPValueForPositon(double xPosition, ProfileType type) throws UnavailableProfileTypeException {

        double[] values = collection.getProfileCollection().getValuesAtPosition(type, xPosition);
        return getDipTestPValue(values);
    }

    /**
     * Test the given collection for non-unimodality at each point in the
     * profile, using Hartigan's Dip Test. Returns a boolean profile with the
     * points at which the dip test p-value is less than the given significance
     * level
     * 
     * @param tag
     *            the border tag to offset from
     * @param significance
     *            the p-value threshold
     * @return a boolean profile of results
     */
    public BooleanProfile testCollectionIsUniModal(Landmark tag, double significance, ProfileType type) {

        IProfile pvals = testCollectionGetPValues(tag, type);
        boolean[]  modes = new boolean[pvals.size()];

        for (int i : pvals) {

            if (pvals.get(i) < significance) {
                modes[i] = true;
            } else {
                modes[i] = false;
            }

        }
        return new BooleanProfile(modes);
    }

    /**
     * Given an array of values, perform a dip test and return the p-value. If
     * the array size is <10, returns 1.
     * 
     * @param values
     * @return
     */
    public static double getDipTestPValue(double[] values) {
        if (values.length < 10)
            return 1;
		double[] result = DistributionTest.diptest(values);
		return result[1];
    }

    /**
     * Given an array of values, perform a dip test and return the test
     * statistic
     * 
     * @param values
     * @return
     */
    public static double getDipTestTestStatistic(double[] values) {
        if (values.length < 10)
            return 1;
		double[] result = DistributionTest.diptest(values);
		return result[0];

    }

    public static double getShapiroWilkStatistic(double[] values) {
        return NormalityTest.shapiro_wilk_statistic(values);
    }

    public static double getShapiroWilkPValue(double[] values) {
        return NormalityTest.shapiro_wilk_pvalue(NormalityTest.shapiro_wilk_statistic(values), values.length);
    }

    public static double getInvNormProbabililty(double p) {
        InvNormal dist = new InvNormal(0, 1);
        return dist.cumulative(p);
    }

}
