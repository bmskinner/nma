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

package com.bmskinner.nuclear_morphology.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.awt.Paint;
import java.io.File;
import java.util.UUID;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.generic.Version;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;
import com.bmskinner.nuclear_morphology.io.SampleDatasetReader;

/**
 * Testing methods of the analysis dataset
 * @author bms41
 * @since 1.13.8
 *
 */
public class DefaultAnalysisDatasetTest {
	
	private static final long RNG_SEED = 1234;
	
	private static final int N_CELLS = 10;
	
	private static final int N_CHILD_DATASETS = 2;

	private Logger logger;
	
    private IAnalysisDataset d;
    private static final UUID CHILD_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CHILD_ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID CHILD_ID_NULL = UUID.fromString("00000000-0000-0000-0000-000000000000");
    
    
    @Rule
    public final ExpectedException exception = ExpectedException.none();
    
    @Before
    public void loadDataset() throws Exception {
    	d = new TestDatasetBuilder(RNG_SEED).cellCount(N_CELLS)
				.ofType(NucleusType.ROUND)
				.withMaxSizeVariation(0)
				.randomOffsetProfiles(true)
				.numberOfClusters(N_CHILD_DATASETS)
				.segmented().build();
    }

    @Test
    public void testDuplicate() throws Exception {
    	IAnalysisDataset dup = d.duplicate();
    	assertEquals(d, dup);
    }

  
    @Test
    public void testAddChildCollection() {

    	int defaultArea = TestDatasetBuilder.DEFAULT_BASE_HEIGHT * TestDatasetBuilder.DEFAULT_BASE_WIDTH;
    	
        ICellCollection c = d.getCollection().filterCollection(PlottableStatistic.AREA, MeasurementScale.PIXELS, defaultArea, defaultArea*2);
        UUID id = c.getID();
        
        d.addChildCollection(c);        
        assertEquals(N_CHILD_DATASETS+1, d.getChildCount());
        assertEquals(c, d.getChildDataset(id).getCollection());
    }

    @Test
    public void testAddChildDataset() {
    	int defaultArea = TestDatasetBuilder.DEFAULT_BASE_HEIGHT * TestDatasetBuilder.DEFAULT_BASE_WIDTH;
        ICellCollection c = d.getCollection().filterCollection(PlottableStatistic.AREA, MeasurementScale.PIXELS, defaultArea, defaultArea*2);
        IAnalysisDataset ch = new DefaultAnalysisDataset(c);
        UUID id = ch.getId();
        
        d.addChildDataset(ch);
        assertEquals(N_CHILD_DATASETS+1, d.getChildCount());
        assertEquals(ch, d.getChildDataset(id));
    }

  
    @Test
    public void testSetSavePath() {
        File f = new File(SampleDatasetReader.SAMPLE_DATASET_PATH+"Test.nmd");
        d.setSavePath(f);
        assertEquals(f, d.getSavePath());
    }

    @Test
    public void testHasMergeSources() {
        assertEquals(0, d.getMergeSources().size());
    }

    @Test
    public void testGetChildCount() {
        assertEquals(N_CHILD_DATASETS, d.getChildCount());
        int defaultArea = TestDatasetBuilder.DEFAULT_BASE_HEIGHT * TestDatasetBuilder.DEFAULT_BASE_WIDTH;
        ICellCollection c = d.getCollection().filterCollection(PlottableStatistic.AREA, MeasurementScale.PIXELS, defaultArea, defaultArea*2);
        d.addChildCollection(c);  
        assertEquals(N_CHILD_DATASETS+1, d.getChildCount());
    }

  
    @Test
    public void testGetVersion() {
        assertEquals(Version.currentVersion(), d.getVersion());
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
        Paint c = ColourSelecter.getColor(0);
        d.setDatasetColour(c);
        assertTrue(d.hasDatasetColour());
    }

    @Test
    public void testHasChildIAnalysisDataset() {
        
    }

    @Test
    public void testHasChildUUID() {
    	
    }

}
