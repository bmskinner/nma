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

package com.bmskinner.nuclear_morphology.components.nuclear;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult.Aggregation;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult.CountType;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult.Normalisation;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.samples.dummy.DummyCell;
import com.bmskinner.nuclear_morphology.samples.dummy.DummyNuclearSignal;
import com.bmskinner.nuclear_morphology.samples.dummy.DummyRodentSpermNucleus;

public class KeyedShellResultTest {
    
    private KeyedShellResult k;
    private ICell c;
    private Nucleus n;
    private INuclearSignal s;
    private static final int N_SHELLS = 5;
    
    @Before
    public void setUp() throws ComponentCreationException{
        k = new KeyedShellResult(N_SHELLS);
        c = new DummyCell();
        n = new DummyRodentSpermNucleus();
        s = new DummyNuclearSignal();
        c.addNucleus(n);
    }

    @Test
    public void testAddShellDataCountTypeICellNucleusLongArray() throws ComponentCreationException {
        
        long[] data = { 0, 0, 10, 0, 0 };
        k.addShellData(CountType.SIGNAL, c, n, data);
    }

    @Test
    public void testAddShellDataCountTypeICellNucleusINuclearSignalLongArray() {
        long[] data = { 0, 0, 10, 0, 0 };
        k.addShellData(CountType.SIGNAL, c, n, s, data);
    }
    
    @Test
    public void testGetRawMeansWithNoValue() {
       double[] raw = k.getProportions(Aggregation.BY_NUCLEUS, Normalisation.NONE);
        assertEquals(N_SHELLS, raw.length);
    }

    @Test
    public void testGetNonNormalisedProportionsWithSingleValue() {

        long[] data = { 0, 0, 1, 0, 0 };
        double[] exp = { 0, 0, 1, 0, 0};
        k.addShellData(CountType.SIGNAL, c, n, data);
        double[] raw = k.getProportions(Aggregation.BY_NUCLEUS, Normalisation.NONE);
        assertTrue(equals(exp, raw));
    }
    
    @Test
    public void testGetNonNormalisedProportionsWithTwoValues() {

        long[] data1 = { 0, 0, 1, 0, 0 };
        long[] data2 = { 0, 0, 0, 1, 0 };
        double[] exp = { 0, 0, 0.5, 0.5, 0};

        k.addShellData(CountType.SIGNAL,  new DummyCell(), n, data1);
        k.addShellData(CountType.SIGNAL,  new DummyCell(), n, data2);
        
        double[] raw = k.getProportions(Aggregation.BY_NUCLEUS, Normalisation.NONE);
        assertTrue(equals(exp, raw));
    }

    @Test
    public void testGetNormalisedProportions() {
        long[] sig = { 1, 2, 3, 4, 5 };
        long[] cnt = { 1, 2, 3, 4, 5 };
        double[] exp = { 0.2, 0.2, 0.2, 0.2, 0.2};

        k.addShellData(CountType.SIGNAL,  new DummyCell(), n, sig);
        k.addShellData(CountType.COUNTERSTAIN,  new DummyCell(), n, cnt);
        
        
     // Check the proportions are correct for non-normalised values
        double[] expRaw = { 1d/15d, 2d/15d, 3d/15d, 4d/15d, 5d/15d};
        double[] raw = k.getProportions(Aggregation.BY_NUCLEUS, Normalisation.NONE);
        assertTrue(equals(expRaw, raw));
        
        double[] norm = k.getProportions(Aggregation.BY_NUCLEUS, Normalisation.DAPI);
        assertTrue(equals(exp, norm));

    }

    @Test
    public void testGetNumberOfShells() {
        assertEquals(N_SHELLS, k.getNumberOfShells());
    }

    @Test
    public void testGetProportions() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetAverageProportion() {
        fail("Not yet implemented");
    }
    
    /**
     * A detailed comparison of arrays that will show *where* differences occur
     * @param exp
     * @param obs
     * @return
     */
    private boolean equals(double[] exp, double[] obs){
    	assertEquals(exp.length, obs.length);
    	for(int i=0; i<obs.length; i++) {
    		assertEquals("Shell "+i,exp[i], obs[i], 0);
    	}
    	return Arrays.equals(obs, exp);
    }
}
