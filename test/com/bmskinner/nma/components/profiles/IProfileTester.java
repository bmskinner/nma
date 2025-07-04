package com.bmskinner.nma.components.profiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.bmskinner.nma.analysis.profiles.NoDetectedIndexException;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.samples.dummy.DummySegmentedCellularComponent;

/**
 * Test the common methods for segment classes implementing the IBorderSegment
 * interface.
 * 
 * @author bms41
 * @since 1.14.0
 *
 */
@RunWith(Parameterized.class)
public class IProfileTester {

	private IProfile profile;
	private static float[] data;

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	@Parameter(0)
	public Class<? extends IProfile> source;

	@Before
	public void setUp() throws Exception {
		DummySegmentedCellularComponent comp = new DummySegmentedCellularComponent();
		data = new float[comp.getBorderLength()];
		for (int i = 0; i < data.length; i++) {
			data[i] = (float) ((Math.sin(Math.toRadians(i)) + 1) * 180);
		}
		profile = createInstance(source);
	}

	/**
	 * Create an instance of the class under test, using the default data.
	 * 
	 * @param source
	 * @return
	 * @throws Exception
	 */
	private static IProfile createInstance(Class<? extends IProfile> source) throws Exception {
		return createInstance(source, data);
	}

	/**
	 * Create an instance of the class under test, using the given data.
	 * 
	 * @param source the class to create
	 * @return
	 * @throws Exception
	 */
	private static IProfile createInstance(Class<? extends IProfile> source, float[] data)
			throws Exception {
		if (source == DefaultProfile.class)
			return new DefaultProfile(data);

		throw new Exception("Unable to create instance of " + source);
	}

	@Parameters
	public static Iterable<Class<? extends IProfile>> arguments() {

		// Since the objects created here persist throughout all tests,
		// we're making class references. The actual objects under test
		// are created fresh from the appropriate class.
		return Arrays.asList(DefaultProfile.class);
	}

	@Test
	public void testSize() {
		assertEquals(profile.size(), data.length);
	}

	@Test
	public void testGetInt() {
		for (int i = 0; i < data.length; i++) {
			assertEquals(data[i], profile.get(i), 0);
		}
	}

	@Test
	public void testGetIntExceptsOnNegativeIndex() {
		exception.expect(IndexOutOfBoundsException.class);
		profile.get(-1);
	}

	@Test
	public void testGetIntExceptsOnOutOfBoundsIndex() {
		exception.expect(IndexOutOfBoundsException.class);
		profile.get(profile.size() + 1);
	}

	@Test
	public void testGetDouble() {
		double d = profile.get(0.5);
		int mid = data.length / 2;
		double exp = profile.get(mid);
		assertEquals(exp, d, 0);
	}

	@Test
	public void testGetDoubleExceptsWhenProportionOutOfLowerBounds() {
		exception.expect(IndexOutOfBoundsException.class);
		profile.get(-0.1);
	}

	@Test
	public void testGetDoubleExceptsWhenProportionOutOfUpperBounds() {
		exception.expect(IndexOutOfBoundsException.class);
		profile.get(1.1);
	}

	@Test
	public void testGetMax() {
		DoubleStream ds = IntStream.range(0, data.length).mapToDouble(i -> data[i]);
		assertEquals(ds.max().getAsDouble(), profile.getMax(), 0);
	}

	@Test
	public void testGetIndexOfMaxBooleanProfile()
			throws SegmentUpdateException, NoDetectedIndexException {

		// Restrict to first half of array
		BooleanProfile b = new BooleanProfile(data.length, false);
		for (int i = 0; i < data.length / 2; i++) {
			b.set(i, true);
		}

		int exp = IntStream.range(0, data.length / 2).boxed()
				.max(Comparator.comparing(profile::get)).get();

		assertEquals(exp, profile.getIndexOfMax(b));
	}

	@Test
	public void testGetIndexOfMaxBooleanProfileExceptsOnAllFalseProfile()
			throws SegmentUpdateException, NoDetectedIndexException {
		BooleanProfile b = new BooleanProfile(data.length, false);
		exception.expect(NoDetectedIndexException.class);
		profile.getIndexOfMax(b);
	}

	@Test
	public void testGetIndexOfMaxBooleanProfileExceptsOnDifferentLength()
			throws SegmentUpdateException, NoDetectedIndexException {
		BooleanProfile b = new BooleanProfile(data.length / 2, false);
		exception.expect(IllegalArgumentException.class);
		profile.getIndexOfMax(b);
	}

	@Test
	public void testGetIndexOfMax() throws SegmentUpdateException, NoDetectedIndexException {
		int exp = IntStream.range(0, data.length).boxed().max(Comparator.comparing(profile::get))
				.get();
		assertEquals(exp, profile.getIndexOfMax());
	}

	@Test
	public void testGetIndexOfFraction() {

		double fraction = 0.5;
		int exp = data.length / 2;
		int i = profile.getIndexOfFraction(fraction);
		assertEquals(exp, i);

	}

