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

import static org.junit.Assert.*;

import java.lang.reflect.Field;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.io.SampleDatasetReader;

/**
 * @author bms41
 * @since 1.13.8
 *
 */
public class DefaultCellCollectionTest extends ComponentTest {
	private static final long RNG_SEED = 1234;
	private static final int N_CELLS = 10;
	private static final int N_CHILD_DATASETS = 2;

    private ICellCollection c;
    
    @Rule
    public final ExpectedException exception = ExpectedException.none();
    
    @Before
    public void setUp() throws Exception {
    	IAnalysisDataset d = new TestDatasetBuilder(RNG_SEED).cellCount(N_CELLS)
				.ofType(NucleusType.ROUND)
				.withMaxSizeVariation(10)
				.randomOffsetProfiles(true)
				.numberOfClusters(N_CHILD_DATASETS)
				.segmented().build();
    	c = d.getCollection();
    }
    
    @Test
    public void testDuplicate() throws Exception {
    	ICellCollection dup = c.duplicate();
    	testDuplicatesByField(c, dup);
//
//    	assertEquals("Id", c.getID(), dup.getID());
//    	assertEquals("Signal group ids", c.getSignalGroupIDs(), dup.getSignalGroupIDs());
//    	assertEquals("Signal groups", c.getSignalGroups(), dup.getSignalGroups());
//    	assertEquals("Folder", c.getFolder(), dup.getFolder());
//    	assertEquals("Output folder", c.getOutputFolder(), dup.getOutputFolder());
//    	assertEquals("Output folder name", c.getOutputFolderName(), dup.getOutputFolderName());
    }
        
    @Test
	public void testIsVirtual() {
		assertFalse(c.isVirtual());
	}

	@Test
	public void testIsReal() {
		assertTrue(c.isReal());
	}

}
