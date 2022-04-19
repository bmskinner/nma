/*******************************************************************************
 *      Copyright (C) 2016 Ben Skinner
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

package com.bmskinner.nuclear_morphology.components.datasets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.io.File;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jdom2.Element;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bmskinner.nuclear_morphology.ComponentTester;
import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.TestResources;
import com.bmskinner.nuclear_morphology.analysis.nucleus.CellCollectionFilterBuilder;
import com.bmskinner.nuclear_morphology.analysis.nucleus.CellCollectionFilterer;
import com.bmskinner.nuclear_morphology.analysis.nucleus.CellCollectionFilterer.CollectionFilteringException;
import com.bmskinner.nuclear_morphology.analysis.nucleus.FilteringOptions;
import com.bmskinner.nuclear_morphology.components.Version;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.measure.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;
import com.bmskinner.nuclear_morphology.logging.ConsoleFormatter;
import com.bmskinner.nuclear_morphology.logging.ConsoleHandler;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.Prefs;

/**
 * Testing methods of the analysis dataset
 * 
 * @author bms41
 * @since 1.13.8
 *
 */
public class DefaultAnalysisDatasetTest extends ComponentTester {

	private static final Logger LOGGER = Logger.getLogger(Loggable.PROJECT_LOGGER);

	private IAnalysisDataset d;
	private static final UUID CHILD_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID CHILD_ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
	private static final UUID CHILD_ID_NULL = UUID.fromString("00000000-0000-0000-0000-000000000000");

	static {
		Prefs.setThreads(2); // Attempt to avoid issue 162
		for (Handler h : LOGGER.getHandlers())
			LOGGER.removeHandler(h);
		Handler h = new ConsoleHandler(new ConsoleFormatter());
		LOGGER.setLevel(Level.FINE);
		h.setLevel(Level.FINE);
		LOGGER.addHandler(h);
	}

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	@Before
	public void loadDataset() throws Exception {
		d = new TestDatasetBuilder(RNG_SEED).cellCount(N_CELLS).ofType(RuleSetCollection.roundRuleSetCollection())
				.withMaxSizeVariation(10).randomOffsetProfiles(true).numberOfClusters(N_CHILD_DATASETS).segmented()
				.build();
	}

	@Test
	public void testDuplicate() throws Exception {
		IAnalysisDataset dup = d.copy();
		testDuplicatesByField(d.getName(), d, dup);
		assertEquals("Hashcodes should match", d.hashCode(), dup.hashCode());
	}

	@Test
	public void testAddChildCollection() throws Exception {

		double defaultArea = d.getCollection().getMedian(Measurement.AREA, CellularComponent.NUCLEUS,
				MeasurementScale.PIXELS);

		FilteringOptions op = new CellCollectionFilterBuilder()
				.add(Measurement.AREA, CellularComponent.NUCLEUS, MeasurementScale.PIXELS, defaultArea, defaultArea * 2)
				.build();

		ICellCollection c = CellCollectionFilterer.filter(d.getCollection(), op);
		UUID id = c.getId();

		assertTrue("Filtered collection should contain cells", c.size() > 0);
		d.addChildCollection(c);

		assertEquals("Dataset should have child count of", N_CHILD_DATASETS + 1, d.getChildCount());

		assertTrue("New child collection should be present", d.hasDirectChild(id));
	}

	@Test
	public void testAddChildDataset() throws Exception {
		double defaultArea = d.getCollection().getMedian(Measurement.AREA, CellularComponent.NUCLEUS,
				MeasurementScale.PIXELS);

		FilteringOptions op = new CellCollectionFilterBuilder()
				.add(Measurement.AREA, CellularComponent.NUCLEUS, MeasurementScale.PIXELS, defaultArea, defaultArea * 2)
				.build();

		ICellCollection c = CellCollectionFilterer.filter(d.getCollection(), op);

		IAnalysisDataset ch = new DefaultAnalysisDataset(c, new File(TestDatasetBuilder.TEST_DATASET_IMAGE_FOLDER));
		UUID id = ch.getId();

		assertTrue("Filtered collection should contain cells", c.size() > 0);
		d.addChildDataset(ch);

		assertEquals("Dataset should have child count of", N_CHILD_DATASETS + 1, d.getChildCount());

		assertTrue("New child collection should be present", d.hasDirectChild(id));
	}

	@Test
	public void testSetSavePath() {
		File f = new File(TestResources.DATASET_FOLDER + "Test.nmd");
		d.setSavePath(f);
		assertEquals(f, d.getSavePath());
	}

	@Test
	public void testHasMergeSources() {
		assertEquals(0, d.getMergeSources().size());
	}

	@Test
	public void testGetChildCount() throws CollectionFilteringException {
		assertEquals(N_CHILD_DATASETS, d.getChildCount());

		double defaultArea = d.getCollection().getMedian(Measurement.AREA, CellularComponent.NUCLEUS,
				MeasurementScale.PIXELS);

		FilteringOptions op = new CellCollectionFilterBuilder()
				.add(Measurement.AREA, CellularComponent.NUCLEUS, MeasurementScale.PIXELS, defaultArea, defaultArea * 2)
				.build();

		ICellCollection c = CellCollectionFilterer.filter(d.getCollection(), op);

		d.addChildCollection(c);
		assertEquals(N_CHILD_DATASETS + 1, d.getChildCount());
	}

	@Test
	public void testGetVersion() {
		assertEquals(Version.currentVersion(), d.getVersionCreated());
	}

	@Test
	public void testGetName() {
		assertEquals(TestDatasetBuilder.TEST_DATASET_NAME, d.getName());
	}

	@Test
	public void testSetName() {
		String s = "Moose";
		d.setName(s);
		assertEquals(s, d.getName());
	}

	@Test
	public void testSetDatasetColour() {
		d.setDatasetColour(Color.RED);
		assertEquals(Color.RED, d.getDatasetColour().get());
	}

	@Test
	public void testGetDatasetColour() {
		assertFalse(d.getDatasetColour().isPresent());
	}

	@Test
	public void testHasDatasetColour() {
		assertFalse(d.hasDatasetColour());
		Color c = ColourSelecter.getColor(0);
		d.setDatasetColour(c);
		assertTrue(d.hasDatasetColour());
	}

	@Test
	public void testHasChildIAnalysisDataset() {
		assertTrue(d.hasChildren());
		for (IAnalysisDataset child : d.getChildDatasets()) {
			assertTrue(d.hasDirectChild(child));
		}
	}

	@Test
	public void testHasChildUUID() {
		assertTrue(d.hasChildren());
		for (IAnalysisDataset child : d.getChildDatasets()) {
			assertTrue(d.hasDirectChild(child.getId()));
		}
	}

	/**
	 * Test that example data can be marshalled and unmarshalled correctly
	 * 
	 * @throws Exception
	 */
	@Test
	public void testXmlSerializes() throws Exception {

		Element e = d.toXmlElement();
		// files are not absolute on test dataset creation
		d.setSavePath(d.getSavePath().getAbsoluteFile());

		IAnalysisDataset test = DatasetCreator.createRoot(e);

		testDuplicatesByField(d.getName(), d, test);
		assertEquals(d, test);
	}

	@Test
	public void testCountSharedWorksWithChild() {
		for (IAnalysisDataset c : d.getAllChildDatasets()) {
			assertEquals(c.getCollection().size(), d.getCollection().countShared(c));
			assertEquals(c.getCollection().size(), c.getCollection().countShared(d));
		}
	}

}