	@Test
	public void testGetIndexOfFractionExceptsOnLessThanZero() {
		exception.expect(IllegalArgumentException.class);
		profile.getIndexOfFraction(-0.1);
	}

	@Test
	public void testGetIndexOfFractionExceptsOnGreaterThanOne() {
		exception.expect(IllegalArgumentException.class);
		profile.getIndexOfFraction(1.1);
	}

	/**
	 * Test method for
	 * {@link com.bmskinner.nma.components.profiles.IProfile#getFractionOfIndex(int)}.
	 */
	@Test
	public void testGetFractionOfIndex() {
		assertEquals(0, profile.getFractionOfIndex(0), 0);

		double f = (double) (profile.size() - 1) / (double) profile.size();
		assertEquals(f, profile.getFractionOfIndex(profile.size() - 1), 0);
	}

	@Test
	public void testGetFractionOfIndexExceptsOnIndexBelowZero() {
		exception.expect(IllegalArgumentException.class);
		profile.getFractionOfIndex(-1);
	}

	@Test
	public void testGetFractionOfIndexExceptsOnIndexAboveBounds() {
		exception.expect(IllegalArgumentException.class);
		profile.getFractionOfIndex(profile.size() + 1);
	}

	/**
	 * Test method for
	 * {@link com.bmskinner.nma.components.profiles.IProfile#getMin()}.
	 */
	@Test
	public void testGetMin() {
		DoubleStream ds = IntStream.range(0, data.length).mapToDouble(i -> data[i]);
		assertEquals(ds.min().getAsDouble(), profile.getMin(), 0);
	}

	/**
	 * Test method for
	 * {@link com.bmskinner.nma.components.profiles.IProfile#getIndexOfMin(com.bmskinner.nma.components.profiles.BooleanProfile)}.
	 * 
	 * @throws SegmentUpdateException
	 * @throws NoDetectedIndexException
	 */
	@Test
	public void testGetIndexOfMinBooleanProfile()
			throws SegmentUpdateException, NoDetectedIndexException {
		// Restrict to second half of array
		BooleanProfile b = new BooleanProfile(data.length, true);
		for (int i = 0; i < data.length / 2; i++) {
			b.set(i, false);
		}
		int exp = IntStream.range(data.length / 2, data.length - 1).boxed()
				.min(Comparator.comparing(profile::get))
				.get();

		assertEquals(exp, profile.getIndexOfMin(b));
	}

	@Test
	public void testGetIndexOfMinBooleanProfileExceptsOnAllFalseProfile()
			throws SegmentUpdateException, NoDetectedIndexException {
		BooleanProfile b = new BooleanProfile(data.length, false);
		exception.expect(NoDetectedIndexException.class);
		profile.getIndexOfMin(b);
	}

	@Test
	public void testGetIndexOfMinBooleanProfileExceptsOnDifferentLength()
			throws SegmentUpdateException, NoDetectedIndexException {
		BooleanProfile b = new BooleanProfile(data.length / 2, false);
		exception.expect(IllegalArgumentException.class);
		profile.getIndexOfMin(b);
	}

	/**
	 * Test method for
	 * {@link com.bmskinner.nma.components.profiles.IProfile#getIndexOfMin()}.
	 * 
	 * @throws SegmentUpdateException
	 * @throws NoDetectedIndexException
	 */
	@Test
	public void testGetIndexOfMin() throws SegmentUpdateException, NoDetectedIndexException {

		int exp = IntStream.range(0, data.length - 1).boxed()
				.min(Comparator.comparing(profile::get)).get();

		assertEquals(exp, profile.getIndexOfMin());
	}

	/**
	 * Test method for
	 * {@link com.bmskinner.nma.components.profiles.IProfile#toDoubleArray()}.
	 */
	@Test
	public void testToDoubleArray() {

		double[] d = new double[data.length];
		for (int i = 0; i < data.length; i++) {
			d[i] = data[i];
		}

		double[] res = profile.toDoubleArray();

		assertTrue(Arrays.equals(d, res));
	}

	/**
	 * Test method for
	 * {@link com.bmskinner.nma.components.profiles.IProfile#absoluteSquareDifference(com.bmskinner.nma.components.profiles.IProfile)}.
	 */
	@Test
	public void testAbsoluteSquareDifferenceIsZeroWhenSameProfile() throws SegmentUpdateException {
		assertEquals(0, profile.absoluteSquareDifference(profile), 0);
	}

	private void testAbsoluteSquareDifferenceOnSameLengthProfiles(IProfile template, float diff)
			throws Exception {
		float[] test = Arrays.copyOf(template.toFloatArray(), template.size());

		test[0] = test[0] + diff;
		IProfile p = createInstance(source, test);
		double expDiff = diff * diff;

		// Check both directions give the same result
		assertEquals(expDiff, template.absoluteSquareDifference(p), 0);
		assertEquals(expDiff, p.absoluteSquareDifference(template), 0);
	}

	@Test
	public void testAbsoluteSquareDifferenceOnSameLengthProfilesPositive() throws Exception {
		testAbsoluteSquareDifferenceOnSameLengthProfiles(profile, 2);
	}

