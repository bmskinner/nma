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

import java.awt.Paint;
import java.io.File;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.generic.Version;
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
    
    private static IAnalysisDataset d;
    private static final UUID CHILD_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CHILD_ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID CHILD_ID_NULL = UUID.fromString("00000000-0000-0000-0000-000000000000");
    
    
    @Rule
    public final ExpectedException exception = ExpectedException.none();
    
    @Before
    public void loadDataset() throws Exception {
        d = SampleDatasetReader.openTestRodentDataset();
        IAnalysisDataset child1 = mock(IAnalysisDataset.class);
		when(child1.getId()).thenReturn(CHILD_ID_1);
		IAnalysisDataset child2 = mock(IAnalysisDataset.class);
		when(child2.getId()).thenReturn(CHILD_ID_2);
		d.addChildDataset(child1);
		d.addChildDataset(child2);
    }

    @Test
    public void testGetChildUUIDs() {
        fail("Not yet implemented");
    }

    @Test
    public void testDeleteClusterGroup() {
        fail("Not yet implemented");
    }

    @Test
    public void testDefaultAnalysisDatasetICellCollection() {
        fail("Not yet implemented");
    }

    @Test
    public void testDefaultAnalysisDatasetICellCollectionFile() {
        fail("Not yet implemented");
    }

    @Test
    public void testDuplicate() throws Exception {
    	IAnalysisDataset dup = d.duplicate();
    	assertEquals(d, dup);
    }

    @Test
    public void testGetLogHandler() {
        fail("Not yet implemented");
    }

    @Test
    public void testAddChildCollection() {

        // Values that take ~half the dataset
        ICellCollection c = d.getCollection().filterCollection(PlottableStatistic.AREA, MeasurementScale.PIXELS, 4850, 6000);
        UUID id = c.getID();
        
        d.addChildCollection(c);        
        assertEquals(1, d.getChildCount());
        assertEquals(c, d.getChildDataset(id).getCollection());
    }

    @Test
    public void testAddChildDataset() {
     // Values that take ~half the dataset
        ICellCollection c = d.getCollection().filterCollection(PlottableStatistic.AREA, MeasurementScale.PIXELS, 4850, 6000);
        IAnalysisDataset ch = new DefaultAnalysisDataset(c);
        UUID id = ch.getId();
        
        d.addChildDataset(ch);
        assertEquals(1, d.getChildCount());
        assertEquals(ch, d.getChildDataset(id));
    }

    @Test
    public void testGetSavePath() {
        fail("Not yet implemented");
    }

    @Test
    public void testSetSavePath() {
        File f = new File(SampleDatasetReader.SAMPLE_DATASET_PATH+"Test.nmd");
        d.setSavePath(f);
        assertEquals(f, d.getSavePath());
    }

    @Test
    public void testGetAllChildUUIDs() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetChildDataset() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetMergeSource() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetAllMergeSources() {
        fail("Not yet implemented");
    }

    @Test
    public void testAddMergeSource() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetMergeSources() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetMergeSourceIDs() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetAllMergeSourceIDs() {
        fail("Not yet implemented");
    }

    @Test
    public void testHasMergeSourceUUID() {
        fail("Not yet implemented");
    }

    @Test
    public void testHasMergeSourceIAnalysisDataset() {
        fail("Not yet implemented");
    }

    @Test
    public void testHasMergeSources() {
        assertEquals(0, d.getMergeSources().size());
    }

    @Test
    public void testGetChildCount() {
        assertFalse(d.hasChildren());
        assertEquals(0, d.getChildCount());
        ICellCollection c = d.getCollection().filterCollection(PlottableStatistic.AREA, MeasurementScale.PIXELS, 4850, 6000);
        d.addChildCollection(c);  
        assertTrue(d.hasChildren());
        assertEquals(1, d.getChildCount());
    }

    @Test
    public void testGetChildDatasets() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetAllChildDatasets() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetCollection() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetAnalysisOptions() {
        fail("Not yet implemented");
    }

    @Test
    public void testHasAnalysisOptions() {
        fail("Not yet implemented");
    }

    @Test
    public void testSetAnalysisOptions() {
        fail("Not yet implemented");
    }

    @Test
    public void testRefreshClusterGroups() {
        fail("Not yet implemented");
    }

    @Test
    public void testIsRoot() {
        fail("Not yet implemented");
    }

    @Test
    public void testSetRoot() {
        fail("Not yet implemented");
    }

    @Test
    public void testDeleteChild() {
        fail("Not yet implemented");
    }

    @Test
    public void testDeleteMergeSource() {
        fail("Not yet implemented");
    }

    @Test
    public void testToString() {
        fail("Not yet implemented");
    }

    @Test
    public void testUpdateSourceImageDirectory() {
        fail("Not yet implemented");
    }

    @Test
    public void testEqualsObject() {
        fail("Not yet implemented");
    }

    @Test
    public void testAbstractAnalysisDataset() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetVersion() {
        assertEquals(Version.v_1_13_8, d.getVersion());
    }

    @Test
    public void testGetUUID() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetName() {
        assertEquals("Testing", d.getName());
    }

    @Test
    public void testSetName() {
        String s = "Moose";
        d.setName(s);
        assertEquals(s, d.getName());
    }

    @Test
    public void testSetDatasetColour() {
        Paint c = ColourSelecter.getColor(0);
        d.setDatasetColour(c);
        assertEquals(c, d.getDatasetColour());
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
        
        IAnalysisDataset child1 = mock(IAnalysisDataset.class);
		when(child1.getId()).thenReturn(CHILD_ID_1);
		
		IAnalysisDataset childNull = mock(IAnalysisDataset.class);
		when(childNull.getId()).thenReturn(CHILD_ID_NULL);
		
		assertTrue(d.hasChild(child1));
		assertFalse(d.hasChild(childNull));
    }

    @Test
    public void testHasChildUUID() {
    	assertTrue(d.hasChild(CHILD_ID_1));
    	assertTrue(d.hasChild(CHILD_ID_2));
    	assertFalse(d.hasChild(CHILD_ID_NULL));
    }

    @Test
    public void testHasRecursiveChild() {
        fail("Not yet implemented");
    }

    @Test
    public void testAddClusterGroup() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetMaxClusterGroupNumber() {
        fail("Not yet implemented");
    }

    @Test
    public void testHasCluster() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetClusterGroups() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetClusterIDs() {
        fail("Not yet implemented");
    }

    @Test
    public void testHasClusters() {
        fail("Not yet implemented");
    }

    @Test
    public void testHasClusterGroup() {
        fail("Not yet implemented");
    }

}
