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
       double[] raw = k.getRawMeans(CountType.SIGNAL, Aggregation.BY_SIGNAL);
        assertEquals(N_SHELLS, raw.length);
    }

    @Test
    public void testGetRawMeansWithSingleValue() {

        long[] data = { 0, 0, 1, 0, 0 };
        double[] exp = { 0, 0, 1, 0, 0};
        k.addShellData(CountType.SIGNAL, c, n, data);
        double[] raw = k.getRawMeans(CountType.SIGNAL, Aggregation.BY_SIGNAL);
        assertTrue(Arrays.equals(raw, exp));
    }
    
    @Test
    public void testGetRawMeansWithTwoValues() {

        long[] data1 = { 0, 0, 1, 0, 0 };
        long[] data2 = { 0, 0, 0, 1, 0 };
        double[] exp = { 0, 0, 0.5, 0.5, 0};
        k.addShellData(CountType.SIGNAL, c, n, data1);
        k.addShellData(CountType.SIGNAL, c, n, data2);
        
        double[] raw = k.getRawMeans(CountType.SIGNAL, Aggregation.BY_SIGNAL);
        assertTrue(Arrays.equals(raw, exp));
    }

    @Test
    public void testGetNormalisedMeans() {
        long[] sig = { 1, 2, 3, 4, 5 };
        long[] cnt = { 1, 2, 3, 4, 5 };
        double[] exp = { 0.2, 0.2, 0.2, 0.2, 0.2};
        k.addShellData(CountType.SIGNAL, c, n, sig);
        k.addShellData(CountType.COUNTERSTAIN, c, n, cnt);
        
        double[] raw = k.getNormalisedMeans(CountType.SIGNAL, Aggregation.BY_SIGNAL);
        assertTrue(Arrays.equals(raw, exp));
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
}