	@Test
	public void testAbsoluteSquareDifferenceOnSameLengthProfilesNegative() throws Exception {
		testAbsoluteSquareDifferenceOnSameLengthProfiles(profile, -2);
	}

	@Test
	public void testInterpolatedProfileHasNoSquareDifferenceToSourceProfile() throws Exception {
		testAbsoluteSquareDifferenceOnSameLengthProfiles(profile, 0);
	}

	/**
	 * Test that the absolute square difference measure works after interpolation by
	 * creating a profile with a known difference and checking we calculate the
	 * expected ASD
	 * 
	 * @param template  the profile to test
	 * @param newLength the new length of hte profile after interpolation
	 * @param diff      the difference to add to the first profile index
	 * @throws Exception
	 */
	private void testAbsoluteSquareDifferenceOnDifferentLengthProfiles(IProfile template,
			int newLength, float diff)
			throws Exception {

		IProfile interpolated = template.interpolate(newLength);

		float[] arr = interpolated.toFloatArray();
		arr[0] = arr[0] + diff;
		IProfile newProfile = new DefaultProfile(arr);

		double expDiff = diff * diff;
		assertEquals(expDiff, template.absoluteSquareDifference(newProfile), 0.001);
	}

	@Test
	public void testAbsoluteSquareDifferenceOnLongerProfilesPositive() throws Exception {
		testAbsoluteSquareDifferenceOnDifferentLengthProfiles(profile, profile.size() * 2, 2);
	}

	@Test
	public void testAbsoluteSquareDifferenceOnShorterProfilesPositive() throws Exception {

		// We can't directly compare because there is loss of precision after
		// interpolation
		// Check that the profiles are being interpolated properly instead, and
		// subsitute values
		int diff = 2;
		IProfile shortened = profile.interpolate(profile.size() / 2);
		IProfile lengthened = shortened.interpolate(profile.size());

		float[] arr = lengthened.toFloatArray();
		arr[0] = arr[0] + diff;

		IProfile differenced = new DefaultProfile(arr);
		double expDiff = diff * diff;

		assertEquals(expDiff, differenced.absoluteSquareDifference(lengthened), 0.001);
		assertEquals(expDiff, lengthened.absoluteSquareDifference(differenced), 0.001);
	}

	/**
	 * Test method for
	 * {@link com.bmskinner.nma.components.profiles.IProfile#duplicate()}.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCopy() throws Exception {
		IProfile p = createInstance(source);
		float[] result = p.duplicate().toFloatArray();

		assertTrue(Arrays.equals(data, result));
	}

	/**
	 * Test method for
	 * {@link com.bmskinner.nma.components.profiles.IProfile#startFrom(int)}.
	 */
	@Test
	public void testOffsetByOne() throws SegmentUpdateException {
		float[] exp1 = new float[data.length];
		for (int i = 0; i < data.length - 1; i++) {
			exp1[i] = data[i + 1];
		}
		exp1[data.length - 1] = data[0];

		float[] result = profile.startFrom(1).toFloatArray();
		assertTrue("Exp: " + Arrays.toString(exp1) + " Was: " + Arrays.toString(result),
				Arrays.equals(exp1, result));
	}

	/**
	 * Test method for
	 * {@link com.bmskinner.nma.components.profiles.IProfile#startFrom(int)}.
	 */
	@Test
	public void testOffsetByFive() throws SegmentUpdateException {
		int offset = 5;
		float[] exp1 = new float[data.length];
		for (int i = 0; i < data.length - offset; i++) {
			exp1[i] = data[i + offset];
		}

		for (int i = offset; i > 0; i--) {
			exp1[data.length - i] = data[offset - i];
		}

		float[] result = profile.startFrom(offset).toFloatArray();
		assertTrue("Exp: " + Arrays.toString(exp1) + " Was: " + Arrays.toString(result),
				Arrays.equals(exp1, result));
	}

	/**
	 * Test method for
	 * {@link com.bmskinner.nma.components.profiles.IProfile#startFrom(int)}.
	 */
	@Test
	public void testOffsetByNegativeOne() throws SegmentUpdateException {

		float[] exp1 = new float[data.length];
		for (int i = 1; i < data.length; i++) {
			exp1[i] = data[i - 1];
		}
		exp1[0] = data[data.length - 1];

		float[] result = profile.startFrom(-1).toFloatArray();
		assertTrue(equals(exp1, result));
	}

