package com.bmskinner.nma.analysis.profiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nma.ComponentTester;
import com.bmskinner.nma.TestDatasetBuilder;
import com.bmskinner.nma.TestResources;
import com.bmskinner.nma.analysis.nucleus.NucleusDetectionMethod;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.measure.MeasurementScale;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.components.options.OptionsFactory;
import com.bmskinner.nma.components.profiles.IProfile;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.components.rules.RuleSetCollection;
import com.bmskinner.nma.stats.Stats;

import ij.Prefs;

/**
 * Test methods for the dataset profiling
 * 
 * @author bms41
 * @since 1.14.0
 *
 */
public class DefaultDatasetProfilingMethodTest extends AbstractProfileMethodTest {

	@Before
	public void setUp() {
		Prefs.setThreads(2); // Attempt to avoid issue 162
	}

	@Test
	public void testSingleCellDataset() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(RNG_SEED).cellCount(1)
				.ofType(RuleSetCollection.roundRuleSetCollection())
				.baseHeight(40).baseWidth(40).profiled().build();
		testProfilingIsConsistentBetweenMedianAndCells(d);
	}

	@Test
	public void testMultiCellIdenticalRectangularDataset() throws Exception {

		IAnalysisDataset d = new TestDatasetBuilder(RNG_SEED).cellCount(N_CELLS)
				.ofType(RuleSetCollection.roundRuleSetCollection()).randomOffsetProfiles(false)
				.profiled().build();
		testProfilesAreIdenticalForAllCells(d);
		testProfilingIsConsistentBetweenMedianAndCells(d);
	}

	@Test
	public void testMultiCellIdenticalRectangularDatasetIsNotAffectedByFixedBorderOffset()
			throws Exception {
		IAnalysisDataset dataset = new TestDatasetBuilder(RNG_SEED).cellCount(N_CELLS)
				.withMaxSizeVariation(0).fixedProfileOffset(20).profiled().build();
		testProfilesAreIdenticalForAllCells(dataset);
		testProfilingIsConsistentBetweenMedianAndCells(dataset);
	}

	@Test
	public void testProfilingIsIndependentOfCellLocation() throws Exception {
		// create cells with different x and y bases and no variation.
		// Check they all generate the same profiles

		IProfile globalMedian = null;
		Nucleus globalCell = null;

		for (int xBase = -10; xBase < 10; xBase++) {
			for (int yBase = -10; yBase < 10; yBase++) {

				IAnalysisDataset dataset = new TestDatasetBuilder(RNG_SEED).cellCount(N_CELLS)
						.xBase(xBase).yBase(yBase)
						.randomOffsetProfiles(false).fixedProfileOffset(20)
						.profiled()
						.build();

				testProfilesAreIdenticalForAllCells(dataset);

				Nucleus loopCell = null;

				// Confirm all cells are identical
				for (ICell cell : dataset.getCollection().getCells()) {
					if (globalCell == null)
						globalCell = cell.getPrimaryNucleus();
					if (loopCell == null)
						loopCell = cell.getPrimaryNucleus();

					Nucleus n = cell.getPrimaryNucleus();
					testNucleiHaveIdenticalPositions(n, loopCell);
					testNucleiHaveIdenticalSizes(n, globalCell);
				}

				// Check the collection
				IProfile median = dataset.getCollection()
						.getProfileCollection()
						.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.MEDIAN);
				assertTrue(median != null);

				if (globalMedian == null)
					globalMedian = median;

				// We can't expect full precision equality due to cumulative single precision
				// interpolation
				assertTrue(equals(globalMedian.toFloatArray(), median.toFloatArray(), 0.001f));

			}
		}
	}

	private void testNucleiHaveIdenticalPositions(Nucleus obs, Nucleus exp) {
		assertEquals(exp.getBase().getX(), obs.getBase().getX(), 0);
		assertEquals(exp.getBase().getY(), obs.getBase().getY(), 0);
	}

	private void testNucleiHaveIdenticalSizes(Nucleus obs, Nucleus exp) {
		assertEquals(exp.getWidth(), obs.getWidth(), 0.001);
		assertEquals(exp.getHeight(), obs.getHeight(), 0.001);
	}

	@Test
	public void testProfilingIsIndependentOfCellRotation() throws Exception {
		// create cells with different x and y bases and no variation.
		// Check they all generate the same profiles

		IProfile globalMedian = null;

		int xBase = 10;
		int yBase = 10;

		// Allow variation in angles from none to 90 degrees
		for (int angle = 0; angle < 90; angle++) {

			IAnalysisDataset dataset = new TestDatasetBuilder(RNG_SEED).cellCount(N_CELLS)
					.xBase(xBase).yBase(yBase).baseWidth(40).baseHeight(50)
					.maxRotation(angle).randomOffsetProfiles(false).fixedProfileOffset(20)
					.profiled()
					.build();

			// Check the collection
			IProfile median = dataset.getCollection()
					.getProfileCollection()
					.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.MEDIAN);
			assertTrue(median != null);

			if (globalMedian == null)
				globalMedian = median;

			// We can't expect full precision equality due to cumulative single precision
			// interpolation
			assertTrue(equals(globalMedian.toFloatArray(), median.toFloatArray(), 0.001f));
		}
	}

	@Test
	public void testSingleSquareCellMedianProfileHasExpectedValuesForQuartiles() throws Exception {
		IAnalysisDataset dataset = new TestDatasetBuilder(RNG_SEED).cellCount(1)
				.baseHeight(40).baseWidth(40).profiled().build();

		IProfile median = dataset.getCollection()
				.getProfileCollection()
				.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.MEDIAN);

		IProfile q1 = dataset.getCollection()
				.getProfileCollection()
				.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.LOWER_QUARTILE);

		IProfile q3 = dataset.getCollection()
				.getProfileCollection()
				.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.LOWER_QUARTILE);

		assertTrue(equals(median.toFloatArray(), q1.toFloatArray(), 0.0001f));
		assertTrue(equals(median.toFloatArray(), q3.toFloatArray(), 0.0001f));
	}

	@Test
	public void testMultipleSquareCellMedianProfileHasExpectedValuesForQuartiles()
			throws Exception {
		IAnalysisDataset dataset = new TestDatasetBuilder(RNG_SEED).cellCount(1)
				.baseHeight(40).baseWidth(40).profiled().build();

		IProfile median = dataset.getCollection()
				.getProfileCollection()
				.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.MEDIAN);

		IProfile q1 = dataset.getCollection()
				.getProfileCollection()
				.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.LOWER_QUARTILE);

		IProfile q3 = dataset.getCollection()
				.getProfileCollection()
				.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.LOWER_QUARTILE);

		assertTrue(equals(median.toFloatArray(), q1.toFloatArray(), 0.0001f));
		assertTrue(equals(median.toFloatArray(), q3.toFloatArray(), 0.0001f));
	}

	@Test
	public void testAllNuclearParametersCalculated() throws Exception {

		File testFolder = TestResources.MOUSE_INPUT_FOLDER.getAbsoluteFile();
		IAnalysisOptions op = OptionsFactory.makeDefaultRodentAnalysisOptions(testFolder);

		NucleusDetectionMethod nm = new NucleusDetectionMethod(TestResources.MOUSE_INPUT_FOLDER,
				op);
		IAnalysisDataset d = nm.call().getFirstDataset();
		new DefaultDatasetProfilingMethod(d).call();

		for (Measurement stat : op.getRuleSetCollection().getMeasurableValues()) {
			if (stat.equals(Measurement.VARIABILITY))
				continue; // we can't test this on a per-nucleus level

			for (Nucleus n : d.getCollection().getNuclei()) {
				assertFalse("Measurement should be a number", Double.isNaN(n.getMeasurement(stat)));
				assertFalse("Measurement should be finite",
						Double.isInfinite(n.getMeasurement(stat)));
			}

			double value = d.getCollection().getMedian(stat, CellularComponent.NUCLEUS,
					MeasurementScale.PIXELS);
			assertFalse("Measurement should be a number", Double.isNaN(value));
			assertFalse("Measurement should be finite",
					Double.isInfinite(value));
		}
	}

	/**
	 * Running profiling should not change calculated measures such as area or
	 * perimeter. Profiling could change values dependent on orientation.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testProfilingDoesNotAlterMeasurementsOnSimulatedData() throws Exception {

		IAnalysisDataset d = new TestDatasetBuilder(ComponentTester.RNG_SEED)
				.cellCount(ComponentTester.N_CELLS)
				.ofType(RuleSetCollection.roundRuleSetCollection())
				.withMaxSizeVariation(1)
				.randomOffsetProfiles(true)
				.build();
		Nucleus nucleus = d.getCollection().getCells().stream().findFirst().get()
				.getPrimaryNucleus();

		Map<Measurement, Double> pre = new HashMap<>();
		for (Measurement stat : nucleus.getMeasurements()) {
			if (Measurement.VARIABILITY.equals(stat))
				continue; // we can't test this on a per-nucleus level
			pre.put(stat, nucleus.getMeasurement(stat));
		}

		// Check everything is OK before we profile
		for (Measurement stat : nucleus.getMeasurements()) {
			if (Measurement.VARIABILITY.equals(stat))
				continue; // we can't test this on a per-nucleus level
			assertEquals(stat + " should not change", pre.get(stat), nucleus.getMeasurement(stat),
					0.0000001);
		}

		new DefaultDatasetProfilingMethod(d).call();

		// Check if any values have changed
		for (Measurement stat : nucleus.getMeasurements()) {
			if (Measurement.VARIABILITY.equals(stat))
				continue; // we can't test this on a per-nucleus level
			assertEquals(stat + " should not change", pre.get(stat), nucleus.getMeasurement(stat),
					0.0000001);
		}
	}

	/**
	 * Running profiling should not change calculated measures such as area or
	 * perimeter. Profiling could change other values dependent on orientation.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testProfilingDoesNotAlterMeasurementsOnRealData() throws Exception {

		// Create a new dataset from mouse data
		File testFolder = TestResources.MOUSE_INPUT_FOLDER.getAbsoluteFile();
		IAnalysisOptions op = OptionsFactory.makeDefaultRodentAnalysisOptions(testFolder);
		NucleusDetectionMethod nm = new NucleusDetectionMethod(TestResources.MOUSE_INPUT_FOLDER,
				op);
		IAnalysisDataset d = nm.call().getFirstDataset();

		// These are the measurements that don't depend on landmarks
		List<@NonNull Measurement> toTest = List.of(Measurement.AREA, Measurement.PERIMETER,
				Measurement.CIRCULARITY, Measurement.MIN_DIAMETER);

		// Store the results before profiling for comparison
		Map<UUID, Map<Measurement, Double>> pre = new HashMap<>();

		// Check the border list has not changed either
		Map<UUID, List<IPoint>> borders = new HashMap<>();

		for (Nucleus n : d.getCollection().getNuclei()) {
			Map<Measurement, Double> mes = new HashMap<>();
			for (Measurement stat : toTest) {
				if (Measurement.VARIABILITY.equals(stat))
					continue; // we can't test this on a per-nucleus level
				mes.put(stat, n.getMeasurement(stat));
			}
			// Store the current values
			pre.put(n.getId(), mes);

			borders.put(n.getId(), n.getBorderList());

			// Check current values match the stored values before we profile
			// Sanity check that nothing non-deterministic is happening
			assertEquals("Border list should not change", borders.get(n.getId()),
					n.getBorderList());

			for (Measurement stat : toTest) {
				if (Measurement.VARIABILITY.equals(stat))
					continue; // we can't test this on a per-nucleus level
				assertEquals(stat + " should not change", pre.get(n.getId()).get(stat),
						n.getMeasurement(stat), 0.0000001);
			}
		}

		new DefaultDatasetProfilingMethod(d).call();

		// Check each of the saved results against their current value.
		// Nothing should have changed in the test measurements.
		for (UUID id : pre.keySet()) {
			Nucleus n = d.getCollection().getNucleus(id).orElseThrow(NullPointerException::new);

			List<IPoint> prevBorder = borders.get(n.getId());
			List<IPoint> currBorder = n.getBorderList();
			assertEquals("Border list should not change", prevBorder, currBorder);

			// Check if any values have changed
			for (Measurement stat : toTest) {
				if (Measurement.VARIABILITY.equals(stat))
					continue; // we can't test this on a per-nucleus level

				assertEquals(stat + " should not change in " + n.getId(), pre.get(id).get(stat),
						n.getMeasurement(stat), 0.0000001);

			}

		}
	}

}
