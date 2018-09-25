package com.bmskinner.nuclear_morphology.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
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
	
	@Rule
    public final ExpectedException exception = ExpectedException.none();

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
		IAnalysisDataset d = new TestDatasetBuilder(RNG_SEED).cellCount(N_CELLS)
				.ofType(NucleusType.ROUND)
				.randomOffsetProfiles(true)
				.addSignalsInChannel(0)
				.segmented().build();
		if(source==DefaultCellCollection.class){
			return d.getCollection();
		}
		
		if(source==VirtualCellCollection.class){
			ICellCollection v = new VirtualCellCollection(d,TestDatasetBuilder.TEST_DATASET_NAME, TestDatasetBuilder.TEST_DATASET_UUID);
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
		Set<ICell> cells = collection.getCells();
		assertEquals(N_CELLS, cells.size());
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
		Set<UUID> ids = collection.getCellIDs();
		assertTrue(collection.streamCells().allMatch(c->ids.contains(c.getId())));
	}

	@Test
	public void testGetNuclei() {
		Set<Nucleus> cells = collection.getNuclei();
		assertEquals(N_CELLS, cells.size());
	}

	@Test
	public void testGetNucleiFile() {
		Set<ICell> cells = collection.getCells( new File(TestDatasetBuilder.TEST_DATASET_IMAGE_FOLDER));
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
		if(collection.isVirtual())
			exception.expect(IllegalArgumentException.class);
		collection.addCell(cell);
		assertTrue(collection.contains(id));
	}

	@Test
	public void testGetCell() {
		for(UUID id : collection.getCellIDs())
			assertEquals(id, collection.getCell(id).getId());
	}

	@Test
	public void testGetNucleusType() {
		assertEquals(NucleusType.ROUND, collection.getNucleusType());
	}

	@Test
	public void testRemoveCell() {
		ICell c = collection.streamCells().findFirst().get();
		assertTrue(collection.contains(c));
		collection.removeCell(c);
		
		Set<ICell> cells = collection.getCells();
		cells.remove(c);
		assertEquals(N_CELLS-1, cells.size());
				
		assertEquals(N_CELLS-1, collection.size());
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
		assertFalse(collection.contains((ICell)null));
	}

	@Test
	public void testContainsUUID() {
		ICell c = collection.streamCells().findFirst().get();
		UUID id = c.getId();
		assertTrue(collection.contains(id));
	}

	@Test
	public void testContainsUUIDReturnsFalseOnNullInput() {
		assertFalse(collection.contains((UUID)null));
	}
	
	@Test
	public void testContainsUUIDReturnsFalseOnUUIDNotPresent() {
		assertFalse(collection.contains(TestDatasetBuilder.RED_SIGNAL_GROUP));
	}
	
	@Test
	public void testContainsExact() {
		ICell c = collection.streamCells().findFirst().get();
		UUID id = c.getNucleus().getID();
		ICell cell = mock(ICell.class);
		when(cell.getId()).thenReturn(id);
		when(cell.hasNucleus()).thenReturn(false);
		when(cell.getNucleus()).thenReturn(null);
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
		assertTrue(collection.getProfileCollection()!=null);
	}

	@Test
	public void testGetFolder() {
		assertTrue(collection.getFolder().equals(new File(TestDatasetBuilder.TEST_DATASET_IMAGE_FOLDER)));
	}

	@Test
	public void testGetOutputFolderName() {
		assertTrue(collection.getOutputFolderName().equals(TestDatasetBuilder.TEST_DATASET_NAME));
	}

	@Test
	public void testGetSignalGroupIDs() {
		assertTrue(collection.getSignalGroupIDs().size()==1);
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
		when( group.getGroupName()).thenReturn("A test group");
		
		assertFalse(collection.hasSignalGroup(TestDatasetBuilder.GREEN_SIGNAL_GROUP));
		collection.addSignalGroup(TestDatasetBuilder.GREEN_SIGNAL_GROUP, group);
		assertTrue(collection.hasSignalGroup(TestDatasetBuilder.GREEN_SIGNAL_GROUP));
	}

	@Test
	public void testGetMaxProfileLength() {
		int exp = collection.streamCells().mapToInt(c->{
			try {
				return c.getNucleus().getProfile(ProfileType.ANGLE).size();
			} catch (UnavailableProfileTypeException e) {
				return Integer.MAX_VALUE;
			}
		}).max().orElse(Integer.MAX_VALUE);
		assertEquals(exp, collection.getMaxProfileLength());
	}

	@Test
	public void testSetScale() {
		final double scale = 12;
		collection.setScale(scale);
		assertTrue(collection.streamCells().allMatch(c->c.getNucleus().getScale()==scale));
	}


}
