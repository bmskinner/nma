/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
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

package com.bmskinner.nma.samples.dummy;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import com.bmskinner.nma.analysis.DefaultAnalysisWorker;
import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.analysis.IAnalysisWorker;
import com.bmskinner.nma.analysis.profiles.DatasetProfilingMethod;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.DefaultCell;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.DefaultAnalysisDataset;
import com.bmskinner.nma.components.datasets.DefaultCellCollection;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.measure.MeasurementScale;
import com.bmskinner.nma.components.rules.RuleSetCollection;

public class DummyRodentCollection extends DefaultCellCollection {
	
	public DummyRodentCollection(int nuclei){
		
		super(RuleSetCollection.mouseSpermRuleSetCollection(), "test", UUID.randomUUID() );

		for(int i=0; i<nuclei; i++){
			
			ICell dummy;
            try {
                dummy = makeDummyCell(i);
                this.addCell(dummy);
            } catch (ComponentCreationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
			
		}
		
	}
	
	private ICell makeDummyCell(int i) throws ComponentCreationException{
		
		
		Nucleus n = new DummyRodentSpermNucleus("Nucleus "+i);
		ICell c = new DefaultCell(n);
		return c;
		
	}
	
	public static void main(String[] args){	
		
		DummyRodentCollection collection = new DummyRodentCollection(10000);
		IAnalysisDataset d = new DefaultAnalysisDataset(collection, new File("C:\\"));		
		IAnalysisMethod profiler = new DatasetProfilingMethod(d);
		
		IAnalysisWorker w = new DefaultAnalysisWorker(profiler);
		w.run();
		
		try {
			w.get();
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		double area;
		try {
			area = collection.getMedian(Measurement.AREA, CellularComponent.NUCLEUS, MeasurementScale.PIXELS);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		
	}

}
