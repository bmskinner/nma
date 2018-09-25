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
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.samples.dummy.DummyRodentSpermNucleus;

public class DefaultCellTest {

	private static final long RNG_SEED = 1234;
	private static final int N_CELLS = 10;
	private static final int N_CHILD_DATASETS = 2;

    private ICell c;
    
    @Rule
    public final ExpectedException exception = ExpectedException.none();
    
    @Before
    public void loadDataset() throws Exception {
    	IAnalysisDataset d = new TestDatasetBuilder(RNG_SEED).cellCount(N_CELLS)
				.ofType(NucleusType.ROUND)
				.withMaxSizeVariation(1)
				.randomOffsetProfiles(true)
				.numberOfClusters(N_CHILD_DATASETS)
				.segmented().build();
    	c = d.getCollection().getCells().stream().findFirst().get();
    }
    
    @Test
    public void testDuplicate() throws Exception {
    	ICell dup = c.duplicate();
    	
    	assertEquals("Id", c.getId(), dup.getId());
    	for(PlottableStatistic stat : c.getStatistics())
    		assertEquals(stat.toString(), c.getStatistic(stat), dup.getStatistic(stat),0);
    	for(Field f : dup.getClass().getDeclaredFields()) {
			 f.setAccessible(true);			 
			 assertEquals(f.getName(), f.get(c), f.get(dup));
		 }
    	assertEquals("Cytoplasm", c.getCytoplasm(), dup.getCytoplasm());
    	assertEquals("Mitochondria", c.getMitochondria(), dup.getMitochondria());
    	assertEquals("Flagella", c.getFlagella(), dup.getFlagella());
    	assertEquals("Nuclei", c.getNuclei(), dup.getNuclei());
    	assertEquals("Cell", c, dup);
    }

}