	/**
	 * Test method for
	 * {@link com.bmskinner.nma.components.profiles.IProfile#smooth(int)}.
	 */
	@Test
	public void testSmooth() {

		float[] exp = new float[data.length];

		exp[0] = (data[0] + data[1] + data[2] + data[data.length - 1] + data[data.length - 2]) / 5;
		exp[1] = (data[1] + data[2] + data[3] + data[0] + data[data.length - 1]) / 5;

		for (int i = 2; i <= data.length - 3; i++) {

			float r1 = data[i - 2];
			float r0 = data[i - 1];

			float f0 = data[i + 1];
			float f1 = data[i + 2];

			exp[i] = (r1 + r0 + data[i] + f0 + f1) / 5;
		}

		exp[data.length - 2] = (data[data.length - 2] + data[data.length - 1] + data[0]
				+ data[data.length - 3]
				+ data[data.length - 4]) / 5;
		exp[data.length
				- 1] = (data[data.length - 3] + data[data.length - 2] + data[data.length - 1]
						+ data[0] + data[1]) / 5;

		IProfile p = profile.smooth(2);
		float[] obs = p.toFloatArray();
		assertTrue(equals(exp, obs));
	}

	@Test
	public void testSmoothExceptsOnZeroWindowSize() {
		exception.expect(IllegalArgumentException.class);
		profile.smooth(0);
	}

	@Test
	public void testSmoothExceptsOnNegativeWindowSize() {
		exception.expect(IllegalArgumentException.class);
		profile.smooth(-1);
	}

	/**
	 * Test method for
	 * {@link com.bmskinner.nma.components.profiles.IProfile#reverse()}.
	 * 
	 * @throws SegmentUpdateException
	 */
	@Test
	public void testReverse() throws SegmentUpdateException {

		float[] arr = Arrays.copyOf(data, data.length);

		for (int i = 0; i < arr.length / 2; i++) {
			float temp = arr[i];
			arr[i] = arr[arr.length - i - 1];
			arr[arr.length - i - 1] = temp;
		}

		profile.reverse();

		float[] res = profile.toFloatArray();

		assertTrue(equals(arr, res));
	}

	@Test
	public void testFindBestFitOffsetHasNoEffectWithZeroOffset() throws SegmentUpdateException {
		int exp = 0;
		IProfile test = profile.startFrom(exp);
		int offset = profile.findBestFitOffset(test);
		assertEquals(exp, offset);
	}

	/**
	 * Test method for
	 * {@link com.bmskinner.nma.components.profiles.IProfile#findBestFitOffset(com.bmskinner.nma.components.profiles.IProfile)}.
	 * 
	 * @throws SegmentUpdateException
	 */
	@Test
	public void testFindBestFitOffsetWithPositiveOffsetIdenticalProfile()
			throws SegmentUpdateException {

		for (int exp = 1; exp < profile.size(); exp++) {
			IProfile test = profile.startFrom(exp);
			int offset = profile.findBestFitOffset(test);
			IProfile recovered = test.startFrom(-offset);
			assertEquals(exp, offset);
			assertEquals(profile, recovered);
		}
	}

	/**
	 * Test method for
	 * {@link com.bmskinner.nma.components.profiles.IProfile#findBestFitOffset(com.bmskinner.nma.components.profiles.IProfile)}.
	 * 
	 * @throws SegmentUpdateException
	 */
	@Test
	public void testFindBestFitOffsetWithNegativeOffsetIdenticalProfile()
			throws SegmentUpdateException {

		for (int exp = -1; exp > -profile.size(); exp--) {
			IProfile test = profile.startFrom(exp);
			int offset = profile.findBestFitOffset(test);
			IProfile recovered = test.startFrom(-offset);

			int posExp = profile.size() + exp;
			assertEquals(posExp, offset);
			assertEquals(profile, recovered);
		}
	}

	/**
	 * Test method for
	 * {@link com.bmskinner.nma.components.profiles.IProfile#getLocalMinima(int)}.
	 * 
	 * @throws SegmentUpdateException
	 */
	@Test
	public void testGetLocalMinimaInt() throws SegmentUpdateException {

		BooleanProfile b = profile.getLocalMinima(3);
		for (int i = 0; i < data.length; i++) {
			boolean isMax = ((Math.sin(Math.toRadians(i)) + 1) * 180) == 0f;
			assertEquals(isMax, b.get(i));
		}
	}

	@Test
	public void testGetLocalMinimaIntExceptsOnZeroWindowSize() {
		exception.expect(IllegalArgumentException.class);
		profile.getLocalMinima(0);
	}

	/**
	 * Test method for
	 * {@link com.bmskinner.nma.components.profiles.IProfile#getLocalMinima(int, double)}.
	 */
	@Test
	public void testGetLocalMinimaIntDouble() {
		BooleanProfile b = profile.getLocalMinima(3, 180);
		for (int i = 0; i < data.length; i++) {
			boolean isMax = ((Math.sin(Math.toRadians(i)) + 1) * 180) == 0f;
			assertEquals(isMax, b.get(i));
		}
		b = profile.getLocalMinima(3, -1);
		for (int i = 0; i < data.length; i++) {
			assertFalse(b.get(i));
		}
	}

	@Test
	public void testGetLocalMinimaWorksOnZeroIndex() throws SegmentUpdateException {
		IProfile offset = profile.startFrom(270);
		BooleanProfile b = offset.getLocalMinima(3, 180);
		assertTrue("Expecting minima at " + offset.get(0), b.get(0));
	}

