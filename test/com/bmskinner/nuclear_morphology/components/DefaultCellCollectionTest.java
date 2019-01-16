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

import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bmskinner.nuclear_morphology.ComponentTester;
import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.nucleus.ConsensusAveragingMethod;
import com.bmskinner.nuclear_morphology.charting.ChartFactoryTest;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileManager;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.stats.Stats;

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
    
    @Override
	@Before
    public void setUp() throws Exception {
    	super.setUp();
    	d = new TestDatasetBuilder(RNG_SEED).cellCount(N_CELLS)
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
    	testDuplicatesByField(dup.duplicate(), dup);
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
	public void testGetConsensusOrientsVertically() throws Exception {
		
		ProfileManager m = c.getProfileManager();
		IProfile p = c.getProfileCollection().getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
		m.updateBorderTag(Tag.TOP_VERTICAL, 0);
		m.updateBorderTag(Tag.BOTTOM_VERTICAL, 10);
		new ConsensusAveragingMethod(d).call();
		
		for(int tIndex=0; tIndex<p.size(); tIndex++) {
			for(int bIndex=0; bIndex<p.size(); bIndex++) {
				if(tIndex==bIndex)
					continue;
				if(Math.abs(tIndex-bIndex)<5)
					continue;
				m.updateBorderTag(Tag.TOP_VERTICAL, tIndex);
				m.updateBorderTag(Tag.BOTTOM_VERTICAL, bIndex);

				assertTrue(c.hasConsensus());

				List<JPanel> panels = new ArrayList<>();
				panels.add(ChartFactoryTest.makeConsensusChartPanel(d));
				ChartFactoryTest.showCharts(panels, "Consensus: "+tIndex+" - "+bIndex);
				
				
				Nucleus n = c.getConsensus(); // is aligned vertically
				IPoint tv = n.getBorderPoint(Tag.TOP_VERTICAL);
				IPoint bv = n.getBorderPoint(Tag.BOTTOM_VERTICAL);
				IPoint bi = IPoint.makeNew(bv.getX(), bv.getY()+10);
				assertEquals(tIndex+" and "+bIndex, 0, bv.findSmallestAngle(tv, bi), 0.1);
//				assertEquals(tIndex+" and "+bIndex, tv.getX(), bv.getX(), 0.001);
//				assertTrue(tIndex+" and "+bIndex, tv.getY()>bv.getY());
				
			}
		}
		
	}

}
