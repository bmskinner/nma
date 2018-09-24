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
public class DefaultCellCollectionTest {
	
	private static final long RNG_SEED = 1234;
    private ICellCollection c;
    
    @Rule
    public final ExpectedException exception = ExpectedException.none();
    
    @Before
    public void loadDataset() throws Exception {
    	IAnalysisDataset d = new TestDatasetBuilder(RNG_SEED).cellCount(10)
				.ofType(NucleusType.ROUND)
				.randomOffsetProfiles(true)
				.segmented().build();
        c = d.getCollection();
    }
    
    @Test
    public void testDefaultCellCollectionFileStringStringNucleusType() {
        fail("Not yet implemented");
    }

    @Test
    public void testDefaultCellCollectionFileStringStringNucleusTypeUUID() {
        fail("Not yet implemented");
    }

    @Test
    public void testDefaultCellCollectionIAnalysisDatasetString() {
        fail("Not yet implemented");
    }

    @Test
    public void testDefaultCellCollectionICellCollectionString() {
        fail("Not yet implemented");
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
