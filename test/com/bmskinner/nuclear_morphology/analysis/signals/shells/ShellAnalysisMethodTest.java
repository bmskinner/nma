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

package com.bmskinner.nuclear_morphology.analysis.signals.shells;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.DefaultShellOptions;
import com.bmskinner.nuclear_morphology.components.options.IShellOptions;
import com.bmskinner.nuclear_morphology.io.SampleDatasetReader;

public class ShellAnalysisMethodTest {
    
    private ShellAnalysisMethod m;
    private IAnalysisDataset d;
    
    @Before
    public void setUp() throws Exception{
        d = SampleDatasetReader.openTestRodentDataset();  
    }

    @Test
    public void testAreaMethodOnRodentDatasetWithNoSignals() throws Exception {
    	IShellOptions o = new DefaultShellOptions();
        m = new ShellAnalysisMethod(d, o);
        m.call();
    }

}
