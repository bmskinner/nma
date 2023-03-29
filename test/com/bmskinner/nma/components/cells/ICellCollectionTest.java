package com.bmskinner.nma.components.cells;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.swing.JPanel;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.bmskinner.nma.ComponentTester;
import com.bmskinner.nma.TestDatasetBuilder;
import com.bmskinner.nma.analysis.nucleus.CellCollectionFilterBuilder;
import com.bmskinner.nma.analysis.nucleus.CellCollectionFilterer;
import com.bmskinner.nma.analysis.nucleus.ConsensusAveragingMethod;
import com.bmskinner.nma.analysis.nucleus.FilteringOptions;
import com.bmskinner.nma.charting.ChartFactoryTest;
import com.bmskinner.nma.components.Statistical;
import com.bmskinner.nma.components.datasets.DefaultAnalysisDataset;
import com.bmskinner.nma.components.datasets.DefaultCellCollection;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.ICellCollection;
import com.bmskinner.nma.components.datasets.VirtualDataset;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.measure.MeasurementScale;
import com.bmskinner.nma.components.options.OptionsFactory;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;
import com.bmskinner.nma.components.profiles.MissingProfileException;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.components.rules.RuleSetCollection;
import com.bmskinner.nma.components.signals.ISignalGroup;

/**
 * Tests for implementations of the ICellCollection interface
 * 
 * @author ben
 * @since 1.14.0
 *
 */
@RunWith(Parameterized.class)
public class ICellCollectionTest {
	private static final int N_CELLS = 10;

	private ICellCollection collection;

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	@Parameter(0)
	public Class<? extends ICellCollection> source;

	@Before
	public void setUp() throws Exception {
		collection = createInstance(source);

	}

	/**
	 * Create an instance of the class under test
	 * 
	 * @param source the class to create
	 * @return
	 * @throws Exception
	 */
	public static ICellCollection createInstance(Class<? extends ICellCollection> source)
			throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(ComponentTester.RNG_SEED)
				.cellCount(N_CELLS)
				.ofType(RuleSetCollection.mouseSpermRuleSetCollection())
				.withMaxSizeVariation(10)
				.randomOffsetProfiles(true)
				.addSignalsInChannel(0).segmented().build();
		if (source == DefaultCellCollection.class) {
			return d.getCollection();
		}

		if (source == VirtualDataset.class) {
			VirtualDataset v = new VirtualDataset(d, TestDatasetBuilder.TEST_DATASET_NAME,
					TestDatasetBuilder.TEST_DATASET_UUID, d.getCollection());
			return v;
		}

