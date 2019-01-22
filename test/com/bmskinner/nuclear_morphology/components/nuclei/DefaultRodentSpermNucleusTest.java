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

package com.bmskinner.nuclear_morphology.components.nuclei;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.*;

import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.samples.dummy.DummyRodentSpermNucleus;


public class DefaultRodentSpermNucleusTest {
    
    private Nucleus testNucleus;
    
    @Before
    public void setUp() throws ComponentCreationException{
        testNucleus = new DummyRodentSpermNucleus();
    }

    @Test
    public void testGetChannel() {
        int expected = 0;
        assertThat(testNucleus.getChannel(), is(expected));
    }

    @Test
    public void testGetScale() {
        double expected = 1;
        assertThat(testNucleus.getScale(), is(expected));
    }

    @Test
    public void testGetStatisticPlottableStatisticMeasurementScale() {
        double scale = 5;
        
        // Get and save the values with default scale 1
        Map<PlottableStatistic, Double> map = new HashMap<>();
        for(PlottableStatistic stat : PlottableStatistic.getNucleusStats(NucleusType.RODENT_SPERM)){
            map.put(stat, testNucleus.getStatistic(stat));
        }
        
        // Update scale
        testNucleus.setScale(scale);
        
        // Get the actual values for microns and pixels
        for(PlottableStatistic stat : PlottableStatistic.getNucleusStats(NucleusType.RODENT_SPERM)){
            double m = testNucleus.getStatistic(stat, MeasurementScale.MICRONS);
            
            double expected = PlottableStatistic.convert(map.get(stat), scale, MeasurementScale.MICRONS, stat.getDimension());
            assertEquals(stat.toString(), expected, m, 0);
            
            double d = testNucleus.getStatistic(stat, MeasurementScale.PIXELS);
            assertEquals(stat.toString(), map.get(stat), d, 0);
        }        
    }

    @Test
    public void testSetStatistic() {
        double epsilon = 0; // the amount of difference permitted 
        double expected = 25;
        
        for(PlottableStatistic stat : PlottableStatistic.getNucleusStats(NucleusType.RODENT_SPERM)){
            testNucleus.setStatistic(stat, expected);
            double d = testNucleus.getStatistic(stat);
            assertEquals(stat.toString(), expected, d, epsilon);
        }
    }
}
