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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult.Aggregation;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult.CountType;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult.Normalisation;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.samples.dummy.DummyCell;

public class KeyedShellResultTest {
    
    private KeyedShellResult k;
    private ICell c;
    private Nucleus n;
    private INuclearSignal s;
    private static final int N_SHELLS = 5;
    
    @Before
    public void setUp() throws ComponentCreationException{
        k = new KeyedShellResult(N_SHELLS);
        c = mock(ICell.class);
        when(c.getId()).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        
        n = mock(Nucleus.class);
        when(n.getID()).thenReturn(UUID.fromString("00000000-0000-0000-0001-000000000001"));

        s = mock(INuclearSignal.class);
        when(s.getID()).thenReturn(UUID.fromString("00000000-0000-0001-0000-000000000001"));
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
    public void testGetNonNormalisedProportionsWithSingleValueByNucleus() {

        long[] data = { 0, 0, 1, 0, 0 };
        double[] exp = { 0, 0, 1, 0, 0};
        k.addShellData(CountType.SIGNAL, c, n, data);
        double[] raw = k.getProportions(Aggregation.BY_NUCLEUS, Normalisation.NONE);
        assertTrue(equals(exp, raw));
    }
    
    @Test
    public void testGetNonNormalisedProportionsWithTwoValuesByNucleus() {

        long[] data1 = { 0, 0, 1, 0, 0 };
        long[] data2 = { 0, 0, 0, 1, 0 };
        double[] exp = { 0, 0, 0.5, 0.5, 0};

        k.addShellData(CountType.SIGNAL,  new DummyCell(), n, data1);
        k.addShellData(CountType.SIGNAL,  new DummyCell(), n, data2);
        
        double[] raw = k.getProportions(Aggregation.BY_NUCLEUS, Normalisation.NONE);
        assertTrue(equals(exp, raw));
    }

    @Test
    public void testGetNormalisedProportionsByNucleus() {
    	testGetNormalisedProportions(Aggregation.BY_NUCLEUS);
    }
    
    @Test
    public void testGetNormalisedProportionsBySignals() {
    	testGetNormalisedProportions(Aggregation.BY_SIGNAL);
    }
    
    private void testGetNormalisedProportions(@NonNull Aggregation agg) {
    	long[] sig = { 1, 2, 3, 4, 5 };
        long[] cnt = { 1, 2, 3, 4, 5 };
        
        if(agg.equals(Aggregation.BY_NUCLEUS))
        	s=null;
        
        k.addShellData(CountType.SIGNAL,  c, n, s, sig);
        k.addShellData(CountType.COUNTERSTAIN,  c, n, cnt);

     // Check the proportions are correct for non-normalised values
        double[] expRaw = { 1d/15d, 2d/15d, 3d/15d, 4d/15d, 5d/15d};
        double[] raw = k.getProportions(agg, Normalisation.NONE);
        assertTrue(equals(expRaw, raw));
        
        double[] exp = { 0.2, 0.2, 0.2, 0.2, 0.2};
        double[] norm = k.getProportions(agg, Normalisation.DAPI);
        assertTrue(equals(exp, norm));
    }

    @Test
    public void testGetNumberOfShells() {
        assertEquals(N_SHELLS, k.getNumberOfShells());
    }

    @Test
    public void testGetOverallShellForNonNormalisedDataByNucleus() {
        long[] sig = { 1, 1, 1, 1, 1 };
        double exp = 2;

        k.addShellData(CountType.SIGNAL,  c, n, sig);
        
        double obs = k.getOverallShell(Aggregation.BY_NUCLEUS, Normalisation.NONE);
        assertEquals(exp, obs, 0);
    }
    
    @Test
    public void testGetOverallShellForNonNormalisedDataBySignal() {
    	long[] sig = { 1, 1, 1, 1, 1 };
        double exp = 2;

        k.addShellData(CountType.SIGNAL,  c, n, s, sig);
        
        double obs = k.getOverallShell(Aggregation.BY_SIGNAL, Normalisation.NONE);
        assertEquals(exp, obs, 0);
    }
    
    @Test
    public void testGetOverallShellForNormalisedDataByNucleus() {
    	testGetOverallShellForNormalisedData(Aggregation.BY_NUCLEUS);
    }
    
    @Test
    public void testGetOverallShellForNormalisedDataBySignal() {
    	testGetOverallShellForNormalisedData(Aggregation.BY_SIGNAL);
    }
    
    private void testGetOverallShellForNormalisedData(@NonNull Aggregation agg) {
    	long[] sig = { 1, 2, 3, 4, 5 };
        long[] cnt = { 1, 2, 3, 4, 5 };
        
        if(agg.equals(Aggregation.BY_NUCLEUS))
        	s=null;
        
        k.addShellData(CountType.SIGNAL,  c, n, s, sig);
        k.addShellData(CountType.COUNTERSTAIN,  c, n, cnt);
        
        double exp = 2d;
        double obs = k.getOverallShell(agg, Normalisation.DAPI);
        assertEquals(exp, obs, 0);
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
