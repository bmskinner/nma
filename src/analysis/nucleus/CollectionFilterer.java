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
package analysis.nucleus;

import java.util.logging.Level;
import java.util.logging.Logger;

import stats.NucleusStatistic;
import components.Cell;
import components.CellCollection;
import components.generic.MeasurementScale;
import components.nuclei.Nucleus;

public class CollectionFilterer {

	private static Logger logger;
	
	public static final int FAILURE_THRESHOLD = 1;
	public static final int FAILURE_FERET     = 2;
	public static final int FAILURE_ARRAY     = 4;
	public static final int FAILURE_AREA      = 8;
	public static final int FAILURE_PERIM     = 16;
	public static final int FAILURE_OTHER     = 32;
	public static final int FAILURE_SIGNALS   = 64;
	
	private static double maxDifferenceFromMedian = 1.6; // used to filter the nuclei, and remove those too small, large or irregular to be real
	private static double maxWibblinessFromMedian = 1.4; // filter for the irregular borders more stringently


	public static boolean run(CellCollection collection, CellCollection failCollection, Logger fileLogger){

		logger = fileLogger;
		
		try{

			logger.log(Level.FINE, "Filtering collection...");
			refilterNuclei(collection, failCollection);
			logger.log(Level.FINE, "Filtering complete");
		} catch(Exception e){

			logger.log(Level.SEVERE, "Error filtering", e);
			return false;
		}  finally {
//			handler.close();
		}
		return true;
	}
	
	private static void refilterNuclei(CellCollection collection, CellCollection failCollection) throws Exception{

	    double medianArea = collection.getMedianStatistic(NucleusStatistic.AREA, MeasurementScale.PIXELS);
	    double medianPerimeter = collection.getMedianStatistic(NucleusStatistic.PERIMETER, MeasurementScale.PIXELS);
	    double medianPathLength = collection.getMedianPathLength();
	    double medianArrayLength = collection.getMedianArrayLength();
	    double medianFeretLength = collection.getMedianStatistic(NucleusStatistic.MAX_FERET, MeasurementScale.PIXELS);

	    int beforeSize = collection.getNucleusCount();

	    double maxPathLength = medianPathLength * CollectionFilterer.maxWibblinessFromMedian;
	    double minArea = medianArea / CollectionFilterer.maxDifferenceFromMedian;
	    double maxArea = medianArea * CollectionFilterer.maxDifferenceFromMedian;
	    double maxPerim = medianPerimeter * CollectionFilterer.maxDifferenceFromMedian;
	    double minPerim = medianPerimeter / CollectionFilterer.maxDifferenceFromMedian;
	    double minFeret = medianFeretLength / CollectionFilterer.maxDifferenceFromMedian;

	    int area = 0;
	    int perim = 0;
	    int pathlength = 0;
	    int arraylength = 0;
	    int feretlength = 0;

	    logger.log(Level.FINE, "Prefiltered values found");

	    for(Cell c : collection.getCells()){

	      Nucleus n = c.getNucleus();
	      
	      if(n.getStatistic(NucleusStatistic.AREA) > maxArea || n.getStatistic(NucleusStatistic.AREA) < minArea ){
	        n.updateFailureCode(FAILURE_AREA);
	        area++;
	      }
	      if(n.getStatistic(NucleusStatistic.PERIMETER) > maxPerim || n.getStatistic(NucleusStatistic.PERIMETER) < minPerim ){
	        n.updateFailureCode(FAILURE_PERIM);
	        perim++;
	      }
	      if(n.getPathLength() > maxPathLength){ // only filter for values too big here - wibbliness detector
	        n.updateFailureCode(FAILURE_THRESHOLD);
	        pathlength++;
	      }
	      if(n.getBorderLength() > medianArrayLength * maxDifferenceFromMedian || n.getBorderLength() < medianArrayLength / maxDifferenceFromMedian ){
	        n.updateFailureCode(FAILURE_ARRAY);
	         arraylength++;
	      }

	      if(n.getStatistic(NucleusStatistic.MAX_FERET) < minFeret){
	        n.updateFailureCode(FAILURE_FERET);
	        feretlength++;
	      }
	      
	      if(n.getFailureCode() > 0){
	        failCollection.addCell(c);
	      }
	    }

	    for( Cell f : failCollection.getCells()){ // should be safer than the i-- above
	    	collection.removeCell(f);
	    }
	      

	    medianArea = collection.getMedianStatistic(NucleusStatistic.AREA, MeasurementScale.PIXELS);
	    medianPerimeter = collection.getMedianStatistic(NucleusStatistic.PERIMETER, MeasurementScale.PIXELS);
	    medianPathLength = collection.getMedianPathLength();
	    medianArrayLength = collection.getMedianArrayLength();
	    medianFeretLength = collection.getMedianStatistic(NucleusStatistic.MAX_FERET, MeasurementScale.PIXELS);

	    int afterSize = collection.getNucleusCount();
	    int removed = beforeSize - afterSize;

//	    logger.log("Postfiltered values found");
//	    exportFilterStats(collection);
//	    logger.log("Removed due to size or length issues: "+removed+" nuclei");
//	    logger.log("Due to area outside bounds "+(int)minArea+"-"+(int)maxArea+": "+area+" nuclei");
//	    logger.log("Due to perimeter outside bounds "+(int)minPerim+"-"+(int)maxPerim+": "+perim+" nuclei");
//	    logger.log("Due to wibbliness >"+(int)maxPathLength+" : "+(int)pathlength+" nuclei");
//	    logger.log("Due to array length: "+arraylength+" nuclei");
//	    logger.log("Due to feret length: "+feretlength+" nuclei");
	    logger.log(Level.INFO, "Remaining: "+collection.getNucleusCount()+" nuclei");
	    
	  }
	
	private static void exportFilterStats(CellCollection collection) throws Exception{

		double medianArea = collection.getMedianStatistic(NucleusStatistic.AREA, MeasurementScale.PIXELS);
	    double medianPerimeter = collection.getMedianStatistic(NucleusStatistic.PERIMETER, MeasurementScale.PIXELS);
	    double medianPathLength = collection.getMedianPathLength();
	    double medianArrayLength = collection.getMedianArrayLength();
	    double medianFeretLength = collection.getMedianStatistic(NucleusStatistic.MAX_FERET, MeasurementScale.PIXELS);	    
	  }
}
