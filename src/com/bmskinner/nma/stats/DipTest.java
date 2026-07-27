package com.bmskinner.nma.stats;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.profiles.BooleanProfile;
import com.bmskinner.nma.components.profiles.DefaultProfile;
import com.bmskinner.nma.components.profiles.IProfile;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;

import jdistlib.disttest.DistributionTest;

/**
 * The purpose is to test the difference at a particular point of a median
 * profile in a collection; for each nucleus in the collection, what is the
 * difference to the median at that point? Is the list of differences bimodal?
 */
public class DipTest {

	private static final Logger LOGGER = Logger.getLogger(DipTest.class.getName());

	final private IAnalysisDataset dataset;
	
	private final Map<ProfileType, IProfile> pValueMap = new HashMap<>();


	public DipTest(IAnalysisDataset dataset) {
		this.dataset = dataset;
		
		for(final ProfileType pt : ProfileType.values()) {
			pValueMap.put(pt, testCollectionGetPValues(pt));
		}
	}

	public IProfile getDipTestPValues(ProfileType pt) {
		return pValueMap.get(pt);
	}

	public BooleanProfile getSignificantIndexes(ProfileType pt, double significance) {
		return pValueMap.get(pt).isLessThan(significance);
	}

	/**
	 * Test the given collection for non-unimodality at each point in the profile,
	 * using Hartigan's Dip Test. Returns a profile with the dip test p-values at
	 * each point
	 * 
	 * @param collection the collection of nuclei
	 * @param tag        the border tag to offset from
	 * @return a boolean profile of results
	 */
	private IProfile testCollectionGetPValues(ProfileType pt) {

		try {

			final float[] pvalues = new float[dataset.getCollection().getMedianArrayLength()];

			final int profileSize = dataset.getCollection().getMedianArrayLength();
			
			// Precompute interpolated profiles for each cell
			final Map<UUID, IProfile> interpolatedProfiles = new HashMap<>();
			for (final ICell c : dataset.getCollection().getCells()) {
				final Nucleus n = c.getPrimaryNucleus();
				final IProfile p = n.getProfile(pt, OrientationMark.REFERENCE)
						.interpolate(dataset.getCollection().getMedianArrayLength());
				interpolatedProfiles.put(n.getId(), p);
			}
			LOGGER.fine("Interpolated %s for all cells".formatted(pt));

			for (int i = 0; i < dataset.getCollection().getMedianArrayLength(); i++) {

				final List<ICell> cells = dataset.getCollection().getCells();

				// Hold value at this index for all nuclei
				final double[] profileValues = new double[cells.size()];

				// Get the current index value from interpolated nucleus profile
				for (int j = 0; j < cells.size(); j++) {
					final Nucleus n = cells.get(j).getPrimaryNucleus();
					final IProfile p = interpolatedProfiles.get(n.getId());
					profileValues[j] = p.get(i);
				}

				pvalues[i] = (float) getDipTestPValue(profileValues);
			}

			return new DefaultProfile(pvalues);

		} catch (final Exception e) {
			LOGGER.log(Level.SEVERE,
					"Error in dip test: %s".formatted(e.getMessage()), e);
			return new DefaultProfile(1, dataset.getCollection().getMedianArrayLength());
		}

	}

	/**
	 * Given an array of values, perform a dip test and return the p-value. If the
	 * array size is <10, returns 1.
	 * 
	 * @param values
	 * @return
	 */
	private static double getDipTestPValue(double[] values) {

		if (values.length < 10)
			return 1;
		final double[] result = DistributionTest.diptest(values);
		return result[1];
	}

	/**
	 * Given an array of values, perform a dip test and return the test statistic
	 * 
	 * @param values
	 * @return
	 */
//	public static double getDipTestTestStatistic(double[] values) {
//		if (values.length < 10)
//			return 1;
//		final double[] result = DistributionTest.diptest(values);
//		return result[0];
//	}

//	public static double getShapiroWilkStatistic(double[] values) {
//		return NormalityTest.shapiro_wilk_statistic(values);
//	}
//
//	public static double getShapiroWilkPValue(double[] values) {
//		return NormalityTest.shapiro_wilk_pvalue(NormalityTest.shapiro_wilk_statistic(values), values.length);
//	}
//
//	public static double getInvNormProbabililty(double p) {
//		final InvNormal dist = new InvNormal(0, 1);
//		return dist.cumulative(p);
//	}

	/**
	 * Get the p-value for a Dip Test at the given x position in the angle profile
	 * 
	 * @param collection
	 * @param xPosition
	 * @return
	 * @throws Exception
	 */
//	public static double getPValueForPositon(CellCollection collection, double xPosition, ProfileCollectionType type)
//			throws Exception {
//
//		final double[] values = collection.getProfileCollection(type).getAggregate().getValuesAtPosition(xPosition);
//		Arrays.sort(values);
//		final double[] result = DistributionTest.diptest_presorted(values);
//		return getDipTestPValue(values);
//	}

	/**
	 * Test the given collection for non-unimodality at each point in the profile,
	 * using Hartigan's Dip Test. Returns a boolean profile with the points at which
	 * the dip test p-value is less than the given significance level
	 * 
	 * @param collection   the collection of nuclei
	 * @param tag          the border tag to offset from
	 * @param significance the p-value threshold
	 * @return a boolean profile of results
	 */
//	public static BooleanProfile testCollectionIsUniModal(CellCollection collection, BorderTag tag, double significance,
//			ProfileCollectionType type) {
//
//		BooleanProfile resultProfile = null;
//		boolean[] modes = null;
//		try {
//
//			final Profile pvals = testCollectionGetPValues(collection, tag, type);
//			modes = new boolean[pvals.size()];
//
//			for (int i = 0; i < pvals.size(); i++) {
//
//				if (pvals.get(i) < significance) {
//					modes[i] = true;
//				} else {
//					modes[i] = false;
//				}
//
//			}
//			resultProfile = new BooleanProfile(modes);
//		} catch (final Exception e) {
//			IJ.log("Error in dip test: " + e.getMessage());
//			for (final StackTraceElement e1 : e.getStackTrace()) {
//				IJ.log(e1.toString());
//			}
//		}
//		return resultProfile;
//	}

}