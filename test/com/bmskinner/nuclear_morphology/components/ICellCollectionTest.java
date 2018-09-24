package com.bmskinner.nuclear_morphology.components;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclei.DefaultNucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.logging.ConsoleHandler;
import com.bmskinner.nuclear_morphology.logging.LogPanelFormatter;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Tests for implementations of the ICellCollection interface
 * @author ben
 * @since 1.14.0
 *
 */
@RunWith(Parameterized.class)
public class ICellCollectionTest {
	
	private static final long RNG_SEED = 1234;
	
	private static final int N_CELLS = 10;
	
	private Logger logger;

	private ICellCollection collection;

	@Parameter(0)
	public Class<? extends ICellCollection> source;

	@Before
	public void setUp() throws Exception {
		collection = createInstance(source);
		logger = Logger.getLogger(Loggable.CONSOLE_LOGGER);
		logger.setLevel(Level.FINE);
		logger.addHandler(new ConsoleHandler(new LogPanelFormatter()));
	}

	/**
	 * Create an instance of the class under test
	 * @param source the class to create
	 * @return
	 * @throws Exception 
	 */
	public static ICellCollection createInstance(Class<? extends ICellCollection> source) throws Exception {

		if(source==DefaultCellCollection.class){
			IAnalysisDataset d = new TestDatasetBuilder(RNG_SEED).cellCount(N_CELLS)
					.ofType(NucleusType.ROUND)
					.randomOffsetProfiles(true)
					.segmented().build();
			return d.getCollection();
		}
		
		if(source==VirtualCellCollection.class){
			IAnalysisDataset d = new TestDatasetBuilder(RNG_SEED).cellCount(10)
					.ofType(NucleusType.ROUND)
					.randomOffsetProfiles(true)
					.segmented().build();
			d.getCollection().filter((c)->true);
			ICellCollection v = new VirtualCellCollection(d, "Test", TestDatasetBuilder.TEST_DATASET_UUID);
			for(ICell c : d.getCollection().getCells()) {
				v.addCell(c);
			}
			
			d.addChildCollection(v);
			return v;
		}

		throw new Exception("Unable to create instance of "+source);
	}

	@SuppressWarnings("unchecked")
	@Parameters
	public static Iterable<Class<? extends ICellCollection>> arguments() {
		return Arrays.asList(DefaultCellCollection.class,
				VirtualCellCollection.class);
	}
	
	@Test
	public void testGetID() {
		assertEquals(TestDatasetBuilder.TEST_DATASET_UUID, collection.getID());
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
		fail("Not yet implemented");
	}

	@Test
	public void testStreamCells() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetCellsFile() {
		Set<ICell> cells = collection.getCells( new File(TestDatasetBuilder.TEST_DATASET_IMAGE_FOLDER));
		assertEquals(N_CELLS, cells.size());
	}

	@Test
	public void testHasCellsFile() {
		assertTrue(collection.hasCells(new File(TestDatasetBuilder.TEST_DATASET_IMAGE_FOLDER)));
	}

	@Test
	public void testGetCellIDs() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetNuclei() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetNucleiFile() {
		fail("Not yet implemented");
	}

	@Test
	public void testHasNuclei() {
		assertTrue(collection.hasNuclei(new File(TestDatasetBuilder.TEST_DATASET_IMAGE_FOLDER)));
	}

	@Test
	public void testAddCell() {
		fail("Not yet implemented");
	}

	@Test
	public void testReplaceCell() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetCell() {
		
	}

	@Test
	public void testGetNucleusType() {
		assertEquals(NucleusType.ROUND, collection.getNucleusType());
	}

	@Test
	public void testRemoveCell() {
		ICell c = collection.streamCells().findFirst().get();
		UUID id = c.getNucleus().getID();
		collection.removeCell(c);
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
	public void testContainsUUID() {
		ICell c = collection.streamCells().findFirst().get();
		UUID id = c.getNucleus().getID();
		assertTrue(collection.contains(id));
	}

	@Test
	public void testContainsExact() {
		fail("Not yet implemented");
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
		fail("Not yet implemented");
	}

	@Test
	public void testCreateProfileCollection() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetFolder() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetOutputFolderName() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetOutputFolder() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetOutputFolder() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetImageFiles() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetSignalGroupIDs() {
		fail("Not yet implemented");
	}

	@Test
	public void testRemoveSignalGroup() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetSignalGroup() {
		fail("Not yet implemented");
	}

	@Test
	public void testHasSignalGroup() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetSignalGroups() {
		fail("Not yet implemented");
	}

	@Test
	public void testAddSignalGroup() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetSignalManager() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetRuleSetCollection() {
		fail("Not yet implemented");
	}

	@Test
	public void testUpdateVerticalNuclei() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetSourceFolder() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetNucleusMostSimilarToMedian() {
		fail("Not yet implemented");
	}

	@Test
	public void testCountSharedIAnalysisDataset() {
		fail("Not yet implemented");
	}

	@Test
	public void testCountSharedICellCollection() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetSharedCount() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetMedianArrayLength() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetMaxProfileLength() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetScale() {
		final double scale = 12;
		collection.setScale(scale);
		assertTrue(collection.streamCells().allMatch(c->c.getNucleus().getScale()==scale));
	}

	@Test
	public void testGetNormalisedDifferenceToMedian() {
		fail("Not yet implemented");
	}

}