	/**
	 * Test method for
	 * {@link com.bmskinner.nma.components.profiles.IProfile#getLocalMaxima(int)}.
	 */
	@Test
	public void testGetLocalMaximaInt() {

		BooleanProfile b = profile.getLocalMaxima(3);

		for (int i = 0; i < data.length; i++) {
			boolean isMax = ((Math.sin(Math.toRadians(i)) + 1) * 180) == 360f;
			assertEquals(isMax, b.get(i));
		}
	}

	@Test
	public void testGetLocalMaximaIntExceptsOnZeroWindowSize() {
		exception.expect(IllegalArgumentException.class);
		profile.getLocalMaxima(0);
	}

	@Test
	public void testGetLocalMaximaIntDouble() {
		BooleanProfile b = profile.getLocalMaxima(3, 180);

		for (int i = 0; i < data.length; i++) {
			boolean isMax = ((Math.sin(Math.toRadians(i)) + 1) * 180) == 360f;
			assertEquals(isMax, b.get(i));
		}

		b = profile.getLocalMaxima(3, 400);
		for (int i = 0; i < data.length; i++) {
			assertFalse(b.get(i));
		}
	}

	/**
	 * Test method for
	 * {@link com.bmskinner.nma.components.profiles.IProfile#getSubregion(int, int)}.
	 */
	@Test
	public void testGetSubregionIntInt() {
		int start = 0;
		int stop = 3;
		IProfile p = profile.getSubregion(start, stop);

		assertEquals(4, p.size());

		for (int i = start; i <= stop; i++) {
			assertEquals(data[i], p.get(i), 0);
		}
	}

	@Test
	public void testGetSubregionIntIntWraps() {
		int start = data.length - 2;
		int stop = 2;
		IProfile p = profile.getSubregion(start, stop);

		for (int i = 0; i < p.size(); i++) {
			int original = i < 2 ? start + i : i - 2;
			assertEquals(data[original], p.get(i), 0);
		}
		assertEquals(5, p.size());
	}

	@Test
	public void testGetSubregionIntIntExceptsOnLowerIndexOutOfBounds() {
		exception.expect(IllegalArgumentException.class);
		profile.getSubregion(-1, 3);
	}

	@Test
	public void testGetSubregionIntIntExceptsOnUpperIndexOutOfBounds() {
		exception.expect(IllegalArgumentException.class);
		profile.getSubregion(-1, profile.size() + 1);
	}

	@Test
	public void testGetSubregionIntIntExceptsOnWrappingLowerIndexOutOfBounds() {
		exception.expect(IllegalArgumentException.class);
		profile.getSubregion(profile.size() + 1, 3);
	}

	/**
	 * Test method for
	 * {@link com.bmskinner.nma.components.profiles.IProfile#getSubregion(com.bmskinner.nma.components.profiles.IProfileSegment)}.
	 * 
	 * @throws SegmentUpdateException
	 */
	@Test
	public void testGetSubregionIBorderSegmentFromNonWrappingSegment()
			throws SegmentUpdateException {

		int start = 0;
		int stop = 3;
		IProfileSegment s = new DefaultProfileSegment(start, stop, data.length);
		IProfile p = profile.getSubregion(s);

		assertEquals(s.length(), p.size());

		for (int i = start; i <= stop; i++) {
			assertEquals(data[i], p.get(i), 0);
		}
	}

	@Test
	public void testGetSubregionIBorderSegmentFromWrappingSegment() throws SegmentUpdateException {

		IProfileSegment s = new DefaultProfileSegment(profile.size() - 10, 10, profile.size());
		IProfile p = profile.getSubregion(s);

		assertEquals(s.length(), p.size());

		Iterator<Integer> it = s.iterator();
		int j = 0;
		while (it.hasNext()) {
			int i = it.next();
			assertEquals(data[i], p.get(j++), 0);
		}
		assertFalse(it.hasNext());
	}

	@Test
	public void testGetSubregionIBorderSegmentExceptsOnSegmentOutOfUpperBounds()
			throws SegmentUpdateException {
		IProfileSegment s = new DefaultProfileSegment(0, 100, 200);
		exception.expect(IllegalArgumentException.class);
		profile.getSubregion(s);
	}

	/**
	 * Test method for
	 * {@link com.bmskinner.nma.components.profiles.IProfile#calculateDeltas(int)}.
	 */
	@Test
	public void testCalculateDeltasSucceedsWithWindowSizeOne() {

		IProfile res = profile.calculateDeltas(1);

		double expAt0 = (data[0] - data[data.length - 1]) + (data[1] - data[0]);
		double expAt1 = (data[2] - data[1]) + (data[1] - data[0]);
		assertEquals(expAt0, res.get(0), 0);
		assertEquals(expAt1, res.get(1), 0);
	}

