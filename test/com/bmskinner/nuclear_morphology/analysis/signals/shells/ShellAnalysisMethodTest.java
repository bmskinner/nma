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

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.components.TestDatasetBuilder.TestComponentShape;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.options.DefaultShellOptions;
import com.bmskinner.nuclear_morphology.io.DatasetExportMethod;

public class ShellAnalysisMethodTest {
    
	private static final long SEED = 1234;
    private IAnalysisDataset d;
    
    @Before
    public void setUp() throws Exception{
        d = new TestDatasetBuilder(SEED).cellCount(10)
        		.withMaxSizeVariation(20)
        		.maxRotation(90)
        		.xBase(50).yBase(50)
        		.baseWidth(50).baseHeight(50)
        		.ofType(NucleusType.ROUND)
        		.withNucleusShape(TestComponentShape.SQUARE)
        		.addSignalsInChannel(0)
        		.addSignalsInChannel(1)
        		.build();
    }

    @Test
    public void test() throws Exception {
    	new ShellAnalysisMethod(d, new DefaultShellOptions()).call();
    }

}
