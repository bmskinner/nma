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

package com.bmskinner.nma.components.cells;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bmskinner.nma.ComponentTester;
import com.bmskinner.nma.TestDatasetBuilder;
import com.bmskinner.nma.components.datasets.DefaultCellCollection;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.ICellCollection;
import com.bmskinner.nma.components.rules.RuleSetCollection;

/**
 * @author bms41
 * @since 1.13.8
 *
 */
public class DefaultCellCollectionTest extends ComponentTester {

    private ICellCollection c;
    private IAnalysisDataset d;
    
    @Rule
    public final ExpectedException exception = ExpectedException.none();
    
	@Before
    public void setUp() throws Exception {
    	d = new TestDatasetBuilder(ComponentTester.RNG_SEED).cellCount(N_CELLS)
				.ofType(RuleSetCollection.roundRuleSetCollection())
				.withMaxSizeVariation(10)
				.randomOffsetProfiles(true)
				.numberOfClusters(N_CHILD_DATASETS)
				.segmented().build();
    	c = d.getCollection();
    }
    
    @Test
    public void testDuplicate() throws Exception {
    	ICellCollection dup = c.duplicate();
    	testDuplicatesByField("Cell collection "+c.getName(), c, dup);
    }
        
    @Test
	public void testIsVirtual() {
		assertFalse(c.isVirtual());
	}

	@Test
	public void testIsReal() {
		assertTrue(c.isReal());
	}
	
	@Test
	public void testXmlSerializes() throws Exception {
		
		Element e = c.toXmlElement();
		
		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());

		ICellCollection test = new DefaultCellCollection(e);

		testDuplicatesByField("Cell collection "+c.getName(), c, test);
		assertEquals(c, test);
	}

}