	@Test
	public void testCalculateDeltasSucceedsWithWindowSizeTwo() {

		IProfile res = profile.calculateDeltas(2);

		double expAt0 = (data[data.length - 1] - data[data.length - 2])
				+ (data[0] - data[data.length - 1])
				+ (data[1] - data[0]) + (data[2] - data[1]);

		double expAt1 = (data[0] - data[data.length - 1]) + (data[1] - data[0])
				+ (data[2] - data[1])
				+ (data[3] - data[2]);

		double expAt2 = (data[1] - data[0]) + (data[2] - data[1]) + (data[3] - data[2])
				+ (data[4] - data[3]);

		assertEquals(expAt0, res.get(0), 0.00001);
		assertEquals(expAt1, res.get(1), 0.00001);
		assertEquals(expAt2, res.get(2), 0.00001);
	}

	@Test
	public void testCalculateDeltasExceptsWithWindowSizeZero() {
		exception.expect(IllegalArgumentException.class);
		profile.calculateDeltas(0);
	}

	/**
	 * Test method for
	 * {@link com.bmskinner.nma.components.profiles.IProfile#toPowerOf(double)}.
	 */
	@Test
	public void testPower() {

		double d = 2;
		float[] exp = new float[data.length];
		for (int i = 0; i < data.length; i++) {
			exp[i] = (float) Math.pow(data[i], d);
		}

		float[] result = profile.toPowerOf(d).toFloatArray();

		assertTrue(equals(exp, result));
	}

	/**
	 * Test method for
	 * {@link com.bmskinner.nma.components.profiles.IProfile#absolute()}.
	 */
	@Test
	public void testAbsolute() {

		float[] exp = new float[data.length];
		for (int i = 0; i < data.length; i++) {
			exp[i] = Math.abs(data[i] - 180);
		}
		IProfile p = profile.subtract(180);
		float[] result = p.absolute().toFloatArray();

		assertTrue(equals(exp, result));
	}

	/**
	 * Test method for
	 * {@link com.bmskinner.nma.components.profiles.IProfile#multiply(double)}.
	 */
	@Test
	public void testMultiplyDouble() {
		double constant = 2;

		IProfile result = profile.multiply(constant);
		float[] exp = new float[data.length];

		for (int i = 0; i < data.length; i++) {
			exp[i] = (float) (data[i] * constant);
		}

		assertTrue(equals(exp, result.toFloatArray()));
	}

	@Test
	public void testMultiplyDoubleNanInputFails() {
		exception.expect(IllegalArgumentException.class);
		profile.multiply(Double.NaN);
	}

	@Test
	public void testMultiplyDoublePositiveInfinityInputFails() {
		exception.expect(IllegalArgumentException.class);
		profile.multiply(Double.POSITIVE_INFINITY);
	}

	@Test
	public void testMultiplyDoubleNegativeInfinityInputFails() {
		exception.expect(IllegalArgumentException.class);
		profile.multiply(Double.NEGATIVE_INFINITY);
	}

	/**
	 * Test method for
	 * {@link com.bmskinner.nma.components.profiles.IProfile#multiply(com.bmskinner.nma.components.profiles.IProfile)}.
	 */
	@Test
	public void testMultiplyIProfile() {

		float[] exp = new float[data.length];
		for (int i = 0; i < data.length; i++) {
			exp[i] = data[i] * data[i];
		}

		IProfile result = profile.multiply(profile);

		assertTrue(equals(exp, result.toFloatArray()));
	}

	@Test
	public void testMultiplyProfileExceptsOnDifferentLength() {
		float[] f = { 10, 10 };
		IProfile p2 = new DefaultProfile(f);
		exception.expect(IllegalArgumentException.class);
		profile.multiply(p2);
	}

	/**
	 * Test method for
	 * {@link com.bmskinner.nma.components.profiles.IProfile#divide(double)}.
	 */
	@Test
	public void testDivideDouble() {

		double constant = 2;
		float[] exp = new float[data.length];
		for (int i = 0; i < data.length; i++) {
			exp[i] = (float) (data[i] / constant);
		}

		IProfile result = profile.divide(constant);
		assertTrue(equals(exp, result.toFloatArray()));
	}

	@Test
	public void testDivideDoubleNegative() {

		double constant = -2;
		float[] exp = new float[data.length];
		for (int i = 0; i < data.length; i++) {
			exp[i] = (float) (data[i] / constant);
		}

		IProfile result = profile.divide(constant);
		assertTrue(equals(exp, result.toFloatArray()));
	}

	@Test
	public void testDivideDoubleNanInputFails() {
		exception.expect(IllegalArgumentException.class);
		profile.divide(Double.NaN);
	}

	@Test
	public void testDivideDoublePositiveInfinityInputFails() {
		exception.expect(IllegalArgumentException.class);
		profile.divide(Double.POSITIVE_INFINITY);
	}

	@Test
	public void testDivideDoubleNegativeInfinityInputFails() {
		exception.expect(IllegalArgumentException.class);
		profile.divide(Double.NEGATIVE_INFINITY);
	}

	/**
	 * Test method for
	 * {@link com.bmskinner.nma.components.profiles.IProfile#divide(com.bmskinner.nma.components.profiles.IProfile)}.
	 */
	@Test
	public void testDivideIProfile() {

		float[] exp = new float[data.length];
		for (int i = 0; i < data.length; i++) {
			exp[i] = data[i] / data[i];
		}

		IProfile result = profile.divide(profile);
		assertTrue(equals(exp, result.toFloatArray()));
	}

