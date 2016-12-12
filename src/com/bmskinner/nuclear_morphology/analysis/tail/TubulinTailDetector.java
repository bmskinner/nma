/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
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
package com.bmskinner.nuclear_morphology.analysis.tail;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.filter.Binary;
import ij.process.BinaryProcessor;
import ij.process.ByteProcessor;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import com.bmskinner.nuclear_morphology.analysis.AnalysisOptions;
import com.bmskinner.nuclear_morphology.analysis.AnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.analysis.AnalysisOptions.CannyOptions;
import com.bmskinner.nuclear_morphology.analysis.detection.CannyEdgeDetector;
import com.bmskinner.nuclear_morphology.analysis.detection.Detector;
import com.bmskinner.nuclear_morphology.analysis.image.ImageFilterer;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.IMutableCell;
import com.bmskinner.nuclear_morphology.components.SpermTail;
import com.bmskinner.nuclear_morphology.components.generic.XYPoint;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.utility.Constants;

import mmorpho.MorphoProcessor;
import mmorpho.StructureElement;
import skeleton_analysis.AnalyzeSkeleton_;
import skeleton_analysis.Edge;
import skeleton_analysis.Graph;
import skeleton_analysis.SkeletonResult;



/**
 * This class is to test ideas for detecting sperm tails stained with
 * anti-tubulin. It can be consetucted as SwingWorker for GUI incorpoation,
 * or the static method run() can be called directly for scripted use. When used
 * as a SwingWorker, the reported progress will be the number of cells processed, and
 * upon completion, a "Finished" property change event is fired.
 */
public class TubulinTailDetector extends AnalysisWorker {

	private final File folder;
	private final int channel;
	
	private static final int WHITE = 255;
	
	public TubulinTailDetector(IAnalysisDataset dataset, File folder, int channel){
		super(dataset);
		this.folder = folder;
		this.channel = channel;
		
//		fileLogger = Logger.getLogger(TubulinTailDetector.class.getName());
//		fileLogger.addHandler(dataset.getLogHandler());
		
		this.setProgressTotal(dataset.getCollection().size());
	}
	
//	@Override
//	protected void process( List<Integer> integers ) {
//		//update the number of entries added
//		int amount = integers.get( integers.size() - 1 );
//		int totalCells = dataset.getCollection().getNucleusCount();
//		int percent = (int) ( (double) amount / (double) totalCells * 100);
//		setProgress(percent); // the integer representation of the percent
//	}
	
	@Override
	protected  Boolean doInBackground() {
		boolean result = true;
		
		
//		logger = new Logger(dataset.getDebugFile(), "TubulinTailDetector");
		fileLogger.log(Level.INFO, "Beginning tail detection");

		try{
			int progress = 0;
			for(ICell c : getDataset().getCollection().getCells()){

				IMutableCell cell = (IMutableCell) c;
				
				Nucleus n = c.getNucleus();
				fileLogger.log(Level.INFO, "Looking for tails associated with nucleus "+n.getSourceFileName()+"-"+n.getNucleusNumber());
				
				// get the image in the folder with the same name as the
				// nucleus source image
				File imageFile = new File(folder + File.separator + n.getSourceFileName());
				fileLogger.log(Level.FINE, "Tail in: "+imageFile.getAbsolutePath());
//				SpermTail tail = null;
				
				TailDetector finder = new TailDetector(getDataset().getAnalysisOptions().getDetectionOptions(IAnalysisOptions.SPERM_TAIL).getCannyOptions(), 
						channel);
				
				// attempt to detect the tails in the image
				try{
					List<SpermTail> tails = finder.detectTail(imageFile, n);
					
					for(SpermTail tail : tails){
						cell.addFlagellum(tail);
					}
					
				} catch(Exception e){
					fileLogger.log(Level.SEVERE, "Error detecting tail", e);
				}
				
				progress++;
				publish(progress);
			}
		} catch (Exception e){
			fileLogger.log(Level.SEVERE, "Error in tubulin tail detection", e);
			return false;
		}

		return result;
	}
//
//	@Override
//	public void done() {
//
//		try {
//			if(this.get()){
//				firePropertyChange("Finished", getProgress(), Constants.Progress.FINISHED.code());
//			} else {
//				firePropertyChange("Error", getProgress(), Constants.Progress.ERROR.code());
//			}
//		} catch (InterruptedException e) {
//			logger.log(Level.SEVERE, "Error in tubulin tail detection", e);
//		} catch (ExecutionException e) {
//			logger.log(Level.SEVERE, "Error in tubulin tail detection", e);
//		}
//
//	} 	

}
