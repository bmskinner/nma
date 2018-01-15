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

package components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.UUID;

import org.junit.Test;

import com.bmskinner.nuclear_morphology.components.DefaultCell;
import com.bmskinner.nuclear_morphology.components.DefaultMitochondrion;
import com.bmskinner.nuclear_morphology.components.IMitochondrion;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;

import samples.dummy.DummyRodentSpermNucleus;

public class DefaultCellTest {

    @Test
    public void testDefaultCellUUID() {
        
        UUID id = UUID.randomUUID();
        DefaultCell c = new DefaultCell(id);
        assertEquals(id, c.getId());
    }

    @Test
    public void testDefaultCellNucleus() {
        Nucleus n = new DummyRodentSpermNucleus();
        
        DefaultCell c = new DefaultCell(n);
        assertEquals(n, c.getNuclei().get(0));
    }

    @Test
    public void testDefaultCellICytoplasm() {
        fail("Not yet implemented");
    }

    @Test
    public void testDefaultCellICell() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetId() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetNucleus() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetNuclei() {
        fail("Not yet implemented");
    }

    @Test
    public void testHasStatistic() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetStatisticPlottableStatistic() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetStatisticPlottableStatisticMeasurementScale() {
        fail("Not yet implemented");
    }

    @Test
    public void testCalculateStatistic() {
        fail("Not yet implemented");
    }

    @Test
    public void testSetStatistic() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetStatistics() {
        fail("Not yet implemented");
    }

    @Test
    public void testSetNucleus() {
        fail("Not yet implemented");
    }

    @Test
    public void testAddNucleus() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetMitochondria() {
        fail("Not yet implemented");
    }

    @Test
    public void testSetMitochondria() {
        fail("Not yet implemented");
    }

    @Test
    public void testAddMitochondrion() {
        fail("Not yet implemented");
        
//        IMitochondrion m = new DefaultMitochondrion();
    }

    @Test
    public void testGetFlagella() {
        fail("Not yet implemented");
    }

    @Test
    public void testAddFlagellum() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetAcrosomes() {
        fail("Not yet implemented");
    }

    @Test
    public void testAddAcrosome() {
        fail("Not yet implemented");
    }

    @Test
    public void testHasAcrosome() {
        fail("Not yet implemented");
    }

    @Test
    public void testHasNucleus() {
        fail("Not yet implemented");
    }

    @Test
    public void testHasFlagellum() {
        fail("Not yet implemented");
    }

    @Test
    public void testHasMitochondria() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetCytoplasm() {
        fail("Not yet implemented");
    }

    @Test
    public void testHasCytoplasm() {
        fail("Not yet implemented");
    }

    @Test
    public void testSetCytoplasm() {
        fail("Not yet implemented");
    }

    @Test
    public void testSetScale() {
        fail("Not yet implemented");
    }

    @Test
    public void testEqualsObject() {
        fail("Not yet implemented");
    }

    @Test
    public void testCompareTo() {
        fail("Not yet implemented");
    }

}