	@Test
	public void testDivideProfileExceptsOnDifferentLength() {
		float[] f = { 10, 10 };
		IProfile divider = new DefaultProfile(f);
		exception.expect(IllegalArgumentException.class);
		profile.divide(divider);
	}

	@Test
	public void testDivideIProfileFailsOnSizeMismatch() {

		float[] div = { 1, 2, 0.5f, 3, 0.25f };
		IProfile divider = new DefaultProfile(div);
		exception.expect(IllegalArgumentException.class);
		profile.divide(divider);
	}

	/**
	 * Test method for
	 * {@link com.bmskinner.nma.components.profiles.IProfile#add(com.bmskinner.nma.components.profiles.IProfile)}.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAddIProfile() throws Exception {

		float[] exp = new float[data.length];
		for (int i = 0; i < data.length; i++) {
			exp[i] = data[i] * 2;
		}

		IProfile p1 = createInstance(source);
		IProfile p2 = createInstance(source);

		IProfile p3 = p1.add(p2);
		float[] result = p3.toFloatArray();

		assertTrue(equals(exp, result));
	}

	@Test
	public void testAddProfileExceptsOnDifferentLength() {
		float[] f = { 10, 10 };
		IProfile p2 = new DefaultProfile(f);
		exception.expect(IllegalArgumentException.class);
		profile.add(p2);
	}

	/**
	 * Test method for
	 * {@link com.bmskinner.nma.components.profiles.IProfile#add(double)}.
	 */
	@Test
	public void testAddDouble() {

		double constant = 2;
		float[] exp = new float[data.length];
		for (int i = 0; i < data.length; i++) {
			exp[i] = (float) (data[i] + constant);
		}

		IProfile result = profile.add(constant);

		assertTrue(equals(exp, result.toFloatArray()));
	}

	@Test
	public void testAddDoubleExceptsOnNan() {
		exception.expect(IllegalArgumentException.class);
		profile.add(Double.NaN);
	}

	@Test
	public void testAddDoubleExceptsOnPositiveInfinity() {
		exception.expect(IllegalArgumentException.class);
		profile.add(Double.POSITIVE_INFINITY);
	}

	@Test
	public void testAddDoubleExceptsOnNegativeInfinity() {
		exception.expect(IllegalArgumentException.class);
		profile.add(Double.NEGATIVE_INFINITY);
	}

	/**
	 * Test method for
	 * {@link com.bmskinner.nma.components.profiles.IProfile#subtract(com.bmskinner.nma.components.profiles.IProfile)}.
	 */
	@Test
	public void testSubtract() {

		IProfile p = profile.add(profile);
		p = p.subtract(profile);
		assertTrue(equals(data, p.toFloatArray()));
	}

	@Test
	public void testSubtractProfileExceptsOnDifferentLength() {
		float[] f = { 10, 10 };
		IProfile p2 = new DefaultProfile(f);
		exception.expect(IllegalArgumentException.class);
		profile.subtract(p2);
	}

	@Test
	public void testSubtractDouble() {

		double sub = 1;
		float[] exp = new float[data.length];
		for (int i = 0; i < data.length; i++) {
			exp[i] = (float) (data[i] - sub);
		}

		IProfile p = profile.subtract(sub);
		assertTrue(equals(exp, p.toFloatArray()));
	}

	@Test
	public void testSubtractDoubleExceptsOnNan() {
		exception.expect(IllegalArgumentException.class);
		profile.subtract(Double.NaN);
	}

	@Test
	public void testSubtractDoubleExceptsOnPositiveInfinity() {
		exception.expect(IllegalArgumentException.class);
		profile.subtract(Double.POSITIVE_INFINITY);
	}

	@Test
	public void testSubtractDoubleExceptsOnNegativeInfinity() {
		exception.expect(IllegalArgumentException.class);
		profile.subtract(Double.NEGATIVE_INFINITY);
	}

	@Test
	public void interpolationShouldLinearExtend() throws Exception {
		IProfile test = profile.interpolate(profile.size() * 2);
		assertEquals(profile.size() * 2, test.size());
	}

	@Test
	public void interpolationShouldShrinkWhenGivenLowerLength() throws SegmentUpdateException {
		IProfile test = profile.interpolate(profile.size() / 2);
		assertEquals(profile.size() / 2, test.size());
	}

	@Test
	public void interpolateExceptsOnLengthZero() throws SegmentUpdateException {
		exception.expect(IllegalArgumentException.class);
		profile.interpolate(0);
	}

	@Test
	public void interpolateExceptsOnLengthNegative() throws SegmentUpdateException {
		exception.expect(IllegalArgumentException.class);
		profile.interpolate(-1);
	}