		throw new Exception("Unable to create instance of " + source);
	}

	@Parameters
	public static Iterable<Class<? extends ICellCollection>> arguments() {
		return Arrays.asList(DefaultCellCollection.class, VirtualDataset.class);
	}

	@Test
	public void testGetID() {
		assertEquals(TestDatasetBuilder.TEST_DATASET_UUID, collection.getId());
	}

	@Test
	public void testSetName() {
		String name = "Cabbages";
		collection.setName(name);
		assertEquals(name, collection.getName());
	}

	@Test
	public void testGetName() {
		assertEquals(TestDatasetBuilder.TEST_DATASET_NAME, collection.getName());
	}

	@Test
	public void testGetCells() {
		List<ICell> cells = collection.getCells();
		assertEquals(N_CELLS, cells.size());
	}

	@Test
	public void testGetCellsFile() {
		Set<ICell> cells = collection
				.getCells(new File(TestDatasetBuilder.TEST_DATASET_IMAGE_FOLDER));
		assertEquals(N_CELLS, cells.size());
	}

	@Test
	public void testHasCellsFile() {
		assertTrue(collection.hasCells(new File(TestDatasetBuilder.TEST_DATASET_IMAGE_FOLDER)));
	}

	@Test
	public void testGetCellIDs() {
		Set<UUID> ids = collection.getCellIDs();
		assertTrue(collection.streamCells().allMatch(c -> ids.contains(c.getId())));
	}

	@Test
	public void testGetNuclei() {
		List<Nucleus> cells = collection.getNuclei();
		assertEquals(N_CELLS, cells.size());
	}

	@Test
	public void testGetNucleiFile() {
		Set<ICell> cells = collection
				.getCells(new File(TestDatasetBuilder.TEST_DATASET_IMAGE_FOLDER));
		assertEquals(N_CELLS, cells.size());
	}

	@Test
	public void testHasNuclei() {
		assertTrue(collection.hasNuclei(new File(TestDatasetBuilder.TEST_DATASET_IMAGE_FOLDER)));
	}

	@Test
	public void testAddCell() {
		ICell cell = mock(ICell.class);
		UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
		when(cell.getId()).thenReturn(id);
		if (collection.isVirtual())
			exception.expect(IllegalArgumentException.class);
		collection.addCell(cell);
		assertTrue(collection.contains(id));
	}

	@Test
	public void testGetCell() {
		for (UUID id : collection.getCellIDs())
			assertEquals(id, collection.getCell(id).getId());
	}

	@Test
	public void testRemoveCell() {
		ICell c = collection.streamCells().findFirst().get();
		assertTrue(collection.contains(c));
		collection.removeCell(c);

		List<ICell> cells = collection.getCells();
		assertEquals(N_CELLS - 1, cells.size());

		assertEquals(N_CELLS - 1, collection.size());
		assertFalse(collection.contains(c));

	}

	@Test
	public void testSize() {
		assertEquals(N_CELLS, collection.size());
	}

	@Test
	public void testGetNucleusCount() {
		assertEquals(N_CELLS, collection.getNucleusCount());

	}

	@Test
	public void testHasCells() {
		assertTrue(collection.hasCells());
	}

	@Test
	public void testContainsICell() {
		ICell c = collection.streamCells().findFirst().get();
		assertTrue(collection.contains(c));
	}

	@Test
	public void testContainsICellReturnsFalseOnNullInput() {
		assertFalse(collection.contains((ICell) null));
	}

	@Test
	public void testContainsUUID() {
		ICell c = collection.streamCells().findFirst().get();
		UUID id = c.getId();
		assertTrue(collection.contains(id));
	}

	@Test
	public void testContainsUUIDReturnsFalseOnNullInput() {
		assertFalse(collection.contains((UUID) null));
	}

	@Test
	public void testContainsUUIDReturnsFalseOnUUIDNotPresent() {
		assertFalse(collection.contains(TestDatasetBuilder.RED_SIGNAL_GROUP));
	}

	@Test
	public void testContainsExact() {
		ICell c = collection.streamCells().findFirst().get();
		UUID id = c.getPrimaryNucleus().getID();
		ICell cell = mock(ICell.class);
		when(cell.getId()).thenReturn(id);
		when(cell.hasNucleus()).thenReturn(false);
		when(cell.getPrimaryNucleus()).thenReturn(null);
		assertFalse(collection.containsExact(cell));
		assertTrue(collection.containsExact(c));
	}

	@Test
	public void testHasLockedCells() {
		assertFalse(collection.hasLockedCells());
	}

	@Test
	public void testSetCellsLocked() {
		assertFalse(collection.hasLockedCells());
		collection.setCellsLocked(true);
		assertTrue(collection.hasLockedCells());
		collection.setCellsLocked(false);
		assertFalse(collection.hasLockedCells());
	}

	@Test
	public void testGetProfileCollection() {
		assertTrue(collection.getProfileCollection() != null);
	}

	@Test
	public void testGetSignalGroupIDs() {
		assertTrue(collection.getSignalGroupIDs().size() == 1);
		assertTrue(collection.hasSignalGroup(TestDatasetBuilder.RED_SIGNAL_GROUP));
	}

	@Test
	public void testRemoveSignalGroup() {
		assertTrue(collection.hasSignalGroup(TestDatasetBuilder.RED_SIGNAL_GROUP));
		collection.removeSignalGroup(TestDatasetBuilder.RED_SIGNAL_GROUP);
		assertFalse(collection.hasSignalGroup(TestDatasetBuilder.RED_SIGNAL_GROUP));
	}

	@Test
	public void testAddSignalGroup() {
		ISignalGroup group = mock(ISignalGroup.class);
		when(group.getGroupName()).thenReturn("A test group");
		when(group.getId()).thenReturn(TestDatasetBuilder.GREEN_SIGNAL_GROUP);

		assertFalse(collection.hasSignalGroup(TestDatasetBuilder.GREEN_SIGNAL_GROUP));
		collection.addSignalGroup(group);
		assertTrue(collection.hasSignalGroup(TestDatasetBuilder.GREEN_SIGNAL_GROUP));
	}

	@Test
	public void testGetMaxProfileLength() {
		int exp = collection.streamCells().mapToInt(c -> {
			try {
				return c.getPrimaryNucleus().getProfile(ProfileType.ANGLE).size();
			} catch (MissingProfileException | ProfileException | MissingLandmarkException e) {
				return Integer.MAX_VALUE;
			}
		}).max().orElse(Integer.MAX_VALUE);
		assertEquals(exp, collection.getMaxProfileLength());
	}

	@Test
	public void testSetConsensus() throws Exception {
		// Run consensus averaging on the collection. Wrap in a new dataset.
		IAnalysisDataset d = new DefaultAnalysisDataset(collection,
				new File(TestDatasetBuilder.TEST_DATASET_IMAGE_FOLDER));
		d.setAnalysisOptions(OptionsFactory.makeDefaultRodentAnalysisOptions(
				new File(TestDatasetBuilder.TEST_DATASET_IMAGE_FOLDER).getAbsoluteFile()));

		assertFalse(collection.hasConsensus());
		new ConsensusAveragingMethod(d).call();

		assertTrue("Collection should have consensus", collection.hasConsensus());
	}

	@Test
	public void testGetConsensusOrientsVertically() throws Exception {

		// Ensure TV and BV are set
		collection.getProfileCollection().setLandmark(
				collection.getRuleSetCollection().getLandmark(OrientationMark.TOP)
						.orElseThrow(MissingLandmarkException::new),
				CellularComponent.wrapIndex(0, collection.getMedianArrayLength()));

		collection.getProfileCollection().setLandmark(
				collection.getRuleSetCollection().getLandmark(OrientationMark.BOTTOM)
						.orElseThrow(MissingLandmarkException::new),
				CellularComponent.wrapIndex(10, collection.getMedianArrayLength()));

		// Run consensus averaging on the collection. Wrap in a new dataset.
		// Analysis options will not be copied - create anew
		File testFolder = new File(TestDatasetBuilder.TEST_DATASET_IMAGE_FOLDER);
		IAnalysisDataset d = new DefaultAnalysisDataset(collection, testFolder);
		d.setAnalysisOptions(OptionsFactory.makeDefaultRodentAnalysisOptions(testFolder));

		new ConsensusAveragingMethod(d).call();

		// Test that the consensus has the same indexes as the template
		// collection
		Nucleus n = d.getCollection().getConsensus();
		IPoint tv = n.getBorderPoint(OrientationMark.TOP);
		IPoint bv = n.getBorderPoint(OrientationMark.BOTTOM);
		assertTrue("Points should be vertical for tv=" + tv + " bv=" + bv,
				ComponentTester.areVertical(tv, bv));

		// Now test that updating the TV to any index still allows orientation

		assertTrue(n.getBorderLength() < d.getCollection().getMedianArrayLength());

		int bIndex = 0; // so that it never overlaps TV in the loop
		// Start from 3 so that the smaller consensus profile does not get
		// the TV assigned to index 0 when interpolating
		for (int tIndex = 1; tIndex < d.getCollection().getMedianArrayLength(); tIndex++) {

			collection.getProfileCollection().setLandmark(
					collection.getRuleSetCollection().getLandmark(OrientationMark.TOP)
							.orElseThrow(MissingLandmarkException::new),
					CellularComponent.wrapIndex(tIndex, collection.getMedianArrayLength()));

			collection.getProfileCollection().setLandmark(
					collection.getRuleSetCollection().getLandmark(OrientationMark.BOTTOM)
							.orElseThrow(MissingLandmarkException::new),
					CellularComponent.wrapIndex(bIndex, collection.getMedianArrayLength()));

			assertNotEquals("TV and BV should not have the same index in the median", bIndex,
					tIndex);
			assertEquals("Median TV should be", tIndex,
					collection.getProfileCollection().getLandmarkIndex(OrientationMark.TOP));
			assertEquals("Median BV should be", bIndex,
					collection.getProfileCollection().getLandmarkIndex(OrientationMark.BOTTOM));

			// Check that the update has been made to the consensus
			n = d.getCollection().getConsensus();
			int nTIndex = n.getBorderIndex(OrientationMark.TOP);
			int nBIndex = n.getBorderIndex(OrientationMark.BOTTOM);
			if (nTIndex == nBIndex)
				continue; // we can't test if they end up on the same index due to differences in
							// perimeter
							// versus the median
//			assertNotEquals("TV index and BV index should not be the same index in consensus nucleus", nTIndex, nBIndex);

			List<JPanel> panels = new ArrayList<>();
			panels.add(ChartFactoryTest.makeConsensusChartPanel(d));

			n = collection.getConsensus(); // is aligned vertically
			tv = n.getBorderPoint(OrientationMark.TOP);
			bv = n.getBorderPoint(OrientationMark.BOTTOM);

			assertNotEquals("TV and BV should not be the same point in consensus nucleus", tv, bv);

			boolean areVertical = ComponentTester.areVertical(tv, bv);
			if (!areVertical)
				ChartFactoryTest.showCharts(panels, "TV: " + tIndex + " BV " + bIndex);
			assertTrue("Points should be vertical for tv=" + tv + " bv=" + bv, areVertical);
		}

	}

	@Test
	public void testFilterCollection() throws Exception {
		double medianArea = collection.getMedian(Measurement.AREA, CellularComponent.NUCLEUS,
				MeasurementScale.PIXELS);

		FilteringOptions op = new CellCollectionFilterBuilder()
				.add(Measurement.AREA, CellularComponent.NUCLEUS, MeasurementScale.PIXELS,
						medianArea, medianArea * 10)
				.build();

		ICellCollection c = CellCollectionFilterer.filter(collection, op);

		assertTrue("Filtering in " + source.getSimpleName(),
				c.getNucleusCount() < collection.getNucleusCount());
	}

	@Test
	public void testGetDifferenceToMedian() throws MissingLandmarkException {

		for (Nucleus n : collection.getNuclei()) {
			double d = collection.getNormalisedDifferenceToMedian(OrientationMark.REFERENCE, n);
			assertNotEquals(Double.NaN, d);
			assertNotEquals(Statistical.ERROR_CALCULATING_STAT, d);
		}

	}

}
