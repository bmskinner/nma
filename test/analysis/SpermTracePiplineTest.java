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

package analysis;

import ij.ImageStack;
import ij.process.ImageProcessor;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.bmskinner.nuclear_morphology.analysis.image.ImageFilterer;
//import com.bmskinner.nuclear_morphology.analysis.nucleus.NucleusDetector;
import com.bmskinner.nuclear_morphology.components.DefaultCellCollection;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.options.DefaultAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.DefaultNucleusDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.MissingOptionException;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;
//import com.bmskinner.nuclear_morphology.utility.Constants;

public class SpermTracePiplineTest {
	
	/*
	 * The pipeline required for the SpermTrace app.
	 * 
	 * - import an image
	 * - run filtering and edge detection
	 * - run nucleus detection
	 * - add nucleus to collection
	 * - run profiling
	 * - run alignments
	 * - run clustering and make tree
	 * - return Newick tree
	 */

	public SpermTracePiplineTest(){
		
	}
	
	public ICellCollection newCollection(){
		ICellCollection c = new DefaultCellCollection(null, null, "test", NucleusType.RODENT_SPERM);
		return c;
	}
	
	public List<ICell> findCells(File imageFile){
		
		List<ICell> cells = new ArrayList<ICell>();
		ImageStack st;
		try {
			st = new ImageImporter(imageFile).importToStack();
		} catch (ImageImportException e1) {
			e1.printStackTrace();
			return cells;
		}
		
//		SpermTraceAnalysisOptions op = new SpermTraceAnalysisOptions();
//		
//		try {
//			ImageStack filt = new ImageFilterer(st)
//				.runKuwaharaFiltering(Constants.COUNTERSTAIN, 3)
//				.runEdgeDetector(Constants.COUNTERSTAIN, op.getDetectionOptions(IAnalysisOptions.NUCLEUS).getCannyOptions())
//				.toStack();
//			
//			
//			NucleusDetector nd = new NucleusDetector(op, null);
//			
//			cells = nd.getCells(filt, imageFile);
//			
//			
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		return cells;
	}
	
	public ICellCollection addCell(File imageFile, ICellCollection collection){
		
		if(collection == null){
			collection = newCollection();
		}
		
		List<ICell> cells = findCells(imageFile);
		
		for(ICell c : cells ){
			collection.addCell(c);

		}
		return collection;
	}
	
	public class SpermTraceAnalysisOptions extends DefaultAnalysisOptions {

		public SpermTraceAnalysisOptions(){
			
			this.setNucleusType(NucleusType.RODENT_SPERM);
			this.setAngleWindowProportion(0.05);
			
			IMutableDetectionOptions nucleusOptions = OptionsFactory.makeNucleusDetectionOptions( (File) null);
			
			nucleusOptions.setMinSize(100);
			nucleusOptions.setMaxSize(20000);
			nucleusOptions.setMinCirc(0);
			nucleusOptions.setMaxCirc(1);
			nucleusOptions.setNormaliseContrast(false);
			nucleusOptions.setScale(1);
			
			try {
				nucleusOptions.getCannyOptions().setClosingObjectRadius(5);
				nucleusOptions.getCannyOptions().setUseCanny(true);
			} catch (MissingOptionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			this.setDetectionOptions(IAnalysisOptions.NUCLEUS, nucleusOptions);

		}
		
		
	}
	
}