	@Test
	public void squareDiffsAreCalculatedCorrectlyForEqualLengthProfiles() throws Exception {

		IProfile dataProfile = createInstance(source);

		float[] arr = dataProfile.toFloatArray();
		arr[0] = arr[0] + 2;
		arr[1] = arr[1] - 3;
		int expDiff = 4 + 9;
		IProfile templateProfile = createInstance(source, arr);

		double value = dataProfile.absoluteSquareDifference(templateProfile);
		assertEquals(expDiff, value, 0);
	}

	@Test
	public void testGetWindowWithinCentreOfProfile() {
		int mid = data.length / 2;
		IProfile r = profile.getWindow(mid, 2);
		float[] exp = { data[mid - 2], data[mid - 1], data[mid], data[mid + 1], data[mid + 2] };
		assertTrue(equals(exp, r.toFloatArray()));
	}

	@Test
	public void testGetWindowAtStartOfProfile() {
		float[] exp = { data[data.length - 1], data[0], data[1], data[2], data[3] };
		IProfile r = profile.getWindow(1, 2);
		assertTrue(equals(exp, r.toFloatArray()));
	}

	@Test
	public void testGetWindowAtEndOfProfile() {

		IProfile r = profile.getWindow(data.length - 1, 2);
		float[] exp = { data[data.length - 3], data[data.length - 2], data[data.length - 1],
				data[0], data[1] };
		assertTrue(equals(exp, r.toFloatArray()));
	}

	@Test
	public void testEqualsWithSameObjectRef() {
		assertEquals(profile, profile);
	}

	@Test
	public void testEqualsFalseWithNull() {
		assertFalse(profile.equals(null));
	}

	@Test
	public void testEqualsFalseWithNonProfile() {
		Object o = new Object();
		assertNotEquals(profile, o);
	}

	@Test
	public void testEqualsFalseWithDifferentData() throws Exception {
		float[] d = new float[data.length];
		for (int i = 0; i < data.length; i++) {
			d[i] = data[i] * 2;
		}
		IProfile p = createInstance(source, d);
		assertNotEquals(profile, p);
	}

	@Test
	public void testWrapsDoesNotAffectIndexWhenWithinProfile() {
		for (int i = 0; i < profile.size(); i++) {
			assertEquals(i, profile.wrap(i));
		}
	}

	@Test
	public void testWrapsAssignsNegativeIndexesToCorrectProfileIndex() {
		for (int i = -1; i > -profile.size(); i--) {
			assertEquals("Testing " + i + " against profile size " + profile.size(),
					profile.size() + i,
					profile.wrap(i));
		}
	}

	@Test
	public void testWrapsAssignsPositiveIndexesToCorrectProfileIndex() {
		for (int i = profile.size(); i < profile.size() * 2; i++) {
			assertEquals("Testing " + i + " against profile size " + profile.size(),
					i - profile.size(),
					profile.wrap(i));
		}
	}

	@Test
	public void testWrapsHasContigouousRange() {
		// Ensure that wrapping profile indexes does not 'skip'
		int prev = Integer.MIN_VALUE;
		for (int i = -profile.size() * 2; i < profile.size() * 3; i++) {

			int current = profile.wrap(i);

			if (prev > Integer.MIN_VALUE) {
				int exp = CellularComponent.wrapIndex(prev + 1, profile.size());
				assertEquals("Testing " + i + " against profile size " + profile.size(), exp,
						current);
			}
			prev = current;
		}
	}

	@Test
	public void testIteratorReturnsFullRange() {
		for (int i : profile) {
			assertTrue(i >= 0 && i < profile.size());
		}
	}

	/**
	 * Test float array equality. Not in junit.
	 * 
	 * @param exp
	 * @param obs
	 */
	public static boolean equals(double[] exp, double[] obs) {
		double epsilon = 0.001;
		boolean equal = true;
		equal &= obs.length == exp.length;
		assertEquals(exp.length, obs.length);

		for (int i = 0; i < exp.length; i++) {
			equal &= (Double.isNaN(exp[i]) && Double.isNaN(obs[i]))
					|| Math.abs(exp[i] - obs[i]) <= epsilon;
			assertEquals("Index " + i, exp[i], obs[i], epsilon);
		}
		return equal;
	}

	/**
	 * Test float array equality. Not in junit.
	 * 
	 * @param exp
	 * @param obs
	 * @param epsilon
	 */
	public static boolean equals(float[] exp, float[] obs, float epsilon) {
		boolean equal = true;
		equal &= obs.length == exp.length;
		assertEquals(exp.length, obs.length);

		for (int i = 0; i < exp.length; i++) {
			equal &= (Float.isNaN(exp[i]) && Float.isNaN(obs[i]))
					|| Math.abs(exp[i] - obs[i]) <= epsilon;
			assertEquals("Index " + i, exp[i], obs[i], epsilon);
		}
		return equal;
	}

	/**
	 * Test float array equality. Not in junit. Uses the default epsilon.
	 * 
	 * @param exp
	 * @param obs
	 */
	public static boolean equals(float[] exp, float[] obs) {
		return equals(exp, obs, 0.001f);
	}

}
