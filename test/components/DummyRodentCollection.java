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

package components;

import java.io.File;
import java.util.concurrent.ExecutionException;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetProfilingMethod;
import com.bmskinner.nuclear_morphology.components.DefaultAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.DefaultCell;
import com.bmskinner.nuclear_morphology.components.DefaultCellCollection;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.stats.NucleusStatistic;

public class DummyRodentCollection extends DefaultCellCollection {
	
	public DummyRodentCollection(int nuclei){
		
		super( new File("C:\\"), "out", "test", NucleusType.RODENT_SPERM );

		for(int i=0; i<nuclei; i++){
			
			ICell dummy = makeDummyCell(i);
			this.addCell(dummy);
		}
		
	}
	
	private ICell makeDummyCell(int i){
		
		
		Nucleus n = new DummyRodentSpermNucleus("Nucleus "+i);
		ICell c = new DefaultCell(n);
		return c;
		
	}
	
	public static void main(String[] args){
		
		System.out.println("Making collection");
		
		
		DummyRodentCollection collection = new DummyRodentCollection(10000);
		IAnalysisDataset d = new DefaultAnalysisDataset(collection);
//		System.out.println(collection.toString());
		
//		for(Nucleus n : collection.getNuclei()){
//			System.out.println(n.toString());
//		}
		
		IAnalysisMethod profiler = new DatasetProfilingMethod(d);
		
		IAnalysisWorker w = new DefaultAnalysisWorker(profiler);
		w.run();
		
		System.out.println("Waiting for profiler...");
		try {
			w.get();
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("Profiling complete");
		
//		collection.getNuclearStatistics(NucleusStatistic.AREA, MeasurementScale.PIXELS);
		
		double area;
		try {
			System.out.println("Fetching areas");
			area = collection.getMedianStatistic(NucleusStatistic.AREA, MeasurementScale.PIXELS);
			System.out.println("Median area: "+area);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		
	}

}
