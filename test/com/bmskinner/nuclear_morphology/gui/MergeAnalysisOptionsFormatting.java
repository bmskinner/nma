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
 ********************************** *******************************************************************************/

package com.bmskinner.nuclear_morphology.gui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;

import javax.swing.table.TableModel;

import org.junit.Test;

import com.bmskinner.nuclear_morphology.charting.datasets.AnalysisDatasetTableCreator;
import com.bmskinner.nuclear_morphology.charting.options.DefaultTableOptions.TableType;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.options.DefaultAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.DefaultNucleusDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableDetectionOptions;

public class MergeAnalysisOptionsFormatting {
		
	
	@Test
	public void sameOptionsProduceSameTable(){
		
		IAnalysisOptions op1 = makeTestOptions1();
		IAnalysisOptions op2 = makeTestOptions1();
		
		TableOptions tableOptions = new TableOptionsBuilder()
			.setDatasets(null)
			.setType(TableType.ANALYSIS_PARAMETERS)
			.build();
		
		TableModel model = new AnalysisDatasetTableCreator(tableOptions).createAnalysisTable();

			fail("Profile should not be created with null input");
		

	}
	
	public IAnalysisOptions makeTestOptions1(){
		IMutableAnalysisOptions op = new DefaultAnalysisOptions();
		
		op.setAngleWindowProportion(0.05);
		op.setNucleusType(NucleusType.RODENT_SPERM);
		
		IMutableDetectionOptions nucleus = new DefaultNucleusDetectionOptions( (File) null );
		op.setDetectionOptions(IAnalysisOptions.NUCLEUS, nucleus);
		return op;
	}
}
